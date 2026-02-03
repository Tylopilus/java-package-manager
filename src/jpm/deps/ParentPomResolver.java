package jpm.deps;

import jpm.utils.FileUtils;
import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Resolves parent POM chains using immutable PomInfo records.
 * Uses Java 21 pattern matching and virtual thread-friendly design.
 */
public class ParentPomResolver {
    
    private static final int MAX_PARENT_DEPTH = 10;
    
    private final MavenClient mavenClient;
    private final DocumentBuilder docBuilder;
    private int parentPomsDownloaded = 0;
    private long downloadStartTime = 0;
    
    public ParentPomResolver(MavenClient mavenClient) throws Exception {
        this.mavenClient = mavenClient;
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        factory.setValidating(false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        this.docBuilder = factory.newDocumentBuilder();
    }
    
    /**
     * Resolves the parent POM chain for a given artifact.
     * Uses pattern matching for switch expressions where applicable.
     * 
     * @param groupId the group ID
     * @param artifactId the artifact ID  
     * @param version the version
     * @param depth current recursion depth
     * @param visited set of visited coordinates to detect cycles
     * @return the resolved PomInfo with parent chain
     * @throws IOException if download fails
     */
    public PomInfo resolveParentChain(String groupId, String artifactId, String version, 
                                      int depth, Set<String> visited) throws IOException {
        // Check depth limit
        if (depth > MAX_PARENT_DEPTH) {
            System.err.println("  Warning: Parent POM chain exceeds maximum depth (" + MAX_PARENT_DEPTH + 
                ") for " + groupId + ":" + artifactId + ":" + version);
            return null;
        }
        
        // Check for cycles
        String key = groupId + ":" + artifactId + ":" + version;
        if (visited.contains(key)) {
            System.err.println("  Warning: Cyclic parent POM reference detected for " + key);
            return null;
        }
        visited.add(key);
        
        // Start timing on first call
        if (depth == 0) {
            downloadStartTime = System.currentTimeMillis();
            parentPomsDownloaded = 0;
        }
        
        // Download and parse the POM
        String pomContent = downloadParentPom(groupId, artifactId, version);
        if (pomContent == null) {
            System.err.println("  Warning: Failed to download parent POM: " + key);
            return null;
        }
        
        parentPomsDownloaded++;
        
        try {
            PomInfo pomInfo = parsePom(pomContent, groupId, artifactId, version);
            
            // Check if this POM has a parent and resolve recursively
            if (pomInfo.parent() != null) {
                PomInfo parentInfo = pomInfo.parent();
                PomInfo resolvedParent = resolveParentChain(
                    parentInfo.groupId(), 
                    parentInfo.artifactId(), 
                    parentInfo.version(), 
                    depth + 1, 
                    visited
                );
                // Create new PomInfo with resolved parent using record's withParent
                pomInfo = pomInfo.withParent(resolvedParent);
            }
            
            // Print timing on root call
            if (depth == 0 && parentPomsDownloaded > 0) {
                long elapsed = System.currentTimeMillis() - downloadStartTime;
                System.out.println("  Downloaded " + parentPomsDownloaded + " parent POMs in " + elapsed + "ms");
            }
            
            return pomInfo;
            
        } catch (Exception e) {
            System.err.println("  Warning: Failed to parse parent POM " + key + ": " + e.getMessage());
            return null;
        }
    }
    
    private String downloadParentPom(String groupId, String artifactId, String version) throws IOException {
        // First check local cache
        File cacheDir = FileUtils.getDependencyDir(groupId, artifactId, version);
        File cachedPom = new File(cacheDir, artifactId + "-" + version + ".pom");
        
        if (cachedPom.exists()) {
            try {
                return FileUtils.readFile(cachedPom);
            } catch (IOException e) {
                // Fall through to download
            }
        }
        
        // Download from Maven Central
        String pomContent = mavenClient.downloadPom(groupId, artifactId, version);
        
        if (pomContent != null) {
            // Cache locally
            try {
                FileUtils.ensureDirectory(cacheDir);
                FileUtils.writeFile(cachedPom, pomContent);
            } catch (IOException e) {
                // Non-fatal - just don't cache
            }
        }
        
        return pomContent;
    }
    
    private PomInfo parsePom(String pomContent, String groupId, String artifactId, String version) throws Exception {
        // Start with empty PomInfo and build up using immutable operations
        PomInfo pomInfo = new PomInfo(groupId, artifactId, version, new HashMap<>(), new HashMap<>(), null);
        
        InputStream is = new ByteArrayInputStream(pomContent.getBytes(StandardCharsets.UTF_8));
        Document doc = docBuilder.parse(is);
        
        // Extract properties using immutable updates
        pomInfo = extractProperties(doc, pomInfo);
        
        // Extract dependency management versions
        pomInfo = extractDependencyManagement(doc, pomInfo);
        
        // Extract parent info
        PomInfo parentInfo = extractParentInfo(doc);
        if (parentInfo != null) {
            pomInfo = pomInfo.withParent(parentInfo);
        }
        
        return pomInfo;
    }
    
    private PomInfo extractProperties(Document doc, PomInfo pomInfo) {
        NodeList propNodes = doc.getElementsByTagName("properties");
        if (propNodes.getLength() > 0) {
            Element propsElement = (Element) propNodes.item(0);
            NodeList children = propsElement.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node node = children.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    String name = node.getNodeName();
                    String value = node.getTextContent();
                    if (value != null) {
                        String trimmedValue = value.trim();
                        // Use immutable withProperty to add each property
                        pomInfo = pomInfo.withProperty("${" + name + "}", trimmedValue)
                                        .withProperty(name, trimmedValue);
                    }
                }
            }
        }
        return pomInfo;
    }
    
    private PomInfo extractDependencyManagement(Document doc, PomInfo pomInfo) {
        NodeList dmNodes = doc.getElementsByTagName("dependencyManagement");
        if (dmNodes.getLength() > 0) {
            Element dmElement = (Element) dmNodes.item(0);
            NodeList depNodes = dmElement.getElementsByTagName("dependency");
            
            for (int i = 0; i < depNodes.getLength(); i++) {
                Element dep = (Element) depNodes.item(i);
                String depGroupId = getTextContent(dep, "groupId");
                String depArtifactId = getTextContent(dep, "artifactId");
                String depVersion = getTextContent(dep, "version");
                
                if (depGroupId != null && depArtifactId != null && depVersion != null) {
                    String key = depGroupId + ":" + depArtifactId;
                    // Use immutable withManagedVersion
                    pomInfo = pomInfo.withManagedVersion(key, depVersion.trim());
                }
            }
        }
        return pomInfo;
    }
    
    private PomInfo extractParentInfo(Document doc) {
        NodeList parentNodes = doc.getElementsByTagName("parent");
        if (parentNodes.getLength() > 0) {
            Element parentElement = (Element) parentNodes.item(0);
            
            // Only consider direct <project> children, not <dependencyManagement> parents
            if (isDirectProjectChild(parentElement)) {
                String parentGroupId = getTextContent(parentElement, "groupId");
                String parentArtifactId = getTextContent(parentElement, "artifactId");
                String parentVersion = getTextContent(parentElement, "version");
                
                if (parentGroupId != null && parentArtifactId != null && parentVersion != null) {
                    // Create parent PomInfo using record constructor
                    return new PomInfo(
                        parentGroupId.trim(),
                        parentArtifactId.trim(),
                        parentVersion.trim(),
                        new HashMap<>(),
                        new HashMap<>(),
                        null
                    );
                }
            }
        }
        return null;
    }
    
    private boolean isDirectProjectChild(Element element) {
        Node parent = element.getParentNode();
        if (parent != null && parent.getNodeType() == Node.ELEMENT_NODE) {
            String parentName = parent.getNodeName();
            return "project".equals(parentName);
        }
        return false;
    }
    
    private String getTextContent(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() > 0) {
            return nodes.item(0).getTextContent();
        }
        return null;
    }
}
