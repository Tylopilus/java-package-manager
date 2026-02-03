package jpm.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration for a build profile (dev, release, test, etc.).
 * Uses Java records for immutable profile settings with inheritance support.
 */
public record ProfileConfig(
    String name,
    List<String> compilerArgs,
    List<String> jvmArgs,
    boolean optimize,
    boolean stripDebug,
    String inherits,
    Map<String, String> dependencies) {

  public ProfileConfig(String name) {
    this(name, List.of(), List.of(), false, false, null, new HashMap<>());
  }

  /**
   * Compact constructor for defensive copying.
   */
  public ProfileConfig {
    compilerArgs = compilerArgs != null ? List.copyOf(compilerArgs) : List.of();
    jvmArgs = jvmArgs != null ? List.copyOf(jvmArgs) : List.of();
    dependencies = dependencies != null ? new HashMap<>(dependencies) : new HashMap<>();
  }

  /**
   * Creates a default dev profile configuration.
   *
   * @return dev profile with debug symbols and assertions enabled
   */
  public static ProfileConfig dev() {
    return new ProfileConfig(
        "dev",
        List.of("-g", "-parameters"), // Debug symbols, method params
        List.of("-ea"), // Enable assertions
        false,
        false,
        null,
        new HashMap<>());
  }

  /**
   * Creates a default release profile configuration.
   *
   * @return release profile with optimizations enabled
   */
  public static ProfileConfig release() {
    return new ProfileConfig(
        "release",
        List.of("-O", "-parameters"), // Optimization, method params
        List.of("-server", "-Xmx2g"), // Server VM, 2GB heap
        true,
        true, // Strip debug symbols
        null,
        new HashMap<>());
  }

  /**
   * Creates a default test profile configuration.
   * Inherits from dev profile.
   *
   * @return test profile optimized for running tests
   */
  public static ProfileConfig test() {
    return new ProfileConfig(
        "test",
        List.of("-g:vars", "-parameters"), // Local var debug info
        List.of("-ea"), // Enable assertions
        false,
        false,
        "dev", // Inherits from dev
        new HashMap<>());
  }

  /**
   * Merges this profile with a parent profile.
   * Child settings override parent settings.
   *
   * @param parent the parent profile to merge with
   * @return a new merged profile
   */
  public ProfileConfig mergeWith(ProfileConfig parent) {
    // Combine compiler args: parent first, then child
    var mergedCompilerArgs = new java.util.ArrayList<String>();
    mergedCompilerArgs.addAll(parent.compilerArgs());
    // Add child args that aren't already in parent
    for (var arg : this.compilerArgs()) {
      if (!mergedCompilerArgs.contains(arg)) {
        mergedCompilerArgs.add(arg);
      }
    }

    // Combine JVM args: parent first, then child
    var mergedJvmArgs = new java.util.ArrayList<String>();
    mergedJvmArgs.addAll(parent.jvmArgs());
    for (var arg : this.jvmArgs()) {
      if (!mergedJvmArgs.contains(arg)) {
        mergedJvmArgs.add(arg);
      }
    }

    // Merge dependencies: child overrides parent
    var mergedDependencies = new HashMap<>(parent.dependencies());
    mergedDependencies.putAll(this.dependencies());

    return new ProfileConfig(
        this.name(),
        mergedCompilerArgs,
        mergedJvmArgs,
        this.optimize() || parent.optimize(),
        this.stripDebug() || parent.stripDebug(),
        parent.inherits(), // Keep the parent's inheritance chain
        mergedDependencies);
  }

  /**
   * Returns the effective compiler arguments for this profile.
   * If inherits is set, you should call mergeWith first.
   *
   * @return list of compiler arguments
   */
  public List<String> getEffectiveCompilerArgs() {
    return compilerArgs;
  }

  /**
   * Returns the effective JVM arguments for this profile.
   *
   * @return list of JVM arguments
   */
  public List<String> getEffectiveJvmArgs() {
    return jvmArgs;
  }

  /**
   * Creates a new profile with additional compiler arguments.
   *
   * @param args additional compiler arguments
   * @return new profile with merged args
   */
  public ProfileConfig withCompilerArgs(List<String> args) {
    var newArgs = new java.util.ArrayList<>(this.compilerArgs);
    newArgs.addAll(args);
    return new ProfileConfig(name, newArgs, jvmArgs, optimize, stripDebug, inherits, dependencies);
  }

  /**
   * Creates a new profile with additional JVM arguments.
   *
   * @param args additional JVM arguments
   * @return new profile with merged args
   */
  public ProfileConfig withJvmArgs(List<String> args) {
    var newArgs = new java.util.ArrayList<>(this.jvmArgs);
    newArgs.addAll(args);
    return new ProfileConfig(
        name, compilerArgs, newArgs, optimize, stripDebug, inherits, dependencies);
  }
}
