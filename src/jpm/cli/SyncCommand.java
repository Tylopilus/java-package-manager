package jpm.cli;

import java.io.IOException;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;

@Command(name = "sync", description = "Sync IDE configuration (.classpath file)")
public class SyncCommand implements Callable<Integer> {

  @Override
  public Integer call() {
    try {
      var config = CommandUtils.loadConfigOrFail();
      if (config == null) {
        return 1;
      }

      CommandUtils.syncIdeConfig(config);

      return 0;

    } catch (IOException e) {
      CliErrorHandler.error("Syncing IDE configuration", e);
      return 1;
    }
  }
}
