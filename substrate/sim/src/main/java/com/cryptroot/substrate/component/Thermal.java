package com.cryptroot.substrate.component;

import com.cryptroot.core.world.EntityComponent;

/** Physical heat state: anything with a temperature. */
public final class Thermal implements EntityComponent {
  public float temperature;
  public final float heatCapacity;
  public final float conductivity;

  public Thermal(float temperature, float heatCapacity, float conductivity) {
    this.temperature = temperature;
    this.heatCapacity = Math.max(0.001f, heatCapacity);
    this.conductivity = conductivity;
  }
}
