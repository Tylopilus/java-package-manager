package jpm.config;

import com.moandjiezana.toml.Toml;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import jpm.utils.Constants;
import jpm.utils.FileUtils;

/**
 * Parser for JPM configuration files (jpm.toml).
 * Handles reading and writing of project configuration with record-based JpmConfig.
 */
public class ConfigParser {

  /**
   * Loads a JPM configuration from a TOML file.
   *
   * @param configFile the configuration file to load
   * @return the loaded JpmConfig, or null if file doesn't exist
   * @throws IOException if reading fails
   */
  public static JpmConfig load(File configFile) throws IOException {
    if (!configFile.exists()) {
      return null;
    }

    var toml = new Toml().read(configFile);

    // Parse package section
    var packageToml = toml.getTable("package");
    var pkg = new JpmConfig.PackageConfig(
        packageToml != null ? packageToml.getString("name") : null,
        packageToml != null ? packageToml.getString("version") : null,
        packageToml != null ? packageToml.getString("java-version") : null);

    // Parse dependencies section
    var deps = new HashMap<String, String>();
    var depsToml = toml.getTable("dependencies");
    if (depsToml != null) {
      for (var entry : depsToml.entrySet()) {
        String key = stripQuotes(entry.getKey());
        deps.put(key, entry.getValue().toString());
      }
    }

    // Parse profile sections
    var profiles = new HashMap<String, ProfileConfig>();
    for (var entry : toml.entrySet()) {
      var key = entry.getKey();
      if (key.startsWith("profile.")) {
        var profileName = key.substring("profile.".length());
        var profileToml = toml.getTable(key);
        if (profileToml != null) {
          profiles.put(profileName, parseProfile(profileName, profileToml));
        }
      }
    }

    // Parse fmt section
    var fmt = parseFmt(toml);

    return new JpmConfig(pkg, deps, profiles, fmt);
  }

  private static FmtConfig parseFmt(Toml toml) {
    var fmtToml = toml.getTable("fmt");
    if (fmtToml == null) {
      return new FmtConfig();
    }

    var lineLength = fmtToml.getLong("line-length");
    var organizeImports = fmtToml.getBoolean("organize-imports");
    var skipPatterns = fmtToml.getList("skip-patterns");

    var formatter = fmtToml.getString("formatter");

    return new FmtConfig(
        lineLength != null ? lineLength.intValue() : null,
        organizeImports,
        skipPatterns != null ? skipPatterns.stream().map(Object::toString).toList() : null,
        formatter);
  }

  /**
   * Saves a JPM configuration to a TOML file.
   *
   * @param config the configuration to save
   * @param configFile the file to write to
   * @throws IOException if writing fails
   */
  public static void save(JpmConfig config, File configFile) throws IOException {
    var toml = new StringBuilder();

    // Package section
    toml.append("""
        [package]
        name = "%s"
        version = "%s"
        java-version = "%s"
        """.formatted(
            escape(config.package_().name()),
            escape(config.package_().version()),
            escape(config.package_().javaVersion())));

    // Dependencies section
    if (!config.dependencies().isEmpty()) {
      toml.append("\n[dependencies]\n");
      for (var entry : config.dependencies().entrySet()) {
        var key = entry.getKey();
        var value = entry.getValue();
        // Keys with special characters need quotes
        if (key.contains(":") || key.contains(".") || key.contains("-")) {
          toml.append("\"").append(escape(key)).append("\"");
        } else {
          toml.append(key);
        }
        toml.append(" = \"").append(escape(value)).append("\"\n");
      }
    }

    // Profiles section
    if (!config.profiles().isEmpty()) {
      for (var entry : config.profiles().entrySet()) {
        var profileName = entry.getKey();
        var profile = entry.getValue();
        toml.append("\n[profile.%s]\n".formatted(profileName));
        if (profile.inherits() != null) {
          toml.append("inherits = \"%s\"\n".formatted(escape(profile.inherits())));
        }
        if (profile.optimize()) {
          toml.append("optimize = true\n");
        }
        if (profile.stripDebug()) {
          toml.append("strip-debug = true\n");
        }
        if (!profile.compilerArgs().isEmpty()) {
          toml.append("compiler-args = [");
          var args = profile.compilerArgs();
          for (int i = 0; i < args.size(); i++) {
            if (i > 0) toml.append(", ");
            toml.append("\"").append(escape(args.get(i))).append("\"");
          }
          toml.append("]\n");
        }
        if (!profile.jvmArgs().isEmpty()) {
          toml.append("jvm-args = [");
          var args = profile.jvmArgs();
          for (int i = 0; i < args.size(); i++) {
            if (i > 0) toml.append(", ");
            toml.append("\"").append(escape(args.get(i))).append("\"");
          }
          toml.append("]\n");
        }
      }
    }

    FileUtils.writeFile(configFile, toml.toString());
  }

  private static ProfileConfig parseProfile(String name, Toml toml) {
    var compilerArgs = toml.getList("compiler-args");
    var jvmArgs = toml.getList("jvm-args");
    var deps = new HashMap<String, String>();

    var depsToml = toml.getTable("dependencies");
    if (depsToml != null) {
      for (var entry : depsToml.entrySet()) {
        String key = stripQuotes(entry.getKey());
        deps.put(key, entry.getValue().toString());
      }
    }

    return new ProfileConfig(
        name,
        compilerArgs != null ? compilerArgs.stream().map(Object::toString).toList() : List.of(),
        jvmArgs != null ? jvmArgs.stream().map(Object::toString).toList() : List.of(),
        toml.getBoolean("optimize", false),
        toml.getBoolean("strip-debug", false),
        toml.getString("inherits"),
        deps);
  }

  private static String escape(String value) {
    if (value == null) return "";
    return value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t");
  }

  private static String stripQuotes(String value) {
    if (value == null) return "";
    if (value.startsWith("\"") && value.endsWith("\"")) {
      return value.substring(1, value.length() - 1);
    }
    return value;
  }

  /**
   * Loads an existing configuration or creates a new one with defaults.
   *
   * @param configFile the configuration file
   * @return existing or new JpmConfig
   * @throws IOException if reading fails
   */
  public static JpmConfig loadOrCreate(File configFile) throws IOException {
    var config = load(configFile);
    if (config == null) {
      var pkg = new JpmConfig.PackageConfig(null, "0.1.0", Constants.DEFAULT_JAVA_VERSION);
      config = new JpmConfig(pkg, new HashMap<>(), new HashMap<>(), new FmtConfig());
    }
    return config;
  }
}
