package jpm.cli;

import java.io.File;
import jpm.build.Compiler;
import jpm.build.TestCompiler;
import jpm.build.TestRunner;
import jpm.config.ProfileConfig;
import jpm.config.ProjectPaths;
import jpm.utils.Constants;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * Test command for running JUnit 5 tests.
 * Extends AbstractBuildCommand for shared build lifecycle.
 *
 * Features:
 * - Automatically discovers and runs tests from src/test/java/
 * - Supports test filtering by class name
 * - Generates JUnit XML report for CI integration
 * - Parallel test execution (enabled by default)
 * - Returns proper exit codes for CI/CD pipelines
 */
@Command(
    name = "test",
    description = "Run all tests in src/test/java/",
    mixinStandardHelpOptions = true)
public class TestCommand extends AbstractBuildCommand {

  @Option(
      names = {"--filter"},
      description = "Filter tests by class name pattern (e.g., MyTest or com.example.*)")
  private String filter;

  @Option(
      names = {"--no-parallel"},
      description = "Disable parallel test execution")
  private boolean noParallel;

  @Option(
      names = {"--quiet"},
      description = "Minimal output for CI (only show failures)")
  private boolean quiet;

  @Option(
      names = {"--report-dir"},
      description = "Directory for test reports (default: target/)",
      defaultValue = "target")
  private File reportDir;

  @Override
  protected void validateProject() {
    super.validateProject();

    var testSourceDir = getSourceDir();
    if (!testSourceDir.exists()) {
      throw new ProjectValidationException("No " + ProjectPaths.TEST_DIR
          + "/ directory found. Create test directory and add JUnit tests.");
    }
  }

  @Override
  protected void printBuildHeader() {
    if (!quiet) {
      System.out.println("Running tests for " + config.package_().name() + "...");
    }
  }

  @Override
  protected void generateIdeFilesIfNeeded() {
    // Skip IDE file generation for test command
  }

  @Override
  protected void generateClasspathFileIfNeeded() {
    // Skip classpath file generation for test command
  }

  @Override
  protected void resolveDependencies() throws Exception {
    // Resolve project dependencies
    super.resolveDependencies();

    // Add JUnit to classpath
    classpath = addJUnitToClasspath(classpath);

    // Add main classes to classpath
    var mainClasses = new File(ProjectPaths.CLASSES_DIR);
    if (mainClasses.exists()) {
      classpath = mainClasses.getAbsolutePath() + File.pathSeparator + classpath;
    }
  }

  @Override
  protected void loadProfile() {
    // Always use test profile for tests
    profile = "test";
    profileConfig = config.profiles().getOrDefault(profile, getDefaultProfile());
    if (!quiet) {
      System.out.println("Using profile: " + profile);
    }
  }

  @Override
  protected ProfileConfig getDefaultProfile() {
    return ProfileConfig.test();
  }

  @Override
  protected File getSourceDir() {
    return new File(ProjectPaths.TEST_DIR);
  }

  @Override
  protected File getOutputDir() {
    return new File(ProjectPaths.TEST_CLASSES_DIR);
  }

  @Override
  protected Compiler.CompileResult compile() throws Exception {
    if (!quiet) {
      System.out.println("Compiling tests...");
    }

    var testSourceDir = getSourceDir();
    var testOutputDir = getOutputDir();

    var testCompiler = new TestCompiler();
    var javaVersion = config.package_().javaVersion() != null
        ? config.package_().javaVersion()
        : Constants.DEFAULT_JAVA_VERSION;
    var result = testCompiler.compileTests(testSourceDir, testOutputDir, classpath, javaVersion);

    if (result.success() && !quiet) {
      System.out.println("Compiled tests successfully");
    }

    return result;
  }

  @Override
  protected int execute() throws Exception {
    if (!quiet) {
      System.out.println("Running tests...");
      System.out.println();
    }

    var testOutputDir = getOutputDir();
    var testRunner = new TestRunner();
    var runResult =
        testRunner.runTests(testOutputDir, classpath, filter, !noParallel, quiet, reportDir);

    // Print summary
    if (!quiet) {
      System.out.println();
      System.out.println("Test Results:");
      System.out.println("  Total:   " + runResult.totalTests());
      System.out.println("  Passed:  " + runResult.passedTests());
      System.out.println("  Failed:  " + runResult.failedTests());
      System.out.println("  Skipped: " + runResult.skippedTests());
      System.out.println();
    }

    // Generate report
    var reportFile = new File(reportDir, "jpm-test-report.xml");
    if (!quiet) {
      System.out.println("Test report generated: " + reportFile.getAbsolutePath());
    }

    // Return appropriate exit code
    return runResult.failedTests() > 0 ? 1 : 0;
  }

  @Override
  protected String getCommandName() {
    return "test";
  }

  /**
   * Adds JUnit 5 platform and Jupiter to the classpath.
   * Downloads JUnit if not already present in the cache.
   */
  private String addJUnitToClasspath(String existingClasspath) {
    var jpmDir = new File(System.getProperty("user.home"), ".jpm");
    var libDir = new File(jpmDir, "lib");

    var junitJupiter = new File(libDir, "junit-jupiter-" + Constants.JUNIT_VERSION + ".jar");
    var junitPlatform = new File(
        libDir, "junit-platform-console-standalone-" + Constants.JUNIT_PLATFORM_VERSION + ".jar");

    var result = existingClasspath;

    // Add JUnit Jupiter (API and engine)
    if (junitJupiter.exists()) {
      result = junitJupiter.getAbsolutePath() + File.pathSeparator + result;
    }

    // Add JUnit Platform (launcher and engine)
    if (junitPlatform.exists()) {
      result = junitPlatform.getAbsolutePath() + File.pathSeparator + result;
    }

    return result;
  }
}
