package jpm.build;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import jpm.build.format.Formatter;
import jpm.build.format.FormatterException;
import jpm.build.format.FormatterFactory;
import jpm.build.format.PalantirFormatter;
import jpm.config.FmtConfig;
import jpm.utils.FileCollector;
import jpm.utils.FileUtils;
import jpm.utils.UserOutput;

/**
 * High-level code formatter orchestrator.
 * Uses Palantir Formatter implementation for actual formatting.
 */
public class CodeFormatter {

  private final Formatter formatter;
  private final FmtConfig config;

  public CodeFormatter() {
    this(new PalantirFormatter(), new FmtConfig());
  }

  public CodeFormatter(FmtConfig config) {
    this(FormatterFactory.create("palantir", config), config);
  }

  public CodeFormatter(Formatter formatter, FmtConfig config) {
    this.formatter = formatter;
    this.config = config;
  }

  /**
   * Formats all Java files at the given path.
   *
   * @param path the file or directory to format
   * @param checkOnly if true, only check formatting without modifying files
   * @return FormatResult with statistics
   * @throws IOException if file operations fail
   */
  public FormatResult formatPath(File path, boolean checkOnly) throws IOException {
    var javaFiles = FileCollector.findFilesByExtension(path, ".java", file -> !shouldSkip(file));

    var totalFiles = 0;
    var formattedFiles = 0;
    var skippedFiles = 0;
    var failedFiles = 0;
    var unformattedFiles = new ArrayList<String>();

    for (var file : javaFiles) {
      totalFiles++;

      try {
        var original = Files.readString(file.toPath(), StandardCharsets.UTF_8);
        String formatted;

        if (config.shouldOrganizeImports()) {
          formatted = formatter.formatAndOrganizeImports(original);
        } else {
          formatted = formatter.format(original);
        }

        if (formatter.isEquivalent(original, formatted)) {
          // File is already formatted
          if (checkOnly) {
            // In check mode, this is a "correct" file
          }
        } else {
          // File needs formatting
          if (checkOnly) {
            unformattedFiles.add(file.getPath());
          } else {
            // Write formatted content
            FileUtils.writeFile(file, formatted);
            formattedFiles++;
          }
        }
      } catch (FormatterException e) {
        UserOutput.error("  Error formatting " + file + ": " + e.getMessage());
        failedFiles++;
      }
    }

    return new FormatResult(
        totalFiles, formattedFiles, skippedFiles, failedFiles, unformattedFiles);
  }

  /**
   * Formats a single Java file.
   *
   * @param file the file to format
   * @param checkOnly if true, only check formatting without modifying
   * @return true if file was formatted (or needs formatting in check mode)
   * @throws IOException if file operations fail
   */
  public boolean formatFile(File file, boolean checkOnly) throws IOException {
    if (!file.exists() || !file.getName().endsWith(".java")) {
      return false;
    }

    try {
      var original = Files.readString(file.toPath(), StandardCharsets.UTF_8);
      String formatted;

      if (config.shouldOrganizeImports()) {
        formatted = formatter.formatAndOrganizeImports(original);
      } else {
        formatted = formatter.format(original);
      }

      if (formatter.isEquivalent(original, formatted)) {
        return false; // No changes needed
      }

      if (!checkOnly) {
        FileUtils.writeFile(file, formatted);
      }

      return true;
    } catch (FormatterException e) {
      UserOutput.error("  Error formatting " + file + ": " + e.getMessage());
      return false;
    }
  }

  /**
   * Gets the underlying formatter implementation name.
   *
   * @return the formatter name and version
   */
  public String getFormatterInfo() {
    return formatter.getName() + " " + formatter.getVersion();
  }

  private boolean shouldSkip(File file) {
    var patterns = config.getSkipPatterns();
    if (patterns.isEmpty()) {
      return false;
    }

    var path = file.getPath();
    for (var pattern : patterns) {
      if (matchesGlob(path, pattern)) {
        return true;
      }
    }
    return false;
  }

  private boolean matchesGlob(String path, String pattern) {
    // Convert glob pattern to regex
    var regex = pattern
        .replace("**", "###DOUBLESTAR###")
        .replace("*", "[^/]*")
        .replace("###DOUBLESTAR###", ".*");
    return path.matches(regex);
  }

  /**
   * Result of a formatting operation.
   */
  public record FormatResult(
      int totalFiles,
      int formattedFiles,
      int skippedFiles,
      int failedFiles,
      List<String> unformattedFiles) {

    public int totalFiles() {
      return totalFiles;
    }

    public int formattedFiles() {
      return formattedFiles;
    }

    public int skippedFiles() {
      return skippedFiles;
    }

    public int failedFiles() {
      return failedFiles;
    }

    public List<String> unformattedFiles() {
      return unformattedFiles;
    }
  }
}
