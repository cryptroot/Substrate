package com.cryptroot.substrate.component;

import com.cryptroot.core.world.EntityComponent;

/**
 * Reproductive readiness: a per-entity cooldown length and how much of it remains. Combined with
 * {@link Age}'s maturity gate (component composition, not a species check) to decide eligibility
 * in {@code CourtshipSystem}/{@code ReproductionSystem}.
 */
public final class Fertility implements EntityComponent {
  public final float cooldownTicks;
  public float cooldownRemaining;

  public Fertility(float cooldownTicks) {
    this.cooldownTicks = cooldownTicks;
  }

  public boolean ready() {
    return cooldownRemaining <= 0;
  }
}
