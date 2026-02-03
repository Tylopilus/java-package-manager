package jpm.cli;

import jpm.build.ClasspathGenerator;
import jpm.config.ConfigParser;
import jpm.config.JpmConfig;
import jpm.deps.CacheManager;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.util.concurrent.Callable;

@Command(name = "remove", description = "Remove a dependency")
public class RemoveCommand implements Callable<Integer> {
    @Parameters(index = "0", description = "Artifact name (artifactId or group:artifact)")
    private String artifact;

    @Override
    public Integer call() {
        try {
            // Load config
            File configFile = new File("jpm.toml");
            if (!configFile.exists()) {
                System.err.println("Error: No jpm.toml found.");
                return 1;
            }

            JpmConfig config = ConfigParser.load(configFile);

            // Find dependency to remove
            String keyToRemove = null;
            String groupId = null;

            for (String key : config.getDependencies().keySet()) {
                String[] parts = key.split(":");
                if (parts.length == 2) {
                    if (parts[1].equals(artifact) || key.equals(artifact)) {
                        keyToRemove = key;
                        groupId = parts[0];
                        break;
                    }
                }
            }

            if (keyToRemove == null) {
                System.err.println("Error: Dependency '" + artifact + "' not found in jpm.toml");
                return 1;
            }

            // Remove from config
            String version = config.getDependencies().get(keyToRemove);
            config.removeDependency(artifact);
            ConfigParser.save(config, configFile);

            // Clean from cache
            CacheManager cacheManager = new CacheManager();
            cacheManager.cleanArtifact(groupId, artifact);

            System.out.println("Removed " + keyToRemove + "=" + version);

            // Auto-sync IDE configuration
            System.out.println("Syncing IDE configuration...");
            ClasspathGenerator generator = new ClasspathGenerator();
            generator.generateClasspath(config, new File("."));
            System.out.println("Updated .classpath file");

            return 0;

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            return 1;
        }
    }
}
