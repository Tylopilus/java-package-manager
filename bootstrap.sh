#!/bin/bash
# Bootstrap script for jpm
# This script builds jpm from scratch without jpm existing yet
# Compiles with Java 21+ features targeting Java 21 bytecode

set -e

JPM_DIR="$HOME/.jpm"
CACHE_DIR="$JPM_DIR/cache"
BIN_DIR="$JPM_DIR/bin"
LIB_DIR="$JPM_DIR/lib"
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "==> Bootstrapping jpm..."

# Create directories
mkdir -p "$BIN_DIR" "$CACHE_DIR" "$LIB_DIR"

# Download dependencies
PICOLI_VERSION="4.7.6"
TOML4J_VERSION="0.7.2"
GSON_VERSION="2.10.1"

PICOLI_JAR="picocli-${PICOLI_VERSION}.jar"
TOML4J_JAR="toml4j-${TOML4J_VERSION}.jar"
GSON_JAR="gson-${GSON_VERSION}.jar"

PICOLI_URL="https://repo1.maven.org/maven2/info/picocli/picocli/${PICOLI_VERSION}/${PICOLI_JAR}"
TOML4J_URL="https://repo1.maven.org/maven2/com/moandjiezana/toml/toml4j/${TOML4J_VERSION}/${TOML4J_JAR}"
GSON_URL="https://repo1.maven.org/maven2/com/google/code/gson/gson/${GSON_VERSION}/${GSON_JAR}"

# Download picocli
if [ ! -f "$LIB_DIR/$PICOLI_JAR" ]; then
    echo "  -> Downloading picocli ${PICOLI_VERSION}..."
    curl -sL "$PICOLI_URL" -o "$LIB_DIR/$PICOLI_JAR"
fi

# Download toml4j
if [ ! -f "$LIB_DIR/$TOML4J_JAR" ]; then
    echo "  -> Downloading toml4j ${TOML4J_VERSION}..."
    curl -sL "$TOML4J_URL" -o "$LIB_DIR/$TOML4J_JAR"
fi

# Download Gson (required by toml4j)
if [ ! -f "$LIB_DIR/$GSON_JAR" ]; then
    echo "  -> Downloading gson ${GSON_VERSION}..."
    curl -sL "$GSON_URL" -o "$LIB_DIR/$GSON_JAR"
fi

# Download JUnit 5 (for testing support)
JUNIT_VERSION="5.11.3"
JUNIT_PLATFORM_VERSION="1.11.3"
JUNIT_JAR="junit-jupiter-${JUNIT_VERSION}.jar"
JUNIT_PLATFORM_JAR="junit-platform-console-standalone-${JUNIT_PLATFORM_VERSION}.jar"
JUNIT_URL="https://repo1.maven.org/maven2/org/junit/jupiter/junit-jupiter/${JUNIT_VERSION}/${JUNIT_JAR}"
JUNIT_PLATFORM_URL="https://repo1.maven.org/maven2/org/junit/platform/junit-platform-console-standalone/${JUNIT_PLATFORM_VERSION}/${JUNIT_PLATFORM_JAR}"

if [ ! -f "$LIB_DIR/$JUNIT_JAR" ]; then
    echo "  -> Downloading junit-jupiter ${JUNIT_VERSION}..."
    curl -sL "$JUNIT_URL" -o "$LIB_DIR/$JUNIT_JAR" || echo "  Warning: Failed to download JUnit Jupiter"
fi

if [ ! -f "$LIB_DIR/$JUNIT_PLATFORM_JAR" ]; then
    echo "  -> Downloading junit-platform ${JUNIT_PLATFORM_VERSION}..."
    curl -sL "$JUNIT_PLATFORM_URL" -o "$LIB_DIR/$JUNIT_PLATFORM_JAR" || echo "  Warning: Failed to download JUnit Platform"
fi

# Download Google Java Format (for code formatting alternative)
GOOGLE_FORMAT_VERSION="1.24.0"
GOOGLE_FORMAT_JAR="google-java-format-${GOOGLE_FORMAT_VERSION}-all-deps.jar"
GOOGLE_FORMAT_URL="https://repo1.maven.org/maven2/com/google/googlejavaformat/google-java-format/${GOOGLE_FORMAT_VERSION}/${GOOGLE_FORMAT_JAR}"

if [ ! -f "$LIB_DIR/$GOOGLE_FORMAT_JAR" ]; then
    echo "  -> Downloading google-java-format ${GOOGLE_FORMAT_VERSION}..."
    curl -sL "$GOOGLE_FORMAT_URL" -o "$LIB_DIR/$GOOGLE_FORMAT_JAR" || echo "  Warning: Failed to download Google formatter"
fi

# Download Palantir Java Format (for code formatting)
PALANTIR_VERSION="2.86.0"
PALANTIR_JAR="palantir-java-format-${PALANTIR_VERSION}.jar"
PALANTIR_URL="https://repo1.maven.org/maven2/com/palantir/javaformat/palantir-java-format/${PALANTIR_VERSION}/${PALANTIR_JAR}"

if [ ! -f "$LIB_DIR/$PALANTIR_JAR" ]; then
    echo "  -> Downloading palantir-java-format ${PALANTIR_VERSION}..."
    curl -sL "$PALANTIR_URL" -o "$LIB_DIR/$PALANTIR_JAR" || echo "  Warning: Failed to download Palantir formatter"
fi

# Download Palantir Java Format SPI (required for FormatterException)
PALANTIR_SPI_JAR="palantir-java-format-spi-${PALANTIR_VERSION}.jar"
PALANTIR_SPI_URL="https://repo1.maven.org/maven2/com/palantir/javaformat/palantir-java-format-spi/${PALANTIR_VERSION}/${PALANTIR_SPI_JAR}"

if [ ! -f "$LIB_DIR/$PALANTIR_SPI_JAR" ]; then
    echo "  -> Downloading palantir-java-format-spi ${PALANTIR_VERSION}..."
    curl -sL "$PALANTIR_SPI_URL" -o "$LIB_DIR/$PALANTIR_SPI_JAR" || echo "  Warning: Failed to download Palantir SPI"
fi

# Download Guava (required by Palantir)
GUAVA_VERSION="33.5.0-jre"
GUAVA_JAR="guava-${GUAVA_VERSION}.jar"
GUAVA_URL="https://repo1.maven.org/maven2/com/google/guava/guava/${GUAVA_VERSION}/${GUAVA_JAR}"

if [ ! -f "$LIB_DIR/$GUAVA_JAR" ]; then
    echo "  -> Downloading guava ${GUAVA_VERSION}..."
    curl -sL "$GUAVA_URL" -o "$LIB_DIR/$GUAVA_JAR" || echo "  Warning: Failed to download Guava"
fi

# Download Guava failureaccess (transitive dependency)
FAILUREACCESS_VERSION="1.0.2"
FAILUREACCESS_JAR="failureaccess-${FAILUREACCESS_VERSION}.jar"
FAILUREACCESS_URL="https://repo1.maven.org/maven2/com/google/guava/failureaccess/${FAILUREACCESS_VERSION}/${FAILUREACCESS_JAR}"

if [ ! -f "$LIB_DIR/$FAILUREACCESS_JAR" ]; then
    echo "  -> Downloading failureaccess ${FAILUREACCESS_VERSION}..."
    curl -sL "$FAILUREACCESS_URL" -o "$LIB_DIR/$FAILUREACCESS_JAR" || echo "  Warning: Failed to download failureaccess"
fi

# Download FunctionalJava (required by Palantir)
FJ_VERSION="4.8"
FJ_JAR="functionaljava-${FJ_VERSION}.jar"
FJ_URL="https://repo1.maven.org/maven2/org/functionaljava/functionaljava/${FJ_VERSION}/${FJ_JAR}"

if [ ! -f "$LIB_DIR/$FJ_JAR" ]; then
    echo "  -> Downloading functionaljava ${FJ_VERSION}..."
    curl -sL "$FJ_URL" -o "$LIB_DIR/$FJ_JAR" || echo "  Warning: Failed to download functionaljava"
fi

# Download Jackson dependencies (required by Palantir)
JACKSON_VERSION="2.18.2"

# Jackson Core
JACKSON_CORE_JAR="jackson-core-${JACKSON_VERSION}.jar"
JACKSON_CORE_URL="https://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-core/${JACKSON_VERSION}/${JACKSON_CORE_JAR}"

if [ ! -f "$LIB_DIR/$JACKSON_CORE_JAR" ]; then
    echo "  -> Downloading jackson-core ${JACKSON_VERSION}..."
    curl -sL "$JACKSON_CORE_URL" -o "$LIB_DIR/$JACKSON_CORE_JAR" || echo "  Warning: Failed to download jackson-core"
fi

# Jackson Databind
JACKSON_DATABIND_JAR="jackson-databind-${JACKSON_VERSION}.jar"
JACKSON_DATABIND_URL="https://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-databind/${JACKSON_VERSION}/${JACKSON_DATABIND_JAR}"

if [ ! -f "$LIB_DIR/$JACKSON_DATABIND_JAR" ]; then
    echo "  -> Downloading jackson-databind ${JACKSON_VERSION}..."
    curl -sL "$JACKSON_DATABIND_URL" -o "$LIB_DIR/$JACKSON_DATABIND_JAR" || echo "  Warning: Failed to download jackson-databind"
fi

# Jackson Annotations
JACKSON_ANNOTATIONS_JAR="jackson-annotations-${JACKSON_VERSION}.jar"
JACKSON_ANNOTATIONS_URL="https://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-annotations/${JACKSON_VERSION}/${JACKSON_ANNOTATIONS_JAR}"

if [ ! -f "$LIB_DIR/$JACKSON_ANNOTATIONS_JAR" ]; then
    echo "  -> Downloading jackson-annotations ${JACKSON_VERSION}..."
    curl -sL "$JACKSON_ANNOTATIONS_URL" -o "$LIB_DIR/$JACKSON_ANNOTATIONS_JAR" || echo "  Warning: Failed to download jackson-annotations"
fi

# Jackson Datatype JDK8
JACKSON_DATATYPE_JAR="jackson-datatype-jdk8-${JACKSON_VERSION}.jar"
JACKSON_DATATYPE_URL="https://repo1.maven.org/maven2/com/fasterxml/jackson/datatype/jackson-datatype-jdk8/${JACKSON_VERSION}/${JACKSON_DATATYPE_JAR}"

if [ ! -f "$LIB_DIR/$JACKSON_DATATYPE_JAR" ]; then
    echo "  -> Downloading jackson-datatype-jdk8 ${JACKSON_VERSION}..."
    curl -sL "$JACKSON_DATATYPE_URL" -o "$LIB_DIR/$JACKSON_DATATYPE_JAR" || echo "  Warning: Failed to download jackson-datatype-jdk8"
fi

# Jackson Module Parameter Names
JACKSON_MODULE_JAR="jackson-module-parameter-names-${JACKSON_VERSION}.jar"
JACKSON_MODULE_URL="https://repo1.maven.org/maven2/com/fasterxml/jackson/module/jackson-module-parameter-names/${JACKSON_VERSION}/${JACKSON_MODULE_JAR}"

if [ ! -f "$LIB_DIR/$JACKSON_MODULE_JAR" ]; then
    echo "  -> Downloading jackson-module-parameter-names ${JACKSON_VERSION}..."
    curl -sL "$JACKSON_MODULE_URL" -o "$LIB_DIR/$JACKSON_MODULE_JAR" || echo "  Warning: Failed to download jackson-module-parameter-names"
fi

# Download JSR305 (required by Palantir)
JSR305_VERSION="3.0.2"
JSR305_JAR="jsr305-${JSR305_VERSION}.jar"
JSR305_URL="https://repo1.maven.org/maven2/com/google/code/findbugs/jsr305/${JSR305_VERSION}/${JSR305_JAR}"

if [ ! -f "$LIB_DIR/$JSR305_JAR" ]; then
    echo "  -> Downloading jsr305 ${JSR305_VERSION}..."
    curl -sL "$JSR305_URL" -o "$LIB_DIR/$JSR305_JAR" || echo "  Warning: Failed to download jsr305"
fi

# Download Error Prone Annotations (Guava transitive dependency)
ERROR_PRONE_VERSION="2.36.0"
ERROR_PRONE_JAR="error_prone_annotations-${ERROR_PRONE_VERSION}.jar"
ERROR_PRONE_URL="https://repo1.maven.org/maven2/com/google/errorprone/error_prone_annotations/${ERROR_PRONE_VERSION}/${ERROR_PRONE_JAR}"

if [ ! -f "$LIB_DIR/$ERROR_PRONE_JAR" ]; then
    echo "  -> Downloading error_prone_annotations ${ERROR_PRONE_VERSION}..."
    curl -sL "$ERROR_PRONE_URL" -o "$LIB_DIR/$ERROR_PRONE_JAR" || echo "  Warning: Failed to download error_prone_annotations"
fi

# Download Eclipse JDT Core (for code formatting)
ECLIPSE_VERSION="3.33.0"
ECLIPSE_JAR="org.eclipse.jdt.core-${ECLIPSE_VERSION}.jar"
ECLIPSE_URL="https://repo1.maven.org/maven2/org/eclipse/jdt/org.eclipse.jdt.core/${ECLIPSE_VERSION}/${ECLIPSE_JAR}"

if [ ! -f "$LIB_DIR/$ECLIPSE_JAR" ]; then
    echo "  -> Downloading Eclipse JDT Core ${ECLIPSE_VERSION}..."
    curl -sL "$ECLIPSE_URL" -o "$LIB_DIR/$ECLIPSE_JAR" || echo "  Warning: Failed to download Eclipse JDT Core"
fi

# Download Eclipse Equinox Common (Eclipse JDT transitive dependency)
EQUINOX_COMMON_VERSION="3.18.0"
EQUINOX_COMMON_JAR="org.eclipse.equinox.common-${EQUINOX_COMMON_VERSION}.jar"
EQUINOX_COMMON_URL="https://repo1.maven.org/maven2/org/eclipse/platform/org.eclipse.equinox.common/${EQUINOX_COMMON_VERSION}/${EQUINOX_COMMON_JAR}"

if [ ! -f "$LIB_DIR/$EQUINOX_COMMON_JAR" ]; then
    echo "  -> Downloading Eclipse Equinox Common ${EQUINOX_COMMON_VERSION}..."
    curl -sL "$EQUINOX_COMMON_URL" -o "$LIB_DIR/$EQUINOX_COMMON_JAR" || echo "  Warning: Failed to download Eclipse Equinox Common"
fi

# Download Eclipse Core Runtime (Eclipse JDT transitive dependency)
CORE_RUNTIME_VERSION="3.29.0"
CORE_RUNTIME_JAR="org.eclipse.core.runtime-${CORE_RUNTIME_VERSION}.jar"
CORE_RUNTIME_URL="https://repo1.maven.org/maven2/org/eclipse/platform/org.eclipse.core.runtime/${CORE_RUNTIME_VERSION}/${CORE_RUNTIME_JAR}"

if [ ! -f "$LIB_DIR/$CORE_RUNTIME_JAR" ]; then
    echo "  -> Downloading Eclipse Core Runtime ${CORE_RUNTIME_VERSION}..."
    curl -sL "$CORE_RUNTIME_URL" -o "$LIB_DIR/$CORE_RUNTIME_JAR" || echo "  Warning: Failed to download Eclipse Core Runtime"
fi

# Download Eclipse Core Jobs (Eclipse transitive dependency)
CORE_JOBS_VERSION="3.14.0"
CORE_JOBS_JAR="org.eclipse.core.jobs-${CORE_JOBS_VERSION}.jar"
CORE_JOBS_URL="https://repo1.maven.org/maven2/org/eclipse/platform/org.eclipse.core.jobs/${CORE_JOBS_VERSION}/${CORE_JOBS_JAR}"

if [ ! -f "$LIB_DIR/$CORE_JOBS_JAR" ]; then
    echo "  -> Downloading Eclipse Core Jobs ${CORE_JOBS_VERSION}..."
    curl -sL "$CORE_JOBS_URL" -o "$LIB_DIR/$CORE_JOBS_JAR" || echo "  Warning: Failed to download Eclipse Core Jobs"
fi

# Download Eclipse Core Contenttype (Eclipse transitive dependency)
CORE_CONTENTTYPE_VERSION="3.9.0"
CORE_CONTENTTYPE_JAR="org.eclipse.core.contenttype-${CORE_CONTENTTYPE_VERSION}.jar"
CORE_CONTENTTYPE_URL="https://repo1.maven.org/maven2/org/eclipse/platform/org.eclipse.core.contenttype/${CORE_CONTENTTYPE_VERSION}/${CORE_CONTENTTYPE_JAR}"

if [ ! -f "$LIB_DIR/$CORE_CONTENTTYPE_JAR" ]; then
    echo "  -> Downloading Eclipse Core Contenttype ${CORE_CONTENTTYPE_VERSION}..."
    curl -sL "$CORE_CONTENTTYPE_URL" -o "$LIB_DIR/$CORE_CONTENTTYPE_JAR" || echo "  Warning: Failed to download Eclipse Core Contenttype"
fi

# Download Eclipse Text (required for TextEdit)
TEXT_VERSION="3.13.0"
TEXT_JAR="org.eclipse.text-${TEXT_VERSION}.jar"
TEXT_URL="https://repo1.maven.org/maven2/org/eclipse/platform/org.eclipse.text/${TEXT_VERSION}/${TEXT_JAR}"

if [ ! -f "$LIB_DIR/$TEXT_JAR" ]; then
    echo "  -> Downloading Eclipse Text ${TEXT_VERSION}..."
    curl -sL "$TEXT_URL" -o "$LIB_DIR/$TEXT_JAR" || echo "  Warning: Failed to download Eclipse Text"
fi

# Download Eclipse JFace Text (required for Document and IDocument)
JFACE_TEXT_VERSION="3.24.0"
JFACE_TEXT_JAR="org.eclipse.jface.text-${JFACE_TEXT_VERSION}.jar"
JFACE_TEXT_URL="https://repo1.maven.org/maven2/org/eclipse/platform/org.eclipse.jface.text/${JFACE_TEXT_VERSION}/${JFACE_TEXT_JAR}"

if [ ! -f "$LIB_DIR/$JFACE_TEXT_JAR" ]; then
    echo "  -> Downloading Eclipse JFace Text ${JFACE_TEXT_VERSION}..."
    curl -sL "$JFACE_TEXT_URL" -o "$LIB_DIR/$JFACE_TEXT_JAR" || echo "  Warning: Failed to download Eclipse JFace Text"
fi

# Download Eclipse Core Commands (transitive dependency of JFace Text)
CORE_COMMANDS_VERSION="3.11.0"
CORE_COMMANDS_JAR="org.eclipse.core.commands-${CORE_COMMANDS_VERSION}.jar"
CORE_COMMANDS_URL="https://repo1.maven.org/maven2/org/eclipse/platform/org.eclipse.core.commands/${CORE_COMMANDS_VERSION}/${CORE_COMMANDS_JAR}"

if [ ! -f "$LIB_DIR/$CORE_COMMANDS_JAR" ]; then
    echo "  -> Downloading Eclipse Core Commands ${CORE_COMMANDS_VERSION}..."
    curl -sL "$CORE_COMMANDS_URL" -o "$LIB_DIR/$CORE_COMMANDS_JAR" || echo "  Warning: Failed to download Eclipse Core Commands"
fi

# Create source directories
echo "  -> Creating project structure..."
mkdir -p "$PROJECT_ROOT/src/jpm/cli"
mkdir -p "$PROJECT_ROOT/src/jpm/config"
mkdir -p "$PROJECT_ROOT/src/jpm/deps"
mkdir -p "$PROJECT_ROOT/src/jpm/build"
mkdir -p "$PROJECT_ROOT/src/jpm/utils"

# Create jpm.toml if it doesn't exist
if [ ! -f "$PROJECT_ROOT/jpm.toml" ]; then
    echo "  -> Creating jpm.toml..."
    cat > "$PROJECT_ROOT/jpm.toml" << 'EOF'
[package]
name = "jpm"
version = "0.3.0"
java-version = "21"

[dependencies]
"info.picocli:picocli" = "4.7.6"
"com.moandjiezana.toml:toml4j" = "0.7.2"
EOF
fi

# Check for Java and verify version
echo "  -> Checking Java version..."
if ! command -v javac &> /dev/null; then
    echo "Error: javac not found. Please install JDK 21 or later."
    exit 1
fi

# Extract major version number (e.g., "21.0.2" -> 21)
JAVA_VERSION=$(java -version 2>&1 | grep -oP 'version "\K[^"]+' | cut -d'.' -f1)
if [[ -z "$JAVA_VERSION" ]] || [[ "$JAVA_VERSION" -lt 21 ]]; then
    echo "Error: Java 21+ required (found Java ${JAVA_VERSION:-unknown})"
    echo "Please install JDK 21 or later to use JPM."
    exit 1
fi

echo "     Found Java ${JAVA_VERSION} ✓"

# Find all Java source files (excluding tests)
echo "  -> Finding source files..."
SOURCE_FILES=$(find "$PROJECT_ROOT/src" -name "*.java" ! -path "*/test/*" 2>/dev/null || true)

if [ -z "$SOURCE_FILES" ]; then
    echo "  -> No source files found. Cannot bootstrap without source code."
    echo "     Please ensure the jpm source files are present in $PROJECT_ROOT/src"
    exit 1
fi

# Compile
echo "  -> Compiling source files..."
TARGET_DIR="$PROJECT_ROOT/target/classes"
mkdir -p "$TARGET_DIR"

# Build classpath
CLASSPATH="$LIB_DIR/$PICOLI_JAR:$LIB_DIR/$GOOGLE_FORMAT_JAR:$LIB_DIR/$PALANTIR_JAR:$LIB_DIR/$PALANTIR_SPI_JAR:$LIB_DIR/$FJ_JAR:$LIB_DIR/$GUAVA_JAR:$LIB_DIR/$FAILUREACCESS_JAR:$LIB_DIR/$JACKSON_CORE_JAR:$LIB_DIR/$JACKSON_DATABIND_JAR:$LIB_DIR/$JACKSON_ANNOTATIONS_JAR:$LIB_DIR/$JACKSON_DATATYPE_JAR:$LIB_DIR/$JACKSON_MODULE_JAR:$LIB_DIR/$JSR305_JAR:$LIB_DIR/$ERROR_PRONE_JAR:$LIB_DIR/$TOML4J_JAR:$LIB_DIR/$GSON_JAR:$LIB_DIR/$ECLIPSE_JAR:$LIB_DIR/$EQUINOX_COMMON_JAR:$LIB_DIR/$CORE_RUNTIME_JAR:$LIB_DIR/$CORE_JOBS_JAR:$LIB_DIR/$CORE_CONTENTTYPE_JAR"

# Compile all Java files with Java 21 bytecode target
# Using --release 21 ensures Java 21 API compatibility and bytecode version
javac --release 21 -cp "$CLASSPATH" -d "$TARGET_DIR" $SOURCE_FILES

if [ $? -ne 0 ]; then
    echo "Error: Compilation failed"
    exit 1
fi

# Verify bytecode version (should be 65 = Java 21)
echo "  -> Verifying Java 21 bytecode..."
BYTECODE_VERSION=$(javap -verbose "$TARGET_DIR/jpm/Main.class" 2>/dev/null | grep "major version" | awk '{print $3}')
if [ "$BYTECODE_VERSION" = "65" ]; then
    echo "     Verified Java 21 bytecode (major version 65) ✓"
else
    echo "     Warning: Unexpected bytecode version ${BYTECODE_VERSION}"
fi

# Create JAR
echo "  -> Creating jpm.jar..."
JAR_FILE="$BIN_DIR/jpm.jar"

# Create manifest
cat > "$TARGET_DIR/MANIFEST.MF" << EOF
Manifest-Version: 1.0
Main-Class: jpm.Main
EOF

# Package JAR
jar cfm "$JAR_FILE" "$TARGET_DIR/MANIFEST.MF" -C "$TARGET_DIR" .

# Create wrapper script
echo "  -> Creating jpm wrapper script..."
cat > "$BIN_DIR/jpm" << EOF
#!/bin/bash
# JVM arguments required for Palantir and Google Java Format (access to jdk.compiler internals)
JVM_EXPORTS="--add-exports=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED --add-exports=jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED --add-exports=jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED --add-exports=jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED --add-exports=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED --add-exports=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED --add-opens=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED"
java \$JVM_EXPORTS -cp "$JAR_FILE:$LIB_DIR/$PICOLI_JAR:$LIB_DIR/$GOOGLE_FORMAT_JAR:$LIB_DIR/$PALANTIR_JAR:$LIB_DIR/$PALANTIR_SPI_JAR:$LIB_DIR/$FJ_JAR:$LIB_DIR/$GUAVA_JAR:$LIB_DIR/$FAILUREACCESS_JAR:$LIB_DIR/$JACKSON_CORE_JAR:$LIB_DIR/$JACKSON_DATABIND_JAR:$LIB_DIR/$JACKSON_ANNOTATIONS_JAR:$LIB_DIR/$JACKSON_DATATYPE_JAR:$LIB_DIR/$JACKSON_MODULE_JAR:$LIB_DIR/$JSR305_JAR:$LIB_DIR/$ERROR_PRONE_JAR:$LIB_DIR/$TOML4J_JAR:$LIB_DIR/$GSON_JAR:$LIB_DIR/$ECLIPSE_JAR:$LIB_DIR/$EQUINOX_COMMON_JAR:$LIB_DIR/$CORE_RUNTIME_JAR:$LIB_DIR/$CORE_JOBS_JAR:$LIB_DIR/$CORE_CONTENTTYPE_JAR" jpm.Main "\$@"
EOF

chmod +x "$BIN_DIR/jpm"

echo ""
echo "==> Bootstrap complete!"
echo ""
echo "JPM version: 0.3.0"
echo "Java version required: 21+"
echo ""
echo "Add jpm to your PATH:"
echo "  export PATH=\"\$HOME/.jpm/bin:\$PATH\""
echo ""
echo "Or run directly:"
echo "  ~/.jpm/bin/jpm --help"
echo ""
