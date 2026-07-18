package com.cryptroot.substrate.tick;

import com.cryptroot.substrate.world.SimWorld;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Fixed-phase headless simulation loop. Each tick: flush deferred entity adds/removes, then run
 * every registered system in explicit phase order.
 *
 * <p>Fail soft: a system that throws is logged to the causality log and skipped for that tick —
 * never allowed to halt the simulation. A crash teaches nothing; a log line teaches everything.
 */
public final class SimulationLoop {

  private final SimWorld world;
  private final Map<Phase, List<SimSystem>> systems = new EnumMap<>(Phase.class);
  private long tick = 0;

  public SimulationLoop(SimWorld world) {
    this.world = Objects.requireNonNull(world);
    for (Phase p : Phase.values()) systems.put(p, new ArrayList<>());
  }

  public SimulationLoop register(Phase phase, SimSystem system) {
    Objects.requireNonNull(phase);
    Objects.requireNonNull(system);
    systems.get(phase).add(system);
    return this;
  }

  public long currentTick() {
    return tick;
  }

  public SimWorld world() {
    return world;
  }

  public void runTicks(int count) {
    for (int i = 0; i < count; i++) tick();
  }

  public void tick() {
    world.world().flushAdditions();
    world.world().flushRemovals();
    for (Phase phase : Phase.values()) {
      for (SimSystem system : systems.get(phase)) {
        try {
          system.tick(world, tick);
        } catch (RuntimeException e) {
          // Fail soft: record and continue. The simulation must never halt on a weird
          // component combination — see CLAUDE.md rule 8.
          world
              .log()
              .record(
                  tick,
                  phase.name(),
                  system.systemName(),
                  "loop",
                  "system-error",
                  "running",
                  e.getClass().getSimpleName() + ": " + e.getMessage());
        }
      }
    }
    tick++;
  }
}
