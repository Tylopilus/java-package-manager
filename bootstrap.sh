#!/bin/bash
# Bootstrap script for jpm
# This script builds jpm from scratch without jpm existing yet

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
version = "0.1.0"
java-version = "21"

[dependencies]
"info.picocli:picocli" = "4.7.6"
"com.moandjiezana.toml:toml4j" = "0.7.2"
EOF
fi

# Check for Java
echo "  -> Checking Java version..."
if ! command -v javac &> /dev/null; then
    echo "Error: javac not found. Please install JDK 21 or later."
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2)
echo "     Found Java ${JAVA_VERSION}"

# Find all Java source files
echo "  -> Finding source files..."
SOURCE_FILES=$(find "$PROJECT_ROOT/src" -name "*.java" 2>/dev/null || true)

if [ -z "$SOURCE_FILES" ]; then
    echo "  -> No source files found yet. Creating stub Main.java..."
    
    # Create stub Main.java for initial compilation
    cat > "$PROJECT_ROOT/src/jpm/Main.java" << 'EOF'
package jpm;

import jpm.cli.NewCommand;
import jpm.cli.AddCommand;
import jpm.cli.RemoveCommand;
import jpm.cli.BuildCommand;
import jpm.cli.RunCommand;
import jpm.cli.CleanCommand;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(
    name = "jpm",
    mixinStandardHelpOptions = true,
    version = "jpm 0.1.0",
    description = "Java Package Manager - Cargo for Java",
    subcommands = {
        NewCommand.class,
        AddCommand.class,
        RemoveCommand.class,
        BuildCommand.class,
        RunCommand.class,
        CleanCommand.class
    }
)
public class Main implements Runnable {
    public static void main(String[] args) {
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public void run() {
        System.out.println("jpm - Java Package Manager");
        System.out.println("Use 'jpm --help' for available commands");
    }
}
EOF

    # Create stub command files
    cat > "$PROJECT_ROOT/src/jpm/cli/NewCommand.java" << 'EOF'
package jpm.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import java.util.concurrent.Callable;

@Command(name = "new", description = "Create a new Java project")
public class NewCommand implements Callable<Integer> {
    @Parameters(index = "0", description = "Project name")
    private String projectName;

    @Override
    public Integer call() {
        System.out.println("Creating new project: " + projectName);
        return 0;
    }
}
EOF

    cat > "$PROJECT_ROOT/src/jpm/cli/AddCommand.java" << 'EOF'
package jpm.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import java.util.concurrent.Callable;

@Command(name = "add", description = "Add a dependency")
public class AddCommand implements Callable<Integer> {
    @Parameters(index = "0", description = "Dependency (group:artifact:version)")
    private String dependency;

    @Override
    public Integer call() {
        System.out.println("Adding dependency: " + dependency);
        return 0;
    }
}
EOF

    cat > "$PROJECT_ROOT/src/jpm/cli/RemoveCommand.java" << 'EOF'
package jpm.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import java.util.concurrent.Callable;

@Command(name = "remove", description = "Remove a dependency")
public class RemoveCommand implements Callable<Integer> {
    @Parameters(index = "0", description = "Artifact name")
    private String artifact;

    @Override
    public Integer call() {
        System.out.println("Removing dependency: " + artifact);
        return 0;
    }
}
EOF

    cat > "$PROJECT_ROOT/src/jpm/cli/BuildCommand.java" << 'EOF'
package jpm.cli;

import picocli.CommandLine.Command;
import java.util.concurrent.Callable;

@Command(name = "build", description = "Build the project")
public class BuildCommand implements Callable<Integer> {
    @Override
    public Integer call() {
        System.out.println("Building project...");
        return 0;
    }
}
EOF

    cat > "$PROJECT_ROOT/src/jpm/cli/RunCommand.java" << 'EOF'
package jpm.cli;

import picocli.CommandLine.Command;
import java.util.concurrent.Callable;

@Command(name = "run", description = "Build and run the project")
public class RunCommand implements Callable<Integer> {
    @Override
    public Integer call() {
        System.out.println("Building and running project...");
        return 0;
    }
}
EOF

    cat > "$PROJECT_ROOT/src/jpm/cli/CleanCommand.java" << 'EOF'
package jpm.cli;

import picocli.CommandLine.Command;
import java.util.concurrent.Callable;

@Command(name = "clean", description = "Clean build artifacts")
public class CleanCommand implements Callable<Integer> {
    @Override
    public Integer call() {
        System.out.println("Cleaning project...");
        return 0;
    }
}
EOF

    # Re-scan for source files
    SOURCE_FILES=$(find "$PROJECT_ROOT/src" -name "*.java")
fi

# Compile
echo "  -> Compiling source files..."
TARGET_DIR="$PROJECT_ROOT/target/classes"
mkdir -p "$TARGET_DIR"

# Build classpath
CLASSPATH="$LIB_DIR/$PICOLI_JAR:$LIB_DIR/$TOML4J_JAR:$LIB_DIR/$GSON_JAR"

# Compile all Java files
javac -cp "$CLASSPATH" -d "$TARGET_DIR" $SOURCE_FILES

if [ $? -ne 0 ]; then
    echo "Error: Compilation failed"
    exit 1
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
echo "Add jpm to your PATH:"
echo "  export PATH=\"\$HOME/.jpm/bin:\$PATH\""
echo ""
echo "Or run directly:"
echo "  ~/.jpm/bin/jpm --help"
echo ""
