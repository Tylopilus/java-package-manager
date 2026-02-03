package jpm.deps;

import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.io.*;
import java.util.*;

public class PomParser {
    
    public static class Dependency {
        public final String groupId;
        public final String artifactId;
        public final String version;
        public final String scope;
        public final boolean optional;
        
        public Dependency(String groupId, String artifactId, String version, String scope, boolean optional) {
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.version = version;
            this.scope = scope;
            this.optional = optional;
        }
        
        public boolean shouldInclude() {
            // Include compile and runtime scope (or null/empty scope which defaults to compile)
            // Exclude: test, provided
            if ("test".equals(scope) || "provided".equals(scope)) {
                return false;
            }
            if (optional) {
                return false;
            }
            return true;
        }
    }
    
    private final DocumentBuilder docBuilder;
    private final ParentPomResolver parentResolver;
    
    public PomParser() throws Exception {
        this(null);
    }
    
    public PomParser(ParentPomResolver parentResolver) throws Exception {
        this.parentResolver = parentResolver;
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        factory.setValidating(false);
        // Disable DTD loading for security
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        this.docBuilder = factory.newDocumentBuilder();
    }
    
    public List<Dependency> parseDependencies(String pomContent) throws Exception {
        return parseDependencies(pomContent, null, null, null);
    }
    
    public List<Dependency> parseDependencies(String pomContent, String groupId, String artifactId, String version) throws Exception {
        List<Dependency> deps = new ArrayList<>();
        
        if (pomContent == null || pomContent.trim().isEmpty()) {
            return deps;
        }
        
        InputStream is = new ByteArrayInputStream(pomContent.getBytes());
        Document doc = docBuilder.parse(is);
        
        // Get current POM coordinates (may be passed in or parsed from POM)
        if (groupId == null) {
            groupId = getTextContent(doc.getDocumentElement(), "groupId");
        }
        if (artifactId == null) {
            artifactId = getTextContent(doc.getDocumentElement(), "artifactId");
        }
        if (version == null) {
            version = getTextContent(doc.getDocumentElement(), "version");
        }
        
        // Build full property map including parent chain
        Map<String, String> allProperties = buildFullPropertyMap(doc, groupId, artifactId, version);
        Map<String, String> allManagedVersions = buildFullManagedVersionsMap(doc);
        
        // Find dependencies
        NodeList depNodes = doc.getElementsByTagName("dependency");
        for (int i = 0; i < depNodes.getLength(); i++) {
            Element depElement = (Element) depNodes.item(i);
            
            // Skip if this is inside dependencyManagement section (we only want actual dependencies)
            if (isInDependencyManagement(depElement)) {
                continue;
            }
            
            String depGroupId = getTextContent(depElement, "groupId");
            String depArtifactId = getTextContent(depElement, "artifactId");
            String depVersion = getTextContent(depElement, "version");
            String depScope = getTextContent(depElement, "scope");
            String depOptional = getTextContent(depElement, "optional");
            
            // Substitute properties in version
            if (depVersion != null) {
                depVersion = substituteProperties(depVersion, allProperties);
            }
            
            // Use managed version if available and no version specified
            if ((depVersion == null || depVersion.isEmpty()) && depGroupId != null && depArtifactId != null) {
                String key = depGroupId + ":" + depArtifactId;
                depVersion = allManagedVersions.get(key);
                if (depVersion != null) {
                    depVersion = substituteProperties(depVersion, allProperties);
                }
            }
            
            // Substitute properties in groupId and artifactId too (rare but possible)
            if (depGroupId != null) {
                depGroupId = substituteProperties(depGroupId, allProperties);
            }
            if (depArtifactId != null) {
                depArtifactId = substituteProperties(depArtifactId, allProperties);
            }
            
            if (depGroupId != null && depArtifactId != null) {
                deps.add(new Dependency(
                    depGroupId, 
                    depArtifactId, 
                    depVersion, 
                    depScope, 
                    "true".equals(depOptional)
                ));
            }
        }
        
        return deps;
    }
    
    private Map<String, String> buildFullPropertyMap(Document doc, String groupId, String artifactId, String version) {
        Map<String, String> allProps = new HashMap<>();
        
        // Add built-in Maven properties first (lowest priority)
        if (groupId != null) {
            allProps.put("${project.groupId}", groupId);
            allProps.put("${pom.groupId}", groupId);
        }
        if (artifactId != null) {
            allProps.put("${project.artifactId}", artifactId);
            allProps.put("${pom.artifactId}", artifactId);
        }
        if (version != null) {
            allProps.put("${project.version}", version);
            allProps.put("${pom.version}", version);
            allProps.put("${version}", version);
        }
        
        // Extract current POM properties
        Map<String, String> currentProps = extractProperties(doc);
        
        // If we have parent resolver, resolve parent chain and merge
        if (parentResolver != null && groupId != null && artifactId != null && version != null) {
            try {
                PomInfo parentInfo = parentResolver.resolveParentChain(groupId, artifactId, version, 0, new HashSet<>());
                if (parentInfo != null) {
                    Map<String, String> inheritedProps = parentInfo.getAllProperties();
                    // Parent properties override built-ins, current overrides parent
                    allProps.putAll(inheritedProps);
                }
            } catch (IOException e) {
                System.err.println("  Warning: Failed to resolve parent chain for properties: " + e.getMessage());
            }
        }
        
        // Current POM properties have highest priority
        allProps.putAll(currentProps);
        
        return allProps;
    }
    
    private Map<String, String> buildFullManagedVersionsMap(Document doc) {
        Map<String, String> allManaged = new HashMap<>();
        
        // Extract current POM managed versions
        Map<String, String> currentManaged = extractDependencyManagement(doc);
        
        // If we have parent resolver, get inherited managed versions
        if (parentResolver != null) {
            // Try to get parent info from doc
            String groupId = getTextContent(doc.getDocumentElement(), "groupId");
            String artifactId = getTextContent(doc.getDocumentElement(), "artifactId");
            String version = getTextContent(doc.getDocumentElement(), "version");
            
            if (groupId != null && artifactId != null && version != null) {
                try {
                    PomInfo parentInfo = parentResolver.resolveParentChain(groupId, artifactId, version, 0, new HashSet<>());
                    if (parentInfo != null) {
                        Map<String, String> inheritedManaged = parentInfo.getAllManagedVersions();
                        // Parent managed versions are base, current overrides
                        allManaged.putAll(inheritedManaged);
                    }
                } catch (IOException e) {
                    // Non-fatal, just don't inherit managed versions
                }
            }
        }
        
        // Current POM managed versions override inherited
        allManaged.putAll(currentManaged);
        
        return allManaged;
    }
    
    private Map<String, String> extractProperties(Document doc) {
        Map<String, String> props = new HashMap<>();
        
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
                        value = value.trim();
                        props.put(name, value);
                        props.put("${" + name + "}", value);
                    }
                }
            }
        }
        
        return props;
    }
    
    private Map<String, String> extractDependencyManagement(Document doc) {
        Map<String, String> managed = new HashMap<>();
        
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
                    managed.put(depGroupId + ":" + depArtifactId, depVersion.trim());
                }
            }
        }
        
        return managed;
    }
    
    private boolean isInDependencyManagement(Element element) {
        Node parent = element.getParentNode();
        while (parent != null) {
            if (parent.getNodeType() == Node.ELEMENT_NODE) {
                Element parentElement = (Element) parent;
                if ("dependencyManagement".equals(parentElement.getNodeName())) {
                    return true;
                }
                if ("dependencies".equals(parentElement.getNodeName())) {
                    return false;
                }
            }
            parent = parent.getParentNode();
        }
        return false;
    }
    
    private String getTextContent(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() > 0) {
            String content = nodes.item(0).getTextContent();
            return content != null ? content.trim() : null;
        }
        return null;
    }
    
    /**
     * Cached sorted property entries for efficient substitution.
     * Sorts by key length (longest first) to avoid partial substitutions.
     */
    private List<Map.Entry<String, String>> getSortedPropertyEntries(Map<String, String> properties) {
        // Create list and sort by key length descending (longest keys first)
        List<Map.Entry<String, String>> entries = new ArrayList<>(properties.entrySet());
        entries.sort((a, b) -> Integer.compare(b.getKey().length(), a.getKey().length()));
        return entries;
    }
    
    private String substituteProperties(String value, Map<String, String> properties) {
        if (value == null) return null;
        
        String result = value;
        List<Map.Entry<String, String>> entries = getSortedPropertyEntries(properties);
        
        for (Map.Entry<String, String> entry : entries) {
            result = result.replace(entry.getKey(), entry.getValue());
        }
        return result;
    }
}
