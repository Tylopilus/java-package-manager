package jpm.deps;

import jpm.net.HttpClientManager;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class MavenClient {
    private static final String MAVEN_CENTRAL = "https://repo1.maven.org/maven2/";
    private static final Duration TIMEOUT = Duration.ofSeconds(30);
    
    private final HttpClient httpClient;
    
    public MavenClient() {
        this.httpClient = HttpClientManager.getClient();
    }
    
    public boolean downloadArtifact(String groupId, String artifactId, String version, 
                                   File outputDir, String extension) throws IOException {
        String path = buildPath(groupId, artifactId, version, extension);
        String url = MAVEN_CENTRAL + path;
        
        File outputFile = new File(outputDir, artifactId + "-" + version + "." + extension);
        
        if (outputFile.exists()) {
            return true; // Already cached
        }
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(TIMEOUT)
            .GET()
            .build();
        
        try {
            HttpResponse<Path> response = httpClient.send(
                request, 
                HttpResponse.BodyHandlers.ofFile(outputFile.toPath())
            );
            
            if (response.statusCode() == 200) {
                return true;
            } else {
                Files.deleteIfExists(outputFile.toPath());
                return false;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
    
    public String downloadPom(String groupId, String artifactId, String version) throws IOException {
        String path = buildPath(groupId, artifactId, version, "pom");
        String url = MAVEN_CENTRAL + path;
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(TIMEOUT)
            .GET()
            .build();
        
        try {
            HttpResponse<String> response = httpClient.send(
                request, 
                HttpResponse.BodyHandlers.ofString()
            );
            
            if (response.statusCode() == 200) {
                return response.body();
            } else {
                return null;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }
    
    private String buildPath(String groupId, String artifactId, String version, String extension) {
        String groupPath = groupId.replace('.', '/');
        return groupPath + "/" + artifactId + "/" + version + "/" + artifactId + "-" + version + "." + extension;
    }
    
    /**
     * Batch download multiple artifacts in parallel.
     * This significantly speeds up dependency resolution by downloading
     * artifacts concurrently rather than sequentially.
     * 
     * @param artifacts List of artifact coordinates to download
     * @return List of booleans indicating success for each download
     */
    public List<Boolean> downloadArtifactsBatch(List<ArtifactSpec> artifacts) {
        if (artifacts.isEmpty()) {
            return new ArrayList<>();
        }
        
        // Use executor for parallel downloads
        ExecutorService executor = Executors.newFixedThreadPool(
            Math.min(artifacts.size(), 10) // Max 10 concurrent downloads
        );
        
        List<Future<Boolean>> futures = new ArrayList<>();
        
        for (ArtifactSpec spec : artifacts) {
            Future<Boolean> future = executor.submit(() -> {
                try {
                    return downloadArtifact(spec.groupId, spec.artifactId, spec.version, 
                                          spec.outputDir, spec.extension);
                } catch (IOException e) {
                    System.err.println("  Error downloading " + spec + ": " + e.getMessage());
                    return false;
                }
            });
            futures.add(future);
        }
        
        // Collect results
        List<Boolean> results = new ArrayList<>(artifacts.size());
        for (Future<Boolean> future : futures) {
            try {
                results.add(future.get());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                results.add(false);
            } catch (ExecutionException e) {
                results.add(false);
            }
        }
        
        executor.shutdown();
        return results;
    }
    
    /**
     * Record representing an artifact specification for batch operations.
     * Uses Java 16+ records for concise immutable data classes.
     */
    public record ArtifactSpec(String groupId, String artifactId, String version, 
                               File outputDir, String extension) {
        @Override
        public String toString() {
            return groupId + ":" + artifactId + ":" + version;
        }
    }
}
