package com.cryptroot.substrate.template;

import com.cryptroot.core.world.WorldEntity;
import com.cryptroot.substrate.component.Digestive;
import com.cryptroot.substrate.component.FearProne;
import com.cryptroot.substrate.component.Flammable;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Open registry mapping a JSON component key to the function that builds and attaches that
 * component. This replaces a hardcoded switch so new components (V2: Genome, Fertility, Age,
 * Lineage, ...) register a parser instead of editing a growing branch — see README-v2 "Hardcoded
 * Component Parsing". The registry never inspects entity identity; it only translates data into
 * components.
 */
public final class ComponentParsers {

  /** Builds one component from its JSON node and attaches it to the entity. */
  @FunctionalInterface
  public interface ComponentParser {
    void attach(WorldEntity e, JsonNode node, MaterialRegistry materials);
  }

  private final Map<String, ComponentParser> parsers = new HashMap<>();

  /** A registry preloaded with the standard v1 component parsers. */
  public static ComponentParsers defaults() {
    ComponentParsers p = new ComponentParsers();
    p.register(
        "thermal",
        (e, n, m) ->
            e.with(
                Thermal.class,
                new Thermal(
                    f(n, "temperature", 20),
                    f(n, "heatCapacity", 1),
                    f(n, "conductivity", 0.1f))));
    p.register(
        "flammable",
        (e, n, m) ->
            e.with(
                Flammable.class,
                new Flammable(
                    f(n, "ignitionPoint", Float.POSITIVE_INFINITY),
                    f(n, "burnRate", 0),
                    f(n, "heatOutput", 0),
                    f(n, "fuel", 0))));
    p.register("liquidCoated", (e, n, m) -> e.with(LiquidCoated.class, new LiquidCoated()));
    p.register(
        "digestive",
        (e, n, m) ->
            e.with(Digestive.class, new Digestive(f(n, "tolerance", 1), f(n, "metabolismRate", 0.05f))));
    p.register(
        "fearProne",
        (e, n, m) ->
            e.with(
                FearProne.class,
                new FearProne(
                    f(n, "fearThreshold", 10),
                    f(n, "fearDecayRate", 0.5f),
                    strings(n.path("fearedTraits")))));
    p.register("traits", (e, n, m) -> e.with(Traits.class, new Traits(strings(n.path("traits")))));
    p.register(
        "mobility",
        (e, n, m) -> {
          Mobility mob = new Mobility(f(n, "speed", 0.5f));
          JsonNode penalties = n.path("terrainPenalties");
          Iterator<Map.Entry<String, JsonNode>> pit = penalties.fields();
          while (pit.hasNext()) {
            Map.Entry<String, JsonNode> entry = pit.next();
            mob.terrainPenalty.put(m.idOf(entry.getKey()), (float) entry.getValue().asDouble(1));
          }
          e.with(Mobility.class, mob);
        });
    p.register(
        "grooming",
        (e, n, m) ->
            e.with(Grooming.class, new Grooming(f(n, "rate", 0.1f), f(n, "triggerAmount", 0.01f))));
    p.register(
        "health",
        (e, n, m) ->
            e.with(Health.class, new Health(f(n, "hp", 10), f(n, "painThresholdTemperature", 60))));
    p.register("impairment", (e, n, m) -> e.with(Impairment.class, new Impairment()));
    p.register("moveIntent", (e, n, m) -> e.with(MoveIntent.class, new MoveIntent()));
    return p;
  }

  /** Registers (or overrides) the parser for a component key. */
  public ComponentParsers register(String key, ComponentParser parser) {
    parsers.put(Objects.requireNonNull(key, "key"), Objects.requireNonNull(parser, "parser"));
    return this;
  }

  /** Attaches the component for {@code key}, failing fast if no parser is registered for it. */
  void attach(WorldEntity e, String key, JsonNode node, MaterialRegistry materials, String templateName) {
    ComponentParser parser = parsers.get(key);
    if (parser == null) {
      throw new IllegalArgumentException(
          "template '" + templateName + "': unknown component key '" + key + "'");
    }
    parser.attach(e, node, materials);
  }

  static float f(JsonNode n, String field, float dflt) {
    return (float) n.path(field).asDouble(dflt);
  }

  static Set<String> strings(JsonNode array) {
    Set<String> out = new HashSet<>();
    for (JsonNode item : array) out.add(item.asText());
    return out;
  }
}
