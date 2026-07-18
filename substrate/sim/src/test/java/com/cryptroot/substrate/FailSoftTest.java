package com.cryptroot.substrate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cryptroot.substrate.tick.Phase;
import com.cryptroot.substrate.tick.SimSystem;
import com.cryptroot.substrate.tick.SimulationLoop;
import com.cryptroot.substrate.world.SimWorld;
import org.junit.jupiter.api.Test;

/** A throwing system must never halt the simulation — it gets logged and skipped. */
class FailSoftTest {

  @Test
  void throwingSystemIsLoggedAndSimulationContinues() {
    SimWorld w = TestWorlds.world(4, 4, 1L);
    SimSystem bomb =
        new SimSystem() {
          @Override
          public void tick(SimWorld world, long tick) {
            throw new IllegalStateException("kaboom");
          }
        };
    SimulationLoop loop = new SimulationLoop(w).register(Phase.ENVIRONMENT, bomb);

    loop.runTicks(5);

    assertEquals(5, loop.currentTick(), "loop must keep ticking past the failure");
    var errors = w.log().bySubject("loop");
    assertEquals(5, errors.size(), "each failure must be recorded");
    assertTrue(errors.get(0).after().contains("kaboom"));
  }
}
