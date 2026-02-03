package jpm.cli;

import jpm.build.ClasspathGenerator;
import jpm.config.ConfigParser;
import jpm.config.JpmConfig;
import picocli.CommandLine.Command;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;

@Command(name = "sync", description = "Sync IDE configuration (.classpath file)")
public class SyncCommand implements Callable<Integer> {

    @Override
    public Integer call() {
        try {
            File configFile = new File("jpm.toml");
            if (!configFile.exists()) {
                System.err.println("Error: No jpm.toml found. Run 'jpm new <name>' first.");
                return 1;
            }

            System.out.println("Syncing IDE configuration...");

            // Load config
            JpmConfig config = ConfigParser.load(configFile);

            // Generate .classpath file
            ClasspathGenerator generator = new ClasspathGenerator();
            generator.generateClasspath(config, new File("."));

            System.out.println("Generated .classpath file for IDE integration");
            return 0;

        } catch (IOException e) {
            System.err.println("Error syncing IDE configuration: " + e.getMessage());
            return 1;
        }
    }
}
