package jpm.cli;

import jpm.build.Compiler;
import jpm.config.ProjectPaths;
import jpm.utils.UserOutput;
import picocli.CommandLine.Command;

/**
 * Build command - compiles the project source code.
 * Extends AbstractBuildCommand for shared build lifecycle.
 */
@Command(name = "build", description = "Build the project")
public class BuildCommand extends AbstractBuildCommand {

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
  protected int execute() {
    UserOutput.info("Build successful! Output in " + ProjectPaths.CLASSES_DIR + "/");
    return 0;
  }

  @Override
  protected String getCommandName() {
    return "build";
  }
}
