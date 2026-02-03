package jpm.build;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Compiler {
    
    /**
     * Record representing the result of a compilation operation.
     * Uses Java 16+ records for concise immutable data classes.
     */
    public record CompileResult(boolean success, String output, int exitCode) {}

    public CompileResult compile(File sourceDir, File outputDir, String classpath) throws IOException {
        return compileWithArgs(sourceDir, outputDir, classpath, List.of());
    }

    public CompileResult compileWithArgs(File sourceDir, File outputDir, String classpath, List<String> compilerArgs) throws IOException {
        // Find all Java source files
        var sourceFiles = findSourceFiles(sourceDir);
        
        if (sourceFiles.isEmpty()) {
            return new CompileResult(false, "No Java source files found in " + sourceDir, 1);
        }
        
        // Ensure output directory exists
        outputDir.mkdirs();
        
        // Build javac command
        var command = new ArrayList<String>();
        command.add("javac");
        command.add("-d");
        command.add(outputDir.getAbsolutePath());
        
        if (classpath != null && !classpath.isEmpty()) {
            command.add("-cp");
            command.add(classpath);
        }

        if (!compilerArgs.isEmpty()) {
            command.addAll(compilerArgs);
        }

        command.addAll(sourceFiles);
        
        // Execute
        var pb = new ProcessBuilder(command);
        pb.inheritIO();
        pb.directory(sourceDir);
        
        var process = pb.start();
        
        try {
            var exitCode = process.waitFor();
            return new CompileResult(exitCode == 0, "", exitCode);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new CompileResult(false, "Compilation interrupted", 1);
        }
    }
    
    private List<String> findSourceFiles(File sourceDir) throws IOException {
        if (!sourceDir.exists()) {
            return new ArrayList<>();
        }
        
        var sourcePath = sourceDir.toPath().toAbsolutePath().normalize();
        
        try (var stream = Files.walk(sourcePath)) {
            return stream
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".java"))
                .map(p -> sourcePath.relativize(p).toString())
                .collect(Collectors.toList());
        }
    }
}
