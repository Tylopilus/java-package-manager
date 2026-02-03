package jpm.build;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import jpm.utils.ProcessExecutor;

/**
 * Executes Java applications with configurable JVM and program arguments.
 * Provides a unified interface for running Java classes with dependency classpath support.
 */
public class Runner {

  /**
   * Record representing the result of a run operation.
   */
  public record RunResult(boolean success, int exitCode) {}

  /**
   * Executes a Java class with optional JVM arguments and program arguments.
   *
   * @param mainClass the fully qualified main class to run
   * @param classesDir the directory containing compiled classes
   * @param classpath the dependency classpath (can be null or empty)
   * @param jvmArgs JVM arguments (e.g., -ea, -server, -Xmx2g), can be null
   * @param args program arguments to pass to main(), can be null
   * @return RunResult with exit code
   * @throws IOException if process execution fails
   */
  public RunResult run(
      String mainClass, File classesDir, String classpath, List<String> jvmArgs, String[] args)
      throws IOException {

    var command = buildJavaCommand(mainClass, classesDir, classpath, jvmArgs, args);
    return executeProcess(command);
  }

  /**
   * Convenience method to run with only JVM arguments (no program args).
   */
  public RunResult run(String mainClass, File classesDir, String classpath, List<String> jvmArgs)
      throws IOException {
    return run(mainClass, classesDir, classpath, jvmArgs, null);
  }

  /**
   * Convenience method to run with only program arguments (no custom JVM args).
   */
  public RunResult run(String mainClass, File classesDir, String classpath, String[] args)
      throws IOException {
    return run(mainClass, classesDir, classpath, Collections.emptyList(), args);
  }

  /**
   * Convenience method for simple execution without any extra arguments.
   */
  public RunResult run(String mainClass, File classesDir, String classpath) throws IOException {
    return run(mainClass, classesDir, classpath, Collections.emptyList(), null);
  }

  /**
   * Builds the Java command with all components.
   */
  private List<String> buildJavaCommand(
      String mainClass, File classesDir, String classpath, List<String> jvmArgs, String[] args) {

    var command = new ArrayList<String>();
    command.add("java");

    // Add JVM arguments
    if (jvmArgs != null && !jvmArgs.isEmpty()) {
      command.addAll(jvmArgs);
    }

    // Build and add classpath
    command.add("-cp");
    command.add(buildClasspath(classesDir, classpath));

    // Add main class
    command.add(mainClass);

    // Add program arguments
    if (args != null) {
      for (var arg : args) {
        command.add(arg);
      }
    }

    return command;
  }

  /**
   * Builds the classpath string from classes directory and dependencies.
   */
  private String buildClasspath(File classesDir, String classpath) {
    var fullClasspath = new StringBuilder();
    fullClasspath.append(classesDir.getAbsolutePath());

    if (classpath != null && !classpath.isEmpty()) {
      fullClasspath.append(File.pathSeparator);
      fullClasspath.append(classpath);
    }

    return fullClasspath.toString();
  }

  /**
   * Executes the Java process and returns the result.
   */
  private RunResult executeProcess(List<String> command) throws IOException {
    var result = ProcessExecutor.execute(command);
    return new RunResult(result.success(), result.exitCode());
  }
}
