#!/bin/bash
# Quick rebuild script for jpm development
# Compiles with Java 21 bytecode target for runtime compatibility

set -e

JPM_DIR="$HOME/.jpm"
LIB_DIR="$JPM_DIR/lib"
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TARGET_DIR="$PROJECT_ROOT/target/classes"
BIN_DIR="$JPM_DIR/bin"

echo "==> Rebuilding jpm..."

# Classpath
CLASSPATH="$LIB_DIR/picocli-4.7.6.jar:$LIB_DIR/toml4j-0.7.2.jar:$LIB_DIR/gson-2.10.1.jar"

# Find all Java source files (excluding tests)
SOURCE_FILES=$(find "$PROJECT_ROOT/src" -name "*.java" ! -path "*/test/*")

# Compile with Java 21 bytecode target
# --release 21 ensures API compatibility and bytecode version
echo "  -> Compiling with Java 21 target..."
mkdir -p "$TARGET_DIR"
javac --release 21 -cp "$CLASSPATH" -d "$TARGET_DIR" $SOURCE_FILES

# Create JAR
echo "  -> Creating jpm.jar..."
jar cfm "$BIN_DIR/jpm.jar" "$TARGET_DIR/MANIFEST.MF" -C "$TARGET_DIR" .

echo "==> Rebuild complete!"
echo ""
echo "Run: ~/.jpm/bin/jpm --version"
