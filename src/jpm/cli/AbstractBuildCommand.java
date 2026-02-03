package jpm.cli;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;

import jpm.build.ClasspathBuilder;
import jpm.build.Compiler;
import jpm.build.IdeFileGenerator;
import jpm.config.ConfigParser;
import jpm.config.JpmConfig;
import jpm.config.ProfileConfig;
import jpm.config.ProjectPaths;
import jpm.deps.DependencyResolver;
import jpm.deps.ResolvedDependency;

import picocli.CommandLine.Option;

/**
 * Abstract base class for build-related commands (build, run, test).
 * Provides common lifecycle: validate -> configure -> resolve deps -> compile -> execute.
 *
 * Subclasses override compile() and execute() to provide command-specific behavior.
 */
public abstract class AbstractBuildCommand implements Callable<Integer> {

  @Option(
      names = {"--force-resolve"},
      description = "Force re-resolution of dependencies, ignoring lockfile")
  protected boolean forceResolve;

  @Option(
      names = {"--no-ide-files"},
      description = "Skip generation of IDE configuration files (.project, .classpath)")
  protected boolean noIdeFiles;

  @Option(
      names = {"--profile"},
      description = "Build profile to use (dev, release, test)",
      defaultValue = "dev")
  protected String profile;

  // Shared state populated during lifecycle
  protected File projectDir;
  protected JpmConfig config;
  protected List<ResolvedDependency> resolvedDeps;
  protected String classpath;
  protected ProfileConfig profileConfig;

  @Override
  public Integer call() {
    try {
      projectDir = new File(".");

      validateProject();
      loadConfiguration();
      printBuildHeader();
      generateIdeFilesIfNeeded();
      resolveDependencies();
      generateClasspathFileIfNeeded();
      loadProfile();

      var compileResult = compile();
      if (!compileResult.success()) {
        System.err.println("Build failed with exit code " + compileResult.exitCode());
        return compileResult.exitCode();
      }

      return execute();

    } catch (ProjectValidationException e) {
      CliErrorHandler.error(e.getMessage());
      return 1;
    } catch (Exception e) {
      CliErrorHandler.error(e.getMessage());
      return 1;
    }
  }

  /**
   * Validates that the project is properly configured.
   * Subclasses can override to add additional validation.
   */
  protected void validateProject() {
    var configFile = new File(ProjectPaths.CONFIG_FILE);
    if (!configFile.exists()) {
      throw new ProjectValidationException(
          "No " + ProjectPaths.CONFIG_FILE + " found. Run 'jpm new <name>' first.");
    }
  }

  /**
   * Validates that a required source directory exists.
   */
  protected void validateSourceDirExists(File sourceDir, String sourceDirPath) {
    if (!sourceDir.exists()) {
      throw new ProjectValidationException("No " + sourceDirPath + "/ directory found");
    }
  }

  /**
   * Loads the project configuration from jpm.toml.
   */
  protected void loadConfiguration() throws IOException {
    config = ConfigParser.load(new File(ProjectPaths.CONFIG_FILE));
  }

  /**
   * Prints the build header with project name and version.
   * Subclasses can override for custom headers.
   */
  protected void printBuildHeader() {
    System.out.println(
        "Building " + config.package_().name() + " v" + config.package_().version());
  }

  /**
   * Generates IDE files (.project) if needed and not disabled.
   */
  protected void generateIdeFilesIfNeeded() throws IOException {
    if (!noIdeFiles && IdeFileGenerator.shouldGenerateIdeFiles(projectDir)) {
      IdeFileGenerator.generateProjectFile(projectDir, config);
    }
  }

  /**
   * Resolves project dependencies and builds the classpath.
   */
  protected void resolveDependencies() throws Exception {
    classpath = "";
    if (!config.dependencies().isEmpty()) {
      var resolver = new DependencyResolver();
      resolvedDeps = resolver.resolveWithLockfile(projectDir, config, forceResolve);
      classpath = ClasspathBuilder.buildClasspath(resolvedDeps);
      System.out.println("Resolved " + resolvedDeps.size() + " dependencies");
    }
  }

  /**
   * Generates .classpath file after dependency resolution if needed.
   */
  protected void generateClasspathFileIfNeeded() throws IOException {
    if (!noIdeFiles && !new File(projectDir, ProjectPaths.DOT_CLASSPATH).exists()) {
      IdeFileGenerator.generateClasspathFile(projectDir, config, classpath);
      System.out.println("Generated IDE configuration files (.project, .classpath)");
    }
  }

  /**
   * Loads the build profile configuration.
   * Uses the profile specified via --profile option, defaults to "dev".
   */
  protected void loadProfile() {
    profileConfig = config.profiles().getOrDefault(profile, getDefaultProfile());
    System.out.println("Using profile: " + profile);
  }

  /**
   * Returns the default profile to use if none is configured.
   * Subclasses can override (e.g., TestCommand uses test profile).
   */
  protected ProfileConfig getDefaultProfile() {
    return ProfileConfig.dev();
  }

  /**
   * Returns the source directory for compilation.
   * Subclasses can override for different source locations.
   */
  protected File getSourceDir() {
    return new File(ProjectPaths.SRC_DIR);
  }

  /**
   * Returns the output directory for compiled classes.
   * Subclasses can override for different output locations.
   */
  protected File getOutputDir() {
    return new File(ProjectPaths.CLASSES_DIR);
  }

  /**
   * Compiles project sources using the active profile configuration.
   */
  protected Compiler.CompileResult compileSources(File sourceDir, File outputDir) throws Exception {
    var compiler = new Compiler();
    var compilerArgs = profileConfig.getEffectiveCompilerArgs();
    return compiler.compile(sourceDir, outputDir, classpath, compilerArgs);
  }

  /**
   * Compiles the project source code.
   * Subclasses must implement this to provide compilation logic.
   *
   * @return the compilation result
   */
  protected abstract Compiler.CompileResult compile() throws Exception;

  /**
   * Executes the command-specific action after successful compilation.
   * For BuildCommand this prints success message.
   * For RunCommand this runs the main class.
   * For TestCommand this runs the tests.
   *
   * @return exit code (0 for success)
   */
  protected abstract int execute() throws Exception;

  /**
   * Returns the name of this command for logging purposes.
   */
  protected abstract String getCommandName();
}
