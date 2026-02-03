package jpm.cli;

import java.io.File;
import java.util.concurrent.Callable;
import jpm.build.CodeFormatter;
import jpm.config.ConfigParser;
import jpm.config.FmtConfig;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * Format command for Java source code using Palantir Java Format.
 *
 * Features:
 * - Format all Java files in src/ and src/test/java/
 * - Check mode for CI integration (fails if unformatted)
 * - Support for specific file or directory formatting
 * - Respects .gitignore and skip patterns from jpm.toml
 *
 * Configuration is read from jpm.toml [fmt] section:
 *   [fmt]
 *   line-length = 120
 *   organize-imports = true
 *   skip-patterns = ["**&#47;target&#47;**"]
 *
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
      if (organizeImports) {
        fmtConfig = new FmtConfig(fmtConfig.lineLength(), true, fmtConfig.skipPatterns());
      } else if (noOrganizeImports) {
        fmtConfig = new FmtConfig(fmtConfig.lineLength(), false, fmtConfig.skipPatterns());
      }

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
          System.err.println("Error: File or directory not found: " + target);
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
            System.out.println("  UNFORMATTED: " + unformatted);
          }
        }
      }

      // Print summary
      System.out.println();
      if (check) {
        System.out.println("Format Check Results:");
        System.out.println("  Total files:    " + totalFiles);
        System.out.println(
            "  Correct:        " + (totalFiles - failedFiles - allUnformattedFiles.size()));
        System.out.println("  Needs format:   " + allUnformattedFiles.size());
        System.out.println("  Skipped:        " + skippedFiles);
        System.out.println("  Failed:         " + failedFiles);

        if (!allUnformattedFiles.isEmpty()) {
          System.out.println();
          System.out.println("Run 'jpm fmt' to fix formatting issues.");
          return 1;
        }
      } else {
        System.out.println("Format Results (" + formatter.getFormatterInfo() + "):");
        System.out.println("  Total files:    " + totalFiles);
        System.out.println("  Formatted:      " + formattedFiles);
        System.out.println("  Skipped:        " + skippedFiles);
        System.out.println("  Failed:         " + failedFiles);
      }

      return failedFiles > 0 ? 1 : 0;

    } catch (Exception e) {
      System.err.println("Error formatting code: " + e.getMessage());
      e.printStackTrace();
      return 1;
    }
  }
}
