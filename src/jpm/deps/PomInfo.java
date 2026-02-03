package jpm.deps;

import java.util.HashMap;
import java.util.Map;

public class PomInfo {
    private String groupId;
    private String artifactId;
    private String version;
    private Map<String, String> properties;
    private Map<String, String> managedVersions;
    private PomInfo parent;
    
    public PomInfo() {
        this.properties = new HashMap<>();
        this.managedVersions = new HashMap<>();
    }
    
    public String getGroupId() {
        return groupId;
    }
    
    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }
    
    public String getArtifactId() {
        return artifactId;
    }
    
    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }
    
    public String getVersion() {
        return version;
    }
    
    public void setVersion(String version) {
        this.version = version;
    }
    
    public Map<String, String> getProperties() {
        return properties;
    }
    
    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }
    
    public void addProperty(String key, String value) {
        this.properties.put(key, value);
    }
    
    public Map<String, String> getManagedVersions() {
        return managedVersions;
    }
    
    public void setManagedVersions(Map<String, String> managedVersions) {
        this.managedVersions = managedVersions;
    }
    
    public void addManagedVersion(String key, String version) {
        this.managedVersions.put(key, version);
    }
    
    public PomInfo getParent() {
        return parent;
    }
    
    public void setParent(PomInfo parent) {
        this.parent = parent;
    }
    
    public String getFullArtifactKey() {
        return groupId + ":" + artifactId;
    }
    
    public Map<String, String> getAllProperties() {
        Map<String, String> allProps = new HashMap<>();
        
        // Add parent properties first (will be overridden by child)
        if (parent != null) {
            allProps.putAll(parent.getAllProperties());
        }
        
        // Add current POM properties (override parent)
        allProps.putAll(properties);
        
        // Add built-in Maven properties
        allProps.put("${project.groupId}", groupId);
        allProps.put("${pom.groupId}", groupId);
        allProps.put("${project.artifactId}", artifactId);
        allProps.put("${pom.artifactId}", artifactId);
        allProps.put("${project.version}", version);
        allProps.put("${pom.version}", version);
        allProps.put("${version}", version);
        
        return allProps;
    }
    
    public Map<String, String> getAllManagedVersions() {
        Map<String, String> allManaged = new HashMap<>();
        
        // Add parent managed versions first
        if (parent != null) {
            allManaged.putAll(parent.getAllManagedVersions());
        }
        
        // Add current POM managed versions (override parent)
        allManaged.putAll(managedVersions);
        
        return allManaged;
    }
    
    @Override
    public String toString() {
        return "PomInfo{" +
                "groupId='" + groupId + '\'' +
                ", artifactId='" + artifactId + '\'' +
                ", version='" + version + '\'' +
                ", properties=" + properties.size() +
                ", managedVersions=" + managedVersions.size() +
                ", hasParent=" + (parent != null) +
                '}';
    }
}
