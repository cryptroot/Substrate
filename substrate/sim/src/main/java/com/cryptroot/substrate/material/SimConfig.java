package com.cryptroot.substrate.material;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/**
 * Global physics tunables loaded from {@code data/simconfig.json}. These are simulation-wide
 * constants (diffusion rates, radii) — per-material and per-entity thresholds live in materials
 * and templates respectively. Nothing here gates by entity kind.
 *
 * @param ambientTemperature temperature tiles relax toward
 * @param ambientCoupling 0..1 share of the gap to ambient closed per tick
 * @param tileDiffusionRate 0..1 share of a tile↔tile temperature delta exchanged per tick
 * @param quenchHeatPerDepth degrees removed from a tile per unit of liquid depth per tick
 * @param combustionRadiateFraction share of a burning tile's heat output radiated to each neighbor
 * @param coatingPickupRate liquid depth transferred onto a standing entity's coating per tick
 * @param fearRadius chebyshev tile radius scanned by the fear system
 * @param fearGainPerSource fear added per feared trait encountered per tick (before distance falloff)
 * @param drunkCourageFactor how much impairment raises the effective fear threshold
 * @param impairmentSpeedFactor how much impairment slows movement (1.0 = fully at level 1)
 * @param staggerChancePerImpairment probability per impairment level of a random stagger step
 * @param heatDamageFactor hp lost per degree above an entity's pain threshold per tick
 * @param matingRadius chebyshev tile radius scanned by the courtship system for a partner
 * @param mutationRate 0..1 probability per inherited allele of a mutation jitter being applied
 * @param mutationJitter max magnitude of a mutation's random offset to an allele
 */
public record SimConfig(
    float ambientTemperature,
    float ambientCoupling,
    float tileDiffusionRate,
    float quenchHeatPerDepth,
    float combustionRadiateFraction,
    float coatingPickupRate,
    int fearRadius,
    float fearGainPerSource,
    float drunkCourageFactor,
    float impairmentSpeedFactor,
    float staggerChancePerImpairment,
    float heatDamageFactor,
    int matingRadius,
    float mutationRate,
    float mutationJitter) {

  public static SimConfig loadFromResource(String resourcePath) {
    Objects.requireNonNull(resourcePath, "resourcePath must not be null");
    try (InputStream in = SimConfig.class.getResourceAsStream(resourcePath)) {
      if (in == null) throw new IllegalArgumentException("resource not found: " + resourcePath);
      return load(new ObjectMapper().readTree(in));
    } catch (IOException e) {
      throw new IllegalStateException("failed to load sim config from " + resourcePath, e);
    }
  }

  public static SimConfig load(JsonNode n) {
    return new SimConfig(
        (float) n.path("ambientTemperature").asDouble(20),
        (float) n.path("ambientCoupling").asDouble(0.02),
        (float) n.path("tileDiffusionRate").asDouble(0.15),
        (float) n.path("quenchHeatPerDepth").asDouble(40),
        (float) n.path("combustionRadiateFraction").asDouble(0.25),
        (float) n.path("coatingPickupRate").asDouble(0.05),
        n.path("fearRadius").asInt(4),
        (float) n.path("fearGainPerSource").asDouble(3),
        (float) n.path("drunkCourageFactor").asDouble(0.5),
        (float) n.path("impairmentSpeedFactor").asDouble(0.6),
        (float) n.path("staggerChancePerImpairment").asDouble(0.3),
        (float) n.path("heatDamageFactor").asDouble(0.1),
        n.path("matingRadius").asInt(3),
        (float) n.path("mutationRate").asDouble(0.05),
        (float) n.path("mutationJitter").asDouble(0.1));
  }
}
