package com.cryptroot.substrate.genetics;

import com.cryptroot.core.world.WorldEntity;
import com.cryptroot.substrate.component.Digestive;
import com.cryptroot.substrate.component.FearProne;
import com.cryptroot.substrate.component.Flammable;
import com.cryptroot.substrate.component.Mobility;
import com.cryptroot.substrate.component.Traits;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Open registry of "how to write one phenotype value onto whatever component it drives". This is
 * the same register-instead-of-switch shape as {@code ComponentParsers}: a new phenotype target
 * (a new component field genetics should drive) is a call to {@link #register}, never a new
 * branch in {@code ExpressionSystem}.
 */
public final class ExpressionTargets {

  /** Writes {@code phenotype} onto {@code e}; returns the before/after strings if it changed a
   * value, or empty if the entity lacks the target component (fail soft, not an error). */
  @FunctionalInterface
  public interface Target {
    Optional<String[]> apply(WorldEntity e, float phenotype, String param);
  }

  private final Map<String, Target> targets = new HashMap<>();

  public static ExpressionTargets defaults() {
    ExpressionTargets t = new ExpressionTargets();
    t.register(
        "fearThreshold",
        (e, v, param) ->
            e.get(FearProne.class)
                .map(
                    f -> {
                      float before = f.fearThreshold;
                      f.fearThreshold = v;
                      return new String[] {String.valueOf(before), String.valueOf(v)};
                    }));
    t.register(
        "fearedTrait",
        (e, v, param) ->
            e.get(FearProne.class)
                .map(
                    f -> {
                      boolean had = f.fearedTraits.contains(param);
                      boolean expressed = v > 0;
                      if (expressed == had) return null;
                      if (expressed) f.fearedTraits.add(param);
                      else f.fearedTraits.remove(param);
                      return new String[] {String.valueOf(had), String.valueOf(expressed)};
                    }));
    t.register(
        "flammableIgnitionPoint",
        (e, v, param) ->
            e.get(Flammable.class)
                .map(
                    f -> {
                      float before = f.ignitionPoint;
                      f.ignitionPoint = v;
                      return new String[] {String.valueOf(before), String.valueOf(v)};
                    }));
    t.register(
        "digestiveTolerance",
        (e, v, param) ->
            e.get(Digestive.class)
                .map(
                    d -> {
                      float before = d.tolerance;
                      d.tolerance = Math.max(0.001f, v);
                      return new String[] {String.valueOf(before), String.valueOf(d.tolerance)};
                    }));
    t.register(
        "mobilitySpeed",
        (e, v, param) ->
            e.get(Mobility.class)
                .map(
                    m -> {
                      float before = m.speed;
                      m.speed = Math.max(0f, v);
                      return new String[] {String.valueOf(before), String.valueOf(m.speed)};
                    }));
    t.register(
        "trait",
        (e, v, param) ->
            e.get(Traits.class)
                .map(
                    traits -> {
                      boolean had = traits.traits.contains(param);
                      boolean expressed = v > 0;
                      if (expressed == had) return null;
                      if (expressed) traits.traits.add(param);
                      else traits.traits.remove(param);
                      return new String[] {String.valueOf(had), String.valueOf(expressed)};
                    }));
    return t;
  }

  public ExpressionTargets register(String key, Target target) {
    targets.put(Objects.requireNonNull(key, "key"), Objects.requireNonNull(target, "target"));
    return this;
  }

  /** Target for {@code key}, or {@code null} if unknown (callers must fail soft & log). */
  public Target get(String key) {
    return targets.get(key);
  }
}
