# SUBSTRATE

*A simulation built to misbehave.*

## Design thesis

Most game simulations are built to prevent unintended interactions. Ours is built to permit them, on purpose, as the primary source of content.

Dwarf Fortress didn't get funny because someone wrote a joke. It got funny because a handful of generic systems — temperature, fluid flow, ingestion, fear, pathing — were built to operate on *properties* rather than on *named entities*, and then those systems were left free to compose. A cat groomed itself, found ethanol on its fur because grooming checks for "any liquid on this body," and got drunk because the drunkenness system never checked "is this drinker a dwarf." Nobody designed cat-drunkenness. Three unrelated, honestly-implemented systems collided, and the collision was the content.

This project takes that as an explicit design goal rather than an accident to route around. We are building Frankenstein's Monster on purpose: assemble honest, narrow, well-behaved simulation systems out of a shared component substrate, refuse to gate them by entity type, and let them collide. The patch notes are the roadmap.

## The core bet

**If every system operates on components (physical/behavioral properties) instead of on entity identity, then adding a new system multiplies against every existing system instead of just adding to the pile.** Ten narrow systems built this way don't give you ten features — they give you something close to the combinatorial space of ten systems interacting, most of which nobody explicitly designed. That combinatorial space is the game.

This only works if we resist the instinct — and it *will* be an instinct, repeatedly — to special-case things back to sanity. Every `if (entity.isDwarf())` we write is a door we've closed on an interaction we didn't foresee.

## Architectural principles

These are the rules the codebase should be able to be checked against. If a PR violates one, that's worth a conversation before merging, not an automatic block — sometimes a special case really is correct — but the default posture is suspicion of anything that narrows a system's scope by entity type.

### 1. Properties, not permissions

Components describe *what a thing physically or behaviorally is*, never *what kind of entity it's allowed to be*. `Flammable`, `Thermal`, `LiquidCoated`, `Digestive`, `FearProne`, `SocialBond` — these attach to whatever entity happens to have the relevant physical reality, full stop. A torch, a dwarf's beard, and a haystack are all just `Flammable` with different thresholds. Nothing in the component says "creature-only" or "item-only."

### 2. Systems are blind to entity kind

A system iterates over "everything with components X and Y" and never asks what the entity *is*. The ingestion system doesn't know about cats or dwarves — it knows about `LiquidCoated` + `Digestive`. The moment a system contains a switch on entity type or a name-based check, it has stopped being a generic simulator and started being a scripted behavior, and scripted behaviors don't compose with anything unforeseen.

```java
// Good — reaches for the property, doesn't care what has it
for (Entity e : world.with(LiquidCoated.class, Digestive.class)) { ... }

// Bad — this interaction can now only ever happen to dwarves,
// forever, no matter what else gets built later
if (entity instanceof Dwarf && entity.isDrinking()) { ... }
```

### 3. Shared substrate over specialized subsystems

Resist building a "cat AI module" and a separate "dwarf AI module." Build a `FearSystem`, a `GroomingSystem`, a `ThermalSystem` that run over the whole entity population. Species differentiate by which components they're instantiated with and what numeric thresholds those components carry — not by which code path executes for them.

### 4. Gradients over booleans

`isOnFire: boolean` gives you one bit of drama. `heat: float`, `ignitionPoint: float`, `combustionRate: float` gives you smoldering, slow catch, flash ignition, and "nearly caught fire but the rain saved it" — all for free, all from the same field being compared against different thresholds by different systems. Wherever a boolean is tempting, ask whether it's actually a threshold on a number, and if so, keep the number.

### 5. Fail soft, fail loud

An unforeseen interaction should produce an event, a log line, and maybe a corpse — never a crash and never silent nothing. If two systems fight over the same entity in the same tick, that's a bug in tick ordering, not a reason to firewall the systems apart from each other.

## V1 (shipped): horizontal composition within a tick

Keep the *world* small so the *interaction space* can be large. A flat 2D grid, a handful of creature templates, a handful of item/material types — the fun scales with how many components each thing has, not with map size or roster size.

**World:** flat 2D tile grid (`TileSubstrate`). Each tile carries a material, a temperature, and a liquid depth — same substrate idea applied to terrain as to creatures.

**Components** (`substrate/sim/.../component`): `Thermal`, `Flammable`, `LiquidCoated`, `Digestive`, `Impairment`, `FearProne`, `Traits`, `Mobility`, `Grooming`, `Health`, `MoveIntent`, `GridPosition`, `Identity` (numeric id + origin template name, read only for logging — never for behavior).

**Systems** (`substrate/sim/.../system`), each narrow and blind to entity type:

- `ThermalDiffusionSystem` — heat spreads between adjacent tiles and touching entities
- `FluidFlowSystem` — liquids flow across the tile grid (runs before combustion so water can quench before ignition checks)
- `CombustionSystem` — anything `Flammable` above its ignition point catches fire and heats its neighbors (tiles and entities alike)
- `GroomingSystem` — entities with `Grooming` + `LiquidCoated` move body coating to their mouth
- `IngestionSystem` — mouth-coated liquid moves into `Digestive`
- `IntoxicationSystem` — reads `Digestive` contents against material potency, produces graded `Impairment`
- `HealthSystem` — heat above pain threshold and toxic digestive contents damage `Health`; death is a log line and a despawn
- `FearSystem` — compares nearby entities' `Traits` against an entity's own `FearProne.fearedTraits`, **including its own traits** (no self-exclusion) — this is the werewolf-afraid-of-dogs case: a werewolf with `FearProne { fears: [CANINE] }` and its own `CANINE` trait frightens itself, on purpose
- `MovementSystem` — resolves `MoveIntent` (flee) or wanders; impairment slows movement and adds stagger

Species (`critter`, `werewolf`, `torch`) are pure JSON component bundles in `data/templates/`, loaded by `TemplateLoader` via an open `ComponentParsers` registry (register-a-parser, not a growing switch) — adding a species, or a component type, never touches system code.

## Guardrails: staying playable inside the chaos

Embracing weird interactions is not the same as embracing an unplayable game. A few load-bearing pieces of infrastructure make the difference between "delightfully deranged" and "incomprehensible":

**Causality logging.** Every state change a system makes should be able to answer "why did this happen" — which system touched what, reading what values, on what tick. DF's community mythology runs entirely on players being able to reconstruct "the cat got drunk because it groomed spilled booze off its fur." If your systems can't produce that trail, the emergent behavior is invisible instead of delightful. Log structurally (entity id, system name, component before/after, tick) so it can be queried later, not just printed.

**Thresholds as data, not code.** Ignition points, fear thresholds, intoxication tolerance — put these in data files, not constants buried in system logic. Tuning "how easily does everything catch fire" should be a config change, because you will be doing that constantly during playtesting.

**Fail-soft boundaries.** No system should be able to throw and take the simulation down. An entity with nonsensical component combinations (a `Digestive`-less entity somehow in `IngestionSystem`'s query, say) should be filtered out by the query itself, not crash a system that assumed it wouldn't see one.

**Curate, don't eliminate.** When playtesting turns up a weird interaction, the question isn't "is this a bug," it's "is this a *fun* bug." Fun weirdness (self-scaring werewolves) stays. Softlock weirdness (a fire that can never be extinguished because of a missing negative feedback loop) gets a damping term added to the *system*, not a special case added to the *entity*. Fix the physics, not the symptom.

**A tick order you can reason about.** Interaction bugs are often really ordering bugs — a tile floods before the drowning check runs, or fear resolves before flee-pathing reads it. Keep tick phase ordering explicit and documented (e.g., `ENVIRONMENT → INGESTION → METABOLISM → COGNITION → MOVEMENT`) so that when something happens twice or not at all, it's traceable to a phase, not a mystery.

## Worked example: designing *for* the collision

Take the target scenario — cats getting drunk from licking ethanol off their fur — and notice what we *don't* build:

- ❌ A `CatDrunkenness` component
- ❌ A check in the cat AI for "is there booze nearby"
- ❌ Any code that mentions "cat" and "ethanol" in the same file

What we do build instead:

1. `Ethanol` is a `Material` with an `intoxicating: true` flag and a potency value — same struct every other material uses.
2. Spilled liquid on the floor is tracked by `FluidFlowSystem`, no different from water or blood.
3. Any entity with a grooming/self-cleaning behavior runs through `GroomingSystem`, which checks the tile/body it's touching for `LiquidCoated` residue and transfers some onto the entity — this system doesn't know or care what species is running it.
4. `IngestionSystem` moves `LiquidCoated` residue on the mouth/fur into `Digestive` for any entity that has both components.
5. `IntoxicationSystem` reads `Digestive` contents each tick, checks material `intoxicating` flags against a tolerance value, and applies impairment.

Give the cat `LiquidCoated`-eligible fur, a grooming behavior, and a `Digestive` component with a low tolerance, and the rest is arithmetic. Nobody authored "the cat gets drunk." Five honestly-scoped systems did their jobs in sequence.

## V2 (shipped): vertical composition across generations

Where V1 collides unrelated systems *within* a tick, V2 adds the other axis: traits colliding *across generations*, so a lineage feels cursed rather than being a roster of independently-configured critters. Full design rationale lives in [README-v2.md](README-v2.md); this section documents what's actually built.

**The core mechanic is a genome layer separate from phenotype.** `Genome` holds two allele values per gene id (one per parent) — nothing more. `ExpressionSystem` runs first in every tick (`Phase.GENETICS`), reads each carried gene against a data-defined rule, and writes the resulting phenotype value into whichever existing component that gene drives. The system itself never knows what a gene "means":

- **`BLEND`** — phenotype = average of the two alleles (the mix)
- **`MAX`** — phenotype = the stronger allele (both parents' traits express fully, independently)
- **`THRESHOLD`** — phenotype only writes once the combined alleles clear a data-defined cutoff; below it, the gene sits in `Genome` fully carried and never expressed (the "neither, for now" case — until a later generation's alleles finally clear the bar)

Genes are defined in `data/genes.json`; which component field a gene drives is resolved through an open `ExpressionTargets` registry (`fearThreshold`, `fearedTrait`, `flammableIgnitionPoint`, `digestiveTolerance`, `mobilitySpeed`, `trait`, ...) — adding a new heritable target is registering a target function, never a new branch in `ExpressionSystem`.

**Lifecycle plumbing** (required so reproduction doesn't produce an unbounded population): `Age` (current age, maturity, lifespan) + `AgingSystem` age every entity every tick, kill it past its lifespan, and count down `Fertility`'s post-birth cooldown. `Phase.LIFECYCLE` runs last so newborns start clean next tick.

**Reproduction** mirrors the existing `MoveIntent` pattern: `CourtshipSystem` (cognition phase) sets `ReproduceIntent.partnerId` to the nearest mature, cooldown-ready candidate within `matingRadius`; `ReproductionSystem` (lifecycle phase) resolves mutual pairs the same tick. Offspring are built generically — re-instantiate parent A's own template (its existing component bundle), then replace its `Genome` with one combined from both parents' alleles (per-gene: one allele picked from each parent, with a small data-tunable mutation jitter), and attach a fresh `Age`/`Lineage`. **Explicit design decision** (flagged per CLAUDE.md, not a silent default): there is **no species/breeding-group compatibility gate** — any two mutually-intending, ready, mature entities pair regardless of template, so a critter/werewolf hybrid is allowed to happen exactly like any other honest cross-system collision. `Lineage` (parent ids + generation) is attached to every offspring so "why does this entity fear both fire and water" is traceable through the causality log, not just inferable from the current tick.

## Repository Shape

```
JavaGameCore/          — ECS engine + demo/tiled/performance modules (see JavaGameCore/CAPABILITIES.md)
  core/                — World/WorldEntity ECS, render pipeline, grid, pathfinding, physics, events
substrate/
  sim/
    src/main/java/com/cryptroot/substrate/
      component/       — property components (Thermal, Flammable, FearProne, Genome, Age, Lineage, ...)
      genetics/        — GeneRule, GeneDefinition, GeneRegistry, ExpressionTargets (data-defined gene rules/targets)
      material/        — data-driven Material/MaterialRegistry/SimConfig
      system/          — generic simulators, one file per system, no entity-type checks
      substrate/       — TileSubstrate (tile material/temperature/fluid arrays)
      template/        — TemplateLoader + ComponentParsers (JSON component bundles, registry not a switch)
      tick/            — Phase order, SimulationLoop, SimWorld wiring
      log/             — causality logging (CausalityLog, ChangeRecord), queryable after the fact
    src/main/resources/data/ — materials.json, simconfig.json, genes.json, templates/*.json (all tunables)
    src/test/          — NoIdentityGateTest (mechanical CLAUDE.md enforcement), DeterminismTest, emergent-behavior tests
  harness/             — ScenarioRunner: runs a JSON scenario headlessly and prints a causality trace for one entity
```

## Language and Build System

Language: Java 21 LTS
Build: Apache Maven — see `substrate/pom.xml`; `mvn -f substrate/pom.xml clean test -Dspotless.check.skip=true` runs the full suite.

## Closing note

The failure mode for this project isn't "too weird." It's "someone got scared of the weirdness halfway through and quietly added an `if (!isDwarf) return;`." Every time that urge shows up, it's worth remembering: that line is exactly how you keep a cat from ever discovering the booze on its own fur.
