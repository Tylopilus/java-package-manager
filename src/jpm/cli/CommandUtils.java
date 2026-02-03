package jpm.cli;

import java.io.File;
import java.io.IOException;
import jpm.build.IdeFileGenerator;
import jpm.config.ConfigParser;
import jpm.config.JpmConfig;
import jpm.config.ProjectPaths;
import jpm.utils.FileUtils;
import jpm.utils.UserOutput;
import jpm.utils.XmlUtils;

public class CommandUtils {

  public static JpmConfig loadConfigOrFail() {
    File configFile = new File(ProjectPaths.CONFIG_FILE);
    if (!configFile.exists()) {
      CliErrorHandler.error(
          "No " + ProjectPaths.CONFIG_FILE + " found. Run 'jpm new <name>' first.");
      return null;
    }
    try {
      return ConfigParser.load(configFile);
    } catch (IOException e) {
      CliErrorHandler.error("Failed to load config: " + e.getMessage());
      return null;
    }
  }

  /**
   * Ensures that the .project file exists.
   */
  public static void ensureProjectFiles(JpmConfig config) throws IOException {
    File projectFile = new File(ProjectPaths.DOT_PROJECT);
    if (!projectFile.exists()) {
      UserOutput.info("\nCreating " + ProjectPaths.DOT_PROJECT + " file...");
      String projectXml = XmlUtils.generateProjectFile(config.package_().name());
      FileUtils.writeFile(projectFile, projectXml);
    }
  }

  /**
   * Regenerates the .classpath file.
   */
  public static void syncIdeConfig(JpmConfig config) throws IOException {
    UserOutput.info("\nSyncing IDE configuration...");
    // We will use IdeFileGenerator directly later when we refactor it.
    // For now, let's assume we are calling the method that does the work.
    // The plan says "Refactor IdeFileGenerator... Update references in AddCommand..."
    // So here I should probably call IdeFileGenerator.
    IdeFileGenerator.generateClasspathFileWithDeps(new File("."), config);
    UserOutput.info("Updated " + ProjectPaths.DOT_CLASSPATH + " file");
  }
}
