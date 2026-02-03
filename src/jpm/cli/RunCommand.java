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
    protected int execute() throws Exception {
        System.out.println("Running...\n");

        var runner = new Runner();
        var mainClass = "Main";
        var jvmArgs = profileConfig.getEffectiveJvmArgs();
        var outputDir = getOutputDir();

        Runner.RunResult runResult;
        if (args != null && args.length > 0) {
            runResult = runner.runWithArgsAndJvmArgs(mainClass, outputDir, classpath, args, jvmArgs);
        } else {
            runResult = runner.runWithJvmArgs(mainClass, outputDir, classpath, jvmArgs);
        }

        return runResult.exitCode();
    }

    @Override
    protected String getCommandName() {
        return "run";
    }
}
