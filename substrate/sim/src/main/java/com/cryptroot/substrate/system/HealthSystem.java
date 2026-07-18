package com.cryptroot.substrate.system;

import com.cryptroot.core.world.WorldEntity;
import com.cryptroot.substrate.component.Digestive;
import com.cryptroot.substrate.component.Health;
import com.cryptroot.substrate.component.Thermal;
import com.cryptroot.substrate.material.Material;
import com.cryptroot.substrate.tick.SimSystem;
import com.cryptroot.substrate.world.SimWorld;
import java.util.Map;

/**
 * Physical integrity: heat above an entity's pain threshold and toxic digestive contents damage
 * hp. Death is a log line and a despawn — never an exception.
 */
public final class HealthSystem implements SimSystem {

  @Override
  public void tick(SimWorld w, long tick) {
    float heatFactor = w.config().heatDamageFactor();
    for (WorldEntity e : w.entitiesWith(Health.class)) {
      Health health = e.get(Health.class).orElseThrow();
      float before = health.hp;
      long causeId = -1;

      Thermal thermal = e.get(Thermal.class).orElse(null);
      if (thermal != null && thermal.temperature > health.painThresholdTemperature) {
        float dmg = (thermal.temperature - health.painThresholdTemperature) * heatFactor;
        health.hp -= dmg;
        causeId =
            w.log()
                .record(
                    tick, "METABOLISM", systemName(), w.subjectOf(e), "Health.hp(heat)", before,
                    health.hp);
      }

      Digestive digestive = e.get(Digestive.class).orElse(null);
      if (digestive != null) {
        for (Map.Entry<Integer, Float> entry : digestive.contents.entrySet()) {
          Material m = w.materials().get(entry.getKey());
          if (m.toxicity() <= 0) continue;
          float hpBefore = health.hp;
          health.hp -= entry.getValue() * m.toxicity();
          causeId =
              w.log()
                  .record(
                      tick,
                      "METABOLISM",
                      systemName(),
                      w.subjectOf(e),
                      "Health.hp(toxicity:" + m.name() + ")",
                      hpBefore,
                      health.hp);
        }
      }

      if (health.hp <= 0) {
        w.log()
            .record(
                tick, "METABOLISM", systemName(), w.subjectOf(e), "existence", "alive", "dead",
                causeId);
        w.world().queueRemove(e);
      }
    }
  }
}
