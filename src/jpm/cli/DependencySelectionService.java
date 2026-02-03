package jpm.cli;

import java.io.File;
import java.io.IOException;
import jpm.config.ConfigParser;
import jpm.config.JpmConfig;
import jpm.config.ProjectPaths;
import jpm.deps.MavenSearchClient;
import jpm.utils.Constants;
import jpm.utils.UserOutput;

/**
 * Resolves dependency inputs into full coordinates.
 * Handles Maven search, selection, and confirmation.
 */
public class DependencySelectionService {

  private final MavenSearchClient searchClient;
  private final DependencyPrompter prompter;
  private final boolean autoConfirm;
  private final boolean noSearch;
  private final JpmConfig config;

  public DependencySelectionService(
      MavenSearchClient searchClient,
      DependencyPrompter prompter,
      boolean autoConfirm,
      boolean noSearch,
      JpmConfig config) {
    this.searchClient = searchClient;
    this.prompter = prompter;
    this.autoConfirm = autoConfirm;
    this.noSearch = noSearch;
    this.config = config;
  }

  public DependencyInfo resolveDependency(String input) throws IOException {
    var parts = input.split(":");

    return switch (parts.length) {
      case 3 -> new DependencyInfo(parts[0], parts[1], parts[2]);
      case 2 -> {
        if (parts[0].contains(".")) {
          var version = searchClient.getLatestStableVersion(parts[0], parts[1]);
          if (version == null) {
            CliErrorHandler.error("Could not find version for " + parts[0] + ":" + parts[1]);
            yield null;
          }
          yield confirmAndCreate(parts[0], parts[1], version);
        }

        yield searchAndSelect(parts[0], parts[1]);
      }
      case 1 -> {
        if (noSearch) {
          CliErrorHandler.error(
              "Exact coordinates required. Use --no-search=false to enable search");
          yield null;
        }
        yield searchAndSelect(parts[0], null);
      }
      default -> {
        CliErrorHandler.error("Invalid format: " + input);
        UserOutput.error(
            "Expected: group:artifact:version, group:artifact, artifact:version, or artifact");
        yield null;
      }
    };
  }

  private DependencyInfo searchAndSelect(String artifactId, String explicitVersion)
      throws IOException {
    var results =
        searchClient.searchByArtifactId(artifactId, Constants.DEFAULT_SEARCH_RESULTS_LIMIT);

    if (results.isEmpty()) {
      UserOutput.info("No packages found matching \"" + artifactId + "\"");
      return null;
    }

    UserOutput.info("Found " + results.size() + " results:");
    var displayCount = Math.min(results.size(), Constants.DEFAULT_SEARCH_RESULTS_LIMIT);
    for (int i = 0; i < displayCount; i++) {
      UserOutput.info("  " + (i + 1) + ". " + results.get(i));
    }

    if (results.size() == 1) {
      var result = results.get(0);
      var version = explicitVersion != null
          ? explicitVersion
          : searchClient.getLatestStableVersion(result.groupId(), result.artifactId());
      return confirmAndCreate(result.groupId(), result.artifactId(), version);
    }

    var selection = prompter.promptForSelection(displayCount);
    if (selection < 0) {
      UserOutput.info("Cancelled");
      return null;
    }

    var selected = results.get(selection);
    var version = explicitVersion != null
        ? explicitVersion
        : searchClient.getLatestStableVersion(selected.groupId(), selected.artifactId());

    return confirmAndCreate(selected.groupId(), selected.artifactId(), version);
  }

  private DependencyInfo confirmAndCreate(String groupId, String artifactId, String version)
      throws IOException {
    var ga = groupId + ":" + artifactId;

    if (config != null) {
      if (config.dependencies().containsKey(ga)) {
        var existingVersion = config.dependencies().get(ga);
        if (existingVersion.equals(version)) {
          UserOutput.info("Note: " + ga + " already at version " + version);
          return new DependencyInfo(groupId, artifactId, version);
        }
        UserOutput.info("Update: " + existingVersion + " -> " + version);
      }
    } else {
      var configFile = new File(ProjectPaths.CONFIG_FILE);
      if (configFile.exists()) {
        try {
          var loadedConfig = ConfigParser.loadOrCreate(configFile);
          if (loadedConfig.dependencies().containsKey(ga)) {
            var existingVersion = loadedConfig.dependencies().get(ga);
            if (existingVersion.equals(version)) {
              UserOutput.info("Note: " + ga + " already at version " + version);
              return new DependencyInfo(groupId, artifactId, version);
            }
            UserOutput.info("Update: " + existingVersion + " -> " + version);
          }
        } catch (Exception e) {
          // Ignore, will be checked again later
        }
      }
    }

    UserOutput.info("Found: " + ga + ":" + version);

    if (!prompter.confirm("Add?", autoConfirm)) {
      UserOutput.info("Skipped");
      return null;
    }

    return new DependencyInfo(groupId, artifactId, version);
  }
}
