package jpm.config;

/**
 * Constants for project file paths and directory names.
 * Centralizes all hardcoded path strings.
 */
public final class ProjectPaths {
    
    // Configuration files
    public static final String CONFIG_FILE = "jpm.toml";
    public static final String LOCK_FILE = "jpm.lock";
    
    // Source directories
    public static final String SRC_DIR = "src";
    public static final String TEST_DIR = "src/test/java";
    
    // Output directories
    public static final String TARGET_DIR = "target";
    public static final String CLASSES_DIR = "target/classes";
    public static final String TEST_CLASSES_DIR = "target/test-classes";
    
    // IDE and Git files
    public static final String DOT_PROJECT = ".project";
    public static final String DOT_CLASSPATH = ".classpath";
    public static final String DOT_GITIGNORE = ".gitignore";
    
    private ProjectPaths() {
        // Prevent instantiation
    }
}
