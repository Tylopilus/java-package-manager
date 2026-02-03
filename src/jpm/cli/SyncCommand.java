package jpm.cli;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;
import jpm.build.ClasspathGenerator;
import jpm.config.ConfigParser;
import picocli.CommandLine.Command;

@Command(name = "sync", description = "Sync IDE configuration (.classpath file)")
public class SyncCommand implements Callable<Integer> {

  @Override
  public Integer call() {
    try {
      var configFile = new File("jpm.toml");
      if (!configFile.exists()) {
        CliErrorHandler.error("No jpm.toml found. Run 'jpm new <name>' first.");
        return 1;
      }

      System.out.println("Syncing IDE configuration...");

      // Load config
      var config = ConfigParser.load(configFile);

      // Generate .classpath file
      var generator = new ClasspathGenerator();
      generator.generateClasspath(config, new File("."));

      System.out.println("Generated .classpath file for IDE integration");
      return 0;

    } catch (IOException e) {
      CliErrorHandler.error("Syncing IDE configuration", e);
      return 1;
    }
  }
}
