package com.cryptroot.substrate.tick;

import com.cryptroot.substrate.world.SimWorld;

/**
 * A generic simulator. Implementations query {@link SimWorld#entitiesWith} by component
 * composition only — never by entity kind, name, species, or tag. See CLAUDE.md.
 */
public interface SimSystem {

  /** Stable name used in causality records. */
  default String systemName() {
    return getClass().getSimpleName();
  }

  void tick(SimWorld world, long tick);
}
