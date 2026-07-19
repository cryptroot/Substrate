package com.cryptroot.substrate.genetics;

/**
 * Data-defined description of one gene: which combination rule to use and which phenotype target
 * (an {@link ExpressionTargets} key) it drives. {@code threshold} only matters for {@link
 * GeneRule#THRESHOLD}; {@code param} is an optional extra string a target may need (e.g. which
 * trait tag to add).
 *
 * @param id gene key, matched against {@link com.cryptroot.substrate.component.Genome} entries
 * @param rule how the two alleles combine into a phenotype value
 * @param target key into {@link ExpressionTargets} naming which component field this gene drives
 * @param param optional target-specific parameter (e.g. a trait name)
 * @param threshold cutoff used only by {@link GeneRule#THRESHOLD}
 */
public record GeneDefinition(String id, GeneRule rule, String target, String param, float threshold) {}
