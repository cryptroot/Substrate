package com.cryptroot.substrate.log;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

/**
 * Structured, queryable causality log. Every state change any system makes flows through {@link
 * #record}, so "why did this happen" is always answerable after the fact.
 *
 * <p>Bounded ring buffer: oldest records are evicted past {@code capacity}. Deterministic: record
 * ids are assigned in call order, so two runs with the same seed produce identical logs.
 */
public final class CausalityLog {

  private final Deque<ChangeRecord> records;
  private final int capacity;
  private long nextId = 0;

  public CausalityLog(int capacity) {
    if (capacity <= 0) throw new IllegalArgumentException("capacity must be > 0");
    this.capacity = capacity;
    this.records = new ArrayDeque<>();
  }

  public CausalityLog() {
    this(200_000);
  }

  /** Appends a change record and returns its id (usable as {@code causeId} for chained effects). */
  public long record(
      long tick,
      String phase,
      String system,
      String subject,
      String field,
      Object before,
      Object after,
      long causeId) {
    long id = nextId++;
    if (records.size() >= capacity) records.removeFirst();
    records.addLast(
        new ChangeRecord(
            id, tick, phase, system, subject, field, String.valueOf(before), String.valueOf(after),
            causeId));
    return id;
  }

  /** Convenience for root-cause records (no upstream cause). */
  public long record(
      long tick, String phase, String system, String subject, String field, Object before,
      Object after) {
    return record(tick, phase, system, subject, field, before, after, -1);
  }

  public List<ChangeRecord> all() {
    return new ArrayList<>(records);
  }

  public List<ChangeRecord> bySubject(String subject) {
    Objects.requireNonNull(subject, "subject must not be null");
    List<ChangeRecord> out = new ArrayList<>();
    for (ChangeRecord r : records) if (subject.equals(r.subject())) out.add(r);
    return out;
  }

  public List<ChangeRecord> bySystem(String system) {
    Objects.requireNonNull(system, "system must not be null");
    List<ChangeRecord> out = new ArrayList<>();
    for (ChangeRecord r : records) if (system.equals(r.system())) out.add(r);
    return out;
  }

  public List<ChangeRecord> byTickRange(long fromInclusive, long toInclusive) {
    List<ChangeRecord> out = new ArrayList<>();
    for (ChangeRecord r : records) {
      if (r.tick() >= fromInclusive && r.tick() <= toInclusive) out.add(r);
    }
    return out;
  }

  /**
   * Walks the {@code causeId} chain from the given record back to its root cause. Returned list is
   * ordered root-first. Records evicted from the ring buffer terminate the walk early.
   */
  public List<ChangeRecord> chain(long recordId) {
    List<ChangeRecord> out = new ArrayList<>();
    long cursor = recordId;
    while (cursor >= 0) {
      ChangeRecord found = find(cursor);
      if (found == null) break;
      out.add(0, found);
      cursor = found.causeId();
    }
    return out;
  }

  private ChangeRecord find(long id) {
    for (ChangeRecord r : records) if (r.id() == id) return r;
    return null;
  }

  /** Writes all retained records as JSON Lines. */
  public void writeJsonl(Writer writer) throws IOException {
    for (ChangeRecord r : records) {
      writer.write(
          "{\"id\":" + r.id()
              + ",\"tick\":" + r.tick()
              + ",\"phase\":\"" + escape(r.phase())
              + "\",\"system\":\"" + escape(r.system())
              + "\",\"subject\":\"" + escape(r.subject())
              + "\",\"field\":\"" + escape(r.field())
              + "\",\"before\":\"" + escape(r.before())
              + "\",\"after\":\"" + escape(r.after())
              + "\",\"causeId\":" + r.causeId()
              + "}\n");
    }
  }

  private static String escape(String s) {
    return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
  }
}
