package jpm.cli;

import java.io.File;
import java.util.concurrent.Callable;
import jpm.build.ClasspathGenerator;
import jpm.config.ConfigParser;
import jpm.deps.CacheManager;
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
      var configFile = new File("jpm.toml");
      if (!configFile.exists()) {
        CliErrorHandler.error("No jpm.toml found.");
        return 1;
      }

      var config = ConfigParser.load(configFile);

      // Find dependency to remove
      String keyToRemove = null;
      String groupId = null;

      for (var key : config.dependencies().keySet()) {
        var parts = key.split(":");
        if (parts.length == 2) {
          if (parts[1].equals(artifact) || key.equals(artifact)) {
            keyToRemove = key;
            groupId = parts[0];
            break;
          }
        }
      }

      if (keyToRemove == null) {
        CliErrorHandler.error("Dependency '" + artifact + "' not found in jpm.toml");
        return 1;
      }

      // Remove from config
      var version = config.dependencies().get(keyToRemove);
      config.removeDependency(artifact);
      ConfigParser.save(config, configFile);

      // Clean from cache
      var cacheManager = new CacheManager();
      cacheManager.cleanArtifact(groupId, artifact);

      System.out.println("Removed " + keyToRemove + "=" + version);

      // Auto-sync IDE configuration
      System.out.println("Syncing IDE configuration...");
      var generator = new ClasspathGenerator();
      generator.generateClasspath(config, new File("."));
      System.out.println("Updated .classpath file");

      return 0;

    } catch (Exception e) {
      CliErrorHandler.error(e.getMessage());
      return 1;
    }
  }
}
