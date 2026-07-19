package com.cryptroot.substrate;

import com.cryptroot.substrate.genetics.GeneRegistry;
import com.cryptroot.substrate.material.MaterialRegistry;
import com.cryptroot.substrate.material.SimConfig;
import com.cryptroot.substrate.substrate.TileSubstrate;
import com.cryptroot.substrate.template.TemplateLoader;
import com.cryptroot.substrate.template.TemplateRegistry;
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

  /** Dirt-filled world wired with the standard gene registry and every standard template
   * registered, for tests that exercise genetics/lifecycle/reproduction. */
  public static SimWorld geneticsWorld(int cols, int rows, long seed) {
    MaterialRegistry materials = materials();
    SimConfig config = config();
    TileSubstrate tiles =
        new TileSubstrate(cols, rows, materials.idOf("dirt"), config.ambientTemperature());
    tiles.seedFuel(materials);
    GeneRegistry genes = GeneRegistry.loadFromResource("/data/genes.json");
    TemplateRegistry templates = TemplateRegistry.empty();
    TemplateLoader loader = new TemplateLoader(materials);
    templates.register(loader.loadFromResource("/data/templates/critter.json"));
    templates.register(loader.loadFromResource("/data/templates/werewolf.json"));
    templates.register(loader.loadFromResource("/data/templates/torch.json"));
    return new SimWorld(tiles, materials, config, seed, genes, templates);
  }
}
