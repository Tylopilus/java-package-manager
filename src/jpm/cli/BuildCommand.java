package jpm.cli;

import java.io.File;
import java.util.concurrent.Callable;
import jpm.build.ClasspathBuilder;
import jpm.build.Compiler;
import jpm.build.IdeFileGenerator;
import jpm.config.ConfigParser;
import jpm.deps.DependencyResolver;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "build", description = "Build the project")
public class BuildCommand implements Callable<Integer> {

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
      var result = compiler.compileWithArgs(sourceDir, outputDir, classpath, compilerArgs);

      if (result.success()) {
        System.out.println("Build successful! Output in target/classes/");
        return 0;
      } else {
        System.err.println("Build failed with exit code " + result.exitCode());
        return result.exitCode();
      }

    } catch (Exception e) {
      System.err.println("Error: " + e.getMessage());
      e.printStackTrace();
      return 1;
    }
  }
}
