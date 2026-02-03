package jpm;

import jpm.utils.UserOutput;

/**
 * Version management and Java compatibility checking for JPM.
 * Provides constants for the current CLI version and minimum Java version requirements.
 */
public final class JpmVersion {

  public static final String CURRENT = "0.3.0";
  public static final int MINIMUM_JAVA_VERSION = 21;

  private JpmVersion() {
    // Prevent instantiation
  }

  /**
   * Checks if the current Java runtime meets the minimum version requirement.
   * Exits with error code 1 if the requirement is not met.
   */
  public static void checkJavaVersion() {
    int version = Runtime.version().feature();
    if (version < MINIMUM_JAVA_VERSION) {
      UserOutput.error(
          "Error: Java " + MINIMUM_JAVA_VERSION + "+ required (found Java " + version + ")");
      UserOutput.error("Please upgrade to Java " + MINIMUM_JAVA_VERSION + " or later to use JPM.");
      System.exit(1);
    }
  }

  /**
   * Checks if a given Java version is compatible with JPM requirements.
   *
   * @param version the Java version to check
   * @return true if the version meets minimum requirements
   */
  public static boolean isCompatible(int version) {
    return version >= MINIMUM_JAVA_VERSION;
  }

  /**
   * Returns a formatted version string for display.
   *
   * @return the current JPM version
   */
  public static String getDisplayVersion() {
    return "jpm " + CURRENT;
  }
}
