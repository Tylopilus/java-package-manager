package jpm.deps;

// Java 16+ record for dependency information
public record PomDependency(
    String groupId, String artifactId, String version, String scope, boolean optional) {

  public boolean shouldInclude() {
    // Include compile and runtime scope (or null/empty scope which defaults to compile)
    // Exclude: test, provided
    if ("test".equals(scope) || "provided".equals(scope)) {
      return false;
    }
    if (optional) {
      return false;
    }
    return true;
  }
}
