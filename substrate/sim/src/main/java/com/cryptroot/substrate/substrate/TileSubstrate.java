package com.cryptroot.substrate.substrate;

import com.cryptroot.substrate.material.MaterialRegistry;
import java.util.Arrays;

/**
 * The flat 2D tile substrate: struct-of-arrays over the grid. Tiles carry the same kind of
 * physical properties entities do (material, temperature, fuel, liquid) so the same systems act on
 * both.
 *
 * <p>Tiles are arrays rather than entities purely for performance (cost-based narrowing, not
 * identity gating — see CLAUDE.md "performance-driven narrowing").
 */
public final class TileSubstrate {

  private final int columns;
  private final int rows;

  final int[] materialId;
  final float[] temperature;
  final float[] fuel;
  final int[] liquidMaterialId;
  final float[] liquidDepth;
  final float[] elevation;

  public TileSubstrate(int columns, int rows, int fillMaterialId, float initialTemperature) {
    if (columns <= 0 || rows <= 0) throw new IllegalArgumentException("grid must be non-empty");
    this.columns = columns;
    this.rows = rows;
    int n = columns * rows;
    materialId = new int[n];
    temperature = new float[n];
    fuel = new float[n];
    liquidMaterialId = new int[n];
    liquidDepth = new float[n];
    elevation = new float[n];
    Arrays.fill(materialId, fillMaterialId);
    Arrays.fill(temperature, initialTemperature);
    Arrays.fill(liquidMaterialId, MaterialRegistry.NONE);
  }

  public int columns() {
    return columns;
  }

  public int rows() {
    return rows;
  }

  public boolean inBounds(int col, int row) {
    return col >= 0 && col < columns && row >= 0 && row < rows;
  }

  public int index(int col, int row) {
    if (!inBounds(col, row)) throw new IndexOutOfBoundsException(col + "," + row);
    return row * columns + col;
  }

  public String subject(int col, int row) {
    return "tile:" + col + "," + row;
  }

  // --- direct accessors (systems read; writes should be logged by the caller) ---

  public int materialId(int col, int row) {
    return materialId[index(col, row)];
  }

  public void setMaterialId(int col, int row, int id) {
    materialId[index(col, row)] = id;
  }

  public float temperature(int col, int row) {
    return temperature[index(col, row)];
  }

  public void setTemperature(int col, int row, float t) {
    temperature[index(col, row)] = t;
  }

  public float fuel(int col, int row) {
    return fuel[index(col, row)];
  }

  public void setFuel(int col, int row, float f) {
    fuel[index(col, row)] = f;
  }

  public int liquidMaterialId(int col, int row) {
    return liquidMaterialId[index(col, row)];
  }

  public float liquidDepth(int col, int row) {
    return liquidDepth[index(col, row)];
  }

  public void setLiquid(int col, int row, int materialId, float depth) {
    int i = index(col, row);
    if (depth <= 0) {
      liquidMaterialId[i] = MaterialRegistry.NONE;
      liquidDepth[i] = 0;
    } else {
      liquidMaterialId[i] = materialId;
      liquidDepth[i] = depth;
    }
  }

  public float elevation(int col, int row) {
    return elevation[index(col, row)];
  }

  public void setElevation(int col, int row, float e) {
    elevation[index(col, row)] = e;
  }

  /** Seeds each tile's fuel from its material definition. Call after painting materials. */
  public void seedFuel(MaterialRegistry materials) {
    for (int i = 0; i < materialId.length; i++) {
      fuel[i] = materials.get(materialId[i]).fuel();
    }
  }
}
