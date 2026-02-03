package jpm.cli;

import java.io.File;
import java.util.concurrent.Callable;
import jpm.config.ConfigParser;
import jpm.config.ProjectPaths;
import jpm.deps.CacheManager;
import jpm.utils.UserOutput;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "remove", description = "Remove a dependency")
public class RemoveCommand implements Callable<Integer> {
  @Parameters(index = "0", description = "Artifact name (artifactId or group:artifact)")
  private String artifact;

  @Override
  public Integer call() {
    try {
      // Load config
      var config = CommandUtils.loadConfigOrFail();
      if (config == null) {
        return 1;
      }
      var configFile = new File(ProjectPaths.CONFIG_FILE);

      // Find dependency to remove
      String keyToRemove = config.findDependencyKey(artifact);

      if (keyToRemove == null) {
        CliErrorHandler.error("Dependency '" + artifact + "' not found in jpm.toml");
        return 1;
      }

      String groupId = keyToRemove.split(":")[0];

      // Remove from config
      var version = config.dependencies().get(keyToRemove);
      config.removeDependency(artifact);
      ConfigParser.save(config, configFile);

      // Clean from cache
      var cacheManager = new CacheManager();
      cacheManager.cleanArtifact(groupId, artifact);

      UserOutput.info("Removed " + keyToRemove + "=" + version);

      // Auto-sync IDE configuration
      CommandUtils.syncIdeConfig(config);

      return 0;

    } catch (Exception e) {
      CliErrorHandler.error(e.getMessage());
      return 1;
    }
  }
}
