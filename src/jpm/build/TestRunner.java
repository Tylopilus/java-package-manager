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
import jpm.utils.UserOutput;
import jpm.utils.XmlUtils;

/**
 * Runs JUnit 5 tests and generates reports.
 * Uses reflection to detect and execute JUnit tests without compile-time JUnit dependency.
 * Generates JUnit XML report for CI integration.
 */
public class TestRunner {

  /**
   * Runs tests from the compiled test classes directory.
   *
   * @param testOutputDir Directory containing compiled test classes
   * @param classpath Full classpath for test execution
   * @param filter Optional filter pattern for test class names
   * @param parallel Whether to run tests in parallel (currently unused)
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
      // Create class loader for test classes including classpath dependencies
      var urls = buildClasspathUrls(testOutputDir, classpath);
      var classLoader = new URLClassLoader(urls.toArray(new URL[0]), getClass().getClassLoader());

      // Find all test classes
      var testClassNames = findTestClasses(testOutputDir, filter);

      if (testClassNames.isEmpty()) {
        return new TestRunResult(0, 0, 0, 0, testResults);
      }

      if (!quiet) {
        UserOutput.info("Found " + testClassNames.size() + " test class(es)");
        UserOutput.print("");
      }

      // Load and run each test class
      for (var className : testClassNames) {
        try {
          var clazz = Class.forName(className, true, classLoader);
          var result = runTestClass(clazz, quiet);

          totalTests += result.total();
          passedTests += result.passed();
          failedTests += result.failed();
          skippedTests += result.skipped();
          testResults.addAll(result.details());
        } catch (ClassNotFoundException e) {
          if (!quiet) {
            UserOutput.warn("Warning: Could not load test class: " + className);
          }
        }
      }

      // Generate JUnit XML report
      generateJUnitReport(
          reportDir, testResults, totalTests, passedTests, failedTests, skippedTests);

      return new TestRunResult(totalTests, passedTests, failedTests, skippedTests, testResults);

    } catch (Exception e) {
      throw new IOException("Test execution failed: " + e.getMessage(), e);
    }
  }

  /**
   * Builds the list of URLs for the classloader.
   */
  private List<URL> buildClasspathUrls(File testOutputDir, String classpath) throws IOException {
    var urls = new ArrayList<URL>();
    urls.add(testOutputDir.toURI().toURL());

    if (classpath != null && !classpath.isEmpty()) {
      for (var entry : classpath.split(File.pathSeparator)) {
        if (!entry.isEmpty()) {
          var file = new File(entry);
          if (file.exists()) {
            urls.add(file.toURI().toURL());
          }
        }
      }
    }

    return urls;
  }

  /**
   * Finds all test class names in the test output directory.
   */
  private List<String> findTestClasses(File testOutputDir, String filter) {
    var classNames = new ArrayList<String>();

    if (!testOutputDir.exists()) {
      return classNames;
    }

    findTestClassesRecursive(testOutputDir, testOutputDir, filter, classNames);
    return classNames;
  }

  private void findTestClassesRecursive(
      File rootDir, File currentDir, String filter, List<String> classNames) {
    var files = currentDir.listFiles();
    if (files == null) return;

    for (var file : files) {
      if (file.isDirectory()) {
        findTestClassesRecursive(rootDir, file, filter, classNames);
      } else if (file.getName().endsWith(".class")) {
        var className = getClassName(rootDir, file);

        // Apply filter if specified
        if (filter == null || className.matches(filter.replace("*", ".*"))) {
          classNames.add(className);
        }
      }
    }
  }

  private String getClassName(File rootDir, File classFile) {
    var relativePath =
        classFile.getAbsolutePath().substring(rootDir.getAbsolutePath().length() + 1);
    return relativePath.replace(File.separator, ".").replace(".class", "");
  }

  /**
   * Runs all tests in a single test class using reflection.
   */
  private ClassResult runTestClass(Class<?> clazz, boolean quiet) {
    var total = 0;
    var passed = 0;
    var failed = 0;
    var skipped = 0;
    var details = new ArrayList<TestResult>();

    for (var method : clazz.getDeclaredMethods()) {
      if (isTestMethod(method)) {
        total++;
        var startTime = System.currentTimeMillis();

        try {
          // Check for @Disabled annotation
          if (hasAnnotation(method, "org.junit.jupiter.api.Disabled")) {
            skipped++;
            details.add(new TestResult(clazz.getName() + "#" + method.getName(), "SKIPPED", 0));
            continue;
          }

          // Create instance and run test
          var instance = createTestInstance(clazz);
          runBeforeEach(clazz, instance);
          method.invoke(instance);
          runAfterEach(clazz, instance);

          passed++;
          var duration = System.currentTimeMillis() - startTime;
          details.add(new TestResult(clazz.getName() + "#" + method.getName(), "PASSED", duration));

          if (!quiet) {
            UserOutput.info("  ✓ " + clazz.getSimpleName() + "." + method.getName());
          }

        } catch (Exception e) {
          failed++;
          var duration = System.currentTimeMillis() - startTime;
          var cause = e.getCause() != null ? e.getCause() : e;

          details.add(new TestResult(
              clazz.getName() + "#" + method.getName(), "FAILED", duration, cause.getMessage()));

          if (!quiet) {
            UserOutput.error("  ✗ " + clazz.getSimpleName() + "." + method.getName());
            UserOutput.error("    " + cause.getMessage());
          }
        }
      }
    }

    return new ClassResult(total, passed, failed, skipped, details);
  }

  private Object createTestInstance(Class<?> clazz) throws Exception {
    var constructor = clazz.getDeclaredConstructor();
    constructor.setAccessible(true);
    return constructor.newInstance();
  }

  private void runBeforeEach(Class<?> clazz, Object instance) throws Exception {
    for (var method : clazz.getDeclaredMethods()) {
      if (hasAnnotation(method, "org.junit.jupiter.api.BeforeEach")) {
        method.setAccessible(true);
        method.invoke(instance);
      }
    }
  }

  private void runAfterEach(Class<?> clazz, Object instance) throws Exception {
    for (var method : clazz.getDeclaredMethods()) {
      if (hasAnnotation(method, "org.junit.jupiter.api.AfterEach")) {
        method.setAccessible(true);
        method.invoke(instance);
      }
    }
  }

  /**
   * Checks if a method has a specific annotation by name.
   * Uses string comparison to avoid compile-time dependency on JUnit.
   */
  private boolean isTestMethod(java.lang.reflect.Method method) {
    return hasAnnotation(method, "org.junit.jupiter.api.Test");
  }

  private boolean hasAnnotation(java.lang.reflect.Method method, String annotationName) {
    for (var annotation : method.getAnnotations()) {
      if (annotation.annotationType().getName().equals(annotationName)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Generates JUnit XML report using XmlUtils.
   */
  private void generateJUnitReport(
      File reportDir, List<TestResult> results, int total, int passed, int failed, int skipped)
      throws IOException {

    FileUtils.ensureDirectory(reportDir);
    var reportFile = new File(reportDir, "jpm-test-report.xml");

    // Convert to XmlUtils format
    var xmlResults = new ArrayList<XmlUtils.TestResult>();
    for (var result : results) {
      xmlResults.add(new XmlUtils.TestResult(
          result.name(), result.status(), result.time(), result.errorMessage()));
    }

    var xmlContent = XmlUtils.generateJUnitReport("jpm-tests", total, failed, skipped, xmlResults);

    try (var writer = new PrintWriter(new FileWriter(reportFile))) {
      writer.print(xmlContent);
    }
  }

  /**
   * Result of running a single test class.
   */
  private record ClassResult(
      int total, int passed, int failed, int skipped, List<TestResult> details) {}

  /**
   * Individual test result.
   */
  public record TestResult(String name, String status, long time, String errorMessage) {
    public TestResult(String name, String status, long time) {
      this(name, status, time, null);
    }
  }

  /**
   * Overall test run result.
   */
  public record TestRunResult(
      int totalTests,
      int passedTests,
      int failedTests,
      int skippedTests,
      List<TestResult> details) {}
}
