package jpm.deps;

import jpm.utils.FileUtils;
import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

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
            
            // Check if this POM has a parent
            if (pomInfo.getParent() != null) {
                PomInfo parentInfo = pomInfo.getParent();
                PomInfo resolvedParent = resolveParentChain(
                    parentInfo.getGroupId(), 
                    parentInfo.getArtifactId(), 
                    parentInfo.getVersion(), 
                    depth + 1, 
                    visited
                );
                pomInfo.setParent(resolvedParent);
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
        PomInfo pomInfo = new PomInfo();
        pomInfo.setGroupId(groupId);
        pomInfo.setArtifactId(artifactId);
        pomInfo.setVersion(version);
        
        InputStream is = new ByteArrayInputStream(pomContent.getBytes(StandardCharsets.UTF_8));
        Document doc = docBuilder.parse(is);
        
        // Extract properties
        extractProperties(doc, pomInfo);
        
        // Extract dependency management versions
        extractDependencyManagement(doc, pomInfo);
        
        // Extract parent info
        PomInfo parentInfo = extractParentInfo(doc);
        if (parentInfo != null) {
            pomInfo.setParent(parentInfo);
        }
        
        return pomInfo;
    }
    
    private void extractProperties(Document doc, PomInfo pomInfo) {
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
                        pomInfo.addProperty("${" + name + "}", value.trim());
                        pomInfo.addProperty(name, value.trim());
                    }
                }
            }
        }
    }
    
    private void extractDependencyManagement(Document doc, PomInfo pomInfo) {
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
                    pomInfo.addManagedVersion(key, depVersion.trim());
                }
            }
        }
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
                    PomInfo parentInfo = new PomInfo();
                    parentInfo.setGroupId(parentGroupId.trim());
                    parentInfo.setArtifactId(parentArtifactId.trim());
                    parentInfo.setVersion(parentVersion.trim());
                    return parentInfo;
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
