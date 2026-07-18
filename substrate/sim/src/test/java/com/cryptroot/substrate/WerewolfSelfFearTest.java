package com.cryptroot.substrate;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cryptroot.core.world.WorldEntity;
import com.cryptroot.substrate.component.FearProne;
import com.cryptroot.substrate.template.TemplateLoader;
import com.cryptroot.substrate.tick.SimulationLoop;
import com.cryptroot.substrate.tick.Simulations;
import com.cryptroot.substrate.world.SimWorld;
import org.junit.jupiter.api.Test;

/**
 * README roadmap step 3: a second species added with a data file and ZERO new systems. The
 * werewolf fears canines and carries a CANINE trait — the fear system deliberately doesn't
 * exclude self-scans, so it frightens itself. A bug we declined to prevent.
 */
class WerewolfSelfFearTest {

  @Test
  void werewolfFrightensItselfWithItsOwnCanineTrait() {
    SimWorld w = TestWorlds.world(16, 16, 5L);
    TemplateLoader loader = new TemplateLoader(w.materials());
    var template = loader.loadFromResource("/data/templates/werewolf.json");
    // Alone in an empty world: the only canine within fear radius is itself.
    WorldEntity werewolf = w.spawn(template.instantiate(8, 8), template.name());
    String subject = w.subjectOf(werewolf);

    SimulationLoop loop = Simulations.standard(w);
    loop.runTicks(30);

    FearProne fear = werewolf.get(FearProne.class).orElseThrow();
    assertTrue(
        fear.currentFear >= fear.fearThreshold,
        "werewolf should have scared itself past its threshold, fear=" + fear.currentFear);
    assertTrue(
        w.log().bySubject(subject).stream()
            .anyMatch(r -> r.system().equals("FearSystem") && r.field().contains("MoveIntent")),
        "the panic should be visible in the causality log as a flee intent");
  }
}
