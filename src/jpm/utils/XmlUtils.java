package jpm.utils;

/**
 * Utility class for XML generation and escaping.
 * Provides centralized XML handling for Eclipse project files and JUnit reports.
 */
public final class XmlUtils {

  private XmlUtils() {
    // Utility class - prevent instantiation
  }

  /**
   * Escapes special XML characters in a string.
   *
   * @param text the text to escape
   * @return escaped XML-safe string
   */
  public static String escape(String text) {
    if (text == null) {
      return "";
    }
    return text.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;");
  }

  /**
   * Generates Eclipse .project file content.
   *
   * @param projectName the project name
   * @return XML content for .project file
   */
  public static String generateProjectFile(String projectName) {
    var name = escape(projectName);
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
        """.formatted(name);
  }

  /**
   * Generates Eclipse .classpath file content with dependencies.
   *
   * @param javaVersion the Java version (e.g., "25")
   * @param dependencyPaths list of dependency JAR paths
   * @return XML content for .classpath file
   */
  public static String generateClasspathFile(
      String javaVersion, java.util.List<String> dependencyPaths) {
    var xml = new StringBuilder();
    xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
    xml.append("<classpath>\n");

    // Source directory
    xml.append("\t<classpathentry kind=\"src\" path=\"src\"/>\n");

    // Output directory
    xml.append("\t<classpathentry kind=\"output\" path=\"target/classes\"/>\n");

    // JRE container
    if (javaVersion != null && !javaVersion.isEmpty()) {
      xml.append(
              "\t<classpathentry kind=\"con\""
                  + " path=\"org.eclipse.jdt.launching.JRE_CONTAINER/org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType/JavaSE-")
          .append(javaVersion)
          .append("\"/>\n");
    } else {
      xml.append(
          "\t<classpathentry kind=\"con\" path=\"org.eclipse.jdt.launching.JRE_CONTAINER\"/>\n");
    }

    // Dependencies
    for (var path : dependencyPaths) {
      if (path != null && !path.isEmpty()) {
        xml.append("\t<classpathentry kind=\"lib\" path=\"")
            .append(escape(path))
            .append("\"/>\n");
      }
    }

    xml.append("</classpath>\n");
    return xml.toString();
  }

  /**
   * Generates JUnit XML report content.
   *
   * @param suiteName the test suite name
   * @param total total number of tests
   * @param failed number of failed tests
   * @param skipped number of skipped tests
   * @param testResults list of test results
   * @return XML content for JUnit report
   */
  public static String generateJUnitReport(
      String suiteName,
      int total,
      int failed,
      int skipped,
      java.util.List<TestResult> testResults) {

    var xml = new StringBuilder();
    xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
    xml.append("<testsuites>\n");
    xml.append(String.format(
        "  <testsuite name=\"%s\" tests=\"%d\" failures=\"%d\" skipped=\"%d\" time=\"0.0\">\n",
        escape(suiteName), total, failed, skipped));

    for (var result : testResults) {
      var parts = result.name().split("#");
      var className = parts[0];
      var methodName = parts.length > 1 ? parts[1] : "test";

      xml.append(String.format(
          "    <testcase classname=\"%s\" name=\"%s\" time=\"%.3f\">\n",
          className, methodName, result.time() / 1000.0));

      if ("FAILED".equals(result.status())) {
        var errorMsg = result.errorMessage() != null ? result.errorMessage() : "Test failed";
        xml.append(String.format("      <failure message=\"%s\"/>\n", escape(errorMsg)));
      } else if ("SKIPPED".equals(result.status())) {
        xml.append("      <skipped/>\n");
      }

      xml.append("    </testcase>\n");
    }

    xml.append("  </testsuite>\n");
    xml.append("</testsuites>\n");

    return xml.toString();
  }

  /**
   * Record representing a single test result for JUnit report generation.
   */
  public record TestResult(String name, String status, long time, String errorMessage) {
    public TestResult(String name, String status, long time) {
      this(name, status, time, null);
    }
  }
}
