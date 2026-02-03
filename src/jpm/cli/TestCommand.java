package jpm.cli;

import jpm.build.TestCompiler;
import jpm.build.TestRunner;
import jpm.config.ConfigParser;
import jpm.config.JpmConfig;
import jpm.deps.DependencyResolver;
import jpm.build.ClasspathBuilder;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.File;
import java.util.concurrent.Callable;

/**
 * Test command for running JUnit 5 tests.
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
    mixinStandardHelpOptions = true
)
public class TestCommand implements Callable<Integer> {
    
    @Option(
        names = {"--filter"},
        description = "Filter tests by class name pattern (e.g., MyTest or com.example.*)"
    )
    private String filter;
    
    @Option(
        names = {"--no-parallel"},
        description = "Disable parallel test execution"
    )
    private boolean noParallel;
    
    @Option(
        names = {"--quiet"},
        description = "Minimal output for CI (only show failures)"
    )
    private boolean quiet;
    
    @Option(
        names = {"--report-dir"},
        description = "Directory for test reports (default: target/)",
        defaultValue = "target"
    )
    private File reportDir;
    
    private static final String JUNIT_VERSION = "5.11.3";
    private static final String JUNIT_PLATFORM_VERSION = "1.11.3";
    
    @Override
    public Integer call() {
        try {
            // Check for jpm.toml
            var configFile = new File("jpm.toml");
            if (!configFile.exists()) {
                System.err.println("Error: No jpm.toml found. Run 'jpm new <name>' first.");
                return 1;
            }
            
            // Check for test directory
            var testSourceDir = new File("src/test/java");
            if (!testSourceDir.exists()) {
                System.err.println("Error: No src/test/java/ directory found.");
                System.err.println("Create test directory and add JUnit tests.");
                return 1;
            }
            
            // Load config
            var config = ConfigParser.load(configFile);
            if (config == null) {
                System.err.println("Error: Failed to load jpm.toml");
                return 1;
            }
            
            if (!quiet) {
                System.out.println("Running tests for " + config.package_().name() + "...");
            }
            
            // Resolve dependencies including dev-dependencies
            var resolver = new DependencyResolver();
            var deps = resolver.resolveWithLockfile(new File("."), config, false);
            
            // Add JUnit 5 to classpath (auto-included)
            var classpath = ClasspathBuilder.buildClasspath(deps);
            classpath = addJUnitToClasspath(classpath);
            
            // Also add main classes to classpath
            var mainClasses = new File("target/classes");
            if (mainClasses.exists()) {
                classpath = mainClasses.getAbsolutePath() + File.pathSeparator + classpath;
            }
            
            // Compile tests
            if (!quiet) {
                System.out.println("Compiling tests...");
            }
            
            var testCompiler = new TestCompiler();
            var testOutputDir = new File("target/test-classes");
            var compileResult = testCompiler.compileTests(testSourceDir, testOutputDir, classpath);
            
            if (!compileResult.success()) {
                System.err.println("Test compilation failed with exit code " + compileResult.exitCode());
                return compileResult.exitCode();
            }
            
            if (!quiet) {
                System.out.println("Compiled tests successfully");
            }
            
            // Run tests
            if (!quiet) {
                System.out.println("Running tests...");
                System.out.println();
            }
            
            var testRunner = new TestRunner();
            var runResult = testRunner.runTests(
                testOutputDir,
                classpath,
                filter,
                !noParallel,
                quiet,
                reportDir
            );
            
            // Print summary
            if (!quiet) {
                System.out.println();
                System.out.println("Test Results:");
                System.out.println("  Total:  " + runResult.totalTests());
                System.out.println("  Passed: " + runResult.passedTests());
                System.out.println("  Failed: " + runResult.failedTests());
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
            
        } catch (Exception e) {
            System.err.println("Error running tests: " + e.getMessage());
            if (!quiet) {
                e.printStackTrace();
            }
            return 1;
        }
    }
    
    /**
     * Adds JUnit 5 platform and Jupiter to the classpath.
     * Downloads JUnit if not already present in the cache.
     */
    private String addJUnitToClasspath(String existingClasspath) {
        var jpmDir = new File(System.getProperty("user.home"), ".jpm");
        var libDir = new File(jpmDir, "lib");
        
        var junitJupiter = new File(libDir, "junit-jupiter-" + JUNIT_VERSION + ".jar");
        var junitPlatform = new File(libDir, "junit-platform-console-standalone-" + JUNIT_PLATFORM_VERSION + ".jar");
        
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
