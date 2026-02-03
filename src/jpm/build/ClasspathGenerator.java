package jpm.build;

import java.io.File;
import java.io.IOException;
import jpm.config.JpmConfig;
import jpm.deps.CacheManager;
import jpm.deps.DependencyResolver;
import jpm.utils.FileUtils;

public class ClasspathGenerator {

  private final CacheManager cacheManager;

  public ClasspathGenerator() throws IOException {
    this.cacheManager = new CacheManager();
  }

  public void generateClasspath(JpmConfig config, File projectDir) throws IOException {
    var classpathFile = new File(projectDir, ".classpath");

    var xml = new StringBuilder();
    xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
    xml.append("<classpath>\n");

    // Source directory
    xml.append("\t<classpathentry kind=\"src\" path=\"src\"/>\n");

    // Output directory
    xml.append("\t<classpathentry kind=\"output\" path=\"target/classes\"/>\n");

    // JRE container
    var javaVersion = config.package_().javaVersion();
    if (javaVersion != null && !javaVersion.isEmpty()) {
      xml.append(
              "\t<classpathentry kind=\"con\""
                  + " path=\"org.eclipse.jdt.launching.JRE_CONTAINER/org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType/JavaSE-")
          .append(javaVersion)
          .append("\"/>\n");
    } else {
      xml.append(
          "\t<classpathentry kind=\"con\" path=\"org.eclipse.jdt.launching.JRE_CONTAINER\"/>\n");
    }

    // Dependency libraries (including transitive dependencies)
    if (!config.dependencies().isEmpty()) {
      try {
        var resolver = new DependencyResolver();
        var deps = resolver.resolveWithLockfile(projectDir, config, false);
        for (var dep : deps) {
          if (dep.jarFile().exists()) {
            xml.append("\t<classpathentry kind=\"lib\" path=\"")
                .append(escapeXml(dep.jarFile().getAbsolutePath()))
                .append("\"/>\n");
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
              xml.append("\t<classpathentry kind=\"lib\" path=\"")
                  .append(escapeXml(jarFile.getAbsolutePath()))
                  .append("\"/>\n");
            }
          }
        }
      }
    }

    xml.append("</classpath>\n");

    FileUtils.writeFile(classpathFile, xml.toString());
  }

  private String escapeXml(String text) {
    return text.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;");
  }
}
