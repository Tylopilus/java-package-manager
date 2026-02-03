package jpm.utils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public class FileUtils {
    
    public static void ensureDirectory(File dir) {
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }
    
    public static void ensureDirectory(String path) {
        ensureDirectory(new File(path));
    }
    
    public static void deleteDirectory(File dir) throws IOException {
        if (!dir.exists()) {
            return;
        }
        
        // Collect all paths first to enable parallel deletion for large directories
        List<Path> paths;
        try (Stream<Path> stream = Files.walk(dir.toPath())) {
            paths = stream.sorted(Comparator.reverseOrder()).toList();
        }
        
        // Use parallel deletion for large directories (> 100 files)
        if (paths.size() > 100) {
            paths.parallelStream()
                .map(Path::toFile)
                .forEach(File::delete);
        } else {
            paths.forEach(path -> path.toFile().delete());
        }
    }
    
    public static void deleteDirectory(String path) throws IOException {
        deleteDirectory(new File(path));
    }
    
    public static void writeFile(File file, String content) throws IOException {
        File parent = file.getParentFile();
        if (parent != null) {
            ensureDirectory(parent);
        }
        Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
    }
    
    public static void writeFile(String path, String content) throws IOException {
        writeFile(new File(path), content);
    }
    
    public static String readFile(File file) throws IOException {
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }
    
    public static String readFile(String path) throws IOException {
        return readFile(new File(path));
    }
    
    public static File getJpmHome() {
        String userHome = System.getProperty("user.home");
        return new File(userHome, ".jpm");
    }
    
    public static File getCacheDir() {
        return new File(getJpmHome(), "cache");
    }
    
    public static File getDependencyDir(String groupId, String artifactId, String version) {
        String groupPath = groupId.replace('.', File.separatorChar);
        return new File(getCacheDir(), groupPath + File.separator + artifactId + File.separator + version);
    }
}
