package jpm.utils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public class FileUtils {

  public static void ensureDirectory(Path path) throws IOException {
    if (!Files.exists(path)) {
      Files.createDirectories(path);
    }
  }

  public static void ensureDirectory(File dir) throws IOException {
    ensureDirectory(dir.toPath());
  }

  public static void ensureDirectory(String path) throws IOException {
    ensureDirectory(Path.of(path));
  }

  public static void deleteDirectory(Path path) throws IOException {
    if (!Files.exists(path)) {
      return;
    }

    // Collect all paths first to enable parallel deletion for large directories
    List<Path> paths;
    try (Stream<Path> stream = Files.walk(path)) {
      paths = stream.sorted(Comparator.reverseOrder()).toList();
    }

    // Use parallel deletion for large directories (> 100 files)
    if (paths.size() > 100) {
      paths.parallelStream().forEach(p -> {
        try {
          Files.delete(p);
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      });
    } else {
      for (Path p : paths) {
        Files.delete(p);
      }
    }
  }

  public static void deleteDirectory(File dir) throws IOException {
    deleteDirectory(dir.toPath());
  }

  public static void deleteDirectory(String path) throws IOException {
    deleteDirectory(Path.of(path));
  }

  public static void writeFile(Path path, String content) throws IOException {
    Path parent = path.getParent();
    if (parent != null) {
      ensureDirectory(parent);
    }
    Files.writeString(path, content, StandardCharsets.UTF_8);
  }

  public static void writeFile(File file, String content) throws IOException {
    writeFile(file.toPath(), content);
  }

  public static void writeFile(String path, String content) throws IOException {
    writeFile(Path.of(path), content);
  }

  public static String readFile(Path path) throws IOException {
    return Files.readString(path, StandardCharsets.UTF_8);
  }

  public static String readFile(File file) throws IOException {
    return readFile(file.toPath());
  }

  public static String readFile(String path) throws IOException {
    return readFile(Path.of(path));
  }

  public static File getJpmHome() {
    String userHome = System.getProperty("user.home");
    return new File(userHome, ".jpm");
  }

  public static File getCacheDir() {
    return new File(getJpmHome(), "cache");
  }

  public static File getDependencyDir(String groupId, String artifactId, String version) {
    String groupPath = groupId.replace('.', File.separatorChar);
    return new File(
        getCacheDir(), groupPath + File.separator + artifactId + File.separator + version);
  }
}
