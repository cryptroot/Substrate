package com.cryptroot.substrate.system;

import com.cryptroot.core.world.WorldEntity;
import com.cryptroot.substrate.component.FearProne;
import com.cryptroot.substrate.component.GridPosition;
import com.cryptroot.substrate.component.Impairment;
import com.cryptroot.substrate.component.MoveIntent;
import com.cryptroot.substrate.component.Traits;
import com.cryptroot.substrate.tick.SimSystem;
import com.cryptroot.substrate.world.SimWorld;

/**
 * Compares the traits of everything nearby against each fearful entity's feared-trait list and
 * accumulates fear. The scan DELIBERATELY includes the entity itself — a werewolf that fears
 * canines and carries a CANINE trait frightens itself. Do not "fix" this; it is a feature we
 * declined to prevent.
 *
 * <p>Impairment raises the effective fear threshold (liquid courage) — a cross-system interaction
 * that costs zero extra code because both are just numbers.
 */
public final class FearSystem implements SimSystem {

  @Override
  public void tick(SimWorld w, long tick) {
    int radius = w.config().fearRadius();
    float gain = w.config().fearGainPerSource();
    float courage = w.config().drunkCourageFactor();

    var fearful = w.entitiesWith(FearProne.class, GridPosition.class);
    var traitBearers = w.entitiesWith(Traits.class, GridPosition.class);

    for (WorldEntity e : fearful) {
      FearProne fear = e.get(FearProne.class).orElseThrow();
      GridPosition pos = e.get(GridPosition.class).orElseThrow();
      float before = fear.currentFear;

      WorldEntity scariest = null;
      GridPosition scariestPos = null;
      for (WorldEntity other : traitBearers) {
        // No self-exclusion here, on purpose. The entity's own traits are physically present
        // and physically nearby (distance 0 — maximum proximity, maximum fear).
        GridPosition oPos = other.get(GridPosition.class).orElseThrow();
        int dist = Math.max(Math.abs(oPos.col - pos.col), Math.abs(oPos.row - pos.row));
        if (dist > radius) continue;
        Traits traits = other.get(Traits.class).orElseThrow();
        for (String feared : fear.fearedTraits) {
          if (traits.traits.contains(feared)) {
            float falloff = 1f / (1 + dist);
            fear.currentFear += gain * falloff;
            if (scariest == null) {
              scariest = other;
              scariestPos = oPos;
            }
          }
        }
      }

      fear.currentFear = Math.max(0, fear.currentFear - fear.fearDecayRate);
      if (Math.abs(fear.currentFear - before) > 0.001f) {
        w.log()
            .record(
                tick,
                "COGNITION",
                systemName(),
                w.subjectOf(e),
                "FearProne.currentFear",
                before,
                fear.currentFear);
      }

      float effectiveThreshold =
          fear.fearThreshold
              * (1 + e.get(Impairment.class).map(i -> i.level).orElse(0f) * courage);
      if (fear.currentFear >= effectiveThreshold && scariestPos != null) {
        final GridPosition src = scariestPos;
        e.get(MoveIntent.class)
            .ifPresent(
                intent -> {
                  float dx = pos.col - src.col;
                  float dy = pos.row - src.row;
                  float len = (float) Math.sqrt(dx * dx + dy * dy);
                  if (len < 0.001f) {
                    // Fleeing from itself: direction is genuinely undefined physics, so panic
                    // direction is left to the movement system's stagger. Log the panic.
                    intent.dx = 0;
                    intent.dy = 0;
                  } else {
                    intent.dx = dx / len;
                    intent.dy = dy / len;
                  }
                  intent.urgency = fear.currentFear / effectiveThreshold;
                  w.log()
                      .record(
                          tick,
                          "COGNITION",
                          systemName(),
                          w.subjectOf(e),
                          "MoveIntent(flee)",
                          "-",
                          "dx=" + intent.dx + ",dy=" + intent.dy + ",urgency=" + intent.urgency);
                });
      }
    }
  }
}
