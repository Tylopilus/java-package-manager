package jpm.cli;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;
import jpm.config.ConfigParser;
import jpm.config.FmtConfig;
import jpm.config.JpmConfig;
import jpm.config.ProjectPaths;
import jpm.utils.FileUtils;
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
        System.err.println("Error: Directory '" + projectName + "' already exists");
        return 1;
      }

      System.out.println("Creating new Java project '" + projectName + "'");

      // Create directories
      FileUtils.ensureDirectory(new File(projectDir, ProjectPaths.SRC_DIR));

      // Create jpm.toml
      var pkg = new JpmConfig.PackageConfig(projectName, "0.1.0", "21");
      var config =
          new JpmConfig(pkg, new java.util.HashMap<>(), new java.util.HashMap<>(), new FmtConfig());

      var configFile = new File(projectDir, ProjectPaths.CONFIG_FILE);
      ConfigParser.save(config, configFile);
      System.out.println("  Created " + ProjectPaths.CONFIG_FILE);

      // Create Main.java template
      var mainClass = generateMainClass(projectName);
      var mainFile = new File(projectDir, ProjectPaths.SRC_DIR + "/Main.java");
      FileUtils.writeFile(mainFile, mainClass);
      System.out.println("  Created " + ProjectPaths.SRC_DIR + "/Main.java");

      // Create .project file (required for IDE integration)
      var projectXml = generateProjectFile(projectName);
      var projectFile = new File(projectDir, ProjectPaths.DOT_PROJECT);
      FileUtils.writeFile(projectFile, projectXml);
      System.out.println("  Created " + ProjectPaths.DOT_PROJECT);

      // Create initial .classpath file
      var classpathXml = generateClasspath(config.package_().javaVersion());
      var classpathFile = new File(projectDir, ProjectPaths.DOT_CLASSPATH);
      FileUtils.writeFile(classpathFile, classpathXml);
      System.out.println("  Created " + ProjectPaths.DOT_CLASSPATH);

      // Create .gitignore
      var gitignore = generateGitignore();
      var gitignoreFile = new File(projectDir, ProjectPaths.DOT_GITIGNORE);
      FileUtils.writeFile(gitignoreFile, gitignore);
      System.out.println("  Created " + ProjectPaths.DOT_GITIGNORE);

      System.out.println("\nProject '" + projectName + "' created successfully!");
      System.out.println("  cd " + projectName);
      System.out.println("  jpm run");

      return 0;

    } catch (IOException e) {
      System.err.println("Error creating project: " + e.getMessage());
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

  private String generateProjectFile(String projectName) {
    return """
        <?xml version="1.0" encoding="UTF-8"?>
        <projectDescription>
        	<name>%s</name>
        	<comment></comment>
        	<projects>
        	</projects>
        	<buildSpec>
        		<buildCommand>
        			<name>org.eclipse.jdt.core.javabuilder</name>
        			<arguments>
        			</arguments>
        		</buildCommand>
        	</buildSpec>
        	<natures>
        		<nature>org.eclipse.jdt.core.javanature</nature>
        	</natures>
        </projectDescription>
        """.formatted(projectName);
  }

  private String generateClasspath(String javaVersion) {
    return """
        <?xml version="1.0" encoding="UTF-8"?>
        <classpath>
        	<classpathentry kind="src" path="src"/>
        	<classpathentry kind="output" path="target/classes"/>
        	<classpathentry kind="con" path="org.eclipse.jdt.launching.JRE_CONTAINER/org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType/JavaSE-%s"/>
        </classpath>
        """.formatted(javaVersion);
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
