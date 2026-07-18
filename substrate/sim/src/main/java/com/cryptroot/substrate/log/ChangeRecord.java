package com.cryptroot.substrate.log;

/**
 * One state change made by one system on one tick, with enough context to explain it later.
 *
 * @param id monotonically increasing record id
 * @param tick simulation tick the change happened on
 * @param phase tick phase name
 * @param system system that made the change
 * @param subject what changed, e.g. {@code "entity:42"} or {@code "tile:3,7"}
 * @param field the component field or tile array that changed
 * @param before value before the change (stringified)
 * @param after value after the change (stringified)
 * @param causeId id of the record that caused this one, or {@code -1} if root cause
 */
public record ChangeRecord(
    long id,
    long tick,
    String phase,
    String system,
    String subject,
    String field,
    String before,
    String after,
    long causeId) {}
