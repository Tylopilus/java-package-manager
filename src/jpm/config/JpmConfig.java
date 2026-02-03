package jpm.config;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration record for JPM projects.
 * Uses Java 16+ records for concise immutable data classes,
 * with a mutable dependencies map for practical configuration management.
 */
public record JpmConfig(
    PackageConfig package_,
    Map<String, String> dependencies
) {
    
    public JpmConfig() {
        this(new PackageConfig(), new HashMap<>());
    }
    
    /**
     * Nested record for package configuration.
     */
    public record PackageConfig(String name, String version, String javaVersion) {
        public PackageConfig() {
            this(null, null, null);
        }
    }
    
    /**
     * Adds a dependency to the configuration.
     * 
     * @param ga the group:artifact coordinate
     * @param version the version string
     */
    public void addDependency(String ga, String version) {
        dependencies.put(ga, version);
    }
    
    /**
     * Removes a dependency by artifact ID.
     * 
     * @param artifactId the artifact ID to remove
     */
    public void removeDependency(String artifactId) {
        dependencies.entrySet().removeIf(entry -> {
            var parts = entry.getKey().split(":");
            return parts.length == 2 && parts[1].equals(artifactId);
        });
    }
    
    /**
     * Creates a copy of this config with a new package configuration.
     * 
     * @param newPackage the new package config
     * @return a new JpmConfig instance
     */
    public JpmConfig withPackage(PackageConfig newPackage) {
        return new JpmConfig(newPackage, new HashMap<>(dependencies));
    }
    
    /**
     * Creates a copy of this config with new dependencies.
     * 
     * @param newDependencies the new dependencies map
     * @return a new JpmConfig instance
     */
    public JpmConfig withDependencies(Map<String, String> newDependencies) {
        return new JpmConfig(package_, new HashMap<>(newDependencies));
    }
}
