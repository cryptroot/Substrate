package com.cryptroot.substrate.system;

import com.cryptroot.core.world.WorldEntity;
import com.cryptroot.substrate.component.Digestive;
import com.cryptroot.substrate.component.LiquidCoated;
import com.cryptroot.substrate.tick.SimSystem;
import com.cryptroot.substrate.world.SimWorld;
import java.util.ArrayList;
import java.util.Map;

/**
 * Whatever has liquid in its mouth and a digestive tract swallows. No check for whether the
 * entity "meant to" ingest it, or whether it is a good idea — those are downstream systems'
 * arithmetic, not this system's opinion.
 */
public final class IngestionSystem implements SimSystem {

  @Override
  public void tick(SimWorld w, long tick) {
    for (WorldEntity e : w.entitiesWith(LiquidCoated.class, Digestive.class)) {
      LiquidCoated coat = e.get(LiquidCoated.class).orElseThrow();
      Digestive digestive = e.get(Digestive.class).orElseThrow();
      if (coat.mouth.isEmpty()) continue;
      for (Map.Entry<Integer, Float> entry : new ArrayList<>(coat.mouth.entrySet())) {
        float amount = entry.getValue();
        if (amount <= 0) continue;
        float before = digestive.contents.getOrDefault(entry.getKey(), 0f);
        digestive.contents.merge(entry.getKey(), amount, Float::sum);
        coat.mouth.remove(entry.getKey());
        w.log()
            .record(
                tick,
                "INGESTION",
                systemName(),
                w.subjectOf(e),
                "Digestive.contents(" + w.materials().nameOf(entry.getKey()) + ")",
                before,
                digestive.contents.get(entry.getKey()));
      }
    }
  }
}
