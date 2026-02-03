package jpm.deps;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for PomInfo record.
 * Tests immutability, property inheritance, and managed version handling.
 */
class PomInfoTest {

  @Test
  @DisplayName("Should create PomInfo with default values")
  void shouldCreateWithDefaults() {
    var pomInfo = new PomInfo();

    assertNull(pomInfo.groupId());
    assertNull(pomInfo.artifactId());
    assertNull(pomInfo.version());
    assertNotNull(pomInfo.properties());
    assertTrue(pomInfo.properties().isEmpty());
    assertNotNull(pomInfo.managedVersions());
    assertTrue(pomInfo.managedVersions().isEmpty());
    assertNull(pomInfo.parent());
  }

  @Test
  @DisplayName("Should create PomInfo with custom values")
  void shouldCreateWithCustomValues() {
    var properties = Map.of("key", "value");
    var managedVersions = Map.of("group:artifact", "1.0.0");

    var pomInfo = new PomInfo(
        "com.example",
        "my-artifact",
        "1.0.0",
        new HashMap<>(properties),
        new HashMap<>(managedVersions),
        null);

    assertEquals("com.example", pomInfo.groupId());
    assertEquals("my-artifact", pomInfo.artifactId());
    assertEquals("1.0.0", pomInfo.version());
  }

  @Test
  @DisplayName("Should calculate full artifact key")
  void shouldCalculateFullArtifactKey() {
    var pomInfo =
        new PomInfo("com.example", "my-artifact", "1.0.0", new HashMap<>(), new HashMap<>(), null);

    assertEquals("com.example:my-artifact", pomInfo.fullArtifactKey());
  }

  @Test
  @DisplayName("Should collect all properties with built-ins")
  void shouldCollectAllProperties() {
    var pomInfo = new PomInfo(
        "com.example",
        "my-artifact",
        "1.0.0",
        new HashMap<>(Map.of("custom.prop", "custom-value")),
        new HashMap<>(),
        null);

    var allProps = pomInfo.allProperties();

    assertEquals("custom-value", allProps.get("custom.prop"));
    assertEquals("com.example", allProps.get("${project.groupId}"));
    assertEquals("com.example", allProps.get("${pom.groupId}"));
    assertEquals("my-artifact", allProps.get("${project.artifactId}"));
    assertEquals("my-artifact", allProps.get("${pom.artifactId}"));
    assertEquals("1.0.0", allProps.get("${project.version}"));
    assertEquals("1.0.0", allProps.get("${pom.version}"));
    assertEquals("1.0.0", allProps.get("${version}"));
  }

  @Test
  @DisplayName("Should inherit parent properties")
  void shouldInheritParentProperties() {
    var parent = new PomInfo(
        "org.parent",
        "parent-artifact",
        "2.0.0",
        new HashMap<>(Map.of("inherited.prop", "parent-value")),
        new HashMap<>(),
        null);

    var child = new PomInfo(
        "com.child",
        "child-artifact",
        "1.0.0",
        new HashMap<>(Map.of("child.prop", "child-value")),
        new HashMap<>(),
        parent);

    var allProps = child.allProperties();

    // Child properties take precedence
    assertEquals("child-value", allProps.get("child.prop"));
    // Inherited from parent
    assertEquals("parent-value", allProps.get("inherited.prop"));
    // Built-ins are from child
    assertEquals("com.child", allProps.get("${project.groupId}"));
  }

  @Test
  @DisplayName("Should inherit managed versions from parent")
  void shouldInheritManagedVersions() {
    var parent = new PomInfo(
        "org.parent",
        "parent",
        "1.0.0",
        new HashMap<>(),
        new HashMap<>(Map.of("managed:dep", "2.0.0")),
        null);

    var child = new PomInfo(
        "com.child",
        "child",
        "1.0.0",
        new HashMap<>(),
        new HashMap<>(Map.of("managed:dep", "3.0.0")),
        parent);

    var allManaged = child.allManagedVersions();

    // Child version overrides parent
    assertEquals("3.0.0", allManaged.get("managed:dep"));
  }

  @Test
  @DisplayName("Should add property immutably")
  void shouldAddPropertyImmutably() {
    var original = new PomInfo();

    var updated = original.withProperty("new.prop", "new-value");

    // Original unchanged
    assertFalse(original.properties().containsKey("new.prop"));

    // Updated has new property
    assertEquals("new-value", updated.properties().get("new.prop"));
  }

  @Test
  @DisplayName("Should add managed version immutably")
  void shouldAddManagedVersionImmutably() {
    var original = new PomInfo();

    var updated = original.withManagedVersion("group:artifact", "1.0.0");

    // Original unchanged
    assertTrue(original.managedVersions().isEmpty());

    // Updated has new managed version
    assertEquals("1.0.0", updated.managedVersions().get("group:artifact"));
  }

  @Test
  @DisplayName("Should set parent immutably")
  void shouldSetParentImmutably() {
    var child = new PomInfo("com.child", "child", "1.0.0", new HashMap<>(), new HashMap<>(), null);

    var parent =
        new PomInfo("org.parent", "parent", "2.0.0", new HashMap<>(), new HashMap<>(), null);

    var updated = child.withParent(parent);

    // Original unchanged
    assertNull(child.parent());

    // Updated has parent
    assertNotNull(updated.parent());
    assertEquals("org.parent", updated.parent().groupId());
  }

  @Test
  @DisplayName("Should handle deep parent chains")
  void shouldHandleDeepParentChains() {
    var grandparent = new PomInfo(
        "org.grandparent",
        "gp",
        "3.0.0",
        new HashMap<>(Map.of("gp.prop", "gp-value")),
        new HashMap<>(),
        null);

    var parent = new PomInfo(
        "org.parent",
        "parent",
        "2.0.0",
        new HashMap<>(Map.of("parent.prop", "parent-value")),
        new HashMap<>(),
        grandparent);

    var child = new PomInfo(
        "com.child",
        "child",
        "1.0.0",
        new HashMap<>(Map.of("child.prop", "child-value")),
        new HashMap<>(),
        parent);

    var allProps = child.allProperties();

    assertEquals("child-value", allProps.get("child.prop"));
    assertEquals("parent-value", allProps.get("parent.prop"));
    assertEquals("gp-value", allProps.get("gp.prop"));
  }

  @Test
  @DisplayName("Should return unmodifiable property maps")
  void shouldReturnUnmodifiableMaps() {
    var pomInfo =
        new PomInfo("com.example", "artifact", "1.0.0", new HashMap<>(), new HashMap<>(), null);

    var allProps = pomInfo.allProperties();

    assertThrows(UnsupportedOperationException.class, () -> allProps.put("test", "value"));
  }
}
