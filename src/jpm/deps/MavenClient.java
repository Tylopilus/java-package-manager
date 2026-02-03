package jpm.deps;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.*;
import jpm.net.HttpClientManager;
import jpm.utils.Constants;
import jpm.utils.UserOutput;

public class MavenClient {
  private static final String MAVEN_CENTRAL = Constants.MAVEN_CENTRAL;

  public MavenClient() {}

  public boolean downloadArtifact(
      String groupId, String artifactId, String version, File outputDir, String extension)
      throws IOException {
    String path = buildPath(groupId, artifactId, version, extension);
    String url = MAVEN_CENTRAL + path;

    File outputFile = new File(outputDir, artifactId + "-" + version + "." + extension);

    if (outputFile.exists()) {
      return true; // Already cached
    }

    return HttpClientManager.downloadFile(url, outputFile.toPath());
  }

  public String downloadPom(String groupId, String artifactId, String version) throws IOException {
    String path = buildPath(groupId, artifactId, version, "pom");
    String url = MAVEN_CENTRAL + path;

    try {
      return HttpClientManager.sendGet(url);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return null;
    } catch (IOException e) {
      // preserve original behavior: return null on failure (e.g. 404)
      // Check if I should log or just return null. The original code returned null on non-200.
      // HttpClientManager.sendGet throws IOException on non-200.
      return null;
    }
  }

  private String buildPath(String groupId, String artifactId, String version, String extension) {
    String groupPath = groupId.replace('.', '/');
    return groupPath + "/" + artifactId + "/" + version + "/" + artifactId + "-" + version + "."
        + extension;
  }

  /**
   * Batch download multiple artifacts in parallel using virtual threads.
   * Virtual threads provide optimal performance for I/O-bound operations
   * like HTTP downloads without the overhead of traditional thread pools.
   *
   * @param artifacts List of artifact coordinates to download
   * @return List of booleans indicating success for each download
   */
  public List<Boolean> downloadArtifactsBatch(List<ArtifactSpec> artifacts) {
    if (artifacts.isEmpty()) {
      return List.of();
    }

    // Use virtual threads for optimal I/O-bound concurrency
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      var futures = artifacts.stream()
          .map(spec -> executor.submit(() -> {
            try {
              return downloadArtifact(
                  spec.groupId(),
                  spec.artifactId(),
                  spec.version(),
                  spec.outputDir(),
                  spec.extension());
            } catch (IOException e) {
              UserOutput.error("Error downloading " + spec + ": " + e.getMessage());
              return false;
            }
          }))
          .toList();

      // Collect results
      return futures.stream()
          .map(f -> {
            try {
              return f.get();
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
              return false;
            } catch (ExecutionException e) {
              return false;
            }
          })
          .toList();
    }
  }
}
