package jpm.deps;

import jpm.config.JpmConfig;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;

public class LockfileManager {
    
    private static final String LOCKFILE_NAME = "jpm.lock";
    
    public static boolean isLockfileValid(File projectDir, JpmConfig config) {
        File lockfile = new File(projectDir, LOCKFILE_NAME);
        if (!lockfile.exists()) {
            return false;
        }
        
        try {
            Lockfile lock = Lockfile.load(lockfile);
            if (lock == null) {
                return false;
            }
            
            // Check if lockfile version matches expected
            if (!"1".equals(lock.getVersion())) {
                return false;
            }
            
            // Compute hash of current dependencies
            String currentHash = computeConfigHash(config);
            
            // Compare with stored hash
            if (!currentHash.equals(lock.getConfigHash())) {
                return false;
            }
            
            // Verify all cached JARs exist
            List<DependencyResolver.ResolvedDependency> deps = lock.toResolvedDependencies();
            if (deps.size() != lock.getDependencies().size()) {
                // Some JARs are missing
                return false;
            }
            
            return true;
        } catch (IOException e) {
            return false;
        }
    }
    
    public static List<DependencyResolver.ResolvedDependency> loadFromLockfile(File projectDir) throws IOException {
        File lockfile = new File(projectDir, LOCKFILE_NAME);
        Lockfile lock = Lockfile.load(lockfile);
        if (lock == null) {
            throw new IOException("Lockfile not found");
        }
        return lock.toResolvedDependencies();
    }
    
    public static void saveToLockfile(File projectDir, List<DependencyResolver.ResolvedDependency> deps, JpmConfig config) throws IOException {
        File lockfile = new File(projectDir, LOCKFILE_NAME);
        String configHash = computeConfigHash(config);
        Lockfile lock = Lockfile.fromResolvedDependencies(deps, configHash);
        lock.save(lockfile);
    }
    
    public static String computeConfigHash(JpmConfig config) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            
            // Include all dependencies in hash computation
            Map<String, String> deps = config.getDependencies();
            for (Map.Entry<String, String> entry : deps.entrySet()) {
                String depString = entry.getKey() + "=" + entry.getValue() + ";";
                digest.update(depString.getBytes(StandardCharsets.UTF_8));
            }
            
            byte[] hashBytes = digest.digest();
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            // Fallback to simple string concatenation if SHA-256 not available
            StringBuilder sb = new StringBuilder();
            Map<String, String> deps = config.getDependencies();
            for (Map.Entry<String, String> entry : deps.entrySet()) {
                sb.append(entry.getKey()).append("=").append(entry.getValue()).append(";");
            }
            return sb.toString();
        }
    }
    
    public static void deleteLockfile(File projectDir) {
        File lockfile = new File(projectDir, LOCKFILE_NAME);
        if (lockfile.exists()) {
            lockfile.delete();
        }
    }
}
