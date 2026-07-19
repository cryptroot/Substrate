package com.cryptroot.substrate.tick;

/**
 * The fixed, documented tick phase order. Interaction bugs are usually ordering bugs; keeping the
 * order explicit means "why did this happen twice / not at all" is traceable to a phase.
 */
public enum Phase {
  GENETICS,
  ENVIRONMENT,
  CONTACT,
  INGESTION,
  METABOLISM,
  COGNITION,
  MOVEMENT,
  LIFECYCLE
}
