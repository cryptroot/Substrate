package com.cryptroot.substrate.component;

import com.cryptroot.core.world.EntityComponent;

/**
 * Stable numeric identity for causality logging ONLY. The template name is retained purely so log
 * lines are human-readable; no system may read it to decide behavior (enforced by
 * NoIdentityGateTest).
 */
public final class Identity implements EntityComponent {
  private final long id;
  private final String templateName;

  public Identity(long id, String templateName) {
    this.id = id;
    this.templateName = templateName == null ? "?" : templateName;
  }

  public long id() {
    return id;
  }

  public String subject() {
    return "entity:" + id + "(" + templateName + ")";
  }

  /** The data bundle this entity was instantiated from. Used only for log readability and to
   * look up a {@link com.cryptroot.substrate.template.TemplateRegistry} entry when constructing
   * offspring generically \u2014 never read to branch system behavior. */
  public String origin() {
    return templateName;
  }
}
