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
    Map<String, String> dependencies,
    Map<String, ProfileConfig> profiles,
    FmtConfig fmt
) {

    public JpmConfig() {
        this(new PackageConfig(), new HashMap<>(), new HashMap<>(), new FmtConfig());
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
        return new JpmConfig(newPackage, new HashMap<>(dependencies), new HashMap<>(profiles), fmt);
    }

    /**
     * Creates a copy of this config with new dependencies.
     *
     * @param newDependencies the new dependencies map
     * @return a new JpmConfig instance
     */
    public JpmConfig withDependencies(Map<String, String> newDependencies) {
        return new JpmConfig(package_, new HashMap<>(newDependencies), new HashMap<>(profiles), fmt);
    }

    /**
     * Creates a copy of this config with new profiles.
     *
     * @param newProfiles the new profiles map
     * @return a new JpmConfig instance
     */
    public JpmConfig withProfiles(Map<String, ProfileConfig> newProfiles) {
        return new JpmConfig(package_, new HashMap<>(dependencies), new HashMap<>(newProfiles), fmt);
    }

    /**
     * Creates a copy of this config with new fmt configuration.
     *
     * @param newFmt the new fmt config
     * @return a new JpmConfig instance
     */
    public JpmConfig withFmt(FmtConfig newFmt) {
        return new JpmConfig(package_, new HashMap<>(dependencies), new HashMap<>(profiles), newFmt);
    }
}
