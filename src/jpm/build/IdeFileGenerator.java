package jpm.build;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import jpm.config.JpmConfig;
import jpm.utils.FileUtils;
import jpm.utils.XmlUtils;

/**
 * Generates Eclipse IDE configuration files (.project and .classpath).
 * Uses centralized XML utilities for consistent file generation.
 */
public class IdeFileGenerator {

  public static boolean shouldGenerateIdeFiles(File projectDir) {
    File projectFile = new File(projectDir, ".project");
    File classpathFile = new File(projectDir, ".classpath");
    return !projectFile.exists() || !classpathFile.exists();
  }

  public static void generateIdeFilesIfMissing(File projectDir, JpmConfig config, String classpath)
      throws IOException {
    boolean projectMissing = !new File(projectDir, ".project").exists();
    boolean classpathMissing = !new File(projectDir, ".classpath").exists();

    if (projectMissing) {
      generateProjectFile(projectDir, config);
    }

    if (classpathMissing) {
      generateClasspathFile(projectDir, config, classpath);
    }

    if (projectMissing || classpathMissing) {
      System.out.println("Generated IDE configuration files (.project, .classpath)");
    }
  }

  public static void generateProjectFile(File projectDir, JpmConfig config) throws IOException {
    File projectFile = new File(projectDir, ".project");

    String projectName = config.package_().name();
    if (projectName == null || projectName.isEmpty()) {
      projectName = projectDir.getName();
    }

    String projectXml = XmlUtils.generateProjectFile(projectName);
    FileUtils.writeFile(projectFile, projectXml);
  }

  public static void generateClasspathFile(File projectDir, JpmConfig config, String classpath)
      throws IOException {
    File classpathFile = new File(projectDir, ".classpath");

    var dependencyPaths = new ArrayList<String>();

    // Add dependencies from classpath
    if (classpath != null && !classpath.isEmpty()) {
      String[] entries = classpath.split(File.pathSeparator);
      for (String entry : entries) {
        if (!entry.isEmpty()) {
          File jarFile = new File(entry);
          if (jarFile.exists()) {
            dependencyPaths.add(jarFile.getAbsolutePath());
          }
        }
      }
    }

    String javaVersion = config.package_().javaVersion();
    String classpathXml = XmlUtils.generateClasspathFile(javaVersion, dependencyPaths);

    FileUtils.writeFile(classpathFile, classpathXml);
  }

  public static void generateClasspathFileWithDeps(File projectDir, JpmConfig config)
      throws IOException {
    // Use ClasspathGenerator for full dependency resolution
    ClasspathGenerator generator = new ClasspathGenerator();
    generator.generateClasspath(config, projectDir);
  }
}
