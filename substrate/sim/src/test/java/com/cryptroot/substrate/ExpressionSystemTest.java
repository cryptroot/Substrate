package com.cryptroot.substrate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cryptroot.core.world.WorldEntity;
import com.cryptroot.substrate.component.FearProne;
import com.cryptroot.substrate.component.Genome;
import com.cryptroot.substrate.system.ExpressionSystem;
import com.cryptroot.substrate.template.TemplateLoader;
import com.cryptroot.substrate.tick.Phase;
import com.cryptroot.substrate.tick.SimulationLoop;
import com.cryptroot.substrate.world.SimWorld;
import org.junit.jupiter.api.Test;

/**
 * Smallest possible slice proving genotype→phenotype expression: one gene (BLEND) driving an
 * existing component (FearProne.fearThreshold), then a second gene/rule (THRESHOLD) proving the
 * same generic system handles both via data alone.
 */
class ExpressionSystemTest {

  @Test
  void blendGeneAveragesTwoAllelesIntoFearThreshold() {
    SimWorld w = TestWorlds.geneticsWorld(6, 6, 10L);
    TemplateLoader loader = new TemplateLoader(w.materials());
    var critter = loader.loadFromResource("/data/templates/critter.json");
    WorldEntity e = w.spawn(critter.instantiate(2, 2), critter.name());
    e.get(Genome.class).orElseThrow().set("fear_sensitivity", 4f, 10f);

    new SimulationLoop(w).register(Phase.GENETICS, new ExpressionSystem()).runTicks(1);

    assertEquals(7f, e.get(FearProne.class).orElseThrow().fearThreshold, 0.001f);
  }

  @Test
  void thresholdGeneStaysSilentUntilCombinedAllelesClearCutoff() {
    SimWorld w = TestWorlds.geneticsWorld(6, 6, 11L);
    TemplateLoader loader = new TemplateLoader(w.materials());
    var critter = loader.loadFromResource("/data/templates/critter.json");

    // Below cutoff (0.6): both alleles low -> carried, never expressed as a feared trait.
    WorldEntity carrier = w.spawn(critter.instantiate(2, 2), critter.name());
    carrier.get(Genome.class).orElseThrow().set("fear_of_water", 0.2f, 0.2f);

    // Above cutoff: two carriers' alleles finally combine past 0.6 -> expressed.
    WorldEntity expresser = w.spawn(critter.instantiate(3, 3), critter.name());
    expresser.get(Genome.class).orElseThrow().set("fear_of_water", 0.9f, 0.9f);

    new SimulationLoop(w).register(Phase.GENETICS, new ExpressionSystem()).runTicks(1);

    assertTrue(
        !carrier.get(FearProne.class).orElseThrow().fearedTraits.contains("WATER"),
        "below-threshold gene must stay carried, not expressed");
    assertTrue(
        expresser.get(FearProne.class).orElseThrow().fearedTraits.contains("WATER"),
        "above-threshold gene must be expressed as the feared trait");
  }

  @Test
  void unknownGeneIsSkippedNotThrown() {
    SimWorld w = TestWorlds.geneticsWorld(6, 6, 12L);
    TemplateLoader loader = new TemplateLoader(w.materials());
    var critter = loader.loadFromResource("/data/templates/critter.json");
    WorldEntity e = w.spawn(critter.instantiate(2, 2), critter.name());
    e.get(Genome.class).orElseThrow().set("nonexistent_gene", 1f, 1f);

    // Must not throw; unknown gene definitions fail soft and are logged.
    new SimulationLoop(w).register(Phase.GENETICS, new ExpressionSystem()).runTicks(1);
  }
}
