package jpm.build.format;

import com.palantir.javaformat.java.Formatter;

/**
 * Palantir Java Format implementation of the Formatter interface.
 * Uses the Palantir Java Format library for opinionated code formatting.
 */
public class PalantirFormatter implements jpm.build.format.Formatter {

  private static final String VERSION = "2.86.0";

  private final Formatter formatter;

  public PalantirFormatter() {
    this.formatter = Formatter.create();
  }

  @Override
  public String format(String source) throws jpm.build.format.FormatterException {
    try {
      return formatter.formatSource(source);
    } catch (com.palantir.javaformat.java.FormatterException e) {
      throw new jpm.build.format.FormatterException(
          "Failed to format source: " + e.getMessage(), e);
    }
  }

  @Override
  public String formatAndOrganizeImports(String source) throws jpm.build.format.FormatterException {
    try {
      return formatter.formatSourceAndFixImports(source);
    } catch (com.palantir.javaformat.java.FormatterException e) {
      throw new jpm.build.format.FormatterException(
          "Failed to format and organize imports: " + e.getMessage(), e);
    } catch (Exception e) {
      // Fallback to simple formatting if import ordering fails
      return format(source);
    }
  }

  @Override
  public String getName() {
    return "palantir-java-format";
  }

  @Override
  public String getVersion() {
    return VERSION;
  }
}
