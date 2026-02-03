package jpm.cli;

import jpm.build.Compiler;
import jpm.build.Runner;
import jpm.build.ClasspathBuilder;
import jpm.build.IdeFileGenerator;
import jpm.config.ConfigParser;
import jpm.config.JpmConfig;
import jpm.deps.DependencyResolver;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.util.List;
import java.util.concurrent.Callable;

@Command(name = "run", description = "Build and run the project")
public class RunCommand implements Callable<Integer> {
    @Parameters(arity = "0..*", description = "Arguments to pass to main class")
    private String[] args;

    @Option(names = {"--force-resolve"}, description = "Force re-resolution of dependencies, ignoring lockfile")
    private boolean forceResolve;

    @Option(names = {"--no-ide-files"}, description = "Skip generation of IDE configuration files (.project, .classpath)")
    private boolean noIdeFiles;

    @Override
    public Integer call() {
        try {
            // Check for jpm.toml
            File configFile = new File("jpm.toml");
            if (!configFile.exists()) {
                System.err.println("Error: No jpm.toml found. Run 'jpm new <name>' first.");
                return 1;
            }

            // Load config
            JpmConfig config = ConfigParser.load(configFile);

            System.out.println("Building " + config.getPackage().getName() + " v" + config.getPackage().getVersion());

            // Generate IDE files if missing and not disabled
            File projectDir = new File(".");
            if (!noIdeFiles && IdeFileGenerator.shouldGenerateIdeFiles(projectDir)) {
                // We'll generate .project now, and .classpath after dependency resolution
                IdeFileGenerator.generateProjectFile(projectDir, config);
            }

            // Resolve dependencies
            String classpath = "";
            if (!config.getDependencies().isEmpty()) {
                DependencyResolver resolver = new DependencyResolver();
                List<DependencyResolver.ResolvedDependency> deps = resolver.resolveWithLockfile(projectDir, config, forceResolve);
                classpath = ClasspathBuilder.buildClasspath(deps);
                System.out.println("Resolved " + deps.size() + " dependencies");
            }

            // Generate .classpath after dependency resolution if needed
            boolean classpathExisted = new File(projectDir, ".classpath").exists();
            if (!noIdeFiles && !classpathExisted) {
                IdeFileGenerator.generateClasspathFile(projectDir, config, classpath);
                classpathExisted = true;
            }

            // Show message if both files were generated
            if (!noIdeFiles) {
                boolean projectExisted = new File(projectDir, ".project").exists();
                if (!projectExisted || !classpathExisted) {
                    System.out.println("Generated IDE configuration files (.project, .classpath)");
                }
            }

            // Show message if both files were generated
            if (!noIdeFiles && (IdeFileGenerator.shouldGenerateIdeFiles(projectDir) ||
                (!new File(projectDir, ".project").exists() && !new File(projectDir, ".classpath").exists()))) {
                // This won't show since we just generated them, but let's check before generation
            }

            if (!noIdeFiles) {
                boolean projectExisted = new File(projectDir, ".project").exists();
                if (!projectExisted) {
                    System.out.println("Generated IDE configuration files (.project, .classpath)");
                }
            }

            // Compile
            File sourceDir = new File("src");
            File outputDir = new File("target/classes");
            
            if (!sourceDir.exists()) {
                System.err.println("Error: No src/ directory found");
                return 1;
            }
            
            Compiler compiler = new Compiler();
            Compiler.CompileResult compileResult = compiler.compile(sourceDir, outputDir, classpath);
            
            if (!compileResult.success) {
                System.err.println("Build failed with exit code " + compileResult.exitCode);
                return compileResult.exitCode;
            }
            
            System.out.println("Running...\n");
            
            // Run
            Runner runner = new Runner();
            String mainClass = "Main";
            
            Runner.RunResult runResult;
            if (args != null && args.length > 0) {
                runResult = runner.runWithArgs(mainClass, outputDir, classpath, args);
            } else {
                runResult = runner.run(mainClass, outputDir, classpath);
            }
            
            return runResult.exitCode;
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            return 1;
        }
    }
}
