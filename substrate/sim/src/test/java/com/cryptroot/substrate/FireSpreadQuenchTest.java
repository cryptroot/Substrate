package com.cryptroot.substrate;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cryptroot.substrate.tick.SimulationLoop;
import com.cryptroot.substrate.tick.Simulations;
import com.cryptroot.substrate.world.SimWorld;
import org.junit.jupiter.api.Test;

/**
 * Fire and water behaving honestly against each other, no creatures.
 */
class FireSpreadQuenchTest {

  @Test
  void fireSpreadsAcrossGrass() {
    SimWorld w = TestWorlds.world(12, 12, 7L);
    int grass = w.materials().idOf("grass");
    for (int c = 2; c <= 9; c++) w.tiles().setMaterialId(c, 5, grass);
    w.tiles().seedFuel(w.materials());
    // Spark one end well above grass's ignition point (150, from materials.json).
    w.tiles().setTemperature(2, 5, 600);

    SimulationLoop loop = Simulations.standard(w);
    loop.runTicks(80);

    // Fuel at the far end must have been consumed — fire travelled, nobody scripted spread.
    assertTrue(
        w.tiles().fuel(9, 5) < w.materials().get(grass).fuel(),
        "fire should have reached and burned the far grass tile");
    assertTrue(
        w.log().bySubject("tile:9,5").stream().anyMatch(r -> r.system().equals("CombustionSystem")),
        "causality log must show combustion at the far tile");
  }

  @Test
  void waterQuenchesBurningTile() {
    SimWorld w = TestWorlds.world(8, 8, 7L);
    int grass = w.materials().idOf("grass");
    w.tiles().setMaterialId(4, 4, grass);
    w.tiles().seedFuel(w.materials());
    w.tiles().setTemperature(4, 4, 400);
    // Flood it.
    w.tiles().setLiquid(4, 4, w.materials().idOf("water"), 3f);

    SimulationLoop loop = Simulations.standard(w);
    loop.runTicks(40);

    assertTrue(
        w.tiles().temperature(4, 4) < 150,
        "flooded tile should cool below grass ignition, was " + w.tiles().temperature(4, 4));
    assertTrue(w.tiles().fuel(4, 4) > 0, "quenched tile should retain fuel");
  }
}
