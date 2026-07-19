package com.cryptroot.substrate.system;

import com.cryptroot.core.world.WorldEntity;
import com.cryptroot.substrate.component.Age;
import com.cryptroot.substrate.component.Fertility;
import com.cryptroot.substrate.tick.SimSystem;
import com.cryptroot.substrate.world.SimWorld;

/**
 * Advances every entity's {@link Age} by one tick and removes it once it exceeds its lifespan.
 * Required plumbing for reproduction to not produce an ever-growing population. 
 * Death is a log line and a despawn, never an exception, same as {@code HealthSystem}.
 *
 * <p>Also counts down {@link Fertility}'s post-birth cooldown — the counterpart write to the
 * cooldown {@code ReproductionSystem} sets after a birth; without something ticking it back down,
 * an entity that has ever reproduced would stay infertile forever.
 */
public final class AgingSystem implements SimSystem {

  @Override
  public void tick(SimWorld w, long tick) {
    for (WorldEntity e : w.entitiesWith(Age.class)) {
      Age age = e.get(Age.class).orElseThrow();
      float before = age.age;
      age.age += 1;
      w.log().record(tick, "LIFECYCLE", systemName(), w.subjectOf(e), "Age.age", before, age.age);
      if (age.age >= age.lifespan) {
        w.log()
            .record(
                tick, "LIFECYCLE", systemName(), w.subjectOf(e), "existence", "alive",
                "dead(old age)");
        w.world().queueRemove(e);
      }
    }
    for (WorldEntity e : w.entitiesWith(Fertility.class)) {
      Fertility fertility = e.get(Fertility.class).orElseThrow();
      if (fertility.cooldownRemaining > 0) fertility.cooldownRemaining -= 1;
    }
  }
}
