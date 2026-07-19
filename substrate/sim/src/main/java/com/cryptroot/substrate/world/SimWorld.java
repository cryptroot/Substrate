package com.cryptroot.substrate.world;

import com.cryptroot.core.world.EntityComponent;
import com.cryptroot.core.world.World;
import com.cryptroot.core.world.WorldEntity;
import com.cryptroot.substrate.component.Identity;
import com.cryptroot.substrate.genetics.GeneRegistry;
import com.cryptroot.substrate.log.CausalityLog;
import com.cryptroot.substrate.material.MaterialRegistry;
import com.cryptroot.substrate.material.SimConfig;
import com.cryptroot.substrate.substrate.TileSubstrate;
import com.cryptroot.substrate.template.TemplateRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.SplittableRandom;

/**
 * Everything a system may touch: the entity world, the tile substrate, materials, config, the
 * causality log, and deterministic randomness. Iteration order is world insertion order, and RNG
 * streams are derived from (seed, tick, system) — same seed, same run, byte for byte.
 */
public final class SimWorld {

  private final World world = new World();
  private final TileSubstrate tiles;
  private final MaterialRegistry materials;
  private final SimConfig config;
  private final GeneRegistry genes;
  private final TemplateRegistry templates;
  private final CausalityLog log;
  private final long seed;
  private long nextEntityId = 0;

  public SimWorld(TileSubstrate tiles, MaterialRegistry materials, SimConfig config, long seed) {
    this(tiles, materials, config, seed, GeneRegistry.empty(), TemplateRegistry.empty());
  }

  public SimWorld(
      TileSubstrate tiles,
      MaterialRegistry materials,
      SimConfig config,
      long seed,
      GeneRegistry genes,
      TemplateRegistry templates) {
    this.tiles = Objects.requireNonNull(tiles);
    this.materials = Objects.requireNonNull(materials);
    this.config = Objects.requireNonNull(config);
    this.genes = Objects.requireNonNull(genes);
    this.templates = Objects.requireNonNull(templates);
    this.log = new CausalityLog();
    this.seed = seed;
  }

  public World world() {
    return world;
  }

  public TileSubstrate tiles() {
    return tiles;
  }

  public MaterialRegistry materials() {
    return materials;
  }

  public SimConfig config() {
    return config;
  }

  public GeneRegistry genes() {
    return genes;
  }

  public TemplateRegistry templates() {
    return templates;
  }

  public CausalityLog log() {
    return log;
  }

  /** Adds the entity, assigning an {@link Identity} for logging if it lacks one. */
  public WorldEntity spawn(WorldEntity entity, String templateName) {
    if (!entity.has(Identity.class)) {
      entity.with(Identity.class, new Identity(nextEntityId++, templateName));
    }
    return world.add(entity);
  }

  /**
   * Queues the entity for addition on the next tick, assigning an {@link Identity} for logging if
   * it lacks one. Safe to call from inside a system's {@code tick()} (e.g. {@code
   * ReproductionSystem} spawning offspring).
   */
  public WorldEntity queueSpawn(WorldEntity entity, String templateName) {
    if (!entity.has(Identity.class)) {
      entity.with(Identity.class, new Identity(nextEntityId++, templateName));
    }
    return world.queueAdd(entity);
  }

  /** Log subject string for an entity; falls back for entities without Identity. */
  public String subjectOf(WorldEntity e) {
    return e.get(Identity.class).map(Identity::subject).orElse("entity:?");
  }

  /**
   * Stable numeric id for an entity, or {@code -1} if it has none. Exists so systems can trace
   * lineage/dedupe pairings without importing {@link Identity} themselves (enforced by
   * NoIdentityGateTest — systems may not even reference {@code Identity.class}).
   */
  public long idOf(WorldEntity e) {
    return e.get(Identity.class).map(Identity::id).orElse(-1L);
  }

  /**
   * Which data bundle {@code e} was instantiated from (or {@code null} if untracked). Lets a
   * system reinstantiate a fresh entity of the same shape via {@link #templates()} without ever
   * importing {@link Identity} itself.
   */
  public String originTemplateOf(WorldEntity e) {
    return e.get(Identity.class).map(Identity::origin).orElse(null);
  }

  /**
   * All entities carrying every listed component, in insertion order. This composition query is
   * the ONLY legitimate gate a system may apply.
   */
  @SafeVarargs
  public final List<WorldEntity> entitiesWith(Class<? extends EntityComponent>... types) {
    List<WorldEntity> out = new ArrayList<>();
    outer:
    for (WorldEntity e : world.entities()) {
      for (Class<? extends EntityComponent> t : types) {
        if (!e.has(t)) continue outer;
      }
      out.add(e);
    }
    return out;
  }

  /** Deterministic per-(tick, system) random stream. */
  public SplittableRandom random(long tick, String systemName) {
    return new SplittableRandom(seed ^ (tick * 0x9E3779B97F4A7C15L) ^ systemName.hashCode());
  }
}
