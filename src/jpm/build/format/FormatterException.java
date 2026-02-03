package jpm.build.format;

/**
 * Exception thrown when code formatting fails.
 * Wraps underlying formatter exceptions while providing a consistent API.
 */
public class FormatterException extends Exception {

  public FormatterException(String message) {
    super(message);
  }

  public FormatterException(String message, Throwable cause) {
    super(message, cause);
  }

  public FormatterException(Throwable cause) {
    super(cause);
  }
}
