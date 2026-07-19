package com.cryptroot.substrate.system;

import com.cryptroot.core.world.WorldEntity;
import com.cryptroot.substrate.component.Genome;
import com.cryptroot.substrate.genetics.ExpressionTargets;
import com.cryptroot.substrate.genetics.GeneDefinition;
import com.cryptroot.substrate.genetics.GeneRule;
import com.cryptroot.substrate.tick.SimSystem;
import com.cryptroot.substrate.world.SimWorld;
import java.util.Map;
import java.util.Optional;

/**
 * Reads {@link Genome}, reads each carried gene's data-defined rule, and writes the resulting
 * phenotype value into whatever component {@link ExpressionTargets} says that gene drives. This
 * system doesn't know or care what species it's running on, or what any given gene "means" —
 * same blindness {@code IngestionSystem} has toward what's being swallowed. "Both", "the mix", and
 * "neither" all fall out of which {@link GeneRule} a gene's data uses, never out of a branch here.
 */
public final class ExpressionSystem implements SimSystem {

  private final ExpressionTargets targets;

  public ExpressionSystem() {
    this(ExpressionTargets.defaults());
  }

  public ExpressionSystem(ExpressionTargets targets) {
    this.targets = targets;
  }

  @Override
  public void tick(SimWorld w, long tick) {
    for (WorldEntity e : w.entitiesWith(Genome.class)) {
      Genome genome = e.get(Genome.class).orElseThrow();
      for (Map.Entry<String, float[]> entry : genome.genes.entrySet()) {
        String geneId = entry.getKey();
        float[] alleles = entry.getValue();
        GeneDefinition def = w.genes().get(geneId);
        if (def == null) {
          w.log()
              .record(tick, "GENETICS", systemName(), w.subjectOf(e), "gene:" + geneId, "-",
                  "skipped(unknown gene definition)");
          continue;
        }
        float phenotype = resolve(def.rule(), alleles, def.threshold());
        ExpressionTargets.Target target = targets.get(def.target());
        if (target == null) {
          w.log()
              .record(tick, "GENETICS", systemName(), w.subjectOf(e),
                  "gene:" + geneId + "->" + def.target(), "-", "skipped(unknown target)");
          continue;
        }
        Optional<String[]> change = target.apply(e, phenotype, def.param());
        change.ifPresent(
            before_after ->
                w.log()
                    .record(
                        tick,
                        "GENETICS",
                        systemName(),
                        w.subjectOf(e),
                        def.target() + (def.param() != null ? "(" + def.param() + ")" : ""),
                        before_after[0],
                        before_after[1]));
      }
    }
  }

  private static float resolve(GeneRule rule, float[] alleles, float threshold) {
    float a = alleles.length > 0 ? alleles[0] : 0f;
    float b = alleles.length > 1 ? alleles[1] : a;
    return switch (rule) {
      case BLEND -> (a + b) / 2f;
      case MAX -> Math.max(a, b);
      case THRESHOLD -> {
        float combined = (a + b) / 2f;
        yield combined >= threshold ? combined : 0f;
      }
    };
  }
}
