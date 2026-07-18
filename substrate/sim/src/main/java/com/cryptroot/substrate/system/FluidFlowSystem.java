package com.cryptroot.substrate.system;

import com.cryptroot.core.world.WorldEntity;
import com.cryptroot.substrate.component.GridPosition;
import com.cryptroot.substrate.component.LiquidCoated;
import com.cryptroot.substrate.material.Material;
import com.cryptroot.substrate.material.MaterialRegistry;
import com.cryptroot.substrate.substrate.TileSubstrate;
import com.cryptroot.substrate.tick.SimSystem;
import com.cryptroot.substrate.world.SimWorld;

/**
 * Liquids equalize toward lower (elevation + depth) neighbors, evaporate with heat, quench hot
 * tiles, and coat whatever LiquidCoated-capable thing is standing in them. It does not know or
 * care whether the liquid is water, blood, or ethanol — that is the material data's business.
 */
public final class FluidFlowSystem implements SimSystem {

  private static final int[][] NEIGHBORS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

  @Override
  public void tick(SimWorld w, long tick) {
    // Quench/evaporate at full standing depth BEFORE the liquid spreads out — otherwise a
    // bucket of water flows away in the same tick it should have been absorbing heat.
    evaporateAndQuench(w, tick);
    flow(w, tick);
    coatEntities(w, tick);
  }

  private void flow(SimWorld w, long tick) {
    TileSubstrate tiles = w.tiles();
    for (int r = 0; r < tiles.rows(); r++) {
      for (int c = 0; c < tiles.columns(); c++) {
        float depth = tiles.liquidDepth(c, r);
        if (depth <= 0) continue;
        int liquidId = tiles.liquidMaterialId(c, r);
        Material liquid = w.materials().get(liquidId);
        float level = tiles.elevation(c, r) + depth;
        for (int[] d : NEIGHBORS) {
          int nc = c + d[0];
          int nr = r + d[1];
          if (!tiles.inBounds(nc, nr)) continue;
          int neighborLiquid = tiles.liquidMaterialId(nc, nr);
          // Liquids only merge with empty tiles or the same material. Mixing chemistry is
          // out of scope for v1 (a system gap, not an identity gate).
          if (neighborLiquid != MaterialRegistry.NONE && neighborLiquid != liquidId) continue;
          float neighborLevel = tiles.elevation(nc, nr) + tiles.liquidDepth(nc, nr);
          float diff = level - neighborLevel;
          if (diff <= 0.001f) continue;
          float moved = Math.min(depth, diff * 0.5f * liquid.viscosity());
          if (moved <= 0) continue;
          depth -= moved;
          tiles.setLiquid(c, r, liquidId, depth);
          tiles.setLiquid(nc, nr, liquidId, tiles.liquidDepth(nc, nr) + moved);
          w.log()
              .record(
                  tick,
                  "ENVIRONMENT",
                  systemName(),
                  tiles.subject(nc, nr),
                  "liquidDepth(" + w.materials().nameOf(liquidId) + ")",
                  tiles.liquidDepth(nc, nr) - moved,
                  tiles.liquidDepth(nc, nr));
          level = tiles.elevation(c, r) + depth;
          if (depth <= 0) break;
        }
      }
    }
  }

  private void evaporateAndQuench(SimWorld w, long tick) {
    TileSubstrate tiles = w.tiles();
    float quench = w.config().quenchHeatPerDepth();
    for (int r = 0; r < tiles.rows(); r++) {
      for (int c = 0; c < tiles.columns(); c++) {
        float depth = tiles.liquidDepth(c, r);
        if (depth <= 0) continue;
        int liquidId = tiles.liquidMaterialId(c, r);
        Material liquid = w.materials().get(liquidId);
        float temp = tiles.temperature(c, r);
        // Evaporation scales with heat — the negative feedback that stops permanent puddles.
        float evaporated = liquid.evaporationRate() * Math.max(0.1f, temp / 100f);
        float newDepth = Math.max(0, depth - evaporated);
        // Liquid absorbs tile heat (this is how rain saves the haystack).
        float newTemp = temp - Math.min(depth, 1f) * quench * 0.01f * Math.max(0, temp - 30);
        tiles.setLiquid(c, r, liquidId, newDepth);
        tiles.setTemperature(c, r, newTemp);
        if (newDepth <= 0) {
          w.log()
              .record(
                  tick,
                  "ENVIRONMENT",
                  systemName(),
                  tiles.subject(c, r),
                  "liquidDepth(" + w.materials().nameOf(liquidId) + ")",
                  depth,
                  0);
        }
      }
    }
  }

  private void coatEntities(SimWorld w, long tick) {
    TileSubstrate tiles = w.tiles();
    float pickup = w.config().coatingPickupRate();
    for (WorldEntity e : w.entitiesWith(LiquidCoated.class, GridPosition.class)) {
      GridPosition pos = e.get(GridPosition.class).orElseThrow();
      if (!tiles.inBounds(pos.col, pos.row)) continue;
      float depth = tiles.liquidDepth(pos.col, pos.row);
      if (depth <= 0) continue;
      int liquidId = tiles.liquidMaterialId(pos.col, pos.row);
      LiquidCoated coat = e.get(LiquidCoated.class).orElseThrow();
      float moved = Math.min(depth, pickup);
      tiles.setLiquid(pos.col, pos.row, liquidId, depth - moved);
      float before = coat.body.getOrDefault(liquidId, 0f);
      LiquidCoated.addTo(coat.body, liquidId, moved);
      w.log()
          .record(
              tick,
              "ENVIRONMENT",
              systemName(),
              w.subjectOf(e),
              "LiquidCoated.body(" + w.materials().nameOf(liquidId) + ")",
              before,
              coat.body.get(liquidId));
    }
  }
}
