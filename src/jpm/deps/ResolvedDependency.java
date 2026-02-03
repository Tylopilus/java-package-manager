package jpm.deps;

import java.io.File;

// Java 16+ record for resolved dependency information
public record ResolvedDependency(
    String groupId, String artifactId, String version, File jarFile) {

  public String getClasspathEntry() {
    return jarFile.getAbsolutePath();
  }

  @Override
  public String toString() {
    return groupId + ":" + artifactId + ":" + version;
  }
}
