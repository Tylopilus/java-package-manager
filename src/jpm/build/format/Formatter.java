package jpm.build.format;

/**
 * Interface for pluggable code formatters.
 * Implementations can use different formatting backends (Palantir, Google, Eclipse, etc.)
 */
public interface Formatter {

  /**
   * Formats the given source code.
   *
   * @param source the unformatted Java source code
   * @return the formatted source code
   * @throws FormatterException if formatting fails
   */
  String format(String source) throws FormatterException;

  /**
   * Formats the source code and organizes/optimizes imports.
   *
   * @param source the unformatted Java source code
   * @return the formatted source code with organized imports
   * @throws FormatterException if formatting fails
   */
  String formatAndOrganizeImports(String source) throws FormatterException;

  /**
   * Checks if two source strings are equal, ignoring formatting differences.
   * Used in check mode to determine if a file needs formatting.
   *
   * @param original the original source code
   * @param formatted the formatted source code
   * @return true if the content is semantically equivalent
   */
  default boolean isEquivalent(String original, String formatted) {
    if (original == null || formatted == null) {
      return original == formatted;
    }
    return original.equals(formatted);
  }

  /**
   * Gets the name of this formatter implementation.
   *
   * @return the formatter name (e.g., "palantir-java-format")
   */
  String getName();

  /**
   * Gets the version of this formatter implementation.
   *
   * @return the formatter version
   */
  String getVersion();
}
