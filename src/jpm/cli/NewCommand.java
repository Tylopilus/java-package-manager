package jpm.cli;

import jpm.config.ConfigParser;
import jpm.config.JpmConfig;
import jpm.utils.FileUtils;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;

@Command(name = "new", description = "Create a new Java project")
public class NewCommand implements Callable<Integer> {
    @Parameters(index = "0", description = "Project name")
    private String projectName;

    @Override
    public Integer call() {
        try {
            File projectDir = new File(projectName);
            
            if (projectDir.exists()) {
                System.err.println("Error: Directory '" + projectName + "' already exists");
                return 1;
            }
            
            System.out.println("Creating new Java project '" + projectName + "'");
            
            // Create directories
            FileUtils.ensureDirectory(new File(projectDir, "src"));
            
            // Create jpm.toml
            JpmConfig config = new JpmConfig();
            config.getPackage().setName(projectName);
            config.getPackage().setVersion("0.1.0");
            config.getPackage().setJavaVersion("21");
            
            File configFile = new File(projectDir, "jpm.toml");
            ConfigParser.save(config, configFile);
            System.out.println("  Created jpm.toml");
            
            // Create Main.java template
            String mainClass = generateMainClass(projectName);
            File mainFile = new File(projectDir, "src/Main.java");
            FileUtils.writeFile(mainFile, mainClass);
            System.out.println("  Created src/Main.java");

            // Create .project file (required for IDE integration)
            String projectXml = generateProjectFile(projectName);
            File projectFile = new File(projectDir, ".project");
            FileUtils.writeFile(projectFile, projectXml);
            System.out.println("  Created .project");

            // Create initial .classpath file
            String classpathXml = generateClasspath(config.getPackage().getJavaVersion());
            File classpathFile = new File(projectDir, ".classpath");
            FileUtils.writeFile(classpathFile, classpathXml);
            System.out.println("  Created .classpath");

            // Create .gitignore
            String gitignore = generateGitignore();
            File gitignoreFile = new File(projectDir, ".gitignore");
            FileUtils.writeFile(gitignoreFile, gitignore);
            System.out.println("  Created .gitignore");
            
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
        return "public class Main {\n" +
               "    public static void main(String[] args) {\n" +
               "        System.out.println(\"Hello, " + projectName + "!\");\n" +
               "    }\n" +
               "}\n";
    }

    private String generateProjectFile(String projectName) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
               "<projectDescription>\n" +
               "\t<name>" + projectName + "</name>\n" +
               "\t<comment></comment>\n" +
               "\t<projects>\n" +
               "\t</projects>\n" +
               "\t<buildSpec>\n" +
               "\t\t<buildCommand>\n" +
               "\t\t\t<name>org.eclipse.jdt.core.javabuilder</name>\n" +
               "\t\t\t<arguments>\n" +
               "\t\t\t</arguments>\n" +
               "\t\t</buildCommand>\n" +
               "\t</buildSpec>\n" +
               "\t<natures>\n" +
               "\t\t<nature>org.eclipse.jdt.core.javanature</nature>\n" +
               "\t</natures>\n" +
               "</projectDescription>\n";
    }

    private String generateClasspath(String javaVersion) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
               "<classpath>\n" +
               "\t<classpathentry kind=\"src\" path=\"src\"/>\n" +
               "\t<classpathentry kind=\"output\" path=\"target/classes\"/>\n" +
               "\t<classpathentry kind=\"con\" path=\"org.eclipse.jdt.launching.JRE_CONTAINER/org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType/JavaSE-" + javaVersion + "\"/>\n" +
               "</classpath>\n";
    }

    private String generateGitignore() {
        return "# jpm build output\n" +
               "target/\n" +
               "\n" +
               "# IDE files (generated, not committed)\n" +
               ".idea/\n" +
               "*.iml\n" +
               ".classpath\n" +
               ".settings/\n" +
               "\n" +
               "# OS files\n" +
               ".DS_Store\n" +
               "Thumbs.db\n" +
               "\n" +
               "# Logs\n" +
               "*.log\n" +
               "\n" +
               "# Local env files\n" +
               ".env\n" +
               ".env.local\n";
    }
}
