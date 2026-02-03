package jpm.cli;

/**
 * Exception thrown when project validation fails.
 * Used by AbstractBuildCommand and subclasses to signal validation errors
 * that should result in a non-zero exit code.
 */
public class ProjectValidationException extends RuntimeException {

  public ProjectValidationException(String message) {
    super(message);
  }

  public ProjectValidationException(String message, Throwable cause) {
    super(message, cause);
  }
}
