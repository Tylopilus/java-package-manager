package jpm.deps;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents parsed POM information with support for parent inheritance.
 * Uses Java 16+ records for immutable data representation.
 */
public record PomInfo(
    String groupId,
    String artifactId,
    String version,
    Map<String, String> properties,
    Map<String, String> managedVersions,
    PomInfo parent) {

  public PomInfo() {
    this(null, null, null, new HashMap<>(), new HashMap<>(), null);
  }

  /**
   * Compact constructor for defensive copying of mutable maps.
   */
  public PomInfo {
    properties = properties != null ? new HashMap<>(properties) : new HashMap<>();
    managedVersions = managedVersions != null ? new HashMap<>(managedVersions) : new HashMap<>();
  }

  /**
   * Returns the full artifact coordinate (groupId:artifactId).
   *
   * @return the artifact key
   */
  public String fullArtifactKey() {
    return groupId + ":" + artifactId;
  }

  /**
   * Returns all properties including inherited from parent chain.
   * Built-in Maven properties are also included.
   *
   * @return unmodifiable map of all properties
   */
  public Map<String, String> allProperties() {
    var all = new HashMap<String, String>();
    if (parent != null) {
      all.putAll(parent.allProperties());
    }
    all.putAll(properties);
    // Add built-in Maven properties
    all.put("${project.groupId}", groupId);
    all.put("${pom.groupId}", groupId);
    all.put("${project.artifactId}", artifactId);
    all.put("${pom.artifactId}", artifactId);
    all.put("${project.version}", version);
    all.put("${pom.version}", version);
    all.put("${version}", version);
    return Collections.unmodifiableMap(all);
  }

  /**
   * Returns all managed versions including inherited from parent chain.
   *
   * @return unmodifiable map of all managed versions
   */
  public Map<String, String> allManagedVersions() {
    var all = new HashMap<String, String>();
    if (parent != null) {
      all.putAll(parent.allManagedVersions());
    }
    all.putAll(managedVersions);
    return Collections.unmodifiableMap(all);
  }

  /**
   * Creates a new PomInfo with an additional property.
   *
   * @param key the property key
   * @param value the property value
   * @return a new PomInfo with the added property
   */
  public PomInfo withProperty(String key, String value) {
    var newProps = new HashMap<>(properties);
    newProps.put(key, value);
    return new PomInfo(groupId, artifactId, version, newProps, managedVersions, parent);
  }

  /**
   * Creates a new PomInfo with an additional managed version.
   *
   * @param key the dependency key (groupId:artifactId)
   * @param version the version
   * @return a new PomInfo with the added managed version
   */
  public PomInfo withManagedVersion(String key, String version) {
    var newManaged = new HashMap<>(managedVersions);
    newManaged.put(key, version);
    return new PomInfo(groupId, artifactId, version, properties, newManaged, parent);
  }

  /**
   * Creates a new PomInfo with a different parent.
   *
   * @param newParent the new parent PomInfo
   * @return a new PomInfo with the updated parent
   */
  public PomInfo withParent(PomInfo newParent) {
    return new PomInfo(groupId, artifactId, version, properties, managedVersions, newParent);
  }
}
