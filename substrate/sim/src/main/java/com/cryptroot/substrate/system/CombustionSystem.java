package com.cryptroot.substrate.system;

import com.cryptroot.core.world.WorldEntity;
import com.cryptroot.substrate.component.Flammable;
import com.cryptroot.substrate.component.GridPosition;
import com.cryptroot.substrate.component.Thermal;
import com.cryptroot.substrate.component.Traits;
import com.cryptroot.substrate.material.Material;
import com.cryptroot.substrate.substrate.TileSubstrate;
import com.cryptroot.substrate.tick.SimSystem;
import com.cryptroot.substrate.world.SimWorld;

/**
 * Anything above its ignition point with fuel remaining burns: fuel is consumed, heat is emitted
 * into the burner and its tile. Applies identically to tiles and to entities with Flammable +
 * Thermal. Fuel depletion + ambient relaxation (ThermalDiffusionSystem) are the damping terms —
 * no entity is ever excluded from burning.
 *
 * <p>While an entity burns, the physical trait {@code BURNING} is present on its Traits (if it has
 * any), so e.g. the fear system can react to a burning thing — including the burner itself.
 */
public final class CombustionSystem implements SimSystem {

  /** Physical state descriptor, not a permission. */
  public static final String BURNING_TRAIT = "BURNING";

  @Override
  public void tick(SimWorld w, long tick) {
    burnTiles(w, tick);
    burnEntities(w, tick);
  }

  private void burnTiles(SimWorld w, long tick) {
    TileSubstrate tiles = w.tiles();
    for (int r = 0; r < tiles.rows(); r++) {
      for (int c = 0; c < tiles.columns(); c++) {
        Material m = w.materials().get(tiles.materialId(c, r));
        float temp = tiles.temperature(c, r);
        float fuel = tiles.fuel(c, r);
        if (fuel <= 0 || temp < m.ignitionPoint()) continue;
        float burned = Math.min(fuel, m.burnRate());
        tiles.setFuel(c, r, fuel - burned);
        float newTemp = temp + burned * m.heatOutput();
        tiles.setTemperature(c, r, newTemp);
        // Flame radiates directly into adjacent tiles — this is how fire physically spreads.
        float radiated = burned * m.heatOutput() * w.config().combustionRadiateFraction();
        if (radiated > 0) {
          radiate(tiles, c - 1, r, radiated);
          radiate(tiles, c + 1, r, radiated);
          radiate(tiles, c, r - 1, radiated);
          radiate(tiles, c, r + 1, radiated);
        }
        w.log()
            .record(
                tick,
                "ENVIRONMENT",
                systemName(),
                tiles.subject(c, r),
                "fuel/temperature",
                fuel + "/" + temp,
                (fuel - burned) + "/" + newTemp);
      }
    }
  }

  private static void radiate(TileSubstrate tiles, int c, int r, float heat) {
    if (tiles.inBounds(c, r)) tiles.setTemperature(c, r, tiles.temperature(c, r) + heat);
  }

  private void burnEntities(SimWorld w, long tick) {
    TileSubstrate tiles = w.tiles();
    for (WorldEntity e : w.entitiesWith(Flammable.class, Thermal.class)) {
      Flammable f = e.get(Flammable.class).orElseThrow();
      Thermal t = e.get(Thermal.class).orElseThrow();
      boolean burning = f.fuelRemaining > 0 && t.temperature >= f.ignitionPoint;
      if (burning) {
        float burned = Math.min(f.fuelRemaining, f.burnRate);
        float beforeFuel = f.fuelRemaining;
        float beforeTemp = t.temperature;
        f.fuelRemaining -= burned;
        t.temperature += burned * f.heatOutput;
        // Radiate half the output into the tile underfoot, if on the substrate.
        e.get(GridPosition.class)
            .ifPresent(
                pos -> {
                  if (tiles.inBounds(pos.col, pos.row)) {
                    tiles.setTemperature(
                        pos.col,
                        pos.row,
                        tiles.temperature(pos.col, pos.row) + burned * f.heatOutput * 0.5f);
                  }
                });
        w.log()
            .record(
                tick,
                "ENVIRONMENT",
                systemName(),
                w.subjectOf(e),
                "Flammable.fuel/Thermal.temperature",
                beforeFuel + "/" + beforeTemp,
                f.fuelRemaining + "/" + t.temperature);
      }
      // Reflect physical burning state as a trait for whatever carries Traits.
      e.get(Traits.class)
          .ifPresent(
              traits -> {
                boolean had = traits.traits.contains(BURNING_TRAIT);
                if (burning && !had) {
                  traits.traits.add(BURNING_TRAIT);
                  w.log()
                      .record(
                          tick, "ENVIRONMENT", systemName(), w.subjectOf(e), "Traits", "-",
                          "+BURNING");
                } else if (!burning && had) {
                  traits.traits.remove(BURNING_TRAIT);
                  w.log()
                      .record(
                          tick, "ENVIRONMENT", systemName(), w.subjectOf(e), "Traits", "+BURNING",
                          "-");
                }
              });
    }
  }
}
