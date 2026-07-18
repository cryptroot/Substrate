package com.cryptroot.substrate.material;

/**
 * A data-driven material definition. Every threshold is a number, never a boolean — {@code
 * intoxicatingPotency == 0} means "not intoxicating", any positive value is a gradient. All values
 * come from {@code data/materials.json}, never from code.
 *
 * @param id dense registry index
 * @param name unique material name (data key; never used to gate systems)
 * @param ignitionPoint temperature at which the material combusts; {@code Infinity} = inert
 * @param burnRate fuel consumed per tick while burning
 * @param heatOutput degrees added per unit of fuel burned
 * @param heatCapacity resistance to temperature change (higher = slower to heat/cool)
 * @param conductivity 0..1 share of temperature delta exchanged with neighbors per tick
 * @param fuel initial combustible fuel per tile of this material
 * @param intoxicatingPotency impairment per unit ingested (0 = none)
 * @param toxicity damage per unit ingested per tick (0 = harmless)
 * @param viscosity 0..1 fraction of a liquid level difference that flows per tick
 * @param evaporationRate liquid depth lost per tick per 100 degrees of tile temperature
 */
public record Material(
    int id,
    String name,
    float ignitionPoint,
    float burnRate,
    float heatOutput,
    float heatCapacity,
    float conductivity,
    float fuel,
    float intoxicatingPotency,
    float toxicity,
    float viscosity,
    float evaporationRate) {}
