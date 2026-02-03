package jpm.deps;

import jpm.net.HttpClientManager;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MavenSearchClient {
    private static final String SEARCH_URL = "https://search.maven.org/solrsearch/select";
    private static final String GAV_URL = "https://search.maven.org/solrsearch/select";
    private static final Duration TIMEOUT = Duration.ofSeconds(30);
    
    private final HttpClient httpClient;
    
    public MavenSearchClient() {
        this.httpClient = HttpClientManager.getClient();
    }
    
    public List<SearchResult> searchByArtifactId(String artifactId, int rows) throws IOException {
        try {
            String encodedArtifactId = java.net.URLEncoder.encode(artifactId, "UTF-8");
            String url = SEARCH_URL + "?q=a:" + encodedArtifactId + "&rows=" + rows + "&wt=json";
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(TIMEOUT)
                .GET()
                .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                return parseSearchResults(response.body());
            } else {
                throw new IOException("Search failed with status: " + response.statusCode());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Search interrupted", e);
        }
    }
    
    public String getLatestStableVersion(String groupId, String artifactId) throws IOException {
        try {
            String encodedGroupId = java.net.URLEncoder.encode(groupId, "UTF-8");
            String encodedArtifactId = java.net.URLEncoder.encode(artifactId, "UTF-8");
            String url = GAV_URL + "?q=g:" + encodedGroupId + "+AND+a:" + encodedArtifactId + "&core=gav&rows=20&wt=json";
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(TIMEOUT)
                .GET()
                .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                return parseLatestStableVersion(response.body());
            } else {
                throw new IOException("Version lookup failed with status: " + response.statusCode());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Version lookup interrupted", e);
        }
    }
    
    private List<SearchResult> parseSearchResults(String json) {
        List<SearchResult> results = new ArrayList<>();
        
        // Simple JSON parsing using regex
        Pattern docPattern = Pattern.compile("\\{[^}]*\"g\"\\s*:\\s*\"([^\"]+)\"[^}]*\"a\"\\s*:\\s*\"([^\"]+)\"[^}]*\"latestVersion\"\\s*:\\s*\"([^\"]+)\"[^}]*\\}");
        Matcher matcher = docPattern.matcher(json);
        
        while (matcher.find()) {
            String groupId = matcher.group(1);
            String artifactId = matcher.group(2);
            String version = matcher.group(3);
            results.add(new SearchResult(groupId, artifactId, version));
        }
        
        return results;
    }
    
    private String parseLatestStableVersion(String json) {
        // Parse all versions from the response
        Pattern versionPattern = Pattern.compile("\"v\"\\s*:\\s*\"([^\"]+)\"");
        Matcher matcher = versionPattern.matcher(json);
        
        String latestStable = null;
        
        while (matcher.find()) {
            String version = matcher.group(1);
            if (isStableVersion(version)) {
                // Return the first stable version (they're sorted by Maven Central, newest first)
                if (latestStable == null || isNewer(version, latestStable)) {
                    latestStable = version;
                }
            }
        }
        
        return latestStable;
    }
    
    private boolean isStableVersion(String version) {
        String lower = version.toLowerCase();
        return !lower.contains("snapshot") &&
               !lower.contains("-rc") &&
               !lower.contains("alpha") &&
               !lower.contains("beta") &&
               !lower.matches(".*-m\\d+.*"); // Milestones like 2.16.0-M1
    }
    
    private boolean isNewer(String v1, String v2) {
        return jpm.utils.Version.isNewer(v1, v2);
    }
    
    public static class SearchResult {
        public final String groupId;
        public final String artifactId;
        public final String latestVersion;
        
        public SearchResult(String groupId, String artifactId, String latestVersion) {
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.latestVersion = latestVersion;
        }
        
        @Override
        public String toString() {
            return groupId + ":" + artifactId + " (v" + latestVersion + ")";
        }
    }
}
