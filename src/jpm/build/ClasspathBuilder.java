package jpm.build;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;
import jpm.deps.DependencyResolver;

public class ClasspathBuilder {

  public static String buildClasspath(List<DependencyResolver.ResolvedDependency> dependencies) {
    return dependencies.stream()
        .map(DependencyResolver.ResolvedDependency::getClasspathEntry)
        .collect(Collectors.joining(File.pathSeparator));
  }

  public static String buildClasspathFromDir(File libDir) {
    if (!libDir.exists() || !libDir.isDirectory()) {
      return "";
    }

    var jars = libDir.listFiles((dir, name) -> name.endsWith(".jar"));
    if (jars == null || jars.length == 0) {
      return "";
    }

    var paths = new String[jars.length];
    for (int i = 0; i < jars.length; i++) {
      paths[i] = jars[i].getAbsolutePath();
    }
    return String.join(File.pathSeparator, paths);
  }

  public static String combineClasspaths(String... classpaths) {
    var result = new StringBuilder();
    var first = true;

    for (String cp : classpaths) {
      if (cp != null && !cp.isEmpty()) {
        if (!first) {
          result.append(File.pathSeparator);
        }
        result.append(cp);
        first = false;
      }
    }

    return result.toString();
  }

  /**
   * Combines multiple classpaths efficiently using String.join.
   * Filters out null/empty strings first to avoid unnecessary separators.
   *
   * @param classpaths Variable number of classpath strings
   * @return Combined classpath string
   */
  public static String combineClasspathsFast(String... classpaths) {
    if (classpaths == null || classpaths.length == 0) {
      return "";
    }

    // Count non-empty classpaths
    var nonEmptyCount = 0;
    for (var cp : classpaths) {
      if (cp != null && !cp.isEmpty()) {
        nonEmptyCount++;
      }
    }

    if (nonEmptyCount == 0) {
      return "";
    }

    // Collect non-empty paths
    var paths = new String[nonEmptyCount];
    var idx = 0;
    for (var cp : classpaths) {
      if (cp != null && !cp.isEmpty()) {
        paths[idx++] = cp;
      }
    }

    return String.join(File.pathSeparator, paths);
  }
}
