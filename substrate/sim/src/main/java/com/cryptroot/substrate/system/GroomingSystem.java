package com.cryptroot.substrate.system;

import com.cryptroot.core.world.WorldEntity;
import com.cryptroot.substrate.component.Grooming;
import com.cryptroot.substrate.component.LiquidCoated;
import com.cryptroot.substrate.tick.SimSystem;
import com.cryptroot.substrate.world.SimWorld;
import java.util.ArrayList;
import java.util.Map;

/**
 * Whatever grooms and is coated in liquid moves that liquid toward its mouth. This system has no
 * idea what the liquid is or what the groomer is — that ignorance is load-bearing.
 */
public final class GroomingSystem implements SimSystem {

  @Override
  public void tick(SimWorld w, long tick) {
    for (WorldEntity e : w.entitiesWith(Grooming.class, LiquidCoated.class)) {
      Grooming grooming = e.get(Grooming.class).orElseThrow();
      LiquidCoated coat = e.get(LiquidCoated.class).orElseThrow();
      if (coat.bodyTotal() < grooming.triggerAmount) continue;
      float budget = grooming.rate;
      for (Map.Entry<Integer, Float> entry : new ArrayList<>(coat.body.entrySet())) {
        if (budget <= 0) break;
        float amount = entry.getValue();
        if (amount <= 0) continue;
        float moved = Math.min(amount, budget);
        budget -= moved;
        float remaining = amount - moved;
        if (remaining <= 0) coat.body.remove(entry.getKey());
        else coat.body.put(entry.getKey(), remaining);
        float mouthBefore = coat.mouth.getOrDefault(entry.getKey(), 0f);
        LiquidCoated.addTo(coat.mouth, entry.getKey(), moved);
        w.log()
            .record(
                tick,
                "CONTACT",
                systemName(),
                w.subjectOf(e),
                "LiquidCoated body->mouth (" + w.materials().nameOf(entry.getKey()) + ")",
                "body=" + amount + ",mouth=" + mouthBefore,
                "body=" + remaining + ",mouth=" + coat.mouth.get(entry.getKey()));
      }
    }
  }
}
