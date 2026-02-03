package jpm.cli;

import jpm.utils.UserOutput;

/**
 * Standardized error handling for CLI commands.
 */
public final class CliErrorHandler {

  private CliErrorHandler() {
    // Utility class - prevent instantiation
  }

  public static void error(String message) {
    if (message == null || message.isBlank()) {
      UserOutput.error("Unknown error");
      return;
    }
    UserOutput.error(message);
  }

  public static void error(String message, Exception e) {
    if (message == null || message.isBlank()) {
      error(messageFromException(e));
      return;
    }

    if (e == null || e.getMessage() == null || e.getMessage().isBlank()) {
      error(message);
      return;
    }

    error(message + ": " + e.getMessage());
  }

  private static String messageFromException(Exception e) {
    if (e == null) {
      return "Unknown error";
    }
    var message = e.getMessage();
    if (message == null || message.isBlank()) {
      return e.getClass().getSimpleName();
    }
    return message;
  }
}
