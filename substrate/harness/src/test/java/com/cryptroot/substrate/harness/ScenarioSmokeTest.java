package com.cryptroot.substrate.harness;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cryptroot.substrate.tick.SimulationLoop;
import com.cryptroot.substrate.world.SimWorld;
import org.junit.jupiter.api.Test;

/** End-to-end: canned scenarios build, run, render, and leave a queryable causality trail. */
class ScenarioSmokeTest {

  @Test
  void boozeSpillScenarioProducesAnExplainableDrunk() {
    Scenario scenario = Scenario.fromResource("/scenarios/booze-spill.json");
    SimulationLoop loop = scenario.build();
    loop.runTicks(scenario.ticks());
    SimWorld w = loop.world();

    assertTrue(
        w.log().all().stream()
            .anyMatch(
                r ->
                    r.system().equals("IntoxicationSystem")
                        && r.subject().startsWith("entity:")),
        "scenario should end with an intoxication record to explain");

    String frame = AsciiRenderer.render(w);
    assertTrue(frame.contains("@"), "entity should be visible in the ASCII frame");
    assertFalse(frame.isBlank());
  }

  @Test
  void grassfireScenarioBurnsAndRuns() {
    Scenario scenario = Scenario.fromResource("/scenarios/grassfire.json");
    SimulationLoop loop = scenario.build();
    loop.runTicks(scenario.ticks());
    assertTrue(
        loop.world().log().bySystem("CombustionSystem").size() > 10,
        "grassfire should produce combustion records");
  }
}
