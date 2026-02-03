package jpm.build;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import jpm.utils.FileCollector;
import jpm.utils.ProcessExecutor;

/**
 * Compiles Java source files using the system javac compiler.
 * Provides a clean interface for compiling with configurable classpath and compiler arguments.
 */
public class Compiler {

  /**
   * Record representing the result of a compilation operation.
   */
  public record CompileResult(boolean success, String message, int exitCode) {}

  /**
   * Compiles all Java source files in the source directory.
   *
   * @param sourceDir directory containing .java files
   * @param outputDir directory for compiled .class files
   * @param classpath dependency classpath (can be null or empty)
   * @return CompileResult with success status and exit code
   * @throws IOException if compilation fails
   */
  public CompileResult compile(File sourceDir, File outputDir, String classpath)
      throws IOException {
    return compile(sourceDir, outputDir, classpath, Collections.emptyList());
  }

  /**
   * Compiles with additional compiler arguments.
   *
   * @param sourceDir directory containing .java files
   * @param outputDir directory for compiled .class files
   * @param classpath dependency classpath (can be null or empty)
   * @param compilerArgs additional arguments for javac (e.g., --release, -g)
   * @return CompileResult with success status and exit code
   * @throws IOException if compilation fails
   */
  public CompileResult compile(
      File sourceDir, File outputDir, String classpath, List<String> compilerArgs)
      throws IOException {

    // Find all Java source files
    var sourceFiles = FileCollector.findRelativePathsByExtension(sourceDir, ".java");

    if (sourceFiles.isEmpty()) {
      return new CompileResult(false, "No Java source files found in " + sourceDir, 1);
    }

    // Ensure output directory exists
    outputDir.mkdirs();

    // Build and execute javac command
    var command = buildJavacCommand(outputDir, classpath, compilerArgs, sourceFiles);
    return executeJavac(command, sourceDir);
  }

  /**
   * Builds the javac command with all necessary arguments.
   */
  private List<String> buildJavacCommand(
      File outputDir, String classpath, List<String> compilerArgs, List<String> sourceFiles) {

    var command = new ArrayList<String>();
    command.add("javac");
    command.add("-d");
    command.add(outputDir.getAbsolutePath());

    if (classpath != null && !classpath.isEmpty()) {
      command.add("-cp");
      command.add(classpath);
    }

    if (!compilerArgs.isEmpty()) {
      command.addAll(compilerArgs);
    }

    command.addAll(sourceFiles);

    return command;
  }

  /**
   * Executes the javac process and returns the result.
   */
  private CompileResult executeJavac(List<String> command, File workingDir) throws IOException {
    var result = ProcessExecutor.execute(command, workingDir);
    return new CompileResult(result.success(), "", result.exitCode());
  }
}
