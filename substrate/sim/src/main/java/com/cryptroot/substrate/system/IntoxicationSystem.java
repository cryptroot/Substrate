package com.cryptroot.substrate.system;

import com.cryptroot.core.world.WorldEntity;
import com.cryptroot.substrate.component.Digestive;
import com.cryptroot.substrate.component.Impairment;
import com.cryptroot.substrate.material.Material;
import com.cryptroot.substrate.tick.SimSystem;
import com.cryptroot.substrate.world.SimWorld;
import java.util.ArrayList;
import java.util.Map;

/**
 * Reads digestive contents against material potency and tolerance, producing a graded impairment
 * level. Impairment is its own component so any future system (heatstroke, toxins) can contribute
 * to it. Contents metabolize away each tick — the damping term against permanent drunkenness.
 */
public final class IntoxicationSystem implements SimSystem {

  @Override
  public void tick(SimWorld w, long tick) {
    for (WorldEntity e : w.entitiesWith(Digestive.class, Impairment.class)) {
      Digestive digestive = e.get(Digestive.class).orElseThrow();
      Impairment impairment = e.get(Impairment.class).orElseThrow();

      float intoxicantLoad = 0;
      for (Map.Entry<Integer, Float> entry : digestive.contents.entrySet()) {
        Material m = w.materials().get(entry.getKey());
        intoxicantLoad += entry.getValue() * m.intoxicatingPotency();
      }
      float target = intoxicantLoad / digestive.tolerance;
      float before = impairment.level;
      // Approach the load-implied level; sober up as contents metabolize.
      impairment.level = before + (target - before) * 0.5f;
      if (Math.abs(impairment.level - before) > 0.001f) {
        w.log()
            .record(
                tick,
                "METABOLISM",
                systemName(),
                w.subjectOf(e),
                "Impairment.level",
                before,
                impairment.level);
      }

      // Metabolize contents down.
      for (Map.Entry<Integer, Float> entry : new ArrayList<>(digestive.contents.entrySet())) {
        float remaining = entry.getValue() - digestive.metabolismRate;
        if (remaining <= 0) digestive.contents.remove(entry.getKey());
        else digestive.contents.put(entry.getKey(), remaining);
      }
    }
  }
}
