package jpm.build.format;

import jpm.config.FmtConfig;

/**
 * Factory for creating Formatter implementations.
 * Only supports Palantir formatter (rust-like style).
 *
 * <p><strong>Supported Formatter:</strong>
 * <ul>
 *   <li><strong>Palantir:</strong> Opinionated formatting, works with all Java versions.
 *       Follows Rust-like style: 4-space indent, same-line braces, 120 char line width.</li>
 * </ul>
 */
public final class FormatterFactory {

  private FormatterFactory() {
    // Utility class
  }

  /**
   * Creates a Palantir Formatter instance.
   * The type parameter is ignored as Palantir is the only supported formatter.
   *
   * @param type ignored
   * @return the Palantir Formatter implementation
   */
  public static Formatter create(String type) {
    return new PalantirFormatter();
  }

  /**
   * Creates a Palantir Formatter instance.
   * The type and config parameters are largely ignored as Palantir is opinionated.
   *
   * @param type ignored
   * @param config configuration (mostly ignored by Palantir)
   * @return the Palantir Formatter implementation
   */
  public static Formatter create(String type, FmtConfig config) {
    return new PalantirFormatter();
  }
}
