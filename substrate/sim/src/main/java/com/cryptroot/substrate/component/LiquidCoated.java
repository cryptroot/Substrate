package com.cryptroot.substrate.component;

import com.cryptroot.core.world.EntityComponent;
import java.util.HashMap;
import java.util.Map;

/**
 * Liquid residue physically on a body, split into two regions: the general body coating and the
 * mouth. Grooming moves body → mouth; ingestion moves mouth → digestive. Keyed by material id.
 */
public final class LiquidCoated implements EntityComponent {
  public final Map<Integer, Float> body = new HashMap<>();
  public final Map<Integer, Float> mouth = new HashMap<>();

  public float bodyTotal() {
    float t = 0;
    for (float v : body.values()) t += v;
    return t;
  }

  public static void addTo(Map<Integer, Float> region, int materialId, float amount) {
    if (amount <= 0) return;
    region.merge(materialId, amount, Float::sum);
  }
}
