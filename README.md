# SUBSTRATE

*A simulation built to misbehave.*

> Working title — swap for whatever you like. The name is a placeholder for the idea: everything in this game sits on top of a shared physical/behavioral substrate, and the game's personality comes from what happens when unrelated systems reach into that substrate at the same time.

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

## Scope for v1

Keep the *world* small so the *interaction space* can be large. A flat 2D grid, a handful of creature templates, a handful of item/material types — the fun scales with how many components each thing has, not with map size or roster size.

**World:** flat 2D tile grid. Each tile carries a material, a temperature, and a liquid depth — same substrate idea applied to terrain as to creatures.

**Suggested starting components** (deliberately generic, deliberately reusable):

| Component | Carries | Used by |
|---|---|---|
| `Thermal` | temperature, heat capacity | fire spread, freezing, cooking, heatstroke |
| `Flammable` | ignition point, burn rate | fire spread |
| `LiquidCoated` | material, amount | grooming, contamination, ingestion |
| `Digestive` | ingested materials, tolerance | intoxication, poisoning, nutrition |
| `FearProne` | fear threshold, current fear, feared-trait tags | flee behavior, panic, self-fright |
| `SocialBond` | relationships map (entity → affinity) | grudges, packs, tantrums |
| `Odor` | scent material, strength | tracking, attraction, repulsion |
| `Mobility` | speed, terrain penalties | pathing, fleeing, chasing |

**Suggested starting systems**, each one narrow, each one blind to entity type:

- `ThermalDiffusionSystem` — heat spreads between adjacent tiles and touching entities
- `CombustionSystem` — anything `Flammable` above its ignition point catches fire and heats its neighbors
- `FluidFlowSystem` — liquids move downhill/downgrade across the tile grid
- `GroomingSystem` — entities with a grooming behavior pick up nearby `LiquidCoated` material onto themselves
- `IngestionSystem` — entities ingest `LiquidCoated` material on themselves or in their mouths into `Digestive`
- `IntoxicationSystem` — reads `Digestive` contents against material properties (is it alcohol?), applies effects
- `FearSystem` — compares nearby entities'/traits' `FearProne` tags against an entity's own feared-trait list, including *its own* tags if self-referential checks aren't excluded

That last one is your werewolf-afraid-of-dogs case: if a werewolf has `FearProne { fears: [Canine] }` and also, say, a partial `Canine` tag from its own lycanthropy, and the fear system doesn't special-case "don't check yourself" — it'll frighten itself. That's not a bug you write. That's a bug you *decline to prevent*.

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

## Suggested repo shape

```
/core          — ECS engine (existing)
/components    — property definitions (Thermal, Flammable, FearProne, ...)
/systems       — generic simulators, one file per system, no entity-type checks
/materials     — data-driven material definitions (ignition points, intoxication, etc.)
/entities      — template definitions built purely as component bundles
/world         — flat grid, tile substrate (material/temp/fluid arrays)
/log           — causality/event logging, queryable after the fact
/tools         — playtesting harness for isolating and replaying odd interaction chains
```

## Roadmap

1. **Substrate first.** Tile grid with material/temperature/fluid arrays. No creatures yet — just get fire and water behaving honestly against each other.
2. **One creature, full component set.** A single generic "critter" template with every starting component, to prove systems compose without a second species to contrast against.
3. **Second species, zero new systems.** Add a second creature template that reuses every existing system with different thresholds. If this requires a new system, something in step 2 was too specific.
4. **Causality logging + playtest harness.** Before adding more systems, make sure you can explain any given death.
5. **Grow the system count, not the map.** Each new system should be checked against the full existing roster for accidental (delightful) interactions before being called done.

## Closing note

The failure mode for this project isn't "too weird." It's "someone got scared of the weirdness halfway through and quietly added an `if (!isDwarf) return;`." Every time that urge shows up, it's worth remembering: that line is exactly how you keep a cat from ever discovering the booze on its own fur.
