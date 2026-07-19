package com.cryptroot.substrate.system;

import com.cryptroot.core.world.WorldEntity;
import com.cryptroot.substrate.component.Age;
import com.cryptroot.substrate.component.Fertility;
import com.cryptroot.substrate.component.Genome;
import com.cryptroot.substrate.component.GridPosition;
import com.cryptroot.substrate.component.Lineage;
import com.cryptroot.substrate.component.ReproduceIntent;
import com.cryptroot.substrate.template.TemplateLoader;
import com.cryptroot.substrate.tick.SimSystem;
import com.cryptroot.substrate.world.SimWorld;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SplittableRandom;

/**
 * Resolves mutual {@link ReproduceIntent} pairs set this tick by {@code CourtshipSystem}, the
 * same same-tick set→resolve pattern {@code MoveIntent}/{@code MovementSystem} already use.
 *
 * <p>No species/breeding-group compatibility gate — any two mutually-intending, ready, mature
 * entities pair: this is a  deliberate design choice, not an oversight). The offspring is built generically by
 * re-instantiating parent A's own component bundle (arbitrary but deterministic tie-break, itself
 * an honest cross-template collision when parents differ), then replacing its {@link Genome} with
 * one combined from both parents' alleles, and attaching fresh {@link Age}/{@link Lineage}.
 */
public final class ReproductionSystem implements SimSystem {

  @Override
  public void tick(SimWorld w, long tick) {
    var candidates = w.entitiesWith(ReproduceIntent.class, Fertility.class, Genome.class, GridPosition.class);
    SplittableRandom rng = w.random(tick, systemName());
    float mutationRate = w.config().mutationRate();
    float mutationJitter = w.config().mutationJitter();

    Set<Long> spawnedThisTick = new HashSet<>();
    for (WorldEntity a : candidates) {
      long aId = w.idOf(a);
      if (spawnedThisTick.contains(aId)) continue;
      ReproduceIntent aIntent = a.get(ReproduceIntent.class).orElseThrow();
      if (aIntent.partnerId == ReproduceIntent.NONE) continue;

      WorldEntity b = find(candidates, w, aIntent.partnerId);
      if (b == null) continue;
      long bId = w.idOf(b);
      ReproduceIntent bIntent = b.get(ReproduceIntent.class).orElseThrow();
      if (bIntent.partnerId != aId) continue; // not mutual this tick

      Fertility aFert = a.get(Fertility.class).orElseThrow();
      Fertility bFert = b.get(Fertility.class).orElseThrow();
      if (!aFert.ready() || !bFert.ready()) continue;

      // Process each mutual pair exactly once: dedupe by numeric id ordering, not by any
      // species/type comparison.
      if (aId > bId) continue;

      WorldEntity child = buildOffspring(w, tick, a, b, rng, mutationRate, mutationJitter);
      if (child == null) continue; // fail soft: parent A's bundle isn't registered

      aFert.cooldownRemaining = aFert.cooldownTicks;
      bFert.cooldownRemaining = bFert.cooldownTicks;
      spawnedThisTick.add(aId);
      spawnedThisTick.add(bId);

      w.log()
          .record(
              tick, "LIFECYCLE", systemName(), w.subjectOf(child), "existence", "-",
              "born(parents=" + w.subjectOf(a) + "," + w.subjectOf(b) + ")");
    }
  }

  private static WorldEntity find(Iterable<WorldEntity> pool, SimWorld w, long id) {
    for (WorldEntity e : pool) {
      if (w.idOf(e) == id) return e;
    }
    return null;
  }

  private WorldEntity buildOffspring(
      SimWorld w, long tick, WorldEntity a, WorldEntity b, SplittableRandom rng,
      float mutationRate, float mutationJitter) {
    String bodyPlan = w.originTemplateOf(a);
    if (bodyPlan == null) return null;
    TemplateLoader.Template template = w.templates().get(bodyPlan);
    if (template == null) {
      w.log()
          .record(tick, "LIFECYCLE", systemName(), w.subjectOf(a), "reproduction", "-",
              "skipped(no template registered for '" + bodyPlan + "')");
      return null;
    }

    GridPosition posA = a.get(GridPosition.class).orElseThrow();
    GridPosition posB = b.get(GridPosition.class).orElseThrow();
    int col = Math.round((posA.col + posB.col) / 2f);
    int row = Math.round((posA.row + posB.row) / 2f);

    WorldEntity child = template.instantiate(col, row);
    child.with(Genome.class, combine(a, b, rng, mutationRate, mutationJitter));

    int generation = Math.max(generationOf(a), generationOf(b)) + 1;
    child.with(Lineage.class, new Lineage(w.idOf(a), w.idOf(b), generation));
    child.get(Age.class).ifPresent(age -> age.age = 0);

    w.queueSpawn(child, bodyPlan);
    return child;
  }

  private static int generationOf(WorldEntity e) {
    return e.get(Lineage.class).map(l -> l.generation).orElse(0);
  }

  private static Genome combine(
      WorldEntity a, WorldEntity b, SplittableRandom rng, float mutationRate,
      float mutationJitter) {
    Genome ga = a.get(Genome.class).orElseThrow();
    Genome gb = b.get(Genome.class).orElseThrow();
    Genome child = new Genome();
    Map<String, float[]> merged = new HashMap<>();
    merged.putAll(ga.genes);
    merged.putAll(gb.genes);
    for (String geneId : merged.keySet()) {
      float[] alleleFromA = pick(ga.genes.get(geneId), rng);
      float[] alleleFromB = pick(gb.genes.get(geneId), rng);
      float a1 = mutate(alleleFromA[rng.nextInt(2)], rng, mutationRate, mutationJitter);
      float a2 = mutate(alleleFromB[rng.nextInt(2)], rng, mutationRate, mutationJitter);
      child.set(geneId, a1, a2);
    }
    return child;
  }

  private static float[] pick(float[] alleles, SplittableRandom rng) {
    return alleles != null ? alleles : new float[] {0f, 0f};
  }

  private static float mutate(float allele, SplittableRandom rng, float rate, float jitter) {
    if (rng.nextDouble() >= rate) return allele;
    return allele + (float) (rng.nextDouble() * 2 - 1) * jitter;
  }
}
