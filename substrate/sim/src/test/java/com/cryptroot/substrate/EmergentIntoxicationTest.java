package com.cryptroot.substrate;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cryptroot.core.world.WorldEntity;
import com.cryptroot.substrate.component.Impairment;
import com.cryptroot.substrate.template.TemplateLoader;
import com.cryptroot.substrate.tick.SimulationLoop;
import com.cryptroot.substrate.tick.Simulations;
import com.cryptroot.substrate.world.SimWorld;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * THE thesis test. A critter walks through spilled ethanol, grooms it off its fur, swallows it,
 * and gets drunk — with zero species-specific code anywhere in the systems package (enforced
 * separately by NoIdentityGateTest). Nobody authored "the critter gets drunk"; five systems did
 * their jobs in sequence, and the causality log can prove it.
 */
class EmergentIntoxicationTest {

  @Test
  void critterGetsDrunkFromGroomingSpilledEthanol() {
    SimWorld w = TestWorlds.world(9, 9, 99L);
    int ethanol = w.materials().idOf("ethanol");
    // Spill a wide, deep puddle so the wandering critter can't stumble out of the scenario.
    for (int c = 0; c < 9; c++) {
      for (int r = 0; r < 9; r++) {
        w.tiles().setLiquid(c, r, ethanol, 2f);
      }
    }

    TemplateLoader loader = new TemplateLoader(w.materials());
    var critterTemplate = loader.loadFromResource("/data/templates/critter.json");
    WorldEntity critter = w.spawn(critterTemplate.instantiate(4, 4), critterTemplate.name());
    String subject = w.subjectOf(critter);

    SimulationLoop loop = Simulations.standard(w);
    loop.runTicks(120);

    float impairment = critter.get(Impairment.class).orElseThrow().level;
    assertTrue(impairment > 0.1f, "critter should be measurably drunk, level=" + impairment);

    // The causality log must be able to reconstruct the whole chain:
    // coated by fluid -> groomed to mouth -> ingested -> impaired.
    List<String> systemsThatTouchedIt =
        w.log().bySubject(subject).stream().map(r -> r.system()).distinct().toList();
    assertTrue(systemsThatTouchedIt.contains("FluidFlowSystem"), "coating step missing");
    assertTrue(systemsThatTouchedIt.contains("GroomingSystem"), "grooming step missing");
    assertTrue(systemsThatTouchedIt.contains("IngestionSystem"), "ingestion step missing");
    assertTrue(systemsThatTouchedIt.contains("IntoxicationSystem"), "intoxication step missing");
  }
}
