package jpm.build;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import jpm.config.JpmConfig;
import jpm.deps.CacheManager;
import jpm.deps.DependencyResolver;
import jpm.utils.FileUtils;
import jpm.utils.XmlUtils;

/**
 * Generates Eclipse .classpath file with resolved dependencies.
 * Uses centralized XML utilities for consistent formatting.
 */
public class ClasspathGenerator {

  private final CacheManager cacheManager;

  public ClasspathGenerator() throws IOException {
    this.cacheManager = new CacheManager();
  }

  public void generateClasspath(JpmConfig config, File projectDir) throws IOException {
    var classpathFile = new File(projectDir, ".classpath");
    var javaVersion = config.package_().javaVersion();
    var dependencyPaths = new ArrayList<String>();

    // Resolve and add dependencies
    if (!config.dependencies().isEmpty()) {
      try {
        var resolver = new DependencyResolver();
        var deps = resolver.resolveWithLockfile(projectDir, config, false);
        for (var dep : deps) {
          if (dep.jarFile().exists()) {
            dependencyPaths.add(dep.jarFile().getAbsolutePath());
          }
        }
      } catch (Exception e) {
        // If resolution fails, fall back to direct dependencies only
        for (var entry : config.dependencies().entrySet()) {
          var parts = entry.getKey().split(":");
          if (parts.length == 2) {
            var groupId = parts[0];
            var artifactId = parts[1];
            var version = entry.getValue();

            var jarFile = cacheManager.getJarFile(groupId, artifactId, version);
            if (jarFile.exists()) {
              dependencyPaths.add(jarFile.getAbsolutePath());
            }
          }
        }
      }
    }

    String classpathXml = XmlUtils.generateClasspathFile(javaVersion, dependencyPaths);
    FileUtils.writeFile(classpathFile, classpathXml);
  }
}
