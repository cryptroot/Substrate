package com.cryptroot.substrate.tick;

import com.cryptroot.substrate.system.CombustionSystem;
import com.cryptroot.substrate.system.FearSystem;
import com.cryptroot.substrate.system.FluidFlowSystem;
import com.cryptroot.substrate.system.GroomingSystem;
import com.cryptroot.substrate.system.HealthSystem;
import com.cryptroot.substrate.system.IngestionSystem;
import com.cryptroot.substrate.system.IntoxicationSystem;
import com.cryptroot.substrate.system.MovementSystem;
import com.cryptroot.substrate.system.ThermalDiffusionSystem;
import com.cryptroot.substrate.world.SimWorld;

/** Wires the standard v1 system roster into a loop in the documented phase order. */
public final class Simulations {

  private Simulations() {}

  public static SimulationLoop standard(SimWorld world) {
    return new SimulationLoop(world)
        .register(Phase.ENVIRONMENT, new ThermalDiffusionSystem())
        .register(Phase.ENVIRONMENT, new FluidFlowSystem())
        .register(Phase.ENVIRONMENT, new CombustionSystem())
        .register(Phase.CONTACT, new GroomingSystem())
        .register(Phase.INGESTION, new IngestionSystem())
        .register(Phase.METABOLISM, new IntoxicationSystem())
        .register(Phase.METABOLISM, new HealthSystem())
        .register(Phase.COGNITION, new FearSystem())
        .register(Phase.MOVEMENT, new MovementSystem());
  }
}
