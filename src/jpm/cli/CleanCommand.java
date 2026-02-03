package jpm.cli;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;
import jpm.utils.FileUtils;
import jpm.utils.UserOutput;
import picocli.CommandLine.Command;

@Command(name = "clean", description = "Clean build artifacts")
public class CleanCommand implements Callable<Integer> {
  @Override
  public Integer call() {
    try {
      var targetDir = new File("target");

      if (targetDir.exists()) {
        UserOutput.info("Cleaning target/ directory...");
        FileUtils.deleteDirectory(targetDir);
        UserOutput.info("Clean complete");
      } else {
        UserOutput.info("Nothing to clean (no target/ directory found)");
      }

      return 0;

    } catch (IOException e) {
      CliErrorHandler.error("Cleaning", e);
      return 1;
    }
  }
}
