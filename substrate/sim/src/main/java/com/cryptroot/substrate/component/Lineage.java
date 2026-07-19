package com.cryptroot.substrate.component;

import com.cryptroot.core.world.EntityComponent;

/**
 * Parentage for causality tracing (and, later, founder-effect/inbreeding hooks). Ids are opaque
 * numeric identifiers, not species lookups \u2014 recorded so "why does this entity fear both fire
 * and water" is traceable back through generations, not just inferable from the current tick.
 */
public final class Lineage implements EntityComponent {
  public final long parentAId;
  public final long parentBId;
  public final int generation;

  public Lineage(long parentAId, long parentBId, int generation) {
    this.parentAId = parentAId;
    this.parentBId = parentBId;
    this.generation = generation;
  }
}
