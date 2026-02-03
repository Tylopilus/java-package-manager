package jpm.deps;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import jpm.config.JpmConfig;

public class LockfileManager {

  private static final String LOCKFILE_NAME = "jpm.lock";

  public static boolean isLockfileValid(File projectDir, JpmConfig config) {
    var lockfile = new File(projectDir, LOCKFILE_NAME);
    if (!lockfile.exists()) {
      return false;
    }

    try {
      var lock = Lockfile.load(lockfile);
      if (lock == null) {
        return false;
      }

      // Check if lockfile version matches expected
      if (!"1".equals(lock.getVersion())) {
        return false;
      }

      // Compute hash of current dependencies
      var currentHash = computeConfigHash(config);

      // Compare with stored hash
      if (!currentHash.equals(lock.getConfigHash())) {
        return false;
      }

      // Verify all cached JARs exist
      var deps = lock.toResolvedDependencies();
      if (deps.size() != lock.getDependencies().size()) {
        // Some JARs are missing
        return false;
      }

      return true;
    } catch (IOException e) {
      return false;
    }
  }

  public static List<ResolvedDependency> loadFromLockfile(File projectDir)
      throws IOException {
    var lockfile = new File(projectDir, LOCKFILE_NAME);
    var lock = Lockfile.load(lockfile);
    if (lock == null) {
      throw new IOException("Lockfile not found");
    }
    return lock.toResolvedDependencies();
  }

  public static void saveToLockfile(
      File projectDir, List<ResolvedDependency> deps, JpmConfig config)
      throws IOException {
    var lockfile = new File(projectDir, LOCKFILE_NAME);
    var configHash = computeConfigHash(config);
    var lock = Lockfile.fromResolvedDependencies(deps, configHash);
    lock.save(lockfile);
  }

  public static String computeConfigHash(JpmConfig config) {
    try {
      var digest = MessageDigest.getInstance("SHA-256");

      // Include all dependencies in hash computation
      var deps = config.dependencies();
      for (var entry : deps.entrySet()) {
        var depString = entry.getKey() + "=" + entry.getValue() + ";";
        digest.update(depString.getBytes(StandardCharsets.UTF_8));
      }

      var hashBytes = digest.digest();
      var hexString = new StringBuilder();
      for (var b : hashBytes) {
        var hex = Integer.toHexString(0xff & b);
        if (hex.length() == 1) {
          hexString.append('0');
        }
        hexString.append(hex);
      }
      return hexString.toString();
    } catch (NoSuchAlgorithmException e) {
      // Fallback to simple string concatenation if SHA-256 not available
      var sb = new StringBuilder();
      var deps = config.dependencies();
      for (var entry : deps.entrySet()) {
        sb.append(entry.getKey()).append("=").append(entry.getValue()).append(";");
      }
      return sb.toString();
    }
  }

  public static void deleteLockfile(File projectDir) {
    var lockfile = new File(projectDir, LOCKFILE_NAME);
    if (lockfile.exists()) {
      lockfile.delete();
    }
  }
}
