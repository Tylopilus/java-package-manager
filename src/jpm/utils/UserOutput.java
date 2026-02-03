package jpm.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Centralized handler for user output and logging.
 * Ensures consistent formatting and allows for future enhancements (e.g., quiet mode, JSON output).
 */
public final class UserOutput {

  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");
  private static boolean debugEnabled = false;
  private static boolean quietMode = false;

  private UserOutput() {
    // Prevent instantiation
  }

  public static void setDebug(boolean debug) {
    debugEnabled = debug;
  }

  public static void setQuiet(boolean quiet) {
    quietMode = quiet;
  }

  public static void info(String message) {
    if (!quietMode) {
      System.out.println(message);
    }
  }

  // For output that should always appear even in quiet mode (like results)
  public static void print(String message) {
    System.out.println(message);
  }

  // For output that does not append a newline
  public static void printNoLn(String message) {
    System.out.print(message);
  }

  public static void success(String message) {
    if (!quietMode) {
      // We could add color here later
      System.out.println(message);
    }
  }

  public static void warn(String message) {
    System.err.println("Warning: " + message);
  }

  public static void error(String message) {
    System.err.println("Error: " + message);
  }

  public static void error(String message, Throwable t) {
    System.err.println("Error: " + message + ": " + t.getMessage());
    if (debugEnabled) {
      t.printStackTrace();
    }
  }

  public static void debug(String message) {
    if (debugEnabled) {
      System.out.println("[DEBUG " + LocalDateTime.now().format(DATE_FORMAT) + "] " + message);
    }
  }
}
