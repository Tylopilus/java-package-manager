package jpm.utils;

public class Version {
    
    public static int compare(String v1, String v2) {
        String[] parts1 = v1.split("[.-]");
        String[] parts2 = v2.split("[.-]");
        
        int length = Math.max(parts1.length, parts2.length);
        
        for (int i = 0; i < length; i++) {
            String p1 = i < parts1.length ? parts1[i] : "0";
            String p2 = i < parts2.length ? parts2[i] : "0";
            
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
}
