package com.cryptroot.substrate.template;

import com.cryptroot.core.world.WorldEntity;
import com.cryptroot.substrate.component.Digestive;
import com.cryptroot.substrate.component.FearProne;
import com.cryptroot.substrate.component.Flammable;
import com.cryptroot.substrate.component.GridPosition;
import com.cryptroot.substrate.component.Grooming;
import com.cryptroot.substrate.component.Health;
import com.cryptroot.substrate.component.Impairment;
import com.cryptroot.substrate.component.LiquidCoated;
import com.cryptroot.substrate.component.Mobility;
import com.cryptroot.substrate.component.MoveIntent;
import com.cryptroot.substrate.component.Thermal;
import com.cryptroot.substrate.component.Traits;
import com.cryptroot.substrate.material.MaterialRegistry;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Builds entities from JSON templates that are pure component bundles with numeric values.
 * Species differ by numbers, never by code paths — adding a species is adding a file, not a
 * class.
 */
public final class TemplateLoader {

  private final MaterialRegistry materials;

  public TemplateLoader(MaterialRegistry materials) {
    this.materials = Objects.requireNonNull(materials);
  }

  public Template loadFromResource(String resourcePath) {
    try (InputStream in = TemplateLoader.class.getResourceAsStream(resourcePath)) {
      if (in == null) throw new IllegalArgumentException("resource not found: " + resourcePath);
      return load(new ObjectMapper().readTree(in));
    } catch (IOException e) {
      throw new IllegalStateException("failed to load template from " + resourcePath, e);
    }
  }

  public Template load(JsonNode root) {
    String name = root.path("name").asText("unnamed");
    JsonNode comps = root.path("components");
    return new Template(name, comps, materials);
  }

  /** A reusable entity blueprint. {@link #instantiate} builds a fresh entity each call. */
  public static final class Template {
    private final String name;
    private final JsonNode comps;
    private final MaterialRegistry materials;

    private Template(String name, JsonNode comps, MaterialRegistry materials) {
      this.name = name;
      this.comps = comps;
      this.materials = materials;
    }

    public String name() {
      return name;
    }

    public WorldEntity instantiate(int col, int row) {
      WorldEntity e = new WorldEntity().with(GridPosition.class, new GridPosition(col, row));
      Iterator<Map.Entry<String, JsonNode>> it = comps.fields();
      while (it.hasNext()) {
        Map.Entry<String, JsonNode> entry = it.next();
        attach(e, entry.getKey(), entry.getValue());
      }
      return e;
    }

    private void attach(WorldEntity e, String key, JsonNode n) {
      switch (key) {
        case "thermal" ->
            e.with(
                Thermal.class,
                new Thermal(
                    f(n, "temperature", 20),
                    f(n, "heatCapacity", 1),
                    f(n, "conductivity", 0.1f)));
        case "flammable" ->
            e.with(
                Flammable.class,
                new Flammable(
                    f(n, "ignitionPoint", Float.POSITIVE_INFINITY),
                    f(n, "burnRate", 0),
                    f(n, "heatOutput", 0),
                    f(n, "fuel", 0)));
        case "liquidCoated" -> e.with(LiquidCoated.class, new LiquidCoated());
        case "digestive" ->
            e.with(
                Digestive.class,
                new Digestive(f(n, "tolerance", 1), f(n, "metabolismRate", 0.05f)));
        case "fearProne" ->
            e.with(
                FearProne.class,
                new FearProne(
                    f(n, "fearThreshold", 10),
                    f(n, "fearDecayRate", 0.5f),
                    strings(n.path("fearedTraits"))));
        case "traits" -> e.with(Traits.class, new Traits(strings(n.path("traits"))));
        case "mobility" -> {
          Mobility m = new Mobility(f(n, "speed", 0.5f));
          JsonNode penalties = n.path("terrainPenalties");
          Iterator<Map.Entry<String, JsonNode>> pit = penalties.fields();
          while (pit.hasNext()) {
            Map.Entry<String, JsonNode> p = pit.next();
            m.terrainPenalty.put(materials.idOf(p.getKey()), (float) p.getValue().asDouble(1));
          }
          e.with(Mobility.class, m);
        }
        case "grooming" ->
            e.with(
                Grooming.class, new Grooming(f(n, "rate", 0.1f), f(n, "triggerAmount", 0.01f)));
        case "health" ->
            e.with(
                Health.class,
                new Health(f(n, "hp", 10), f(n, "painThresholdTemperature", 60)));
        case "impairment" -> e.with(Impairment.class, new Impairment());
        case "moveIntent" -> e.with(MoveIntent.class, new MoveIntent());
        default -> throw new IllegalArgumentException(
            "template '" + name + "': unknown component key '" + key + "'");
      }
    }

    private static float f(JsonNode n, String field, float dflt) {
      return (float) n.path(field).asDouble(dflt);
    }

    private static Set<String> strings(JsonNode array) {
      Set<String> out = new HashSet<>();
      for (JsonNode item : array) out.add(item.asText());
      return out;
    }
  }
}
