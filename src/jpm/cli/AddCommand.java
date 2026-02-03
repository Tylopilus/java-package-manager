package jpm.cli;

import jpm.config.ConfigParser;
import jpm.config.JpmConfig;
import jpm.deps.DependencyResolver;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.util.List;
import java.util.concurrent.Callable;

@Command(name = "add", description = "Add a dependency")
public class AddCommand implements Callable<Integer> {
    @Parameters(index = "0", description = "Dependency (group:artifact:version)")
    private String dependency;

    @Override
    public Integer call() {
        try {
            // Parse dependency string
            String[] parts = dependency.split(":");
            if (parts.length != 3) {
                System.err.println("Error: Dependency must be in format group:artifact:version");
                System.err.println("Example: com.google.guava:guava:32.1.3-jre");
                return 1;
            }
            
            String groupId = parts[0];
            String artifactId = parts[1];
            String version = parts[2];
            String ga = groupId + ":" + artifactId;
            
            // Load or create config
            File configFile = new File("jpm.toml");
            if (!configFile.exists()) {
                System.err.println("Error: No jpm.toml found. Run 'jpm new <name>' first.");
                return 1;
            }
            
            JpmConfig config = ConfigParser.loadOrCreate(configFile);
            
            System.out.println("Adding dependency " + ga + "=" + version);
            
            // Check if already exists
            if (config.getDependencies().containsKey(ga)) {
                String existingVersion = config.getDependencies().get(ga);
                if (existingVersion.equals(version)) {
                    System.out.println("Dependency " + ga + " already at version " + version);
                    return 0;
                } else {
                    System.out.println("Updating " + ga + " " + existingVersion + " -> " + version);
                }
            }
            
            // Resolve dependency and its transitive dependencies
            DependencyResolver resolver = new DependencyResolver();
            List<DependencyResolver.ResolvedDependency> resolved = resolver.resolve(groupId, artifactId, version);
            
            if (resolved.isEmpty()) {
                System.err.println("Error: Failed to resolve dependency " + dependency);
                return 1;
            }
            
            // Add to config
            config.addDependency(ga, version);
            ConfigParser.save(config, configFile);
            
            // Print summary
            System.out.println("\nResolved " + resolved.size() + " dependencies:");
            for (DependencyResolver.ResolvedDependency dep : resolved) {
                System.out.println("  " + dep);
            }
            
            System.out.println("\nAdded " + ga + "=" + version + " to jpm.toml");
            return 0;
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            return 1;
        }
    }
}
