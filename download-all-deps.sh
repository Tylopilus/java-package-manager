#!/bin/bash
# Download all Palantir Java Format 2.86.0 dependencies

set -e

JPM_DIR="$HOME/.jpm"
LIB_DIR="$JPM_DIR/lib"

echo "==> Downloading Palantir Java Format 2.86.0 and dependencies..."

# Core Palantir libraries
PALANTIR_VERSION="2.86.0"

# Main formatter JAR
curl -sL "https://repo1.maven.org/maven2/com/palantir/javaformat/palantir-java-format/${PALANTIR_VERSION}/palantir-java-format-${PALANTIR_VERSION}.jar" \
    -o "$LIB_DIR/palantir-java-format-${PALANTIR_VERSION}.jar"
echo "  -> Downloaded palantir-java-format-${PALANTIR_VERSION}.jar"

# SPI JAR (required for FormatterException)
curl -sL "https://repo1.maven.org/maven2/com/palantir/javaformat/palantir-java-format-spi/${PALANTIR_VERSION}/palantir-java-format-spi-${PALANTIR_VERSION}.jar" \
    -o "$LIB_DIR/palantir-java-format-spi-${PALANTIR_VERSION}.jar"
echo "  -> Downloaded palantir-java-format-spi-${PALANTIR_VERSION}.jar"

# Functional Java (required by Palantir)
curl -sL "https://repo1.maven.org/maven2/org/functionaljava/functionaljava/4.8/functionaljava-4.8.jar" \
    -o "$LIB_DIR/functionaljava-4.8.jar"
echo "  -> Downloaded functionaljava-4.8.jar"

# Guava (updated version for 2.86.0)
curl -sL "https://repo1.maven.org/maven2/com/google/guava/guava/33.5.0-jre/guava-33.5.0-jre.jar" \
    -o "$LIB_DIR/guava-33.5.0-jre.jar"
echo "  -> Downloaded guava-33.5.0-jre.jar"

# Guava failureaccess
curl -sL "https://repo1.maven.org/maven2/com/google/guava/failureaccess/1.0.2/failureaccess-1.0.2.jar" \
    -o "$LIB_DIR/failureaccess-1.0.2.jar"
echo "  -> Downloaded failureaccess-1.0.2.jar"

# Jackson Core
curl -sL "https://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-core/2.18.2/jackson-core-2.18.2.jar" \
    -o "$LIB_DIR/jackson-core-2.18.2.jar"
echo "  -> Downloaded jackson-core-2.18.2.jar"

# Jackson Databind
curl -sL "https://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-databind/2.18.2/jackson-databind-2.18.2.jar" \
    -o "$LIB_DIR/jackson-databind-2.18.2.jar"
echo "  -> Downloaded jackson-databind-2.18.2.jar"

# Jackson Annotations
curl -sL "https://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-annotations/2.18.2/jackson-annotations-2.18.2.jar" \
    -o "$LIB_DIR/jackson-annotations-2.18.2.jar"
echo "  -> Downloaded jackson-annotations-2.18.2.jar"

# Jackson JDK8 Datatype
curl -sL "https://repo1.maven.org/maven2/com/fasterxml/jackson/datatype/jackson-datatype-jdk8/2.18.2/jackson-datatype-jdk8-2.18.2.jar" \
    -o "$LIB_DIR/jackson-datatype-jdk8-2.18.2.jar"
echo "  -> Downloaded jackson-datatype-jdk8-2.18.2.jar"

# Jackson Parameter Names Module
curl -sL "https://repo1.maven.org/maven2/com/fasterxml/jackson/module/jackson-module-parameter-names/2.18.2/jackson-module-parameter-names-2.18.2.jar" \
    -o "$LIB_DIR/jackson-module-parameter-names-2.18.2.jar"
echo "  -> Downloaded jackson-module-parameter-names-2.18.2.jar"

# JSR305 annotations
curl -sL "https://repo1.maven.org/maven2/com/google/code/findbugs/jsr305/3.0.2/jsr305-3.0.2.jar" \
    -o "$LIB_DIR/jsr305-3.0.2.jar"
echo "  -> Downloaded jsr305-3.0.2.jar"

# Error Prone annotations (Guava transitive dep)
curl -sL "https://repo1.maven.org/maven2/com/google/errorprone/error_prone_annotations/2.36.0/error_prone_annotations-2.36.0.jar" \
    -o "$LIB_DIR/error_prone_annotations-2.36.0.jar"
echo "  -> Downloaded error_prone_annotations-2.36.0.jar"

echo ""
echo "==> All dependencies downloaded successfully!"
echo ""
echo "Update rebuild.sh CLASSPATH to include all these JARs:"
echo 'CLASSPATH="$LIB_DIR/picocli-4.7.6.jar:\'
echo '    $LIB_DIR/palantir-java-format-2.86.0.jar:\'
echo '    $LIB_DIR/palantir-java-format-spi-2.86.0.jar:\'
echo '    $LIB_DIR/functionaljava-4.8.jar:\'
echo '    $LIB_DIR/guava-33.5.0-jre.jar:\'
echo '    $LIB_DIR/failureaccess-1.0.2.jar:\'
echo '    $LIB_DIR/jackson-core-2.18.2.jar:\'
echo '    $LIB_DIR/jackson-databind-2.18.2.jar:\'
echo '    $LIB_DIR/jackson-annotations-2.18.2.jar:\'
echo '    $LIB_DIR/jackson-datatype-jdk8-2.18.2.jar:\'
echo '    $LIB_DIR/jackson-module-parameter-names-2.18.2.jar:\'
echo '    $LIB_DIR/jsr305-3.0.2.jar:\'
echo '    $LIB_DIR/error_prone_annotations-2.36.0.jar:\'
echo '    $LIB_DIR/toml4j-0.7.2.jar:\'
echo '    $LIB_DIR/gson-2.10.1.jar"'
