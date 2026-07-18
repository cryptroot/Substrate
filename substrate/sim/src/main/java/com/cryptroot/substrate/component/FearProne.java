package com.cryptroot.substrate.component;

import com.cryptroot.core.world.EntityComponent;
import java.util.HashSet;
import java.util.Set;

/**
 * Fear physiology: a threshold, a current level, a decay rate, and the trait tags this entity
 * fears. The fear system deliberately does NOT exclude the entity's own traits from the scan —
 * a canine-fearing entity with a canine trait frightens itself. That is the product.
 */
public final class FearProne implements EntityComponent {
  public final float fearThreshold;
  public final float fearDecayRate;
  public float currentFear;
  public final Set<String> fearedTraits = new HashSet<>();

  public FearProne(float fearThreshold, float fearDecayRate, Set<String> fearedTraits) {
    this.fearThreshold = fearThreshold;
    this.fearDecayRate = fearDecayRate;
    if (fearedTraits != null) this.fearedTraits.addAll(fearedTraits);
  }
}
