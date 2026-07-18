package com.cryptroot.substrate.harness;

import com.cryptroot.substrate.log.ChangeRecord;
import com.cryptroot.substrate.tick.SimulationLoop;
import com.cryptroot.substrate.world.SimWorld;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Headless playtest harness. Runs a scenario, printing ASCII frames as it goes, then dumps the
 * causality log as JSONL and answers "why" queries against it.
 *
 * <pre>
 *   ScenarioRunner &lt;scenario.json | classpath:/scenarios/name.json&gt; [--frames N] [--log out.jsonl] [--why subjectFragment]
 * </pre>
 */
public final class ScenarioRunner {

  public static void main(String[] args) throws IOException {
    if (args.length < 1) {
      System.err.println(
          "usage: ScenarioRunner <scenario.json|classpath:/scenarios/x.json>"
              + " [--frames N] [--log out.jsonl] [--why subjectFragment]");
      System.exit(2);
    }
    Scenario scenario =
        args[0].startsWith("classpath:")
            ? Scenario.fromResource(args[0].substring("classpath:".length()))
            : Scenario.fromFile(Path.of(args[0]));
    int frames = 8;
    Path logOut = null;
    String why = null;
    for (int i = 1; i < args.length - 1; i++) {
      switch (args[i]) {
        case "--frames" -> frames = Integer.parseInt(args[++i]);
        case "--log" -> logOut = Path.of(args[++i]);
        case "--why" -> why = args[++i];
        default -> {}
      }
    }

    SimulationLoop loop = scenario.build();
    SimWorld w = loop.world();
    int ticks = scenario.ticks();
    int frameEvery = Math.max(1, ticks / Math.max(1, frames));
    for (int t = 0; t < ticks; t++) {
      loop.tick();
      if (t % frameEvery == 0 || t == ticks - 1) {
        System.out.println("--- tick " + t + " ---");
        System.out.println(AsciiRenderer.render(w));
      }
    }

    System.out.println(w.log().all().size() + " causality records retained.");
    if (logOut != null) {
      try (Writer writer = Files.newBufferedWriter(logOut)) {
        w.log().writeJsonl(writer);
      }
      System.out.println("causality log written to " + logOut);
    }
    if (why != null) {
      explain(w, why);
    }
  }

  /** Prints every record touching a matching subject, then the causal chain of the last one. */
  static void explain(SimWorld w, String subjectFragment) {
    List<ChangeRecord> hits =
        w.log().all().stream().filter(r -> r.subject().contains(subjectFragment)).toList();
    if (hits.isEmpty()) {
      System.out.println("no records for subject containing '" + subjectFragment + "'");
      return;
    }
    System.out.println("== history of '" + subjectFragment + "' (" + hits.size() + " records) ==");
    for (ChangeRecord r : hits) {
      System.out.printf(
          "t%04d %-11s %-22s %-45s %s -> %s%n",
          r.tick(), r.phase(), r.system(), r.field(), r.before(), r.after());
    }
    System.out.println("== causal chain of final record ==");
    for (ChangeRecord r : w.log().chain(hits.get(hits.size() - 1).id())) {
      System.out.printf(
          "t%04d %s %s %s: %s -> %s%n",
          r.tick(), r.system(), r.subject(), r.field(), r.before(), r.after());
    }
  }

  private ScenarioRunner() {}
}
