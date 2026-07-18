package com.cryptroot.substrate.material;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Loads material definitions from JSON and hands out dense int ids. Systems reference materials by
 * id; names exist only for data files and log readability.
 */
public final class MaterialRegistry {

  /** Id meaning "no material" (e.g. no liquid on a tile). */
  public static final int NONE = -1;

  private final List<Material> materials = new ArrayList<>();
  private final Map<String, Integer> byName = new HashMap<>();

  public static MaterialRegistry loadFromResource(String resourcePath) {
    Objects.requireNonNull(resourcePath, "resourcePath must not be null");
    try (InputStream in = MaterialRegistry.class.getResourceAsStream(resourcePath)) {
      if (in == null) throw new IllegalArgumentException("resource not found: " + resourcePath);
      return load(new ObjectMapper().readTree(in));
    } catch (IOException e) {
      throw new IllegalStateException("failed to load materials from " + resourcePath, e);
    }
  }

  public static MaterialRegistry load(JsonNode root) {
    MaterialRegistry registry = new MaterialRegistry();
    JsonNode list = root.path("materials");
    for (JsonNode m : list) {
      String name = m.path("name").asText();
      if (name.isEmpty()) throw new IllegalArgumentException("material missing name: " + m);
      int id = registry.materials.size();
      registry.materials.add(
          new Material(
              id,
              name,
              (float) m.path("ignitionPoint").asDouble(Double.POSITIVE_INFINITY),
              (float) m.path("burnRate").asDouble(0),
              (float) m.path("heatOutput").asDouble(0),
              (float) m.path("heatCapacity").asDouble(1),
              (float) m.path("conductivity").asDouble(0.1),
              (float) m.path("fuel").asDouble(0),
              (float) m.path("intoxicatingPotency").asDouble(0),
              (float) m.path("toxicity").asDouble(0),
              (float) m.path("viscosity").asDouble(0.5),
              (float) m.path("evaporationRate").asDouble(0)));
      if (registry.byName.put(name, id) != null) {
        throw new IllegalArgumentException("duplicate material name: " + name);
      }
    }
    return registry;
  }

  public Material get(int id) {
    return materials.get(id);
  }

  public int idOf(String name) {
    Integer id = byName.get(name);
    if (id == null) throw new IllegalArgumentException("unknown material: " + name);
    return id;
  }

  /** Log-friendly name lookup; safe for {@link #NONE}. */
  public String nameOf(int id) {
    return id == NONE ? "none" : materials.get(id).name();
  }

  public int count() {
    return materials.size();
  }
}
