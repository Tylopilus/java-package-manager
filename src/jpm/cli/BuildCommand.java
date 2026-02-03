package jpm.cli;

import jpm.build.Compiler;
import jpm.build.ClasspathBuilder;
import jpm.config.ConfigParser;
import jpm.config.JpmConfig;
import jpm.deps.CacheManager;
import jpm.deps.DependencyResolver;
import jpm.utils.FileUtils;
import picocli.CommandLine.Command;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;

@Command(name = "build", description = "Build the project")
public class BuildCommand implements Callable<Integer> {
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
            
            // Resolve dependencies
            String classpath = "";
            if (!config.getDependencies().isEmpty()) {
                System.out.println("Resolving dependencies...");
                DependencyResolver resolver = new DependencyResolver();
                List<DependencyResolver.ResolvedDependency> deps = resolver.resolveAll(config.getDependencies());
                classpath = ClasspathBuilder.buildClasspath(deps);
                System.out.println("Resolved " + deps.size() + " dependencies");
            }
            
            // Compile
            File sourceDir = new File("src");
            File outputDir = new File("target/classes");
            
            if (!sourceDir.exists()) {
                System.err.println("Error: No src/ directory found");
                return 1;
            }
            
            Compiler compiler = new Compiler();
            Compiler.CompileResult result = compiler.compile(sourceDir, outputDir, classpath);
            
            if (result.success) {
                System.out.println("Build successful! Output in target/classes/");
                return 0;
            } else {
                System.err.println("Build failed with exit code " + result.exitCode);
                return result.exitCode;
            }
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            return 1;
        }
    }
}
