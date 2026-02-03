package jpm.config;

import java.util.HashMap;
import java.util.Map;

public class JpmConfig {
    private PackageConfig package_;
    private Map<String, String> dependencies;
    
    public JpmConfig() {
        this.package_ = new PackageConfig();
        this.dependencies = new HashMap<>();
    }
    
    public PackageConfig getPackage() {
        return package_;
    }
    
    public void setPackage(PackageConfig package_) {
        this.package_ = package_;
    }
    
    public Map<String, String> getDependencies() {
        return dependencies;
    }
    
    public void setDependencies(Map<String, String> dependencies) {
        this.dependencies = dependencies;
    }
    
    public void addDependency(String ga, String version) {
        this.dependencies.put(ga, version);
    }
    
    public void removeDependency(String artifactId) {
        // Find and remove by artifact ID
        dependencies.entrySet().removeIf(entry -> {
            String[] parts = entry.getKey().split(":");
            return parts.length == 2 && parts[1].equals(artifactId);
        });
    }
    
    public static class PackageConfig {
        private String name;
        private String version;
        private String javaVersion;
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public String getVersion() {
            return version;
        }
        
        public void setVersion(String version) {
            this.version = version;
        }
        
        public String getJavaVersion() {
            return javaVersion;
        }
        
        public void setJavaVersion(String javaVersion) {
            this.javaVersion = javaVersion;
        }
    }
}
