package jpm.cli;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import jpm.config.ConfigParser;
import jpm.config.ProjectPaths;
import jpm.deps.DependencyResolver;
import jpm.deps.MavenSearchClient;
import jpm.utils.UserOutput;
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
      var config = CommandUtils.loadConfigOrFail();
      if (config == null) {
        return 1;
      }

      var total = dependencies.size();
      var configFile = new File(ProjectPaths.CONFIG_FILE);

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
          UserOutput.info("\n[" + (i + 1) + "/" + total + "] Processing \"" + dep + "\"...");
        }

        var info = selectionService.resolveDependency(dep);
        if (info != null) {
          depsToAdd.add(info);
        }
      }

      if (depsToAdd.isEmpty()) {
        UserOutput.info("\nNo dependencies to add.");
        return 0;
      }

      // Phase 2: Resolve and add all dependencies
      UserOutput.info("\nResolving " + depsToAdd.size() + " dependencies...");

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
            UserOutput.info(
                "  Added " + ga + ":" + info.version() + " (" + resolved.size() + " deps)");
          }
        } catch (Exception e) {
          UserOutput.error("  Error resolving " + ga, e);
        }
      }

      // Save config
      ConfigParser.save(config, configFile);
      UserOutput.info(
          "\nAdded " + depsToAdd.size() + " dependencies to " + ProjectPaths.CONFIG_FILE);
      UserOutput.info("Total resolved: " + totalResolved + " artifacts");

      // Ensure .project file exists
      CommandUtils.ensureProjectFiles(config);

      // Sync IDE configuration once at the end
      CommandUtils.syncIdeConfig(config);

      return 0;

    } catch (Exception e) {
      CliErrorHandler.error(e.getMessage());
      return 1;
    }
  }
}
