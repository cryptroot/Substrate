package com.cryptroot.substrate.component;

import com.cryptroot.core.world.EntityComponent;

/**
 * Mirrors {@link MoveIntent}: a cognition-phase behavior system ({@code CourtshipSystem}) sets
 * the target partner's numeric id here, and a later-phase system ({@code ReproductionSystem})
 * resolves it the same tick. Re-evaluated fresh every tick, same as {@code MoveIntent} \u2014
 * there is no persisted "engagement".
 */
public final class ReproduceIntent implements EntityComponent {
  public static final long NONE = -1;

  public long partnerId = NONE;

  public void clear() {
    partnerId = NONE;
  }
}
