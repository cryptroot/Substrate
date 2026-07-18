package com.cryptroot.substrate.component;

import com.cryptroot.core.world.EntityComponent;

/**
 * The entity's current locomotive urge, written by cognition-phase systems (fear) and consumed by
 * the movement system. Direction is a unit-ish vector in tile space; urgency scales priority.
 */
public final class MoveIntent implements EntityComponent {
  public float dx;
  public float dy;
  public float urgency;

  public void clear() {
    dx = 0;
    dy = 0;
    urgency = 0;
  }
}
