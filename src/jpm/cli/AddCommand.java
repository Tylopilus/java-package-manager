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
            var configFile = new File("jpm.toml");
            if (!configFile.exists()) {
                System.err.println("Error: No jpm.toml found. Run 'jpm new <name>' first.");
                return 1;
            }
            
            var successCount = 0;
            var total = dependencies.size();
            
            // Track all dependencies to add
            var depsToAdd = new ArrayList<DependencyInfo>();
            
            // Phase 1: Collect all dependencies (with interactive selection)
            for (int i = 0; i < total; i++) {
                var dep = dependencies.get(i);
                
                if (total > 1) {
                    System.out.println("\n[" + (i + 1) + "/" + total + "] Processing \"" + dep + "\"...");
                }
                
                var info = parseDependency(dep);
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
            
            var config = ConfigParser.loadOrCreate(configFile);
            var resolver = new DependencyResolver();
            var totalResolved = 0;
            
            for (var info : depsToAdd) {
                var ga = info.groupId + ":" + info.artifactId;
                
                try {
                    // Resolve the dependency
                    var resolved = resolver.resolve(info.groupId, info.artifactId, info.version);
                    
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
            var projectFile = new File(".project");
            if (!projectFile.exists()) {
                System.out.println("\nCreating .project file...");
                var projectXml = generateProjectFile(config.package_().name());
                FileUtils.writeFile(projectFile, projectXml);
            }
            
            // Sync IDE configuration once at the end
            System.out.println("\nSyncing IDE configuration...");
            var generator = new ClasspathGenerator();
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
        var parts = input.split(":");
        
        // Pattern matching switch for dependency coordinate parsing
        return switch (parts.length) {
            case 3 -> new DependencyInfo(parts[0], parts[1], parts[2]);
            case 2 -> {
                // Could be group:artifact or artifact:version
                if (parts[0].contains(".")) {
                    // group:artifact format - need to find version
                    var version = searchClient.getLatestStableVersion(parts[0], parts[1]);
                    if (version == null) {
                        System.err.println("Error: Could not find version for " + parts[0] + ":" + parts[1]);
                        yield null;
                    }
                    yield confirmAndCreate(parts[0], parts[1], version);
                } else {
                    // artifact:version format - need to find groupId
                    yield searchAndSelect(parts[0], parts[1]);
                }
            }
            case 1 -> {
                if (noSearch) {
                    System.err.println("Error: Exact coordinates required. Use --no-search=false to enable search");
                    yield null;
                }
                yield searchAndSelect(parts[0], null);
            }
            default -> {
                System.err.println("Error: Invalid format: " + input);
                System.err.println("Expected: group:artifact:version, group:artifact, artifact:version, or artifact");
                yield null;
            }
        };
    }
    
    private DependencyInfo searchAndSelect(String artifactId, String explicitVersion) throws IOException {
        // Search Maven Central
        var results = searchClient.searchByArtifactId(artifactId, 10);
        
        if (results.isEmpty()) {
            System.out.println("No packages found matching \"" + artifactId + "\"");
            return null;
        }
        
        // Show results
        System.out.println("Found " + results.size() + " results:");
        var displayCount = Math.min(results.size(), 10);
        for (int i = 0; i < displayCount; i++) {
            System.out.println("  " + (i + 1) + ". " + results.get(i));
        }
        
        // Single exact match shortcut
        if (results.size() == 1) {
            var result = results.get(0);
            var version = explicitVersion != null ? explicitVersion : 
                            searchClient.getLatestStableVersion(result.groupId(), result.artifactId());
            return confirmAndCreate(result.groupId(), result.artifactId(), version);
        }
        
        // Get user selection
        var selection = promptForSelection(displayCount);
        if (selection < 0) {
            System.out.println("Cancelled");
            return null;
        }
        
        var selected = results.get(selection);
        var version = explicitVersion != null ? explicitVersion : 
                        searchClient.getLatestStableVersion(selected.groupId(), selected.artifactId());
        
        return confirmAndCreate(selected.groupId(), selected.artifactId(), version);
    }
    
    private DependencyInfo confirmAndCreate(String groupId, String artifactId, String version) throws IOException {
        var ga = groupId + ":" + artifactId;
        
        // Check if already exists in config
        var configFile = new File("jpm.toml");
        if (configFile.exists()) {
            try {
                var config = ConfigParser.loadOrCreate(configFile);
                if (config.dependencies().containsKey(ga)) {
                    var existingVersion = config.dependencies().get(ga);
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
            if (response != null && !response.isBlank() && 
                !response.trim().toLowerCase().startsWith("y")) {
                System.out.println("Skipped");
                return null;
            }
        }
        
        return new DependencyInfo(groupId, artifactId, version);
    }
    
    private int promptForSelection(int max) {
        System.out.print("Select [1-" + max + "] or press Enter to cancel: ");
        var input = readLine();
        
        if (input == null || input.isBlank()) {
            return -1;
        }
        
        try {
            var choice = Integer.parseInt(input.trim());
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
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <projectDescription>
            	<name>%s</name>
            	<comment></comment>
            	<projects>
            	</projects>
            	<buildSpec>
            		<buildCommand>
            			<name>org.eclipse.jdt.core.javabuilder</name>
            			<arguments>
            			</arguments>
            		</buildCommand>
            	</buildSpec>
            	<natures>
            		<nature>org.eclipse.jdt.core.javanature</nature>
            	</natures>
            </projectDescription>
            """.formatted(projectName);
    }
    
    // Java 16+ record for dependency information
    private record DependencyInfo(String groupId, String artifactId, String version) {
    }
}
