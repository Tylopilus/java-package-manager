package jpm.cli;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import jpm.build.ClasspathGenerator;
import jpm.config.ConfigParser;
import jpm.config.ProjectPaths;
import jpm.deps.DependencyResolver;
import jpm.deps.MavenSearchClient;
import jpm.utils.FileUtils;
import jpm.utils.XmlUtils;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "add", description = "Add one or more dependencies")
public class AddCommand implements Callable<Integer> {

  @Parameters(
      arity = "1..*",
      description = "Dependencies to add (group:artifact:version, or search terms)")
  private List<String> dependencies;

  @Option(
      names = {"-y", "--yes"},
      description = "Auto-confirm all prompts")
  private boolean autoConfirm;

  @Option(
      names = {"--no-search"},
      description = "Disable search, require exact coordinates")
  private boolean noSearch;

  @Override
  public Integer call() {
    try {
      // Check for jpm.toml
      var configFile = new File(ProjectPaths.CONFIG_FILE);
      if (!configFile.exists()) {
        CliErrorHandler.error(
            "No " + ProjectPaths.CONFIG_FILE + " found. Run 'jpm new <name>' first.");
        return 1;
      }

      var total = dependencies.size();

      var config = ConfigParser.loadOrCreate(configFile);
      var selectionService = new DependencySelectionService(
          new MavenSearchClient(),
          new DependencyPrompter(new BufferedReader(new InputStreamReader(System.in))),
          autoConfirm,
          noSearch,
          config);

      // Track all dependencies to add
      var depsToAdd = new ArrayList<DependencyInfo>();

      // Phase 1: Collect all dependencies (with interactive selection)
      for (int i = 0; i < total; i++) {
        var dep = dependencies.get(i);

        if (total > 1) {
          System.out.println("\n[" + (i + 1) + "/" + total + "] Processing \"" + dep + "\"...");
        }

        var info = selectionService.resolveDependency(dep);
        if (info != null) {
          depsToAdd.add(info);
        }
      }

      if (depsToAdd.isEmpty()) {
        System.out.println("\nNo dependencies to add.");
        return 0;
      }

      // Phase 2: Resolve and add all dependencies
      System.out.println("\nResolving " + depsToAdd.size() + " dependencies...");

      var resolver = new DependencyResolver();
      var totalResolved = 0;

      for (var info : depsToAdd) {
        var ga = info.groupId() + ":" + info.artifactId();

        try {
          // Resolve the dependency
          var resolved = resolver.resolve(info.groupId(), info.artifactId(), info.version());

          if (!resolved.isEmpty()) {
            // Add to config
            config.addDependency(ga, info.version());
            totalResolved += resolved.size();
            System.out.println(
                "  Added " + ga + ":" + info.version() + " (" + resolved.size() + " deps)");
          }
        } catch (Exception e) {
          System.err.println("  Error resolving " + ga + ": " + e.getMessage());
        }
      }

      // Save config
      ConfigParser.save(config, configFile);
      System.out.println(
          "\nAdded " + depsToAdd.size() + " dependencies to " + ProjectPaths.CONFIG_FILE);
      System.out.println("Total resolved: " + totalResolved + " artifacts");

      // Ensure .project file exists
      var projectFile = new File(ProjectPaths.DOT_PROJECT);
      if (!projectFile.exists()) {
        System.out.println("\nCreating " + ProjectPaths.DOT_PROJECT + " file...");
        var projectXml = XmlUtils.generateProjectFile(config.package_().name());
        FileUtils.writeFile(projectFile, projectXml);
      }

      // Sync IDE configuration once at the end
      System.out.println("\nSyncing IDE configuration...");
      var generator = new ClasspathGenerator();
      generator.generateClasspath(config, new File("."));
      System.out.println("Generated " + ProjectPaths.DOT_CLASSPATH + " file");

      return 0;

    } catch (Exception e) {
      CliErrorHandler.error(e.getMessage());
      return 1;
    }
  }
}
