package jpm.cli;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;
import jpm.build.IdeFileGenerator;
import jpm.config.ConfigParser;
import jpm.config.FmtConfig;
import jpm.config.JpmConfig;
import jpm.config.ProjectPaths;
import jpm.utils.Constants;
import jpm.utils.FileUtils;
import jpm.utils.UserOutput;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "new", description = "Create a new Java project")
public class NewCommand implements Callable<Integer> {
  @Parameters(index = "0", description = "Project name")
  private String projectName;

  @Override
  public Integer call() {
    try {
      var projectDir = new File(projectName);

      if (projectDir.exists()) {
        CliErrorHandler.error("Directory '" + projectName + "' already exists");
        return 1;
      }

      UserOutput.info("Creating new Java project '" + projectName + "'");

      // Create directories
      FileUtils.ensureDirectory(new File(projectDir, ProjectPaths.SRC_DIR));

      // Create jpm.toml
      var pkg = new JpmConfig.PackageConfig(projectName, "0.1.0", Constants.DEFAULT_JAVA_VERSION);
      var config =
          new JpmConfig(pkg, new java.util.HashMap<>(), new java.util.HashMap<>(), new FmtConfig());

      var configFile = new File(projectDir, ProjectPaths.CONFIG_FILE);
      ConfigParser.save(config, configFile);
      UserOutput.info("  Created " + ProjectPaths.CONFIG_FILE);

      // Create Main.java template
      var mainClass = generateMainClass(projectName);
      var mainFile = new File(projectDir, ProjectPaths.SRC_DIR + "/Main.java");
      FileUtils.writeFile(mainFile, mainClass);
      UserOutput.info("  Created " + ProjectPaths.SRC_DIR + "/Main.java");

      // Create .project file (required for IDE integration)
      IdeFileGenerator.generateProjectFile(projectDir, config);
      UserOutput.info("  Created " + ProjectPaths.DOT_PROJECT);

      // Create initial .classpath file
      IdeFileGenerator.generateClasspathFile(projectDir, config, null);
      UserOutput.info("  Created " + ProjectPaths.DOT_CLASSPATH);

      // Create .gitignore
      var gitignore = generateGitignore();
      var gitignoreFile = new File(projectDir, ProjectPaths.DOT_GITIGNORE);
      FileUtils.writeFile(gitignoreFile, gitignore);
      UserOutput.info("  Created " + ProjectPaths.DOT_GITIGNORE);

      UserOutput.info("\nProject '" + projectName + "' created successfully!");
      UserOutput.print("  cd " + projectName);
      UserOutput.print("  jpm run");

      return 0;

    } catch (IOException e) {
      CliErrorHandler.error("Creating project", e);
      return 1;
    }
  }

  private String generateMainClass(String projectName) {
    return """
        public class Main {
            public static void main(String[] args) {
                System.out.println("Hello, %s!");
            }
        }
        """.formatted(projectName);
  }

  private String generateGitignore() {
    return """
        # jpm build output
        target/

        # IDE files (generated, not committed)
        .idea/
        *.iml
        .classpath
        .settings/

        # OS files
        .DS_Store
        Thumbs.db

        # Logs
        *.log

        # Local env files
        .env
        .env.local
        """;
  }
}
