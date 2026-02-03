package jpm.net;

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
}
