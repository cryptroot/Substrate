package com.cryptroot.substrate.harness;

import com.cryptroot.core.world.WorldEntity;
import com.cryptroot.substrate.component.GridPosition;
import com.cryptroot.substrate.substrate.TileSubstrate;
import com.cryptroot.substrate.world.SimWorld;

/**
 * ASCII snapshot of the world: temperature band per tile, liquid overlays, entity markers.
 * Purely observational — reads state, never writes it.
 */
public final class AsciiRenderer {

  private AsciiRenderer() {}

  public static String render(SimWorld w) {
    TileSubstrate tiles = w.tiles();
    char[][] out = new char[tiles.rows()][tiles.columns()];
    for (int r = 0; r < tiles.rows(); r++) {
      for (int c = 0; c < tiles.columns(); c++) {
        float t = tiles.temperature(c, r);
        char ch;
        if (tiles.liquidDepth(c, r) > 0.05f) ch = '~';
        else if (t >= 300) ch = '#';
        else if (t >= 150) ch = '*';
        else if (t >= 60) ch = '+';
        else if (tiles.fuel(c, r) <= 0 && hasFuelMaterial(w, c, r)) ch = 'x';
        else ch = '.';
        out[r][c] = ch;
      }
    }
    for (WorldEntity e : w.entitiesWith(GridPosition.class)) {
      GridPosition p = e.get(GridPosition.class).orElseThrow();
      if (tiles.inBounds(p.col, p.row)) out[p.row][p.col] = '@';
    }
    StringBuilder sb = new StringBuilder();
    for (int r = tiles.rows() - 1; r >= 0; r--) {
      sb.append(out[r]).append('\n');
    }
    return sb.toString();
  }

  private static boolean hasFuelMaterial(SimWorld w, int c, int r) {
    return w.materials().get(w.tiles().materialId(c, r)).fuel() > 0;
  }
}
