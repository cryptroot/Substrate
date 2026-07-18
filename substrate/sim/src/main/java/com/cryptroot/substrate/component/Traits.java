package com.cryptroot.substrate.component;

import com.cryptroot.core.world.EntityComponent;
import java.util.HashSet;
import java.util.Set;

/**
 * Physical/behavioral trait descriptors carried by an entity (e.g. {@code CANINE}, {@code
 * BURNING}). Traits describe what a thing physically is — systems may add or remove them as
 * physical state changes (CombustionSystem adds BURNING). They are never permissions.
 */
public final class Traits implements EntityComponent {
  public final Set<String> traits = new HashSet<>();

  public Traits(Set<String> initial) {
    if (initial != null) traits.addAll(initial);
  }
}
