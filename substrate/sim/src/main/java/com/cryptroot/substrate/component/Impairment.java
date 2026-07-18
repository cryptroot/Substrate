package com.cryptroot.substrate.component;

import com.cryptroot.core.world.EntityComponent;

/**
 * Graded impairment (0 = sober). A separate component so ANY system (intoxication, heatstroke,
 * toxins) can contribute to it — that multiplication is the point.
 */
public final class Impairment implements EntityComponent {
  public float level;
}
