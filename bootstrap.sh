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

# Download Palantir Java Format (for code formatting)
PALANTIR_VERSION="2.50.0"
PALANTIR_JAR="palantir-java-format-${PALANTIR_VERSION}-all.jar"
PALANTIR_URL="https://repo1.maven.org/maven2/com/palantir/javaformat/palantir-java-format/${PALANTIR_VERSION}/${PALANTIR_JAR}"

if [ ! -f "$LIB_DIR/$PALANTIR_JAR" ]; then
    echo "  -> Downloading palantir-java-format ${PALANTIR_VERSION}..."
    curl -sL "$PALANTIR_URL" -o "$LIB_DIR/$PALANTIR_JAR" || echo "  Warning: Failed to download Palantir formatter"
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
version = "0.2.0-java21"
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

# Find all Java source files
echo "  -> Finding source files..."
SOURCE_FILES=$(find "$PROJECT_ROOT/src" -name "*.java" 2>/dev/null || true)

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
CLASSPATH="$LIB_DIR/$PICOLI_JAR:$LIB_DIR/$TOML4J_JAR:$LIB_DIR/$GSON_JAR"

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
Class-Path: $LIB_DIR/$PICOLI_JAR $LIB_DIR/$TOML4J_JAR $LIB_DIR/$GSON_JAR
EOF

# Package JAR
jar cfm "$JAR_FILE" "$TARGET_DIR/MANIFEST.MF" -C "$TARGET_DIR" .

# Create wrapper script
echo "  -> Creating jpm wrapper script..."
cat > "$BIN_DIR/jpm" << EOF
#!/bin/bash
java -cp "$JAR_FILE:$LIB_DIR/$PICOLI_JAR:$LIB_DIR/$TOML4J_JAR:$LIB_DIR/$GSON_JAR" jpm.Main "\$@"
EOF

chmod +x "$BIN_DIR/jpm"

echo ""
echo "==> Bootstrap complete!"
echo ""
echo "JPM version: 0.2.0-java21"
echo "Java version required: 21+"
echo ""
echo "Add jpm to your PATH:"
echo "  export PATH=\"\$HOME/.jpm/bin:\$PATH\""
echo ""
echo "Or run directly:"
echo "  ~/.jpm/bin/jpm --help"
echo ""
