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
}
