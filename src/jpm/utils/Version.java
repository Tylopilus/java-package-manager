package jpm.utils;

import java.util.WeakHashMap;

/**
 * Version comparison utility with caching for improved performance.
 * Frequently compared versions are cached to avoid repeated parsing.
 */
public class Version {
    
    private static final int MAX_CACHE_SIZE = 1000;
    private static final WeakHashMap<String, VersionComponents> cache = new WeakHashMap<>();
    
    /**
     * Represents parsed version components for efficient comparison.
     */
    private static class VersionComponents {
        final String[] parts;
        
        VersionComponents(String version) {
            this.parts = version.split("[.-]");
        }
        
        String getPart(int index) {
            return index < parts.length ? parts[index] : "0";
        }
        
        int length() {
            return parts.length;
        }
    }
    
    /**
     * Parse version string into components, using cache for frequently used versions.
     * Uses WeakHashMap to allow garbage collection when memory is needed.
     */
    private static VersionComponents parseVersion(String version) {
        VersionComponents components = cache.get(version);
        if (components == null) {
            components = new VersionComponents(version);
            // Limit cache size to prevent memory issues
            if (cache.size() < MAX_CACHE_SIZE) {
                cache.put(version, components);
            }
        }
        return components;
    }
    
    public static int compare(String v1, String v2) {
        VersionComponents c1 = parseVersion(v1);
        VersionComponents c2 = parseVersion(v2);
        
        int maxLength = Math.max(c1.length(), c2.length());
        
        for (int i = 0; i < maxLength; i++) {
            String p1 = c1.getPart(i);
            String p2 = c2.getPart(i);
            
            // Handle SNAPSHOT specially
            if (p1.equalsIgnoreCase("SNAPSHOT") && !p2.equalsIgnoreCase("SNAPSHOT")) {
                return -1;
            }
            if (!p1.equalsIgnoreCase("SNAPSHOT") && p2.equalsIgnoreCase("SNAPSHOT")) {
                return 1;
            }
            
            // Try numeric comparison
            try {
                int n1 = Integer.parseInt(p1);
                int n2 = Integer.parseInt(p2);
                if (n1 != n2) {
                    return Integer.compare(n1, n2);
                }
            } catch (NumberFormatException e) {
                // Fall back to string comparison
                int cmp = p1.compareToIgnoreCase(p2);
                if (cmp != 0) {
                    return cmp;
                }
            }
        }
        
        return 0;
    }
    
    public static boolean isNewer(String v1, String v2) {
        return compare(v1, v2) > 0;
    }
    
    /**
     * Clear the version cache. Useful for testing or memory-constrained environments.
     */
    public static void clearCache() {
        cache.clear();
    }
}
