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
    
    public static class CompileResult {
        public final boolean success;
        public final String output;
        public final int exitCode;
        
        public CompileResult(boolean success, String output, int exitCode) {
            this.success = success;
            this.output = output;
            this.exitCode = exitCode;
        }
    }
    
    public CompileResult compile(File sourceDir, File outputDir, String classpath) throws IOException {
        // Find all Java source files
        List<String> sourceFiles = findSourceFiles(sourceDir);
        
        if (sourceFiles.isEmpty()) {
            return new CompileResult(false, "No Java source files found in " + sourceDir, 1);
        }
        
        // Ensure output directory exists
        outputDir.mkdirs();
        
        // Build javac command
        List<String> command = new ArrayList<>();
        command.add("javac");
        command.add("-d");
        command.add(outputDir.getAbsolutePath());
        
        if (classpath != null && !classpath.isEmpty()) {
            command.add("-cp");
            command.add(classpath);
        }
        
        command.addAll(sourceFiles);
        
        // Execute
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.inheritIO();
        pb.directory(sourceDir);
        
        Process process = pb.start();
        
        try {
            int exitCode = process.waitFor();
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
        
        Path sourcePath = sourceDir.toPath().toAbsolutePath().normalize();
        
        try (Stream<Path> stream = Files.walk(sourcePath)) {
            return stream
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".java"))
                .map(p -> sourcePath.relativize(p).toString())
                .collect(Collectors.toList());
        }
    }
}
