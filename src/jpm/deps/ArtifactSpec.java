package jpm.deps;

import java.io.File;

/**
 * Record representing an artifact specification for batch operations.
 * Uses Java 16+ records for concise immutable data classes.
 */
public record ArtifactSpec(
    String groupId, String artifactId, String version, File outputDir, String extension) {
  @Override
  public String toString() {
    return groupId + ":" + artifactId + ":" + version;
  }
}
