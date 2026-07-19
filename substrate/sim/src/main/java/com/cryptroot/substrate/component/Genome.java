package com.cryptroot.substrate.component;

import com.cryptroot.core.world.EntityComponent;
import java.util.HashMap;
import java.util.Map;

/**
 * The heritable substrate, separate from phenotype. Each gene id maps to two allele values (one
 * inherited from each parent, or both equal for a template-authored founder). {@link
 * com.cryptroot.substrate.system.ExpressionSystem} reads this every tick and writes phenotype
 * values into whatever existing component a gene's data-defined rule targets \u2014 a gene can sit
 * here fully carried and never expressed (the "neither" case) until a later generation's combined
 * alleles clear a threshold.
 */
public final class Genome implements EntityComponent {
  public final Map<String, float[]> genes = new HashMap<>();

  public void set(String geneId, float alleleA, float alleleB) {
    genes.put(geneId, new float[] {alleleA, alleleB});
  }
}
