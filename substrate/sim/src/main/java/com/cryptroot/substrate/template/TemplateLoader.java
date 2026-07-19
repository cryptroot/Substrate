package com.cryptroot.substrate.template;

import com.cryptroot.core.world.WorldEntity;
import com.cryptroot.substrate.component.GridPosition;
import com.cryptroot.substrate.material.MaterialRegistry;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

/**
 * Builds entities from JSON templates that are pure component bundles with numeric values.
 * Species differ by numbers, never by code paths — adding a species is adding a file, not a
 * class. Component construction is delegated to a {@link ComponentParsers} registry, so adding a
 * component type is registering a parser, not editing this loader.
 */
public final class TemplateLoader {

  private final MaterialRegistry materials;
  private final ComponentParsers parsers;

  public TemplateLoader(MaterialRegistry materials) {
    this(materials, ComponentParsers.defaults());
  }

  public TemplateLoader(MaterialRegistry materials, ComponentParsers parsers) {
    this.materials = Objects.requireNonNull(materials);
    this.parsers = Objects.requireNonNull(parsers);
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
    return new Template(name, comps, materials, parsers);
  }

  /** A reusable entity blueprint. {@link #instantiate} builds a fresh entity each call. */
  public static final class Template {
    private final String name;
    private final JsonNode comps;
    private final MaterialRegistry materials;
    private final ComponentParsers parsers;

    private Template(
        String name, JsonNode comps, MaterialRegistry materials, ComponentParsers parsers) {
      this.name = name;
      this.comps = comps;
      this.materials = materials;
      this.parsers = parsers;
    }

    public String name() {
      return name;
    }

    public WorldEntity instantiate(int col, int row) {
      WorldEntity e = new WorldEntity().with(GridPosition.class, new GridPosition(col, row));
      Iterator<Map.Entry<String, JsonNode>> it = comps.fields();
      while (it.hasNext()) {
        Map.Entry<String, JsonNode> entry = it.next();
        parsers.attach(e, entry.getKey(), entry.getValue(), materials, name);
      }
      return e;
    }
  }
}
