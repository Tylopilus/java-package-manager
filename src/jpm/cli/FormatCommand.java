package jpm.cli;

import java.io.File;
import java.util.concurrent.Callable;
import jpm.build.CodeFormatter;
import jpm.config.ConfigParser;
import jpm.config.FmtConfig;
import jpm.utils.UserOutput;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * Format command for Java source code using Palantir formatter.
 *
 * Features:
 * - Format all Java files in src/ and src/test/java/
 * - Opinionated formatting (Palantir/Rust style)
 * - Check mode for CI integration (fails if unformatted)
 * - Support for specific file or directory formatting
 * - Respects .gitignore and skip patterns from jpm.toml
 * - Automatic import organization
 *
 * Configuration is read from jpm.toml [fmt] section:
 *   [fmt]
 *   line-length = 120  # Palantir default
 *   organize-imports = true
 *   skip-patterns = ["**&#47;target&#47;**"]
 *
 * @see jpm.build.format.PalantirFormatter
 */
@Command(name = "fmt", description = "Format Java source code", mixinStandardHelpOptions = true)
public class FormatCommand implements Callable<Integer> {

  @Parameters(arity = "0..*", description = "Files or directories to format (default: src/)")
  private String[] targets;

  @Option(
      names = {"--check"},
      description = "Check formatting without modifying files (fails if unformatted, for CI)")
  private boolean check;

  @Option(
      names = {"--organize-imports"},
      description = "Organize imports while formatting (overrides config)")
  private boolean organizeImports;

  @Option(
      names = {"--no-organize-imports"},
      description = "Skip import organization")
  private boolean noOrganizeImports;

  @Override
  public Integer call() {
    try {
      // Load configuration from jpm.toml
      var configFile = new File("jpm.toml");
      FmtConfig fmtConfig;

      if (configFile.exists()) {
        var jpmConfig = ConfigParser.load(configFile);
        fmtConfig = jpmConfig != null ? jpmConfig.fmt() : new FmtConfig();
      } else {
        fmtConfig = new FmtConfig();
      }

      // CLI flags override config
      Boolean orgImports = null;
      if (organizeImports) {
        orgImports = true;
      } else if (noOrganizeImports) {
        orgImports = false;
      }

      // Create new config, ignoring formatter preference (always Palantir)
      fmtConfig = new FmtConfig(
          fmtConfig.lineLength(),
          orgImports != null ? orgImports : fmtConfig.organizeImports(),
          fmtConfig.skipPatterns(),
          "palantir" // Always force Palantir
          );

      var formatter = new CodeFormatter(fmtConfig);

      // Default to src/ if no targets specified
      if (targets == null || targets.length == 0) {
        targets = new String[] {"src"};
      }

      var totalFiles = 0;
      var formattedFiles = 0;
      var skippedFiles = 0;
      var failedFiles = 0;
      var allUnformattedFiles = new java.util.ArrayList<String>();

      // Process each target
      for (var target : targets) {
        var file = new File(target);
        if (!file.exists()) {
          CliErrorHandler.error("File or directory not found: " + target);
          failedFiles++;
          continue;
        }

        var result = formatter.formatPath(file, check);
        totalFiles += result.totalFiles();
        formattedFiles += result.formattedFiles();
        skippedFiles += result.skippedFiles();
        failedFiles += result.failedFiles();
        allUnformattedFiles.addAll(result.unformattedFiles());

        // Print per-file results in check mode
        if (check && !result.unformattedFiles().isEmpty()) {
          for (var unformatted : result.unformattedFiles()) {
            UserOutput.info("  UNFORMATTED: " + unformatted);
          }
        }
      }

      // Print summary
      UserOutput.print("");
      if (check) {
        UserOutput.info("Format Check Results:");
        UserOutput.info("  Total files:    " + totalFiles);
        UserOutput.info(
            "  Correct:        " + (totalFiles - failedFiles - allUnformattedFiles.size()));
        UserOutput.info("  Needs format:   " + allUnformattedFiles.size());
        UserOutput.info("  Skipped:        " + skippedFiles);
        UserOutput.info("  Failed:         " + failedFiles);

        if (!allUnformattedFiles.isEmpty()) {
          UserOutput.print("");
          UserOutput.info("Run 'jpm fmt' to fix formatting issues.");
          return 1;
        }
      } else {
        UserOutput.info("Format Results (" + formatter.getFormatterInfo() + "):");
        UserOutput.info("  Total files:    " + totalFiles);
        UserOutput.info("  Formatted:      " + formattedFiles);
        UserOutput.info("  Skipped:        " + skippedFiles);
        UserOutput.info("  Failed:         " + failedFiles);
      }

      return failedFiles > 0 ? 1 : 0;

    } catch (Exception e) {
      CliErrorHandler.error("Formatting code", e);
      return 1;
    }
  }
}
