package com.cryptroot.substrate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cryptroot.core.world.WorldEntity;
import com.cryptroot.substrate.component.Age;
import com.cryptroot.substrate.component.Fertility;
import com.cryptroot.substrate.component.Genome;
import com.cryptroot.substrate.component.Lineage;
import com.cryptroot.substrate.template.TemplateLoader;
import com.cryptroot.substrate.tick.SimulationLoop;
import com.cryptroot.substrate.tick.Simulations;
import com.cryptroot.substrate.world.SimWorld;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Two mature, cooldown-ready, mutually-nearby entities produce offspring with a combined
 * {@link Genome} and a {@link Lineage} tracing both parents — no reproduction event is
 * hand-scripted, it falls out of CourtshipSystem + ReproductionSystem doing their jobs.
 */
class ReproductionSystemTest {

  @Test
  void matureReadyNeighborsProduceTracedOffspring() {
    SimWorld w = TestWorlds.geneticsWorld(9, 9, 20L);
    TemplateLoader loader = new TemplateLoader(w.materials());
    var critter = loader.loadFromResource("/data/templates/critter.json");

    WorldEntity a = w.spawn(critter.instantiate(4, 4), critter.name());
    WorldEntity b = w.spawn(critter.instantiate(5, 4), critter.name());
    // Skip straight to maturity/readiness so pairing can happen on tick 1.
    a.get(Age.class).orElseThrow().age = 100;
    b.get(Age.class).orElseThrow().age = 100;

    long aId = w.idOf(a);
    long bId = w.idOf(b);

    SimulationLoop loop = Simulations.standard(w);
    // Reproduction queues offspring for addition; it's flushed at the start of the next tick.
    loop.runTicks(2);

    Optional<WorldEntity> child =
        w.world().entities().stream()
            .filter(e -> e.has(Lineage.class))
            .filter(
                e -> {
                  Lineage l = e.get(Lineage.class).orElseThrow();
                  return (l.parentAId == aId || l.parentAId == bId)
                      && (l.parentBId == aId || l.parentBId == bId);
                })
            .findFirst();

    assertTrue(child.isPresent(), "a mature, ready, adjacent pair should produce offspring");
    assertEquals(1, child.get().get(Lineage.class).orElseThrow().generation);
    assertTrue(child.get().has(Genome.class), "offspring must carry a combined genome");

    // Both parents' cooldown should now be reset (not ready for a second birth immediately).
    assertTrue(a.get(Fertility.class).orElseThrow().cooldownRemaining > 0);
    assertTrue(b.get(Fertility.class).orElseThrow().cooldownRemaining > 0);
  }

  @Test
  void immatureEntitiesNeverReproduce() {
    SimWorld w = TestWorlds.geneticsWorld(9, 9, 21L);
    TemplateLoader loader = new TemplateLoader(w.materials());
    var critter = loader.loadFromResource("/data/templates/critter.json");
    w.spawn(critter.instantiate(4, 4), critter.name());
    w.spawn(critter.instantiate(5, 4), critter.name());
    // Ages default to 0 < maturityAge (60), so no pairing should happen.

    SimulationLoop loop = Simulations.standard(w);
    loop.runTicks(20);

    List<WorldEntity> births = w.world().entities().stream().filter(e -> e.has(Lineage.class)).toList();
    assertTrue(births.isEmpty(), "immature entities must not reproduce");
  }
}
