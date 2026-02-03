package jpm.deps;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;

public class MavenClient {
    private static final String MAVEN_CENTRAL = "https://repo1.maven.org/maven2/";
    private static final Duration TIMEOUT = Duration.ofSeconds(30);
    
    private final HttpClient httpClient;
    
    public MavenClient() {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
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
}
