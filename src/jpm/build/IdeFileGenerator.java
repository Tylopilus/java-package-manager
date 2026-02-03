package jpm.build;

import jpm.config.JpmConfig;
import jpm.deps.DependencyResolver;
import jpm.utils.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class IdeFileGenerator {

    public static boolean shouldGenerateIdeFiles(File projectDir) {
        File projectFile = new File(projectDir, ".project");
        File classpathFile = new File(projectDir, ".classpath");
        return !projectFile.exists() || !classpathFile.exists();
    }

    public static void generateIdeFilesIfMissing(File projectDir, JpmConfig config, String classpath) throws IOException {
        boolean projectMissing = !new File(projectDir, ".project").exists();
        boolean classpathMissing = !new File(projectDir, ".classpath").exists();

        if (projectMissing) {
            generateProjectFile(projectDir, config);
        }

        if (classpathMissing) {
            generateClasspathFile(projectDir, config, classpath);
        }

        if (projectMissing || classpathMissing) {
            System.out.println("Generated IDE configuration files (.project, .classpath)");
        }
    }

    public static void generateProjectFile(File projectDir, JpmConfig config) throws IOException {
        File projectFile = new File(projectDir, ".project");

        String projectName = config.getPackage().getName();
        if (projectName == null || projectName.isEmpty()) {
            projectName = projectDir.getName();
        }

        String projectXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<projectDescription>\n" +
                "\t<name>" + escapeXml(projectName) + "</name>\n" +
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

        FileUtils.writeFile(projectFile, projectXml);
    }

    public static void generateClasspathFile(File projectDir, JpmConfig config, String classpath) throws IOException {
        File classpathFile = new File(projectDir, ".classpath");

        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<classpath>\n");

        // Source directory
        xml.append("\t<classpathentry kind=\"src\" path=\"src\"/>\n");

        // Output directory
        xml.append("\t<classpathentry kind=\"output\" path=\"target/classes\"/>\n");

        // JRE container
        String javaVersion = config.getPackage().getJavaVersion();
        if (javaVersion != null && !javaVersion.isEmpty()) {
            xml.append("\t<classpathentry kind=\"con\" path=\"org.eclipse.jdt.launching.JRE_CONTAINER/org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType/JavaSE-").append(javaVersion).append("\"/>\n");
        } else {
            xml.append("\t<classpathentry kind=\"con\" path=\"org.eclipse.jdt.launching.JRE_CONTAINER\"/>\n");
        }

        // Add dependencies from classpath
        if (classpath != null && !classpath.isEmpty()) {
            String[] entries = classpath.split(File.pathSeparator);
            for (String entry : entries) {
                if (!entry.isEmpty()) {
                    File jarFile = new File(entry);
                    if (jarFile.exists()) {
                        xml.append("\t<classpathentry kind=\"lib\" path=\"").append(escapeXml(jarFile.getAbsolutePath())).append("\"/>\n");
                    }
                }
            }
        }

        xml.append("</classpath>\n");

        FileUtils.writeFile(classpathFile, xml.toString());
    }

    public static void generateClasspathFileWithDeps(File projectDir, JpmConfig config) throws IOException {
        // Use ClasspathGenerator for full dependency resolution
        ClasspathGenerator generator = new ClasspathGenerator();
        generator.generateClasspath(config, projectDir);
    }

    private static String escapeXml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
