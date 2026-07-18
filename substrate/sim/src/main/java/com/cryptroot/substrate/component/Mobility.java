package com.cryptroot.substrate.component;

import com.cryptroot.core.world.EntityComponent;
import java.util.HashMap;
import java.util.Map;

/**
 * Capacity for locomotion. Terrain penalties are keyed by tile material name → speed multiplier
 * (data-driven; resolved to ids at template load).
 */
public final class Mobility implements EntityComponent {
  public final float speed;
  public final Map<Integer, Float> terrainPenalty = new HashMap<>();

  public Mobility(float speed) {
    this.speed = speed;
  }
}
