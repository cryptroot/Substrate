package com.cryptroot.substrate.component;

import com.cryptroot.core.world.EntityComponent;

/** Tile-grid position. */
public final class GridPosition implements EntityComponent {
  public int col;
  public int row;

  public GridPosition(int col, int row) {
    this.col = col;
    this.row = row;
  }
}
