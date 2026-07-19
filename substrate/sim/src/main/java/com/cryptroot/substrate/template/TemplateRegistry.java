package com.cryptroot.substrate.template;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Name \u2192 {@link TemplateLoader.Template} lookup, so a system can re-instantiate a fresh entity
 * from the same data bundle a parent came from (used by {@code ReproductionSystem} to build
 * offspring generically from an existing component bundle). Whoever assembles the
 * {@code SimWorld} (harness/tests) is responsible for registering every template it spawns from;
 * a name with no registered template is a data gap, not a species check, and callers must fail
 * soft on a miss.
 */
public final class TemplateRegistry {

  private final Map<String, TemplateLoader.Template> byName = new HashMap<>();

  public static TemplateRegistry empty() {
    return new TemplateRegistry();
  }

  public TemplateRegistry register(TemplateLoader.Template template) {
    Objects.requireNonNull(template, "template");
    byName.put(template.name(), template);
    return this;
  }

  /** The template registered under {@code name}, or {@code null} if none (fail soft & log). */
  public TemplateLoader.Template get(String name) {
    return byName.get(name);
  }
}
