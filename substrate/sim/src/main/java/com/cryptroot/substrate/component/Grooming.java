package com.cryptroot.substrate.component;

import com.cryptroot.core.world.EntityComponent;

/**
 * Self-cleaning behavior: transfers body coating toward the mouth. A behavioral property any
 * entity may carry, not a species marker.
 */
public final class Grooming implements EntityComponent {
  public final float rate;
  public final float triggerAmount;

  public Grooming(float rate, float triggerAmount) {
    this.rate = rate;
    this.triggerAmount = triggerAmount;
  }
}
