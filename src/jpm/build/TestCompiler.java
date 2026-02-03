package jpm.build;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import jpm.utils.FileCollector;

/**
 * Compiles test sources from src/test/java/ to target/test-classes/.
 * Delegates to Compiler for actual compilation, specializing in test source discovery.
 */
public class TestCompiler {

  private final Compiler compiler;

  public TestCompiler() {
    this.compiler = new Compiler();
  }

  /**
   * Compiles test source files with JUnit support.
   *
   * @param testSourceDir Directory containing test sources (src/test/java/)
   * @param testOutputDir Output directory for compiled test classes (target/test-classes/)
   * @param classpath Full classpath including main classes, dependencies, and test libraries
   * @param javaVersion Java version for --release flag (e.g., "21")
   * @return CompileResult with success status and exit code
   * @throws IOException if compilation fails
   */
  public Compiler.CompileResult compileTests(
      File testSourceDir, File testOutputDir, String classpath, String javaVersion)
      throws IOException {

    // Check for test files
    var testFiles = FileCollector.findFilesByExtension(testSourceDir, ".java");

    if (testFiles.isEmpty()) {
      return new Compiler.CompileResult(true, "No test files found", 0);
    }

    // Build compiler arguments for test compilation
    var compilerArgs = new ArrayList<String>();
    compilerArgs.add("--release");
    compilerArgs.add(javaVersion);

    // Delegate to main compiler
    return compiler.compile(testSourceDir, testOutputDir, classpath, compilerArgs);
  }
}
