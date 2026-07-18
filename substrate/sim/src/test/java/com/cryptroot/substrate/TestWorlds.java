package com.cryptroot.substrate;

import com.cryptroot.substrate.material.MaterialRegistry;
import com.cryptroot.substrate.material.SimConfig;
import com.cryptroot.substrate.substrate.TileSubstrate;
import com.cryptroot.substrate.world.SimWorld;

/** Shared test fixtures. */
public final class TestWorlds {

  private TestWorlds() {}

  public static MaterialRegistry materials() {
    return MaterialRegistry.loadFromResource("/data/materials.json");
  }

  public static SimConfig config() {
    return SimConfig.loadFromResource("/data/simconfig.json");
  }

  /** Small dirt-filled world at ambient temperature. */
  public static SimWorld world(int cols, int rows, long seed) {
    MaterialRegistry materials = materials();
    SimConfig config = config();
    TileSubstrate tiles =
        new TileSubstrate(cols, rows, materials.idOf("dirt"), config.ambientTemperature());
    tiles.seedFuel(materials);
    return new SimWorld(tiles, materials, config, seed);
  }
}
