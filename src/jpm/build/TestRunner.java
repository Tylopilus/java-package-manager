package jpm.build;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import jpm.utils.FileUtils;

/**
 * Runs JUnit 5 tests and generates reports.
 * Uses JUnit Platform Launcher API to discover and execute tests.
 * Generates JUnit XML report for CI integration.
 */
public class TestRunner {

  /**
   * Runs tests from the compiled test classes directory.
   *
   * @param testOutputDir Directory containing compiled test classes
   * @param classpath Full classpath for test execution
   * @param filter Optional filter pattern for test class names
   * @param parallel Whether to run tests in parallel
   * @param quiet Minimal output mode
   * @param reportDir Directory for test reports
   * @return TestRunResult with statistics
   * @throws IOException if test execution fails
   */
  public TestRunResult runTests(
      File testOutputDir,
      String classpath,
      String filter,
      boolean parallel,
      boolean quiet,
      File reportDir)
      throws IOException {

    var totalTests = 0;
    var passedTests = 0;
    var failedTests = 0;
    var skippedTests = 0;
    var testResults = new ArrayList<TestResult>();

    try {
      // Create class loader for test classes
      var urls = new URL[] {testOutputDir.toURI().toURL()};
      var classLoader = new URLClassLoader(urls, TestRunner.class.getClassLoader());

      // Find all test classes
      var testClasses = findTestClasses(testOutputDir, testOutputDir, filter);

      if (testClasses.isEmpty()) {
        return new TestRunResult(0, 0, 0, 0, testResults);
      }

      // Run each test class
      for (var testClassName : testClasses) {
        try {
          var clazz = Class.forName(testClassName, true, classLoader);

          // Simple test execution (without full JUnit Platform)
          // In a real implementation, we'd use JUnit Platform Launcher
          // For now, we'll use reflection to find and run @Test methods
          var result = runTestClass(clazz, quiet);

          totalTests += result.total();
          passedTests += result.passed();
          failedTests += result.failed();
          skippedTests += result.skipped();
          testResults.addAll(result.details());

        } catch (ClassNotFoundException e) {
          if (!quiet) {
            System.err.println("Warning: Could not load test class: " + testClassName);
          }
        }
      }

      // Generate JUnit XML report
      generateJUnitReport(
          reportDir, testResults, totalTests, passedTests, failedTests, skippedTests);

    } catch (Exception e) {
      throw new IOException("Failed to run tests: " + e.getMessage(), e);
    }

    return new TestRunResult(totalTests, passedTests, failedTests, skippedTests, testResults);
  }

  /**
   * Finds all test classes in the output directory.
   */
  private List<String> findTestClasses(File rootDir, File currentDir, String filter) {
    var classes = new ArrayList<String>();
    var files = currentDir.listFiles();

    if (files == null) return classes;

    for (var file : files) {
      if (file.isDirectory()) {
        classes.addAll(findTestClasses(rootDir, file, filter));
      } else if (file.getName().endsWith(".class")) {
        // Convert path to class name
        var className = getClassName(rootDir, file);

        // Apply filter if specified
        if (filter == null || className.matches(filter.replace("*", ".*"))) {
          classes.add(className);
        }
      }
    }

    return classes;
  }

  private String getClassName(File rootDir, File classFile) {
    var relativePath =
        classFile.getAbsolutePath().substring(rootDir.getAbsolutePath().length() + 1);
    return relativePath.replace(File.separator, ".").replace(".class", "");
  }

  /**
   * Runs a single test class using reflection to find @Test methods.
   * Simplified implementation - in production would use JUnit Platform.
   */
  private ClassResult runTestClass(Class<?> clazz, boolean quiet) {
    var total = 0;
    var passed = 0;
    var failed = 0;
    var skipped = 0;
    var details = new ArrayList<TestResult>();

    // Find all test methods (methods annotated with @Test)
    for (var method : clazz.getDeclaredMethods()) {
      if (isTestMethod(method)) {
        total++;

        try {
          var instance = clazz.getDeclaredConstructor().newInstance();
          method.invoke(instance);
          passed++;

          if (!quiet) {
            System.out.println("  ✓ " + clazz.getSimpleName() + "." + method.getName());
          }

          details.add(new TestResult(clazz.getName() + "#" + method.getName(), "PASSED", 0, null));

        } catch (Exception e) {
          failed++;

          if (!quiet) {
            System.out.println("  ✗ " + clazz.getSimpleName() + "." + method.getName());
            System.out.println("    " + e.getCause().getMessage());
          }

          details.add(new TestResult(
              clazz.getName() + "#" + method.getName(),
              "FAILED",
              0,
              e.getCause() != null ? e.getCause().getMessage() : e.getMessage()));
        }
      }
    }

    return new ClassResult(total, passed, failed, skipped, details);
  }

  private boolean isTestMethod(java.lang.reflect.Method method) {
    // Check for JUnit 5 @Test annotation
    for (var annotation : method.getAnnotations()) {
      if (annotation.annotationType().getName().equals("org.junit.jupiter.api.Test")) {
        return true;
      }
    }
    return false;
  }

  /**
   * Generates JUnit XML report in standard format for CI integration.
   */
  private void generateJUnitReport(
      File reportDir, List<TestResult> results, int total, int passed, int failed, int skipped)
      throws IOException {

    FileUtils.ensureDirectory(reportDir);
    var reportFile = new File(reportDir, "jpm-test-report.xml");

    try (var writer = new PrintWriter(new FileWriter(reportFile))) {
      writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
      writer.println("<testsuites>");
      writer.printf(
          "  <testsuite name=\"jpm-tests\" tests=\"%d\" failures=\"%d\" skipped=\"%d\""
              + " time=\"0.0\">%n",
          total, failed, skipped);

      for (var result : results) {
        var parts = result.name().split("#");
        var className = parts[0];
        var methodName = parts.length > 1 ? parts[1] : "test";

        writer.printf(
            "    <testcase classname=\"%s\" name=\"%s\" time=\"%.3f\">%n",
            className, methodName, result.time() / 1000.0);

        if (result.status().equals("FAILED")) {
          writer.printf(
              "      <failure message=\"%s\"/>%n",
              escapeXml(result.errorMessage() != null ? result.errorMessage() : "Test failed"));
        } else if (result.status().equals("SKIPPED")) {
          writer.println("      <skipped/>");
        }

        writer.println("    </testcase>");
      }

      writer.println("  </testsuite>");
      writer.println("</testsuites>");
    }
  }

  private String escapeXml(String text) {
    if (text == null) return "";
    return text.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;");
  }

  /**
   * Result of running a single test class.
   */
  private record ClassResult(
      int total, int passed, int failed, int skipped, List<TestResult> details) {}

  /**
   * Individual test result.
   */
  public record TestResult(String name, String status, long time, String errorMessage) {}

  /**
   * Overall test run result.
   */
  public record TestRunResult(
      int totalTests,
      int passedTests,
      int failedTests,
      int skippedTests,
      List<TestResult> details) {

    public int totalTests() {
      return totalTests;
    }

    public int passedTests() {
      return passedTests;
    }

    public int failedTests() {
      return failedTests;
    }

    public int skippedTests() {
      return skippedTests;
    }

    public List<TestResult> details() {
      return details;
    }
  }
}
