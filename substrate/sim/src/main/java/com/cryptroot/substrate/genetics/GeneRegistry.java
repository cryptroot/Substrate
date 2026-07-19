package com.cryptroot.substrate.genetics;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Loads gene definitions from {@code data/genes.json}. Adding a gene is adding a JSON entry, never
 * a code path \u2014 {@link com.cryptroot.substrate.system.ExpressionSystem} is blind to which
 * genes exist, same as every other generic system here is blind to entity kind.
 */
public final class GeneRegistry {

  private final Map<String, GeneDefinition> genes;

  private GeneRegistry(Map<String, GeneDefinition> genes) {
    this.genes = genes;
  }

  public static GeneRegistry empty() {
    return new GeneRegistry(Map.of());
  }

  public static GeneRegistry loadFromResource(String resourcePath) {
    Objects.requireNonNull(resourcePath, "resourcePath must not be null");
    try (InputStream in = GeneRegistry.class.getResourceAsStream(resourcePath)) {
      if (in == null) throw new IllegalArgumentException("resource not found: " + resourcePath);
      return load(new ObjectMapper().readTree(in));
    } catch (IOException e) {
      throw new IllegalStateException("failed to load gene registry from " + resourcePath, e);
    }
  }

  public static GeneRegistry load(JsonNode root) {
    Map<String, GeneDefinition> genes = new HashMap<>();
    for (JsonNode g : root.path("genes")) {
      String id = g.path("id").asText();
      if (id.isEmpty()) throw new IllegalArgumentException("gene missing id: " + g);
      GeneRule rule = GeneRule.valueOf(g.path("rule").asText());
      String target = g.path("target").asText();
      if (target.isEmpty()) throw new IllegalArgumentException("gene '" + id + "' missing target");
      String param = g.path("param").asText(null);
      float threshold = (float) g.path("threshold").asDouble(0);
      if (genes.put(id, new GeneDefinition(id, rule, target, param, threshold)) != null) {
        throw new IllegalArgumentException("duplicate gene id: " + id);
      }
    }
    return new GeneRegistry(genes);
  }

  /** Definition for {@code geneId}, or {@code null} if unknown (callers must fail soft & log). */
  public GeneDefinition get(String geneId) {
    return genes.get(geneId);
  }
}
