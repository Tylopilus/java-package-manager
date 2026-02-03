package jpm.cli;

import jpm.build.ClasspathGenerator;
import jpm.config.ConfigParser;
import jpm.config.JpmConfig;
import jpm.deps.DependencyResolver;
import jpm.deps.MavenSearchClient;
import jpm.utils.FileUtils;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

@Command(name = "add", description = "Add one or more dependencies")
public class AddCommand implements Callable<Integer> {
    
    @Parameters(arity = "1..*", description = "Dependencies to add (group:artifact:version, or search terms)")
    private List<String> dependencies;
    
    @Option(names = {"-y", "--yes"}, description = "Auto-confirm all prompts")
    private boolean autoConfirm;
    
    @Option(names = {"--no-search"}, description = "Disable search, require exact coordinates")
    private boolean noSearch;
    
    private final MavenSearchClient searchClient;
    private final BufferedReader reader;
    
    public AddCommand() {
        this.searchClient = new MavenSearchClient();
        this.reader = new BufferedReader(new InputStreamReader(System.in));
    }
    
    @Override
    public Integer call() {
        try {
            // Check for jpm.toml
            File configFile = new File("jpm.toml");
            if (!configFile.exists()) {
                System.err.println("Error: No jpm.toml found. Run 'jpm new <name>' first.");
                return 1;
            }
            
            int successCount = 0;
            int total = dependencies.size();
            
            // Track all dependencies to add
            List<DependencyInfo> depsToAdd = new ArrayList<>();
            
            // Phase 1: Collect all dependencies (with interactive selection)
            for (int i = 0; i < total; i++) {
                String dep = dependencies.get(i);
                
                if (total > 1) {
                    System.out.println("\n[" + (i + 1) + "/" + total + "] Processing \"" + dep + "\"...");
                }
                
                DependencyInfo info = parseDependency(dep);
                if (info != null) {
                    depsToAdd.add(info);
                    successCount++;
                }
            }
            
            if (depsToAdd.isEmpty()) {
                System.out.println("\nNo dependencies to add.");
                return 0;
            }
            
            // Phase 2: Resolve and add all dependencies
            System.out.println("\nResolving " + depsToAdd.size() + " dependencies...");
            
            JpmConfig config = ConfigParser.loadOrCreate(configFile);
            DependencyResolver resolver = new DependencyResolver();
            int totalResolved = 0;
            
            for (DependencyInfo info : depsToAdd) {
                String ga = info.groupId + ":" + info.artifactId;
                
                try {
                    // Resolve the dependency
                    List<DependencyResolver.ResolvedDependency> resolved = 
                        resolver.resolve(info.groupId, info.artifactId, info.version);
                    
                    if (!resolved.isEmpty()) {
                        // Add to config
                        config.addDependency(ga, info.version);
                        totalResolved += resolved.size();
                        System.out.println("  Added " + ga + ":" + info.version + " (" + resolved.size() + " deps)");
                    }
                } catch (Exception e) {
                    System.err.println("  Error resolving " + ga + ": " + e.getMessage());
                }
            }
            
            // Save config
            ConfigParser.save(config, configFile);
            System.out.println("\nAdded " + depsToAdd.size() + " dependencies to jpm.toml");
            System.out.println("Total resolved: " + totalResolved + " artifacts");
            
            // Ensure .project file exists
            File projectFile = new File(".project");
            if (!projectFile.exists()) {
                System.out.println("\nCreating .project file...");
                String projectXml = generateProjectFile(config.getPackage().getName());
                FileUtils.writeFile(projectFile, projectXml);
            }
            
            // Sync IDE configuration once at the end
            System.out.println("\nSyncing IDE configuration...");
            ClasspathGenerator generator = new ClasspathGenerator();
            generator.generateClasspath(config, new File("."));
            System.out.println("Generated .classpath file");
            
            return 0;
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            return 1;
        }
    }
    
    private DependencyInfo parseDependency(String input) throws IOException {
        String[] parts = input.split(":");
        
        if (parts.length == 3) {
            // Full coordinates: group:artifact:version
            return new DependencyInfo(parts[0], parts[1], parts[2]);
        } else if (parts.length == 2) {
            // Could be group:artifact or artifact:version
            if (parts[0].contains(".")) {
                // group:artifact - need to find version
                String version = searchClient.getLatestStableVersion(parts[0], parts[1]);
                if (version == null) {
                    System.err.println("Error: Could not find latest stable version for " + parts[0] + ":" + parts[1]);
                    return null;
                }
                return confirmAndCreate(parts[0], parts[1], version);
            } else {
                // artifact:version - need to find groupId
                return searchAndSelect(parts[0], parts[1]);
            }
        } else if (parts.length == 1) {
            // Just artifact name - search
            if (noSearch) {
                System.err.println("Error: Exact coordinates required. Use --no-search=false to enable search");
                return null;
            }
            return searchAndSelect(parts[0], null);
        }
        
        System.err.println("Error: Invalid format: " + input);
        System.err.println("Expected: group:artifact:version, group:artifact, artifact:version, or artifact");
        return null;
    }
    
    private DependencyInfo searchAndSelect(String artifactId, String explicitVersion) throws IOException {
        // Search Maven Central
        List<MavenSearchClient.SearchResult> results = searchClient.searchByArtifactId(artifactId, 10);
        
        if (results.isEmpty()) {
            System.out.println("No packages found matching \"" + artifactId + "\"");
            return null;
        }
        
        // Show results
        System.out.println("Found " + results.size() + " results:");
        int displayCount = Math.min(results.size(), 10);
        for (int i = 0; i < displayCount; i++) {
            System.out.println("  " + (i + 1) + ". " + results.get(i));
        }
        
        // Single exact match shortcut
        if (results.size() == 1) {
            MavenSearchClient.SearchResult result = results.get(0);
            String version = explicitVersion != null ? explicitVersion : 
                            searchClient.getLatestStableVersion(result.groupId, result.artifactId);
            return confirmAndCreate(result.groupId, result.artifactId, version);
        }
        
        // Get user selection
        int selection = promptForSelection(displayCount);
        if (selection < 0) {
            System.out.println("Cancelled");
            return null;
        }
        
        MavenSearchClient.SearchResult selected = results.get(selection);
        String version = explicitVersion != null ? explicitVersion : 
                        searchClient.getLatestStableVersion(selected.groupId, selected.artifactId);
        
        return confirmAndCreate(selected.groupId, selected.artifactId, version);
    }
    
    private DependencyInfo confirmAndCreate(String groupId, String artifactId, String version) throws IOException {
        String ga = groupId + ":" + artifactId;
        
        // Check if already exists in config
        File configFile = new File("jpm.toml");
        if (configFile.exists()) {
            try {
                JpmConfig config = ConfigParser.loadOrCreate(configFile);
                if (config.getDependencies().containsKey(ga)) {
                    String existingVersion = config.getDependencies().get(ga);
                    if (existingVersion.equals(version)) {
                        System.out.println("Note: " + ga + " already at version " + version);
                        return new DependencyInfo(groupId, artifactId, version);
                    } else {
                        System.out.println("Update: " + existingVersion + " -> " + version);
                    }
                }
            } catch (Exception e) {
                // Ignore, will be checked again later
            }
        }
        
        // Show what will be added
        System.out.println("Found: " + ga + ":" + version);
        
        // Confirm
        if (!autoConfirm) {
            System.out.print("Add? [Y/n]: ");
            String response = readLine();
            if (response != null && !response.trim().isEmpty() && 
                !response.trim().toLowerCase().startsWith("y")) {
                System.out.println("Skipped");
                return null;
            }
        }
        
        return new DependencyInfo(groupId, artifactId, version);
    }
    
    private int promptForSelection(int max) {
        System.out.print("Select [1-" + max + "] or press Enter to cancel: ");
        String input = readLine();
        
        if (input == null || input.trim().isEmpty()) {
            return -1;
        }
        
        try {
            int choice = Integer.parseInt(input.trim());
            if (choice >= 1 && choice <= max) {
                return choice - 1; // Convert to 0-indexed
            }
        } catch (NumberFormatException e) {
            // Fall through to error
        }
        
        System.out.println("Invalid selection");
        return promptForSelection(max); // Recurse for retry
    }
    
    private String readLine() {
        try {
            return reader.readLine();
        } catch (IOException e) {
            return null;
        }
    }
    
    private String generateProjectFile(String projectName) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
               "<projectDescription>\n" +
               "\t<name>" + projectName + "</name>\n" +
               "\t<comment></comment>\n" +
               "\t<projects>\n" +
               "\t</projects>\n" +
               "\t<buildSpec>\n" +
               "\t\t<buildCommand>\n" +
               "\t\t\t<name>org.eclipse.jdt.core.javabuilder</name>\n" +
               "\t\t\t<arguments>\n" +
               "\t\t\t</arguments>\n" +
               "\t\t</buildCommand>\n" +
               "\t</buildSpec>\n" +
               "\t<natures>\n" +
               "\t\t<nature>org.eclipse.jdt.core.javanature</nature>\n" +
               "\t</natures>\n" +
               "</projectDescription>\n";
    }
    
    private static class DependencyInfo {
        final String groupId;
        final String artifactId;
        final String version;
        
        DependencyInfo(String groupId, String artifactId, String version) {
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.version = version;
        }
    }
}
