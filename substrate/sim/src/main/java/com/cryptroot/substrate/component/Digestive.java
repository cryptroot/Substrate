package com.cryptroot.substrate.component;

import com.cryptroot.core.world.EntityComponent;
import java.util.HashMap;
import java.util.Map;

/** Digestive tract contents and physiology. Keyed by material id. */
public final class Digestive implements EntityComponent {
  public final Map<Integer, Float> contents = new HashMap<>();
  /** Mutable: a heritable gene may re-express this every tick; see ExpressionSystem. */
  public float tolerance;
  public final float metabolismRate;

  public Digestive(float tolerance, float metabolismRate) {
    this.tolerance = Math.max(0.001f, tolerance);
    this.metabolismRate = metabolismRate;
  }
}
