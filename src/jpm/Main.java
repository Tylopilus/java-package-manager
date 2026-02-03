package jpm;

import jpm.cli.NewCommand;
import jpm.cli.AddCommand;
import jpm.cli.RemoveCommand;
import jpm.cli.BuildCommand;
import jpm.cli.RunCommand;
import jpm.cli.CleanCommand;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(
    name = "jpm",
    mixinStandardHelpOptions = true,
    version = "jpm 0.1.0",
    description = "Java Package Manager - Cargo for Java",
    subcommands = {
        NewCommand.class,
        AddCommand.class,
        RemoveCommand.class,
        BuildCommand.class,
        RunCommand.class,
        CleanCommand.class
    }
)
public class Main implements Runnable {
    public static void main(String[] args) {
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public void run() {
        System.out.println("jpm - Java Package Manager");
        System.out.println("Use 'jpm --help' for available commands");
    }
}
