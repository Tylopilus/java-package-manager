package jpm.utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for executing external processes.
 * Provides a unified interface for running processes like javac and java.
 */
public final class ProcessExecutor {

  private ProcessExecutor() {
    // Utility class - prevent instantiation
  }

  /**
   * Record representing the result of a process execution.
   */
  public record ExecutionResult(int exitCode, boolean success) {}

  /**
   * Executes a process with the given command.
   *
   * @param command list of command and arguments
   * @param workingDir working directory for the process (can be null)
   * @return ExecutionResult with exit code and success status
   * @throws IOException if process creation fails
   */
  public static ExecutionResult execute(List<String> command, File workingDir) throws IOException {
    var pb = new ProcessBuilder(command);
    pb.inheritIO();

    if (workingDir != null) {
      pb.directory(workingDir);
    }

    return execute(pb);
  }

  /**
   * Executes a process with the given command in the current working directory.
   *
   * @param command list of command and arguments
   * @return ExecutionResult with exit code and success status
   * @throws IOException if process creation fails
   */
  public static ExecutionResult execute(List<String> command) throws IOException {
    return execute(command, null);
  }

  /**
   * Executes a process with variable arguments.
   *
   * @param workingDir working directory for the process (can be null)
   * @param command first command argument
   * @param args remaining arguments
   * @return ExecutionResult with exit code and success status
   * @throws IOException if process creation fails
   */
  public static ExecutionResult execute(File workingDir, String command, String... args)
      throws IOException {
    var cmd = new ArrayList<String>();
    cmd.add(command);
    for (var arg : args) {
      cmd.add(arg);
    }
    return execute(cmd, workingDir);
  }

  /**
   * Executes a process with variable arguments in current working directory.
   *
   * @param command first command argument
   * @param args remaining arguments
   * @return ExecutionResult with exit code and success status
   * @throws IOException if process creation fails
   */
  public static ExecutionResult execute(String command, String... args) throws IOException {
    return execute(null, command, args);
  }

  /**
   * Executes the process builder and returns the result.
   */
  private static ExecutionResult execute(ProcessBuilder pb) throws IOException {
    var process = pb.start();

    try {
      var exitCode = process.waitFor();
      return new ExecutionResult(exitCode, exitCode == 0);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return new ExecutionResult(1, false);
    }
  }
}
