package com.cryptroot.substrate.system;

import com.cryptroot.core.world.WorldEntity;
import com.cryptroot.substrate.component.GridPosition;
import com.cryptroot.substrate.component.Impairment;
import com.cryptroot.substrate.component.Mobility;
import com.cryptroot.substrate.component.MoveIntent;
import com.cryptroot.substrate.substrate.TileSubstrate;
import com.cryptroot.substrate.tick.SimSystem;
import com.cryptroot.substrate.world.SimWorld;
import java.util.SplittableRandom;

/**
 * Executes locomotion for whatever has Mobility + GridPosition. Flee/seek intents (if a MoveIntent
 * component is present) beat wandering; impairment slows movement and adds stagger. Terrain
 * penalties come from the tile's material — data, not code.
 */
public final class MovementSystem implements SimSystem {

  @Override
  public void tick(SimWorld w, long tick) {
    TileSubstrate tiles = w.tiles();
    SplittableRandom rng = w.random(tick, systemName());
    float speedFactor = w.config().impairmentSpeedFactor();
    float staggerChance = w.config().staggerChancePerImpairment();

    for (WorldEntity e : w.entitiesWith(Mobility.class, GridPosition.class)) {
      Mobility mobility = e.get(Mobility.class).orElseThrow();
      GridPosition pos = e.get(GridPosition.class).orElseThrow();

      float impairment = e.get(Impairment.class).map(i -> i.level).orElse(0f);
      float terrain =
          tiles.inBounds(pos.col, pos.row)
              ? mobility.terrainPenalty.getOrDefault(tiles.materialId(pos.col, pos.row), 1f)
              : 1f;
      float effectiveSpeed =
          mobility.speed * terrain * Math.max(0f, 1f - impairment * speedFactor);
      if (effectiveSpeed <= 0) continue;
      // Speed is a probability of stepping this tick — a gradient, not a boolean can/can't move.
      if (rng.nextDouble() > Math.min(1f, effectiveSpeed)) continue;

      int dc;
      int dr;
      MoveIntent intent = e.get(MoveIntent.class).orElse(null);
      boolean staggering = impairment > 0 && rng.nextDouble() < impairment * staggerChance;
      if (intent != null && intent.urgency > 0 && !staggering) {
        dc = Math.round(intent.dx);
        dr = Math.round(intent.dy);
        if (dc == 0 && dr == 0) {
          // Undirected panic (e.g. fleeing from itself): random burst.
          dc = rng.nextInt(3) - 1;
          dr = rng.nextInt(3) - 1;
        }
        intent.urgency = Math.max(0, intent.urgency - 0.25f);
      } else if (staggering || rng.nextDouble() < 0.3) {
        dc = rng.nextInt(3) - 1;
        dr = rng.nextInt(3) - 1;
      } else {
        continue;
      }

      int nc = Math.max(0, Math.min(tiles.columns() - 1, pos.col + dc));
      int nr = Math.max(0, Math.min(tiles.rows() - 1, pos.row + dr));
      if (nc == pos.col && nr == pos.row) continue;
      String before = pos.col + "," + pos.row;
      pos.col = nc;
      pos.row = nr;
      w.log()
          .record(
              tick,
              "MOVEMENT",
              systemName(),
              w.subjectOf(e),
              "GridPosition" + (staggering ? "(stagger)" : intent != null && intent.urgency > 0 ? "(flee)" : "(wander)"),
              before,
              nc + "," + nr);
    }
  }
}
