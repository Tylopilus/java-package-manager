package jpm.build;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Runner {
    
    /**
     * Record representing the result of a run operation.
     * Uses Java 16+ records for concise immutable data classes.
     */
    public record RunResult(boolean success, int exitCode) {}
    
    public RunResult run(String mainClass, File classesDir, String classpath) throws IOException {
        // Build java command
        var command = new ArrayList<String>();
        command.add("java");
        
        // Build classpath: classesDir + dependencies
        var fullClasspath = new StringBuilder();
        fullClasspath.append(classesDir.getAbsolutePath());
        
        if (classpath != null && !classpath.isEmpty()) {
            fullClasspath.append(File.pathSeparator);
            fullClasspath.append(classpath);
        }
        
        command.add("-cp");
        command.add(fullClasspath.toString());
        command.add(mainClass);
        
        // Execute
        var pb = new ProcessBuilder(command);
        pb.inheritIO();
        
        var process = pb.start();
        
        try {
            var exitCode = process.waitFor();
            return new RunResult(exitCode == 0, exitCode);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new RunResult(false, 1);
        }
    }
    
    public RunResult runWithArgs(String mainClass, File classesDir, String classpath, String[] args) throws IOException {
        var command = new ArrayList<String>();
        command.add("java");
        
        var fullClasspath = new StringBuilder();
        fullClasspath.append(classesDir.getAbsolutePath());
        
        if (classpath != null && !classpath.isEmpty()) {
            fullClasspath.append(File.pathSeparator);
            fullClasspath.append(classpath);
        }
        
        command.add("-cp");
        command.add(fullClasspath.toString());
        command.add(mainClass);
        
        if (args != null) {
            for (var arg : args) {
                command.add(arg);
            }
        }
        
        var pb = new ProcessBuilder(command);
        pb.inheritIO();
        
        var process = pb.start();
        
        try {
            var exitCode = process.waitFor();
            return new RunResult(exitCode == 0, exitCode);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new RunResult(false, 1);
        }
    }
}
