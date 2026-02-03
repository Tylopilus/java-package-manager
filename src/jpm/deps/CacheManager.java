package jpm.deps;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;
import jpm.utils.FileUtils;

public class CacheManager {

  private final File cacheDir;

  public CacheManager() throws IOException {
    this.cacheDir = FileUtils.getCacheDir();
    FileUtils.ensureDirectory(cacheDir);
  }

  public File getCacheDir() {
    return cacheDir;
  }

  public File getArtifactDir(String groupId, String artifactId, String version) {
    return FileUtils.getDependencyDir(groupId, artifactId, version);
  }

  public boolean isCached(String groupId, String artifactId, String version) {
    File dir = getArtifactDir(groupId, artifactId, version);
    File jarFile = new File(dir, artifactId + "-" + version + ".jar");
    return jarFile.exists();
  }

  public File getJarFile(String groupId, String artifactId, String version) {
    File dir = getArtifactDir(groupId, artifactId, version);
    return new File(dir, artifactId + "-" + version + ".jar");
  }

  public File getPomFile(String groupId, String artifactId, String version) {
    File dir = getArtifactDir(groupId, artifactId, version);
    return new File(dir, artifactId + "-" + version + ".pom");
  }

  public void cleanArtifact(String groupId, String artifactId) throws IOException {
    String groupPath = groupId.replace('.', File.separatorChar);
    File artifactDir = new File(cacheDir, groupPath + File.separator + artifactId);

    if (artifactDir.exists()) {
      try (Stream<Path> stream = Files.walk(artifactDir.toPath())) {
        stream.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
      }
    }
  }

  public void cleanAll() throws IOException {
    if (cacheDir.exists()) {
      try (Stream<Path> stream = Files.walk(cacheDir.toPath())) {
        stream.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
      }
    }
    FileUtils.ensureDirectory(cacheDir);
  }
}
