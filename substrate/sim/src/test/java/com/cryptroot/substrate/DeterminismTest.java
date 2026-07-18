package com.cryptroot.substrate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import com.cryptroot.substrate.log.ChangeRecord;
import com.cryptroot.substrate.template.TemplateLoader;
import com.cryptroot.substrate.tick.SimulationLoop;
import com.cryptroot.substrate.tick.Simulations;
import com.cryptroot.substrate.world.SimWorld;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Same seed → byte-identical causality log. Different seed → (almost surely) different log. */
class DeterminismTest {

  private List<ChangeRecord> run(long seed) {
    SimWorld w = TestWorlds.world(16, 16, seed);
    TemplateLoader loader = new TemplateLoader(w.materials());
    var critter = loader.loadFromResource("/data/templates/critter.json");
    var torch = loader.loadFromResource("/data/templates/torch.json");
    w.spawn(critter.instantiate(3, 3), critter.name());
    w.spawn(critter.instantiate(12, 12), critter.name());
    w.spawn(torch.instantiate(8, 8), torch.name());
    w.tiles().setLiquid(4, 4, w.materials().idOf("ethanol"), 0.6f);
    SimulationLoop loop = Simulations.standard(w);
    loop.runTicks(60);
    return w.log().all();
  }

  @Test
  void sameSeedProducesIdenticalLogs() {
    assertEquals(run(42L), run(42L));
  }

  @Test
  void differentSeedsDiverge() {
    assertNotEquals(run(42L), run(43L));
  }
}
