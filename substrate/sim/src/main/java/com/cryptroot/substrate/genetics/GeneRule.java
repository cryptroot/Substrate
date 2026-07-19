package com.cryptroot.substrate.genetics;

/**
 * How a gene's two alleles combine into one phenotype value. The rule alone \u2014 not any
 * per-trait special-casing \u2014 is what produces "both", "the mix", or "neither".
 */
public enum GeneRule {
  /** Average of the two alleles \u2014 a blended, generalized outcome. */
  BLEND,
  /** The stronger of the two alleles \u2014 lets unrelated inherited traits both express fully. */
  MAX,
  /** Combined value only manifests once it clears {@link GeneDefinition#threshold()}; below it,
   * the phenotype is silently carried and not written \u2014 the "neither, for now" case. */
  THRESHOLD
}
