package jpm.build;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Formats Java source code using Palantir Java Format.
 * Provides both formatting and checking capabilities.
 */
public class CodeFormatter {
    
    private static final String PALANTIR_VERSION = "2.50.0";
    
    /**
     * Formats a file or directory of Java files.
     * 
     * @param path file or directory to format
     * @param checkOnly if true, only check formatting without modifying files
     * @param skipPatterns glob patterns to skip
     * @param lineLength maximum line length
     * @return FormatResult with statistics
     * @throws IOException if formatting fails
     */
    public FormatResult formatPath(File path, boolean checkOnly, String[] skipPatterns, int lineLength) throws IOException {
        var javaFiles = new ArrayList<File>();
        collectJavaFiles(path, javaFiles, skipPatterns);
        
        var totalFiles = javaFiles.size();
        var formattedFiles = 0;
        var skippedFiles = 0;
        var failedFiles = 0;
        var unformattedFiles = new ArrayList<String>();
        
        // Check if Palantir formatter is available
        var formatterJar = getPalantirJar();
        if (!formatterJar.exists()) {
            // Fallback: just count files and report
            System.err.println("Warning: Palantir Java Format not available. Run bootstrap to install.");
            return new FormatResult(totalFiles, 0, 0, 0, unformattedFiles);
        }
        
        for (var javaFile : javaFiles) {
            try {
                if (checkOnly) {
                    // Check if file is formatted correctly
                    if (!isFormatted(javaFile, formatterJar, lineLength)) {
                        unformattedFiles.add(javaFile.getPath());
                    }
                } else {
                    // Format the file
                    if (formatFile(javaFile, formatterJar, lineLength)) {
                        formattedFiles++;
                    } else {
                        failedFiles++;
                    }
                }
            } catch (Exception e) {
                System.err.println("Error processing " + javaFile + ": " + e.getMessage());
                failedFiles++;
            }
        }
        
        return new FormatResult(totalFiles, formattedFiles, skippedFiles, failedFiles, unformattedFiles);
    }
    
    /**
     * Recursively collects all .java files from a directory.
     */
    private void collectJavaFiles(File file, List<File> javaFiles, String[] skipPatterns) {
        if (shouldSkip(file, skipPatterns)) {
            return;
        }
        
        if (file.isDirectory()) {
            var children = file.listFiles();
            if (children != null) {
                for (var child : children) {
                    collectJavaFiles(child, javaFiles, skipPatterns);
                }
            }
        } else if (file.getName().endsWith(".java")) {
            javaFiles.add(file);
        }
    }
    
    /**
     * Checks if a file should be skipped based on patterns.
     */
    private boolean shouldSkip(File file, String[] skipPatterns) {
        if (skipPatterns == null) return false;
        
        var path = file.getPath();
        for (var pattern : skipPatterns) {
            // Simple glob matching (can be improved)
            var regex = pattern.replace("**", ".*").replace("*", "[^/]*");
            if (path.matches(regex)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Gets the Palantir Java Format JAR location.
     */
    private File getPalantirJar() {
        var home = System.getProperty("user.home");
        return new File(home, ".jpm/lib/palantir-java-format-" + PALANTIR_VERSION + "-all.jar");
    }
    
    /**
     * Checks if a file is already formatted.
     * Returns true if formatted correctly, false if needs formatting.
     */
    private boolean isFormatted(File javaFile, File formatterJar, int lineLength) throws IOException {
        var command = new ArrayList<String>();
        command.add("java");
        command.add("-jar");
        command.add(formatterJar.getAbsolutePath());
        command.add("--check");
        command.add(javaFile.getAbsolutePath());
        
        var pb = new ProcessBuilder(command);
        pb.inheritIO();
        
        try {
            var process = pb.start();
            var exitCode = process.waitFor();
            return exitCode == 0; // 0 means already formatted
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
    
    /**
     * Formats a single Java file.
     * Returns true if successful, false otherwise.
     */
    private boolean formatFile(File javaFile, File formatterJar, int lineLength) throws IOException {
        var command = new ArrayList<String>();
        command.add("java");
        command.add("-jar");
        command.add(formatterJar.getAbsolutePath());
        command.add("--replace");
        command.add(javaFile.getAbsolutePath());
        
        var pb = new ProcessBuilder(command);
        pb.inheritIO();
        
        try {
            var process = pb.start();
            var exitCode = process.waitFor();
            return exitCode == 0;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
    
    /**
     * Result of a formatting operation.
     */
    public record FormatResult(
            int totalFiles,
            int formattedFiles,
            int skippedFiles,
            int failedFiles,
            List<String> unformattedFiles) {
        
        public int totalFiles() {
            return totalFiles;
        }
        
        public int formattedFiles() {
            return formattedFiles;
        }
        
        public int skippedFiles() {
            return skippedFiles;
        }
        
        public int failedFiles() {
            return failedFiles;
        }
        
        public List<String> unformattedFiles() {
            return unformattedFiles;
        }
    }
}
