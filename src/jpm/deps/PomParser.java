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
            // Include only compile scope (or null/empty scope which defaults to compile)
            // Exclude: test, provided, runtime (runtime is technically needed at runtime but not compile)
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
    
    public PomParser() throws Exception {
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
        List<Dependency> deps = new ArrayList<>();
        
        if (pomContent == null || pomContent.trim().isEmpty()) {
            return deps;
        }
        
        InputStream is = new ByteArrayInputStream(pomContent.getBytes());
        Document doc = docBuilder.parse(is);
        
        // Extract properties for version substitution
        Map<String, String> properties = extractProperties(doc);
        
        // Extract dependency management versions
        Map<String, String> managedVersions = extractDependencyManagement(doc);
        
        // Find dependencies
        NodeList depNodes = doc.getElementsByTagName("dependency");
        for (int i = 0; i < depNodes.getLength(); i++) {
            Element depElement = (Element) depNodes.item(i);
            
            // Skip if this is inside dependencyManagement section (we only want actual dependencies)
            if (isInDependencyManagement(depElement)) {
                continue;
            }
            
            String groupId = getTextContent(depElement, "groupId");
            String artifactId = getTextContent(depElement, "artifactId");
            String version = getTextContent(depElement, "version");
            String scope = getTextContent(depElement, "scope");
            String optional = getTextContent(depElement, "optional");
            
            // Substitute properties
            if (version != null) {
                version = substituteProperties(version, properties);
            }
            
            // Use managed version if available and no version specified
            if (version == null || version.isEmpty()) {
                String key = groupId + ":" + artifactId;
                version = managedVersions.get(key);
            }
            
            if (groupId != null && artifactId != null) {
                deps.add(new Dependency(
                    groupId, 
                    artifactId, 
                    version, 
                    scope, 
                    "true".equals(optional)
                ));
            }
        }
        
        return deps;
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
                    props.put(name, value);
                    props.put("${" + name + "}", value);
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
                String groupId = getTextContent(dep, "groupId");
                String artifactId = getTextContent(dep, "artifactId");
                String version = getTextContent(dep, "version");
                
                if (groupId != null && artifactId != null && version != null) {
                    managed.put(groupId + ":" + artifactId, version);
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
            return nodes.item(0).getTextContent().trim();
        }
        return null;
    }
    
    private String substituteProperties(String value, Map<String, String> properties) {
        if (value == null) return null;
        
        String result = value;
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }
        return result;
    }
}
