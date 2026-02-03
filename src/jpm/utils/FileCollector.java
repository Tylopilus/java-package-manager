package jpm.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Utility class for collecting files from a directory tree.
 * Provides helpers for common file discovery patterns.
 */
public final class FileCollector {

  private FileCollector() {
    // Utility class - prevent instantiation
  }

  /**
   * Finds files with the given extension under a root directory.
   *
   * @param rootDir directory to search
   * @param extension file extension including dot (e.g., ".java")
   * @return list of matching files
   * @throws IOException if file traversal fails
   */
  public static List<File> findFilesByExtension(File rootDir, String extension) throws IOException {
    return findFilesByExtension(rootDir, extension, file -> true);
  }

  /**
   * Finds files with the given extension under a root directory using a filter.
   *
   * @param rootDir directory to search
   * @param extension file extension including dot (e.g., ".java")
   * @param shouldInclude predicate to include files
   * @return list of matching files
   * @throws IOException if file traversal fails
   */
  public static List<File> findFilesByExtension(
      File rootDir, String extension, Predicate<File> shouldInclude) throws IOException {

    if (rootDir == null || !rootDir.exists()) {
      return Collections.emptyList();
    }

    var rootPath = rootDir.toPath().toAbsolutePath().normalize();

    try (var stream = Files.walk(rootPath)) {
      return stream
          .filter(Files::isRegularFile)
          .map(path -> path.toFile())
          .filter(file -> file.getName().endsWith(extension))
          .filter(shouldInclude)
          .collect(Collectors.toList());
    }
  }

  /**
   * Finds relative paths of files with the given extension under a root directory.
   *
   * @param rootDir directory to search
   * @param extension file extension including dot (e.g., ".java")
   * @return list of relative paths from rootDir
   * @throws IOException if file traversal fails
   */
  public static List<String> findRelativePathsByExtension(File rootDir, String extension)
      throws IOException {

    if (rootDir == null || !rootDir.exists()) {
      return Collections.emptyList();
    }

    var rootPath = rootDir.toPath().toAbsolutePath().normalize();

    try (var stream = Files.walk(rootPath)) {
      return stream
          .filter(Files::isRegularFile)
          .filter(path -> path.toString().endsWith(extension))
          .map(path -> rootPath.relativize(path).toString())
          .collect(Collectors.toList());
    }
  }
}
