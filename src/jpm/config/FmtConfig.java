package jpm.config;

import java.util.List;

/**
 * Configuration record for code formatting settings.
 * Follows cargo's approach: style preferences in config file, not CLI flags.
 */
public record FmtConfig(Integer lineLength, Boolean organizeImports, List<String> skipPatterns) {

  public FmtConfig() {
    this(120, true, List.of());
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
}
