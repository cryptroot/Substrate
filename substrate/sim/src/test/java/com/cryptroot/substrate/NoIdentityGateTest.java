package com.cryptroot.substrate;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Mechanical enforcement of CLAUDE.md rule 1: no system may gate behavior on entity identity.
 * Scans every source file in the systems package for forbidden patterns and fails the build if
 * any appear. If you hit this and believe your gate is legitimate, read CLAUDE.md "If you
 * genuinely believe a gate is necessary" before touching this test.
 */
class NoIdentityGateTest {

  private static final List<Pattern> FORBIDDEN =
      List.of(
          Pattern.compile("\\binstanceof\\b"),
          Pattern.compile("getSpecies"),
          Pattern.compile("\\.name\\(\\)\\s*\\.equals"),
          Pattern.compile("templateName"),
          Pattern.compile("findByTag"),
          Pattern.compile("TagComponent"),
          Pattern.compile("Identity\\.class")); // systems may not even read identity

  @Test
  void systemsContainNoIdentityGates() throws IOException {
    Path systemsDir = Path.of("src/main/java/com/cryptroot/substrate/system");
    assertTrue(Files.isDirectory(systemsDir), "systems dir must exist: " + systemsDir);
    List<String> violations = new ArrayList<>();
    try (Stream<Path> files = Files.walk(systemsDir)) {
      for (Path p : files.filter(f -> f.toString().endsWith(".java")).toList()) {
        String source = Files.readString(p);
        for (Pattern pattern : FORBIDDEN) {
          if (pattern.matcher(source).find()) {
            violations.add(p.getFileName() + " matches forbidden pattern: " + pattern.pattern());
          }
        }
      }
    }
    if (!violations.isEmpty()) {
      fail(
          "Identity gates found in generic systems (see CLAUDE.md — every guard clause is an "
              + "interaction this game will never have):\n"
              + String.join("\n", violations));
    }
  }
}
