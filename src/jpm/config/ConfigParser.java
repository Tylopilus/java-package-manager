package jpm.config;

import com.moandjiezana.toml.Toml;
import jpm.utils.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ConfigParser {
    
    public static JpmConfig load(File configFile) throws IOException {
        if (!configFile.exists()) {
            return null;
        }
        
        Toml toml = new Toml().read(configFile);
        JpmConfig config = new JpmConfig();
        
        // Parse package section
        Toml packageToml = toml.getTable("package");
        if (packageToml != null) {
            JpmConfig.PackageConfig pkg = config.getPackage();
            pkg.setName(packageToml.getString("name"));
            pkg.setVersion(packageToml.getString("version"));
            pkg.setJavaVersion(packageToml.getString("java-version"));
        }
        
        // Parse dependencies section
        Toml depsToml = toml.getTable("dependencies");
        if (depsToml != null) {
            Map<String, String> deps = new HashMap<>();
            for (Map.Entry<String, Object> entry : depsToml.entrySet()) {
                String key = stripQuotes(entry.getKey());
                deps.put(key, entry.getValue().toString());
            }
            config.setDependencies(deps);
        }
        
        return config;
    }
    
    public static void save(JpmConfig config, File configFile) throws IOException {
        StringBuilder toml = new StringBuilder();
        
        // Package section
        toml.append("[package]\n");
        toml.append("name = \"").append(escape(config.getPackage().getName())).append("\"\n");
        toml.append("version = \"").append(escape(config.getPackage().getVersion())).append("\"\n");
        toml.append("java-version = \"").append(escape(config.getPackage().getJavaVersion())).append("\"\n");
        
        // Dependencies section
        if (!config.getDependencies().isEmpty()) {
            toml.append("\n[dependencies]\n");
            for (Map.Entry<String, String> entry : config.getDependencies().entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                // Keys with special characters need quotes
                if (key.contains(":") || key.contains(".") || key.contains("-")) {
                    toml.append("\"").append(escape(key)).append("\"");
                } else {
                    toml.append(key);
                }
                toml.append(" = \"").append(escape(value)).append("\"\n");
            }
        }
        
        FileUtils.writeFile(configFile, toml.toString());
    }
    
    private static String escape(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }
    
    private static String stripQuotes(String value) {
        if (value == null) return "";
        if (value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }
    
    public static JpmConfig loadOrCreate(File configFile) throws IOException {
        JpmConfig config = load(configFile);
        if (config == null) {
            config = new JpmConfig();
            config.getPackage().setVersion("0.1.0");
            config.getPackage().setJavaVersion("21");
        }
        return config;
    }
}
