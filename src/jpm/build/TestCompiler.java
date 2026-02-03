package jpm.build;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import jpm.utils.FileUtils;

/**
 * Compiles test sources from src/test/java/ to target/test-classes/.
 * Similar to Compiler but for test sources with additional test dependencies.
 */
public class TestCompiler {

  /**
   * Compiles test source files.
   *
   * @param testSourceDir Directory containing test sources (src/test/java/)
   * @param testOutputDir Output directory for compiled test classes (target/test-classes/)
   * @param classpath Full classpath including main classes, dependencies, and test libraries
   * @return CompileResult with success status and exit code
   * @throws IOException if compilation fails
   */
  public CompileResult compileTests(File testSourceDir, File testOutputDir, String classpath)
      throws IOException {
    // Ensure output directory exists
    FileUtils.ensureDirectory(testOutputDir);

    // Find all test source files
    var testFiles = findTestFiles(testSourceDir);

    if (testFiles.isEmpty()) {
      return new CompileResult(true, "No test files found", 0);
    }

    // Build javac command
    var command = new ArrayList<String>();
    command.add("javac");
    command.add("--release");
    command.add("21");
    command.add("-cp");
    command.add(classpath);
    command.add("-d");
    command.add(testOutputDir.getAbsolutePath());

    // Add all test files
    for (var testFile : testFiles) {
      command.add(testFile.getAbsolutePath());
    }

    // Execute compilation
    var processBuilder = new ProcessBuilder(command);
    processBuilder.inheritIO();

    try {
      var process = processBuilder.start();
      var exitCode = process.waitFor();

      if (exitCode == 0) {
        return new CompileResult(true, "Test compilation successful", exitCode);
      } else {
        return new CompileResult(false, "Test compilation failed", exitCode);
      }

    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return new CompileResult(false, "Test compilation interrupted", 1);
    }
  }

  /**
   * Recursively finds all .java files in the test source directory.
   */
  private List<File> findTestFiles(File testSourceDir) {
    var testFiles = new ArrayList<File>();

    if (!testSourceDir.exists() || !testSourceDir.isDirectory()) {
      return testFiles;
    }

    findTestFilesRecursive(testSourceDir, testFiles);
    return testFiles;
  }

  private void findTestFilesRecursive(File dir, List<File> testFiles) {
    var files = dir.listFiles();
    if (files == null) return;

    for (var file : files) {
      if (file.isDirectory()) {
        findTestFilesRecursive(file, testFiles);
      } else if (file.getName().endsWith(".java")) {
        testFiles.add(file);
      }
    }
  }

  /**
   * Result of test compilation.
   */
  public record CompileResult(boolean success, String message, int exitCode) {
    public boolean success() {
      return success;
    }

    public int exitCode() {
      return exitCode;
    }

    public String message() {
      return message;
    }
  }
}
