package jpm.cli;

import java.io.BufferedReader;
import java.io.IOException;
import jpm.utils.UserOutput;

/**
 * Handles interactive prompts for dependency selection and confirmation.
 */
public class DependencyPrompter {

  private final BufferedReader reader;

  public DependencyPrompter(BufferedReader reader) {
    this.reader = reader;
  }

  public boolean confirm(String message, boolean autoConfirm) {
    if (autoConfirm) {
      return true;
    }

    UserOutput.printNoLn(message + " [Y/n]: ");
    var response = readLine();
    if (response == null || response.isBlank()) {
      return true;
    }
    return response.trim().toLowerCase().startsWith("y");
  }

  public int promptForSelection(int max) {
    UserOutput.printNoLn("Select [1-" + max + "] or press Enter to cancel: ");
    var input = readLine();

    if (input == null || input.isBlank()) {
      return -1;
    }

    try {
      var choice = Integer.parseInt(input.trim());
      if (choice >= 1 && choice <= max) {
        return choice - 1; // Convert to 0-indexed
      }
    } catch (NumberFormatException e) {
      // Fall through to error
    }

    UserOutput.error("Invalid selection");
    return promptForSelection(max);
  }

  public String readLine() {
    try {
      return reader.readLine();
    } catch (IOException e) {
      return null;
    }
  }
}
