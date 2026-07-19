package com.cryptroot.substrate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cryptroot.core.world.WorldEntity;
import com.cryptroot.substrate.component.Age;
import com.cryptroot.substrate.template.TemplateLoader;
import com.cryptroot.substrate.tick.Phase;
import com.cryptroot.substrate.tick.SimulationLoop;
import com.cryptroot.substrate.tick.Simulations;
import com.cryptroot.substrate.world.SimWorld;
import org.junit.jupiter.api.Test;

/** A population ages and dies of old age with zero reproduction involved. */
class AgingSystemTest {

  @Test
  void entityAgesEveryTick() {
    SimWorld w = TestWorlds.geneticsWorld(9, 9, 5L);
    TemplateLoader loader = new TemplateLoader(w.materials());
    var critter = loader.loadFromResource("/data/templates/critter.json");
    WorldEntity e = w.spawn(critter.instantiate(4, 4), critter.name());

    SimulationLoop loop = Simulations.standard(w);
    loop.runTicks(10);

    assertTrue(e.get(Age.class).orElseThrow().age >= 10, "age should advance roughly one per tick");
  }

  @Test
  void entityDiesOnceItExceedsItsLifespan() {
    SimWorld w = TestWorlds.geneticsWorld(9, 9, 6L);
    TemplateLoader loader = new TemplateLoader(w.materials());
    var critter = loader.loadFromResource("/data/templates/critter.json");
    WorldEntity e = w.spawn(critter.instantiate(4, 4), critter.name());
    float lifespan = e.get(Age.class).orElseThrow().lifespan;

    SimulationLoop loop = Simulations.standard(w);
    loop.runTicks((int) lifespan + 5);

    assertFalse(w.world().entities().contains(e), "entity should have died of old age");
    boolean loggedDeath =
        w.log().bySubject(w.subjectOf(e)).stream()
            .anyMatch(r -> r.after().contains("dead(old age)"));
    assertTrue(loggedDeath, "death must be causality-logged, not a silent removal");
  }

  @Test
  void agingRunsEvenWithoutFullStandardLoop() {
    SimWorld w = TestWorlds.world(4, 4, 1L);
    TemplateLoader loader = new TemplateLoader(w.materials());
    // torch has no Age component; loop must simply skip it (component-presence gate only).
    var torch = loader.loadFromResource("/data/templates/torch.json");
    WorldEntity e = w.spawn(torch.instantiate(2, 2), torch.name());

    SimulationLoop loop =
        new SimulationLoop(w).register(Phase.LIFECYCLE, new com.cryptroot.substrate.system.AgingSystem());
    loop.runTicks(5);

    assertTrue(w.world().entities().contains(e), "entity without Age must be left alone");
  }
}
