package com.cryptroot.substrate.component;

import com.cryptroot.core.world.EntityComponent;

/**
 * Can combust. Thresholds, not booleans: {@code ignitionPoint} leaves room for smoldering and
 * near-misses. Fuel depletion is the built-in damping term against infinite fire.
 */
public final class Flammable implements EntityComponent {
  /** Mutable: a heritable gene may re-express this every tick; see ExpressionSystem. */
  public float ignitionPoint;
  public final float burnRate;
  public final float heatOutput;
  public float fuelRemaining;

  public Flammable(float ignitionPoint, float burnRate, float heatOutput, float fuelRemaining) {
    this.ignitionPoint = ignitionPoint;
    this.burnRate = burnRate;
    this.heatOutput = heatOutput;
    this.fuelRemaining = fuelRemaining;
  }
}
