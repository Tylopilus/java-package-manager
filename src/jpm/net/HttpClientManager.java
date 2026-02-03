package jpm.net;

import java.io.IOException;
import java.net.http.HttpClient;
import java.time.Duration;

/**
 * Shared HttpClient manager for all HTTP operations.
 * Provides a single, reusable HttpClient instance with optimized configuration.
 */
public class HttpClientManager {

  private static final Duration TIMEOUT = Duration.ofSeconds(30);
  private static volatile HttpClient instance;
  private static final Object lock = new Object();

  /**
   * Get the shared HttpClient instance.
   * Creates the client lazily on first access.
   *
   * @return Shared HttpClient instance
   */
  public static HttpClient getClient() {
    if (instance == null) {
      synchronized (lock) {
        if (instance == null) {
          instance = HttpClient.newBuilder()
              .connectTimeout(TIMEOUT)
              .followRedirects(HttpClient.Redirect.NORMAL)
              .build();
        }
      }
    }
    return instance;
  }

  /**
   * Reset the shared client. Useful for testing.
   */
  public static void reset() {
    synchronized (lock) {
      instance = null;
    }
  }

  /**
   * Send a GET request and return the response body as a string.
   *
   * @param url The URL to fetch
   * @return The response body
   * @throws IOException If the request fails or returns a non-200 status code
   * @throws InterruptedException If the request is interrupted
   */
  public static String sendGet(String url) throws IOException, InterruptedException {
    var client = getClient();
    var request = java.net.http.HttpRequest.newBuilder()
        .uri(java.net.URI.create(url))
        .timeout(TIMEOUT)
        .GET()
        .build();

    var response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());

    if (response.statusCode() == 200) {
      return response.body();
    } else {
      throw new IOException("Request failed with status: " + response.statusCode());
    }
  }

  /**
   * Download a file from a URL to a local path.
   *
   * @param url The URL to download from
   * @param target The local path to save the file to
   * @return true if successful, false otherwise
   */
  public static boolean downloadFile(String url, java.nio.file.Path target) {
    var client = getClient();
    var request = java.net.http.HttpRequest.newBuilder()
        .uri(java.net.URI.create(url))
        .timeout(TIMEOUT)
        .GET()
        .build();

    try {
      var response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofFile(target));
      if (response.statusCode() == 200) {
        return true;
      } else {
        java.nio.file.Files.deleteIfExists(target);
        return false;
      }
    } catch (IOException | InterruptedException e) {
      if (e instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
      try {
        java.nio.file.Files.deleteIfExists(target);
      } catch (IOException ignored) {
        // Ignore cleanup errors
      }
      return false;
    }
  }
}
