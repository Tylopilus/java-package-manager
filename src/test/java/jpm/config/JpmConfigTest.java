package jpm.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Unit tests for JpmConfig record.
 * Tests immutability, construction, and business methods.
 */
class JpmConfigTest {
    
    @Test
    @DisplayName("Should create config with default values")
    void shouldCreateWithDefaults() {
        var config = new JpmConfig();
        
        assertNotNull(config.package_());
        assertNull(config.package_().name());
        assertNull(config.package_().version());
        assertNull(config.package_().javaVersion());
        
        assertNotNull(config.dependencies());
        assertTrue(config.dependencies().isEmpty());
    }
    
    @Test
    @DisplayName("Should create config with custom values")
    void shouldCreateWithCustomValues() {
        var pkg = new JpmConfig.PackageConfig("my-app", "1.0.0", "21");
        var deps = Map.of("com.example:lib", "1.0.0");
        
        var config = new JpmConfig(pkg, new HashMap<>(deps), new HashMap<>());
        
        assertEquals("my-app", config.package_().name());
        assertEquals("1.0.0", config.package_().version());
        assertEquals("21", config.package_().javaVersion());
        assertEquals("1.0.0", config.dependencies().get("com.example:lib"));
    }
    
    @Test
    @DisplayName("Should add dependency")
    void shouldAddDependency() {
        var config = new JpmConfig();
        
        config.addDependency("com.example:lib", "1.0.0");
        
        assertEquals("1.0.0", config.dependencies().get("com.example:lib"));
        assertEquals(1, config.dependencies().size());
    }
    
    @Test
    @DisplayName("Should remove dependency by artifact ID")
    void shouldRemoveDependency() {
        var config = new JpmConfig();
        config.addDependency("com.example:lib1", "1.0.0");
        config.addDependency("com.other:lib2", "2.0.0");
        
        config.removeDependency("lib1");
        
        assertFalse(config.dependencies().containsKey("com.example:lib1"));
        assertTrue(config.dependencies().containsKey("com.other:lib2"));
    }
    
    @Test
    @DisplayName("Should handle remove when artifact not found")
    void shouldHandleRemoveWhenNotFound() {
        var config = new JpmConfig();
        config.addDependency("com.example:lib", "1.0.0");
        
        config.removeDependency("nonexistent");
        
        assertEquals(1, config.dependencies().size());
    }
    
    @Test
    @DisplayName("Should create new config with updated package")
    void shouldCreateWithNewPackage() {
        var original = new JpmConfig();
        original.addDependency("com.example:lib", "1.0.0");
        
        var newPkg = new JpmConfig.PackageConfig("new-app", "2.0.0", "21");
        var updated = original.withPackage(newPkg);
        
        // Original unchanged
        assertNull(original.package_().name());
        
        // New config has updated package and copied dependencies
        assertEquals("new-app", updated.package_().name());
        assertEquals("1.0.0", updated.dependencies().get("com.example:lib"));
    }
    
    @Test
    @DisplayName("Should create new config with updated dependencies")
    void shouldCreateWithNewDependencies() {
        var original = new JpmConfig();
        original.addDependency("com.example:lib", "1.0.0");
        
        var newDeps = Map.of("com.other:lib2", "2.0.0");
        var updated = original.withDependencies(newDeps);
        
        // Original unchanged
        assertTrue(original.dependencies().containsKey("com.example:lib"));
        
        // New config has new dependencies and same package
        assertFalse(updated.dependencies().containsKey("com.example:lib"));
        assertTrue(updated.dependencies().containsKey("com.other:lib2"));
    }
    
    @Test
    @DisplayName("Should maintain independence between original and copy")
    void shouldMaintainIndependence() {
        var config1 = new JpmConfig();
        config1.addDependency("com.example:lib", "1.0.0");
        
        var config2 = config1.withPackage(new JpmConfig.PackageConfig("app2", "1.0.0", "21"));
        
        // Modify config2's dependencies
        config2.addDependency("com.other:lib", "2.0.0");
        
        // config1 should be unaffected
        assertFalse(config1.dependencies().containsKey("com.other:lib"));
    }
}
