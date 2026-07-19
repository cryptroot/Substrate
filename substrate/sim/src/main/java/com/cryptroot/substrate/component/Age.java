package com.cryptroot.substrate.component;

import com.cryptroot.core.world.EntityComponent;

/**
 * Lifecycle clock: current age, the age at which {@code Fertility} becomes usable, and the age
 * at which the entity dies of old age. All three are plain numbers from template/offspring data,
 * never a species lookup table \u2014 required plumbing so reproduction doesn't produce an
 * ever-growing population.
 */
public final class Age implements EntityComponent {
  public float age;
  public final float maturityAge;
  public final float lifespan;

  public Age(float age, float maturityAge, float lifespan) {
    this.age = age;
    this.maturityAge = maturityAge;
    this.lifespan = lifespan;
  }
}
