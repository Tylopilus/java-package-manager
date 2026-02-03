package jpm.cli;

import jpm.build.Compiler;
import jpm.build.Runner;
import jpm.config.ProjectPaths;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

/**
 * Run command - builds and runs the project.
 * Extends AbstractBuildCommand for shared build lifecycle.
 */
@Command(name = "run", description = "Build and run the project")
public class RunCommand extends AbstractBuildCommand {

  @Parameters(arity = "0..*", description = "Arguments to pass to main class")
  private String[] args;

  @Override
  protected void validateProject() {
    super.validateProject();
    validateSourceDirExists(getSourceDir(), ProjectPaths.SRC_DIR);
  }

  @Override
  protected Compiler.CompileResult compile() throws Exception {
    return compileSources(getSourceDir(), getOutputDir());
  }

  @Override
  protected int execute() throws Exception {
    System.out.println("Running...\n");

    var runner = new Runner();
    var mainClass = "Main";
    var jvmArgs = profileConfig.getEffectiveJvmArgs();
    var outputDir = getOutputDir();

    Runner.RunResult runResult = runner.run(mainClass, outputDir, classpath, jvmArgs, args);

    return runResult.exitCode();
  }

  @Override
  protected String getCommandName() {
    return "run";
  }
}
