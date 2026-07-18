package com.cryptroot.substrate.component;

import com.cryptroot.core.world.EntityComponent;

/** Physical integrity. Pain threshold is a temperature, not a flag. */
public final class Health implements EntityComponent {
  public float hp;
  public final float painThresholdTemperature;

  public Health(float hp, float painThresholdTemperature) {
    this.hp = hp;
    this.painThresholdTemperature = painThresholdTemperature;
  }
}
