package jpm;

import jpm.cli.AddCommand;
import jpm.cli.BuildCommand;
import jpm.cli.CleanCommand;
import jpm.cli.FormatCommand;
import jpm.cli.NewCommand;
import jpm.cli.RemoveCommand;
import jpm.cli.RunCommand;
import jpm.cli.SyncCommand;
import jpm.cli.TestCommand;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(
    name = "jpm",
    mixinStandardHelpOptions = true,
    version = JpmVersion.CURRENT,
    description = "Java Package Manager - Cargo for Java",
    subcommands = {
      NewCommand.class,
      AddCommand.class,
      RemoveCommand.class,
      BuildCommand.class,
      RunCommand.class,
      TestCommand.class,
      FormatCommand.class,
      CleanCommand.class,
      SyncCommand.class
    })
public class Main implements Runnable {
  public static void main(String[] args) {
    // Check Java version before starting
    JpmVersion.checkJavaVersion();

    int exitCode = new CommandLine(new Main()).execute(args);
    System.exit(exitCode);
  }

  @Override
  public void run() {
    System.out.println(JpmVersion.getDisplayVersion());
    System.out.println("Use 'jpm --help' for available commands");
  }
}
