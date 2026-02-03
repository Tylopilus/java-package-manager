package jpm.deps;

import jpm.config.JpmConfig;
import jpm.utils.FileUtils;
import jpm.utils.Version;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class DependencyResolver {
    
    private final MavenClient mavenClient;
    private final PomParser pomParser;
    private final Set<String> resolvedArtifacts;
    private final Map<String, ResolvedDependency> resolvedDeps;
    
    public DependencyResolver() throws Exception {
        this.mavenClient = new MavenClient();
        this.pomParser = new PomParser(new ParentPomResolver(mavenClient));
        this.resolvedArtifacts = new HashSet<>();
        this.resolvedDeps = new HashMap<>();
    }
    
    public List<ResolvedDependency> resolve(String groupId, String artifactId, String version) throws IOException {
        resolvedArtifacts.clear();
        resolvedDeps.clear();
        
        resolveInternal(groupId, artifactId, version, 0);
        
        return new ArrayList<>(resolvedDeps.values());
    }
    
    public List<ResolvedDependency> resolveAll(Map<String, String> dependencies) throws IOException {
        resolvedArtifacts.clear();
        resolvedDeps.clear();
        
        for (Map.Entry<String, String> entry : dependencies.entrySet()) {
            String[] parts = entry.getKey().split(":");
            if (parts.length == 2) {
                resolveInternal(parts[0], parts[1], entry.getValue(), 0);
            }
        }
        
        return new ArrayList<>(resolvedDeps.values());
    }
    
    private void resolveInternal(String groupId, String artifactId, String version, int depth) throws IOException {
        String key = groupId + ":" + artifactId;
        
        if (resolvedArtifacts.contains(key)) {
            // Check for version conflict
            ResolvedDependency existing = resolvedDeps.get(key);
            if (existing != null && version != null) {
                if (Version.isNewer(version, existing.version)) {
                    // Newer version, re-resolve with newer version
                    System.out.println("    Resolving version conflict: " + key + " " + existing.version + " -> " + version);
                    resolvedDeps.remove(key);
                    resolvedArtifacts.remove(key);
                } else {
                    return; // Keep existing version
                }
            } else {
                return;
            }
        }
        
        if (version == null) {
            System.err.println("  Warning: No version specified for " + key);
            return;
        }
        
        String indent = "  ".repeat(depth);
        System.out.println(indent + "Resolving " + groupId + ":" + artifactId + ":" + version);
        
        resolvedArtifacts.add(key);
        
        // Ensure cache directory exists
        File cacheDir = FileUtils.getDependencyDir(groupId, artifactId, version);
        FileUtils.ensureDirectory(cacheDir);
        
        // Download JAR
        boolean jarDownloaded = mavenClient.downloadArtifact(groupId, artifactId, version, cacheDir, "jar");
        if (!jarDownloaded) {
            System.err.println("  Warning: Failed to download JAR for " + key + ":" + version);
        }
        
        // Download POM for transitive dependencies
        String pomContent = mavenClient.downloadPom(groupId, artifactId, version);
        if (pomContent != null) {
            // Save POM for future reference
            File pomFile = new File(cacheDir, artifactId + "-" + version + ".pom");
            FileUtils.writeFile(pomFile, pomContent);
            
            try {
                // Parse transitive dependencies
                List<PomParser.Dependency> transitiveDeps = pomParser.parseDependencies(pomContent);
                for (PomParser.Dependency dep : transitiveDeps) {
                    if (dep.shouldInclude() && dep.version != null) {
                        resolveInternal(dep.groupId, dep.artifactId, dep.version, depth + 1);
                    }
                }
            } catch (Exception e) {
                System.err.println("  Warning: Failed to parse POM for " + key + ": " + e.getMessage());
            }
        }
        
        // Add to resolved dependencies
        if (jarDownloaded) {
            File jarFile = new File(cacheDir, artifactId + "-" + version + ".jar");
            resolvedDeps.put(key, new ResolvedDependency(groupId, artifactId, version, jarFile));
        }
    }
    
    public List<ResolvedDependency> resolveWithLockfile(File projectDir, JpmConfig config, boolean forceResolve) throws IOException {
        // If not forcing re-resolution, try to use lockfile
        if (!forceResolve) {
            if (LockfileManager.isLockfileValid(projectDir, config)) {
                System.out.println("Using cached dependencies from jpm.lock");
                return LockfileManager.loadFromLockfile(projectDir);
            }
        }
        
        // Perform full resolution
        System.out.println("Resolving dependencies...");
        List<ResolvedDependency> deps = resolveAll(config.getDependencies());
        
        // Save to lockfile
        try {
            LockfileManager.saveToLockfile(projectDir, deps, config);
        } catch (IOException e) {
            System.err.println("Warning: Failed to save lockfile: " + e.getMessage());
        }
        
        return deps;
    }
    
    public static class ResolvedDependency {
        public final String groupId;
        public final String artifactId;
        public final String version;
        public final File jarFile;
        
        public ResolvedDependency(String groupId, String artifactId, String version, File jarFile) {
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.version = version;
            this.jarFile = jarFile;
        }
        
        public String getClasspathEntry() {
            return jarFile.getAbsolutePath();
        }
        
        @Override
        public String toString() {
            return groupId + ":" + artifactId + ":" + version;
        }
    }
}
