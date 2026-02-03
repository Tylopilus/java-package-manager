package jpm.build;

import jpm.deps.DependencyResolver;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

public class ClasspathBuilder {
    
    public static String buildClasspath(List<DependencyResolver.ResolvedDependency> dependencies) {
        return dependencies.stream()
            .map(DependencyResolver.ResolvedDependency::getClasspathEntry)
            .collect(Collectors.joining(File.pathSeparator));
    }
    
    public static String buildClasspathFromDir(File libDir) {
        if (!libDir.exists() || !libDir.isDirectory()) {
            return "";
        }
        
        File[] jars = libDir.listFiles((dir, name) -> name.endsWith(".jar"));
        if (jars == null || jars.length == 0) {
            return "";
        }
        
        StringBuilder classpath = new StringBuilder();
        for (int i = 0; i < jars.length; i++) {
            if (i > 0) {
                classpath.append(File.pathSeparator);
            }
            classpath.append(jars[i].getAbsolutePath());
        }
        return classpath.toString();
    }
    
    public static String combineClasspaths(String... classpaths) {
        StringBuilder result = new StringBuilder();
        boolean first = true;
        
        for (String cp : classpaths) {
            if (cp != null && !cp.isEmpty()) {
                if (!first) {
                    result.append(File.pathSeparator);
                }
                result.append(cp);
                first = false;
            }
        }
        
        return result.toString();
    }
}
