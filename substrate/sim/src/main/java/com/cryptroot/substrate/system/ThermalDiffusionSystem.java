package com.cryptroot.substrate.system;

import com.cryptroot.core.world.WorldEntity;
import com.cryptroot.substrate.component.GridPosition;
import com.cryptroot.substrate.component.Thermal;
import com.cryptroot.substrate.material.Material;
import com.cryptroot.substrate.substrate.TileSubstrate;
import com.cryptroot.substrate.tick.SimSystem;
import com.cryptroot.substrate.world.SimWorld;

/**
 * Heat spreads between adjacent tiles, relaxes toward ambient, and exchanges between each tile and
 * anything Thermal standing on it. Blind to entity kind: a dwarf, a torch, and a haystack heat up
 * identically given the same numbers.
 *
 * <p>Per-tile diffusion deltas are not individually logged (pure noise at grid scale — the
 * temperature field is directly inspectable); tile↔entity exchanges ARE logged since those drive
 * downstream entity behavior.
 */
public final class ThermalDiffusionSystem implements SimSystem {

  @Override
  public void tick(SimWorld w, long tick) {
    TileSubstrate tiles = w.tiles();
    int cols = tiles.columns();
    int rows = tiles.rows();
    float rate = w.config().tileDiffusionRate();
    float ambient = w.config().ambientTemperature();
    float coupling = w.config().ambientCoupling();

    // Double-buffered tile↔tile diffusion so results are iteration-order independent.
    float[] next = new float[cols * rows];
    for (int r = 0; r < rows; r++) {
      for (int c = 0; c < cols; c++) {
        Material m = w.materials().get(tiles.materialId(c, r));
        float t = tiles.temperature(c, r);
        float delta = 0;
        int n = 0;
        if (c > 0) {
          delta += tiles.temperature(c - 1, r) - t;
          n++;
        }
        if (c < cols - 1) {
          delta += tiles.temperature(c + 1, r) - t;
          n++;
        }
        if (r > 0) {
          delta += tiles.temperature(c, r - 1) - t;
          n++;
        }
        if (r < rows - 1) {
          delta += tiles.temperature(c, r + 1) - t;
          n++;
        }
        float diffused = n == 0 ? t : t + (delta / n) * rate * m.conductivity();
        // Ambient relaxation is the global damping term against runaway heat.
        next[r * cols + c] = diffused + (ambient - diffused) * coupling;
      }
    }
    for (int r = 0; r < rows; r++) {
      for (int c = 0; c < cols; c++) {
        tiles.setTemperature(c, r, next[r * cols + c]);
      }
    }

    // Tile ↔ entity exchange, for whatever has Thermal + GridPosition.
    for (WorldEntity e : w.entitiesWith(Thermal.class, GridPosition.class)) {
      Thermal thermal = e.get(Thermal.class).orElseThrow();
      GridPosition pos = e.get(GridPosition.class).orElseThrow();
      if (!tiles.inBounds(pos.col, pos.row)) continue; // physically off-substrate
      float tileT = tiles.temperature(pos.col, pos.row);
      float gap = tileT - thermal.temperature;
      if (Math.abs(gap) < 0.01f) continue;
      float exchanged = gap * thermal.conductivity;
      float before = thermal.temperature;
      thermal.temperature += exchanged / thermal.heatCapacity;
      tiles.setTemperature(pos.col, pos.row, tileT - exchanged * 0.5f);
      w.log()
          .record(
              tick,
              "ENVIRONMENT",
              systemName(),
              w.subjectOf(e),
              "Thermal.temperature",
              before,
              thermal.temperature);
    }
  }
}
