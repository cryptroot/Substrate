package com.cryptroot.substrate.harness;

import com.cryptroot.substrate.material.MaterialRegistry;
import com.cryptroot.substrate.material.SimConfig;
import com.cryptroot.substrate.substrate.TileSubstrate;
import com.cryptroot.substrate.template.TemplateLoader;
import com.cryptroot.substrate.tick.SimulationLoop;
import com.cryptroot.substrate.tick.Simulations;
import com.cryptroot.substrate.world.SimWorld;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Builds a runnable simulation from a scenario JSON:
 *
 * <pre>{@code
 * {
 *   "seed": 42, "ticks": 200, "columns": 16, "rows": 16, "fillMaterial": "dirt",
 *   "paint":   [ { "material": "grass", "fromCol": 2, "toCol": 9, "fromRow": 5, "toRow": 5 } ],
 *   "liquids": [ { "material": "ethanol", "col": 4, "row": 4, "depth": 2.0 } ],
 *   "heat":    [ { "col": 2, "row": 5, "temperature": 600 } ],
 *   "spawns":  [ { "template": "/data/templates/critter.json", "col": 4, "row": 4 } ]
 * }
 * }</pre>
 */
public final class Scenario {

  private final JsonNode root;

  private Scenario(JsonNode root) {
    this.root = root;
  }

  public static Scenario fromFile(Path path) throws IOException {
    return new Scenario(new ObjectMapper().readTree(Files.readString(path)));
  }

  public static Scenario fromResource(String resourcePath) {
    Objects.requireNonNull(resourcePath);
    try (InputStream in = Scenario.class.getResourceAsStream(resourcePath)) {
      if (in == null) throw new IllegalArgumentException("resource not found: " + resourcePath);
      return new Scenario(new ObjectMapper().readTree(in));
    } catch (IOException e) {
      throw new IllegalStateException("failed to load scenario " + resourcePath, e);
    }
  }

  public int ticks() {
    return root.path("ticks").asInt(100);
  }

  public SimulationLoop build() {
    MaterialRegistry materials = MaterialRegistry.loadFromResource("/data/materials.json");
    SimConfig config = SimConfig.loadFromResource("/data/simconfig.json");
    int cols = root.path("columns").asInt(16);
    int rows = root.path("rows").asInt(16);
    int fill = materials.idOf(root.path("fillMaterial").asText("dirt"));
    TileSubstrate tiles = new TileSubstrate(cols, rows, fill, config.ambientTemperature());

    for (JsonNode p : root.path("paint")) {
      int id = materials.idOf(p.path("material").asText());
      for (int r = p.path("fromRow").asInt(); r <= p.path("toRow").asInt(); r++) {
        for (int c = p.path("fromCol").asInt(); c <= p.path("toCol").asInt(); c++) {
          tiles.setMaterialId(c, r, id);
        }
      }
    }
    tiles.seedFuel(materials);
    for (JsonNode l : root.path("liquids")) {
      tiles.setLiquid(
          l.path("col").asInt(),
          l.path("row").asInt(),
          materials.idOf(l.path("material").asText()),
          (float) l.path("depth").asDouble(1));
    }
    for (JsonNode h : root.path("heat")) {
      tiles.setTemperature(
          h.path("col").asInt(), h.path("row").asInt(), (float) h.path("temperature").asDouble());
    }

    SimWorld world = new SimWorld(tiles, materials, config, root.path("seed").asLong(0));
    TemplateLoader loader = new TemplateLoader(materials);
    for (JsonNode s : root.path("spawns")) {
      var template = loader.loadFromResource(s.path("template").asText());
      world.spawn(
          template.instantiate(s.path("col").asInt(), s.path("row").asInt()), template.name());
    }
    return Simulations.standard(world);
  }
}
