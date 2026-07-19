package com.cryptroot.substrate;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cryptroot.core.world.WorldEntity;
import com.cryptroot.substrate.component.Age;
import com.cryptroot.substrate.component.Fertility;
import com.cryptroot.substrate.component.Genome;
import com.cryptroot.substrate.component.GridPosition;
import com.cryptroot.substrate.component.ReproduceIntent;
import com.cryptroot.substrate.template.TemplateLoader;
import com.cryptroot.substrate.tick.SimulationLoop;
import com.cryptroot.substrate.tick.Simulations;
import com.cryptroot.substrate.world.SimWorld;
import java.util.SplittableRandom;
import org.junit.jupiter.api.Test;

/**
 * Playtest-as-test: a starting population left to age, mate, and die over a long run should
 * settle into a bounded size — neither collapsing to zero nor growing without limit. If this
 * starts failing, the fix is tuning lifespan/cooldown/mutation data (see data/*.json), not adding
 * a population cap gate.
 */
class PopulationStabilityTest {

  @Test
  void populationStaysBoundedOverALongRun() {
    SimWorld w = TestWorlds.geneticsWorld(20, 20, 77L);
    TemplateLoader loader = new TemplateLoader(w.materials());
    var critter = loader.loadFromResource("/data/templates/critter.json");

    SplittableRandom seedRng = new SplittableRandom(77L);
    for (int i = 0; i < 14; i++) {
      WorldEntity e =
          w.spawn(critter.instantiate(seedRng.nextInt(20), seedRng.nextInt(20)), critter.name());
      // Stagger starting ages so the founder cohort doesn't all die of old age in sync.
      e.get(Age.class).orElseThrow().age = 40 + seedRng.nextInt(300);
    }

    SimulationLoop loop = Simulations.standard(w);
    loop.runTicks(2000);

    long alive =
        w.world().entities().stream()
            .filter(e -> e.has(Genome.class) && e.has(ReproduceIntent.class))
            .count();

    assertTrue(alive > 0, "population must not collapse to extinction");
    assertTrue(alive < 500, "population must not grow unbounded (lifespan/cooldown damping term)");
  }
}
