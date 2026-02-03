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
    
    /**
     * Runs the main class with profile-specific JVM arguments.
     * 
     * @param mainClass the main class to run
     * @param classesDir the classes directory
     * @param classpath the dependency classpath
     * @param jvmArgs JVM arguments from profile (e.g., -ea, -server, -Xmx2g)
     * @return RunResult with exit code
     * @throws IOException if execution fails
     */
    public RunResult runWithJvmArgs(String mainClass, File classesDir, String classpath, List<String> jvmArgs) throws IOException {
        var command = new ArrayList<String>();
        command.add("java");
        
        // Add profile-specific JVM arguments
        if (jvmArgs != null && !jvmArgs.isEmpty()) {
            command.addAll(jvmArgs);
        }
        
        var fullClasspath = new StringBuilder();
        fullClasspath.append(classesDir.getAbsolutePath());
        
        if (classpath != null && !classpath.isEmpty()) {
            fullClasspath.append(File.pathSeparator);
            fullClasspath.append(classpath);
        }
        
        command.add("-cp");
        command.add(fullClasspath.toString());
        command.add(mainClass);
        
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
    
    /**
     * Runs the main class with both program arguments and profile-specific JVM arguments.
     * 
     * @param mainClass the main class to run
     * @param classesDir the classes directory
     * @param classpath the dependency classpath
     * @param args program arguments to pass to main()
     * @param jvmArgs JVM arguments from profile
     * @return RunResult with exit code
     * @throws IOException if execution fails
     */
    public RunResult runWithArgsAndJvmArgs(String mainClass, File classesDir, String classpath, String[] args, List<String> jvmArgs) throws IOException {
        var command = new ArrayList<String>();
        command.add("java");
        
        // Add profile-specific JVM arguments
        if (jvmArgs != null && !jvmArgs.isEmpty()) {
            command.addAll(jvmArgs);
        }
        
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
