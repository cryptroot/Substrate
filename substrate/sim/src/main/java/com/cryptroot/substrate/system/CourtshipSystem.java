package com.cryptroot.substrate.system;

import com.cryptroot.core.world.WorldEntity;
import com.cryptroot.substrate.component.Age;
import com.cryptroot.substrate.component.Fertility;
import com.cryptroot.substrate.component.GridPosition;
import com.cryptroot.substrate.component.ReproduceIntent;
import com.cryptroot.substrate.tick.SimSystem;
import com.cryptroot.substrate.world.SimWorld;

/**
 * Cognition-phase behavior layer that sets {@link ReproduceIntent}, mirroring how {@code
 * FearSystem} sets {@link com.cryptroot.substrate.component.MoveIntent}. Re-evaluated fresh every
 * tick: whatever is mature, cooldown-ready, and nearest within {@code matingRadius} becomes the
 * target, with no notion of species/breeding-group compatibility — component presence
 * (Fertility + Age + GridPosition) is the only gate, so cross-template pairings are allowed to happen honestly.
 */
public final class CourtshipSystem implements SimSystem {

  @Override
  public void tick(SimWorld w, long tick) {
    int radius = w.config().matingRadius();
    var candidates = w.entitiesWith(Fertility.class, Age.class, GridPosition.class);

    for (WorldEntity e : candidates) {
      Fertility fertility = e.get(Fertility.class).orElseThrow();
      Age age = e.get(Age.class).orElseThrow();
      ReproduceIntent intent = e.get(ReproduceIntent.class).orElse(null);
      if (intent == null || !fertility.ready() || age.age < age.maturityAge) continue;

      GridPosition pos = e.get(GridPosition.class).orElseThrow();
      WorldEntity nearest = null;
      int nearestDist = Integer.MAX_VALUE;
      for (WorldEntity other : candidates) {
        // An entity cannot be its own mate — this is a genuine physical impossibility (there is
        // only one parent slot's worth of "self"), not a species/identity exclusion.
        if (other == e) continue;
        Fertility oFertility = other.get(Fertility.class).orElseThrow();
        Age oAge = other.get(Age.class).orElseThrow();
        if (!oFertility.ready() || oAge.age < oAge.maturityAge) continue;
        GridPosition oPos = other.get(GridPosition.class).orElseThrow();
        int dist = Math.max(Math.abs(oPos.col - pos.col), Math.abs(oPos.row - pos.row));
        if (dist > radius) continue;
        if (dist < nearestDist) {
          nearestDist = dist;
          nearest = other;
        }
      }

      long before = intent.partnerId;
      intent.partnerId = nearest == null ? ReproduceIntent.NONE : w.idOf(nearest);
      if (before != intent.partnerId) {
        w.log()
            .record(
                tick, "COGNITION", systemName(), w.subjectOf(e), "ReproduceIntent.partnerId",
                before, intent.partnerId);
      }
    }
  }
}
