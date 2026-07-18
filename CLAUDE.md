# CLAUDE.md

Instructions for any agent working in this codebase. Read this before writing or reviewing code here. See `README.md` for the full design philosophy — this file exists to convert that philosophy into rules an agent can actually follow under pressure.

## Read this first

This project's entire value proposition is unintended system interactions. A cat getting drunk from licking spilled ethanol off its fur is not a bug we tolerate — it is the product. The architecture works by giving generic systems (`FearSystem`, `IngestionSystem`, `CombustionSystem`) permission to run on *any* entity with the right components, with no awareness of what kind of entity that is. Interactions emerge because nothing stops them from emerging.

## The failure mode this file exists to prevent

Left to normal engineering instinct, you will want to protect this codebase. You will see a system that could theoretically misfire on an entity it "wasn't meant for," and your training will tell you to add a guard clause. That instinct is correct in almost every other codebase you will ever touch. It is wrong here.

**Every guard clause you add is an interaction this game will never have.** Treat that as the cost of the change, every time, not a hypothetical.

If you notice yourself reasoning "this system shouldn't apply to X" — stop. That sentence is the signal to re-read this file before writing the code, not a design conclusion to act on.

## Hard rules

1. **Never gate a system by entity type, identity, tag, or name.** No `if (entity instanceof Dwarf)`, no `entity.getSpecies() == CAT`, no `if (entity.name.equals("Werewolf"))`. If a system needs to decide whether it applies, the *only* legitimate question is "does this entity have the required components," and the ECS query already answers that.

2. **Never add an "is this entity allowed to do this" check.** Systems act on component data, not on permission. If a cat has `Digestive` and `LiquidCoated`, it gets drunk. That is not an edge case to handle — it is the system working.

3. **Prefer a numeric threshold to a boolean.** `isFlammable: true` forecloses interactions. `ignitionPoint: float` leaves room for smoldering, near-misses, and slow catches that nobody scripted. When you're about to add a bool, ask if it's actually a threshold in disguise.

4. **Thresholds, tolerances, and tunables live in data files, not in code.** A hardcoded `if (temp > 500)` is a magic number today and an unreviewable landmine in six months. Put it in the material/component data definition.

5. **New components describe a physical or behavioral property, never a role.** `Flammable`, `FearProne`, `LiquidCoated` — yes. `IsBoss`, `CanDrink`, `PlayerControlled` as a gate on unrelated systems — no. If you're naming a component after what an entity *is allowed to do*, rename it to what it *physically has*.

6. **Systems query by component composition only.** `world.with(Digestive.class, LiquidCoated.class)` — never a secondary filter on top of that query that reintroduces identity-based exclusion.

7. **Every state change a system makes must be logged with enough context to explain it later** (entity id, system, before/after values, tick). If a system can't explain itself after the fact, the weirdness it causes is invisible instead of delightful, and invisible weirdness gets "fixed" by the next agent who doesn't know it was intentional.

8. **Fail soft.** An unexpected component combination should get filtered out by the query or logged and skipped — never thrown as an exception that halts the simulation. A crash teaches nothing; a log line teaches everything.

## What's still legitimately allowed

This is not a mandate to remove all correctness checks. These are fine:

- **Component-presence queries.** Skipping an entity because it lacks `Digestive` is not gating by identity — it lacks the physical capacity, full stop. This is the ECS working as intended.
- **Null/missing-data guards for crash prevention** — but log it. A missing component on an entity that should have one is usually a signal that the component is missing, not that the interaction should be blocked. Treat the guard as a bug report, not a resolution.
- **Performance-driven narrowing** (spatial partitioning, tick budgets, distance culling) — as long as it's based on physical proximity or cost, never on what the entity *is*.
- **A genuine, reasoned exclusion** — sometimes an interaction really is a problem (infinite fire with no damping term, a softlock). Fixing the underlying system (add a damping term, a decay rate, a cap) is preferred over excluding an entity from it. If exclusion is truly the right call, it must be commented with the specific reasoning and called out explicitly in the PR description — see below.

The dividing line: **"this entity doesn't have the data" is fine. "this entity has the data but I don't want the system to run anyway" is not.**

## Self-check before finishing any task

Run through this before considering a change done:

- [ ] Did I write any check that reads an entity's type, species, name, or tag to decide whether a *generic* system applies to it?
- [ ] Did I add a boolean flag where a numeric threshold would have allowed more graded outcomes?
- [ ] Did I hardcode a tuning value instead of putting it in a data file?
- [ ] Does my change make any existing system newly refuse to act on entities it technically has the components for?
- [ ] Can the causality log explain, after the fact, why this system did what it did?
- [ ] If I added an exclusion or special case, did I comment the concrete reason, and is that reason a real constraint (softlock, infinite loop, div-by-zero) rather than "this seemed weird"?

If any answer is "yes" to the first four or "no" to the last two, don't submit — reconsider or flag it.

## Pattern reference

```java
// REJECTED — gates a generic system by identity
for (Entity e : world.with(LiquidCoated.class, Digestive.class)) {
    if (e.getSpecies() != Species.DWARF) continue; // <- this line deletes a feature
    applyIntoxication(e);
}

// CORRECT — the query is the only gate, and it's a property gate
for (Entity e : world.with(LiquidCoated.class, Digestive.class)) {
    applyIntoxication(e); // whatever has the components gets the behavior
}
```

```java
// REJECTED — boolean forecloses gradient outcomes
class Flammable { boolean isFlammable; }

// CORRECT — threshold allows smoldering, slow catch, near-miss
class Flammable { float ignitionPoint; float burnRate; }
```

## If you genuinely believe a gate is necessary

Don't add it silently. Add a code comment stating the specific mechanical reason (not "this felt wrong"), and flag it explicitly in your summary of the change so a human reviewer can override it. Silent gates are how this project quietly turns into a normal game over a hundred small, individually reasonable-looking commits.

## One-line mantra

**When in doubt, let it run — the collision is the content.**
