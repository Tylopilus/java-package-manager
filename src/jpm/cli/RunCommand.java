package jpm.cli;

import java.io.File;
import java.util.concurrent.Callable;
import jpm.build.ClasspathBuilder;
import jpm.build.Compiler;
import jpm.build.IdeFileGenerator;
import jpm.build.Runner;
import jpm.config.ConfigParser;
import jpm.deps.DependencyResolver;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "run", description = "Build and run the project")
public class RunCommand implements Callable<Integer> {
  @Parameters(arity = "0..*", description = "Arguments to pass to main class")
  private String[] args;

  @Option(
      names = {"--force-resolve"},
      description = "Force re-resolution of dependencies, ignoring lockfile")
  private boolean forceResolve;

  @Option(
      names = {"--no-ide-files"},
      description = "Skip generation of IDE configuration files (.project, .classpath)")
  private boolean noIdeFiles;

  @Option(
      names = {"--profile"},
      description = "Build profile to use (dev, release, test)",
      defaultValue = "dev")
  private String profile;

  @Override
  public Integer call() {
    try {
      // Check for jpm.toml
      var configFile = new File("jpm.toml");
      if (!configFile.exists()) {
        System.err.println("Error: No jpm.toml found. Run 'jpm new <name>' first.");
        return 1;
      }

      // Load config
      var config = ConfigParser.load(configFile);

      System.out.println(
          "Building " + config.package_().name() + " v" + config.package_().version());

      // Generate IDE files if missing and not disabled
      var projectDir = new File(".");
      if (!noIdeFiles && IdeFileGenerator.shouldGenerateIdeFiles(projectDir)) {
        // We'll generate .project now, and .classpath after dependency resolution
        IdeFileGenerator.generateProjectFile(projectDir, config);
      }

      // Resolve dependencies
      var classpath = "";
      if (!config.dependencies().isEmpty()) {
        var resolver = new DependencyResolver();
        var deps = resolver.resolveWithLockfile(projectDir, config, forceResolve);
        classpath = ClasspathBuilder.buildClasspath(deps);
        System.out.println("Resolved " + deps.size() + " dependencies");
      }

      // Generate .classpath after dependency resolution if needed
      var classpathExisted = new File(projectDir, ".classpath").exists();
      if (!noIdeFiles && !classpathExisted) {
        IdeFileGenerator.generateClasspathFile(projectDir, config, classpath);
        classpathExisted = true;
      }

      // Show message if both files were generated
      if (!noIdeFiles) {
        var projectExisted = new File(projectDir, ".project").exists();
        if (!projectExisted || !classpathExisted) {
          System.out.println("Generated IDE configuration files (.project, .classpath)");
        }
      }

      // Show message if both files were generated
      if (!noIdeFiles
          && (IdeFileGenerator.shouldGenerateIdeFiles(projectDir)
              || (!new File(projectDir, ".project").exists()
                  && !new File(projectDir, ".classpath").exists()))) {
        // This won't show since we just generated them, but let's check before generation
      }

      if (!noIdeFiles) {
        var projectExisted = new File(projectDir, ".project").exists();
        if (!projectExisted) {
          System.out.println("Generated IDE configuration files (.project, .classpath)");
        }
      }

      // Compile with profile-specific settings
      var sourceDir = new File("src");
      var outputDir = new File("target/classes");

      if (!sourceDir.exists()) {
        System.err.println("Error: No src/ directory found");
        return 1;
      }

      // Load profile configuration
      var profileConfig = config.profiles().getOrDefault(profile, jpm.config.ProfileConfig.dev());

      System.out.println("Using profile: " + profile);

      var compiler = new Compiler();
      var compilerArgs = profileConfig.getEffectiveCompilerArgs();
      var compileResult = compiler.compileWithArgs(sourceDir, outputDir, classpath, compilerArgs);

      if (!compileResult.success()) {
        System.err.println("Build failed with exit code " + compileResult.exitCode());
        return compileResult.exitCode();
      }

      System.out.println("Running...\n");

      // Run with profile-specific JVM args
      var runner = new Runner();
      var mainClass = "Main";
      var jvmArgs = profileConfig.getEffectiveJvmArgs();

      Runner.RunResult runResult;
      if (args != null && args.length > 0) {
        runResult = runner.runWithArgsAndJvmArgs(mainClass, outputDir, classpath, args, jvmArgs);
      } else {
        runResult = runner.runWithJvmArgs(mainClass, outputDir, classpath, jvmArgs);
      }

      return runResult.exitCode();

    } catch (Exception e) {
      System.err.println("Error: " + e.getMessage());
      e.printStackTrace();
      return 1;
    }
  }
}
