package jpm.cli;

import jpm.build.Compiler;
import jpm.config.ProjectPaths;
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

        var sourceDir = getSourceDir();
        if (!sourceDir.exists()) {
            throw new ProjectValidationException("No " + ProjectPaths.SRC_DIR + "/ directory found");
        }
    }

    @Override
    protected Compiler.CompileResult compile() throws Exception {
        var sourceDir = getSourceDir();
        var outputDir = getOutputDir();

        var compiler = new Compiler();
        var compilerArgs = profileConfig.getEffectiveCompilerArgs();
        return compiler.compileWithArgs(sourceDir, outputDir, classpath, compilerArgs);
    }

    @Override
    protected int execute() {
        System.out.println("Build successful! Output in " + ProjectPaths.CLASSES_DIR + "/");
        return 0;
    }

    @Override
    protected String getCommandName() {
        return "build";
    }
}
