package jpm.config;

import java.util.List;

/**
 * Configuration record for code formatting settings.
 * Follows cargo's approach: style preferences in config file, not CLI flags.
 */
public record FmtConfig(
    Integer lineLength, Boolean organizeImports, List<String> skipPatterns, String formatter) {

  public FmtConfig() {
    // Palantir defaults: 120 chars, organize imports, Palantir formatter
    this(120, true, List.of(), "palantir");
  }

  public int getLineLength() {
    return lineLength != null ? lineLength : 120;
  }

  public boolean shouldOrganizeImports() {
    return organizeImports != null ? organizeImports : true;
  }

  public List<String> getSkipPatterns() {
    return skipPatterns != null ? skipPatterns : List.of();
  }

  public String getFormatter() {
    return "palantir"; // Always return palantir as it's the only supported one
  }
}
