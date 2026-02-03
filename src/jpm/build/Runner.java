package jpm.build;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Runner {
    
    public static class RunResult {
        public final boolean success;
        public final int exitCode;
        
        public RunResult(boolean success, int exitCode) {
            this.success = success;
            this.exitCode = exitCode;
        }
    }
    
    public RunResult run(String mainClass, File classesDir, String classpath) throws IOException {
        // Build java command
        List<String> command = new ArrayList<>();
        command.add("java");
        
        // Build classpath: classesDir + dependencies
        StringBuilder fullClasspath = new StringBuilder();
        fullClasspath.append(classesDir.getAbsolutePath());
        
        if (classpath != null && !classpath.isEmpty()) {
            fullClasspath.append(File.pathSeparator);
            fullClasspath.append(classpath);
        }
        
        command.add("-cp");
        command.add(fullClasspath.toString());
        command.add(mainClass);
        
        // Execute
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.inheritIO();
        
        Process process = pb.start();
        
        try {
            int exitCode = process.waitFor();
            return new RunResult(exitCode == 0, exitCode);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new RunResult(false, 1);
        }
    }
    
    public RunResult runWithArgs(String mainClass, File classesDir, String classpath, String[] args) throws IOException {
        List<String> command = new ArrayList<>();
        command.add("java");
        
        StringBuilder fullClasspath = new StringBuilder();
        fullClasspath.append(classesDir.getAbsolutePath());
        
        if (classpath != null && !classpath.isEmpty()) {
            fullClasspath.append(File.pathSeparator);
            fullClasspath.append(classpath);
        }
        
        command.add("-cp");
        command.add(fullClasspath.toString());
        command.add(mainClass);
        
        if (args != null) {
            for (String arg : args) {
                command.add(arg);
            }
        }
        
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.inheritIO();
        
        Process process = pb.start();
        
        try {
            int exitCode = process.waitFor();
            return new RunResult(exitCode == 0, exitCode);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new RunResult(false, 1);
        }
    }
}
