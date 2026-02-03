# JPM - Java Package Manager

A Cargo-inspired package manager for Java with direct Maven Central integration. Written in modern Java 25+, for Java developers.

**Version:** 0.3.0 | **Requires:** Java 25 or later

## Features

- **Simple TOML Configuration** - Clean, readable project manifests
- **Direct Maven Central** - No wrapper around Maven/Gradle
- **Automatic Transitive Resolution** - Downloads dependencies and their dependencies
- **Parent POM Resolution** - Follows Maven parent POMs for correct transitive dependencies
- **Fast Local Cache** - Dependencies cached in `~/.jpm/cache/`
- **Lockfile Support** - `jpm.lock` for fast, reproducible builds
- **Integrated Testing** - `jpm test` with JUnit 5 and CI-ready XML reports
- **Build Profiles** - dev, release, and test profiles with different optimization levels
- **Code Formatting** - Eclipse JDT Core formatter with Rust-like style (100 char lines, grouped imports)
- **Zero External Runtime Dependencies** - Just JDK + bootstrap libraries
- **Self-Hosting** - jpm builds itself
- **Modern Java 25+** - Uses records, virtual threads, and pattern matching

## Quick Start

### Installation

**Prerequisites:** Java 25 or later required

```bash
# Verify Java version (must be 25+)
java -version

# Clone or download jpm
cd jpm
./bootstrap.sh

# Add to your PATH
export PATH="$HOME/.jpm/bin:$PATH"

# Verify installation
jpm --version  # Should show: jpm 0.3.0
```

### Create a New Project

```bash
jpm new my-app
cd my-app
jpm run
```

### Add Dependencies

**Full coordinates (exact):**
```bash
jpm add com.google.guava:guava:32.1.3-jre
jpm add org.slf4j:slf4j-api:2.0.9
```

**Interactive search (partial names):**
```bash
jpm add guava          # Searches Maven Central, shows matches, select interactively
jpm add jackson-databind slf4j-api  # Batch mode - add multiple at once
jpm add --yes guava    # Non-interactive mode, auto-confirm
```

### Build and Run

```bash
jpm build              # Compile only (uses dev profile by default)
jpm build --profile release   # Compile with release optimizations
jpm build --force-resolve     # Force dependency re-resolution
jpm build --no-ide-files      # Skip IDE file generation
jpm run                # Build + run (uses dev profile by default)
jpm run --profile release     # Build + run with release optimizations
jpm clean              # Remove target/
```

### Testing

JPM includes integrated JUnit 5 testing support out of the box:

```bash
jpm test               # Run all tests in src/test/java/
jpm test --filter MyTest    # Run tests matching pattern
jpm test --no-parallel      # Disable parallel execution
```

**JUnit 5 is automatically included** - no setup required. Just add test files to `src/test/java/`:

```java
// src/test/java/MyTest.java
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MyTest {
    @Test
    void shouldDoSomething() {
        assertEquals(4, 2 + 2);
    }
}
```

**CI Integration:** Tests generate `target/jpm-test-report.xml` in standard JUnit XML format for Jenkins, GitHub Actions, etc.

### Code Formatting

Format your code with Eclipse JDT Core formatter, featuring Rust-like defaults:

```bash
jpm fmt                # Format all Java files in src/ (default: Eclipse formatter)
jpm fmt --check        # Check formatting (CI mode - fails if unformatted)
jpm fmt src/Main.java  # Format specific file
jpm fmt --no-organize-imports  # Skip import organization
```

**Rust-like Formatting Style:**
- **100 character line width** (rustfmt default, with overflow for long literals)
- **4 spaces indentation** (no tabs)
- **Opening braces on same line** (K&R style)
- **Spaces around binary operators** (e.g., `a + b` not `a+b`)
- **Trailing commas in multi-line** constructs
- **Grouped imports** (java.* → external → project, separated by blank lines)

**Features:**
- **Format code**: Applies Rust-like Eclipse formatter style
- **Organize imports**: Groups and sorts imports Rust-style (enabled by default)
- **Check mode**: Returns exit code 1 if files need formatting (for CI)
- **Skip patterns**: Exclude files using glob patterns
- **Multiple formatters**: Supports Eclipse (default), Palantir, and Google Java Format

**Configuration (jpm.toml):**

```toml
[fmt]
formatter = "eclipse"         # Formatter: eclipse (default), palantir, or google
line-length = 100            # Rust-style 100 char lines (default)
organize-imports = true      # Enable/disable import organization
skip-patterns = ["**/target/**", "**/generated/**"]  # Files to exclude
```

**CI Integration:** Use `jpm fmt --check` in pre-commit hooks or CI pipelines. It returns exit code 1 if any files need formatting.

**Pluggable Architecture:** The formatter uses a pluggable interface (`jpm.build.format.Formatter`). Available formatters:
- **Eclipse** (default): Rust-like style, fully compatible with all Java versions
- **Palantir**: Alternative opinionated style, all Java versions
- **Google**: Google's strict style, has compatibility issues with Java 25+

## Commands

| Command                 | Description                               | Example                                     |
| ----------------------- | ----------------------------------------- | ------------------------------------------- |
| `jpm new <name>`        | Create new project with template          | `jpm new my-app`                            |
| `jpm add <dep>`         | Add dependency with transitive resolution | `jpm add com.google.guava:guava:32.1.3-jre` |
| `jpm remove <artifact>` | Remove dependency                         | `jpm remove guava`                          |
| `jpm build`             | Compile src/ → target/classes/            | `jpm build`                                 |
| `jpm build --profile <name>` | Compile with specific profile (dev/release/test) | `jpm build --profile release`        |
| `jpm build --force-resolve` | Compile with fresh dependency resolution | `jpm build --force-resolve`          |
| `jpm build --no-ide-files` | Compile without generating IDE files | `jpm build --no-ide-files`          |
| `jpm run`               | Build + execute Main class                | `jpm run`                                   |
| `jpm run --profile <name>` | Run with specific profile                | `jpm run --profile dev`               |
| `jpm run --force-resolve` | Build + run with fresh resolution      | `jpm run --force-resolve`             |
| `jpm run --no-ide-files` | Build + run without generating IDE files | `jpm run --no-ide-files`             |
| `jpm test`              | Run all JUnit 5 tests in src/test/java/   | `jpm test`                                  |
| `jpm test --filter <pattern>` | Run tests matching pattern         | `jpm test --filter UserTest`          |
| `jpm test --no-parallel` | Disable parallel test execution          | `jpm test --no-parallel`              |
| `jpm fmt`               | Format Java code (Eclipse, Rust-like style) | `jpm fmt`                            |
| `jpm fmt --check`       | Check formatting (CI mode)              | `jpm fmt --check`                         |
| `jpm fmt --organize-imports` | Format with import organization    | `jpm fmt --organize-imports`              |
| `jpm fmt --no-organize-imports` | Format without import organization | `jpm fmt --no-organize-imports`      |
| `jpm fmt --formatter <name>` | Use specific formatter (eclipse/palantir/google) | `jpm fmt --formatter palantir`    |
| `jpm clean`             | Delete target/ directory                  | `jpm clean`                                 |
| `jpm sync`              | Sync IDE configuration (`.classpath`, `.project`) | `jpm sync`                          |

## IDE Integration

jpm automatically generates Eclipse project files (`.project` and `.classpath`) that are recognized by most Java IDEs and LSP servers (jdtls, VSCode, Eclipse).

### Project Files

**`.project`** - Eclipse project metadata (committed to git)
- Defines the project name and Java nature
- Required for jdtls to recognize the folder as a Java project
- Generated automatically by `jpm new`, `jpm run`, `jpm build`, and `jpm add`
- **Should be committed** to version control

**`.classpath`** - IDE classpath configuration (generated, not committed)
- Lists source directories, output directories, JRE, and all dependency JARs
- Includes **transitive dependencies** automatically
- Generated automatically by `jpm new`, `jpm run`, `jpm build`, and `jpm add`
- Listed in `.gitignore` by default

### nvim-jdtls / VSCode / Eclipse

When you create or work with a project, jpm generates both files automatically:

```bash
jpm new my-app              # Creates .project and .classpath
jpm run                     # Creates .project and .classpath if missing
jpm build                   # Creates .project and .classpath if missing
jpm add com.google.guava:guava:32.1.3-jre  # Updates .classpath with transitive deps
# Open your IDE - imports and autocompletion work immediately!
```

**Note:** IDE files are only generated if they don't exist. To skip generation, use `--no-ide-files`:

```bash
jpm run --no-ide-files      # Won't create .project or .classpath
```

The `.classpath` file includes:

- Source directory (`src/`)
- Output directory (`target/classes/`)
- JRE container
- All dependency JARs from `~/.jpm/cache/` (including transitive dependencies)

**Note:** If you delete `.project`, run `jpm sync` or `jpm add` to regenerate it.

### Manual Sync

If you need to regenerate the IDE configuration files manually:

```bash
jpm sync  # Regenerates both .project and .classpath
```

## Configuration

Projects use `jpm.toml` for configuration:

```toml
[package]
name = "my-app"
version = "0.1.0"
 java-version = "25"

[dependencies]
"com.google.guava:guava" = "32.1.3-jre"
"org.slf4j:slf4j-api" = "2.0.9"
```

### Dependency Format

Dependencies use Maven coordinates: `groupId:artifactId:version`

### Build Profiles

Define different build configurations for development, testing, and production:

```toml
[package]
name = "my-app"
version = "1.0.0"
 java-version = "25"

[dependencies]
"com.google.guava:guava" = "32.1.3-jre"

# Development profile (default)
[profile.dev]
compiler-args = ["-g", "-parameters"]    # Debug symbols, method params
jvm-args = ["-ea"]                       # Enable assertions

# Release profile - optimized for production
[profile.release]
compiler-args = ["-O", "-parameters"]    # Optimization enabled
jvm-args = ["-server", "-Xmx2g"]         # Server VM, 2GB heap
strip-debug = true

# Test profile - inherits from dev
[profile.test]
inherits = "dev"
compiler-args = ["-g:vars", "-parameters"]  # Local variable debug info
```

**Default Profiles:**

- **dev**: Fast compilation, debug symbols, assertions enabled (default for `jpm build` and `jpm run`)
- **release**: Optimized bytecode, server VM, 2GB heap default, stripped debug symbols
- **test**: Inherits from dev, optimized for test debugging

**Profile Inheritance:**

Profiles can inherit from other profiles using the `inherits` key. Child profile settings override parent settings:

```toml
[profile.staging]
inherits = "release"
jvm-args = ["-server", "-Xmx4g"]    # Override heap size
```

**Usage:**

```bash
jpm build --profile release    # Production build with optimizations
jpm run --profile dev          # Development run (default)
jpm test                       # Automatically uses test profile
```

In `jpm.toml`, they are stored as:

```toml
"groupId:artifactId" = "version"
```

## How It Works

### Dependency Resolution

1. **Parse** `jpm.toml` for direct dependencies
2. **Download** POM files from Maven Central
3. **Follow** parent POMs for inherited properties and versions (up to 10 levels)
4. **Resolve** transitive dependencies recursively
5. **Cache** all artifacts in `~/.jpm/cache/`
6. **Build** classpath from cached JARs

### Lockfile

jpm uses a `jpm.lock` file (similar to Cargo's `Cargo.lock`) for fast, reproducible builds:

- **Automatic Creation**: Generated on first run/build with dependencies
- **Fast Subsequent Runs**: Lockfile allows skipping full dependency resolution
- **Auto-Regeneration**: Automatically updated when dependencies change in `jpm.toml`
- **Version Control**: Commit `jpm.lock` for reproducible builds across environments

```bash
# First run - creates jpm.lock
jpm run
# Output: Resolving dependencies... Resolved 5 dependencies

# Second run - uses lockfile (instant!)
jpm run
# Output: Using cached dependencies from jpm.lock

# Force fresh resolution
jpm run --force-resolve
```

### Parent POM Resolution

When resolving dependencies, jpm follows Maven parent POM chains to correctly resolve property placeholders (e.g., `${jackson.version.annotations}`):

- Downloads parent POMs recursively (up to 10 levels)
- Caches parent POMs locally forever
- Merges properties from parent → child hierarchy
- Shows timing: "Downloaded 4 parent POMs in 529ms"

This ensures libraries like Jackson and Spring Boot work correctly without manual transitive dependency management.

### IDE File Generation

When you run `jpm new` or `jpm add`:

1. **`.project`** is created with Eclipse project metadata (if missing)
2. **`.classpath`** is generated including all transitive dependencies
3. Files are written to the project root
4. jdtls and other Eclipse-based IDEs immediately recognize the project

### Cache Structure

```
~/.jpm/
├── cache/
│   └── com/
│       └── google/
│           └── guava/
│               └── guava/
│                   └── 32.1.3-jre/
│                       ├── guava-32.1.3-jre.jar
│                       └── guava-32.1.3-jre.pom
└── lib/
    ├── picocli-4.7.6.jar
    ├── toml4j-0.7.2.jar
    └── gson-2.10.1.jar
```

### Version Conflict Resolution

When multiple versions of the same artifact are needed:

- **Newest wins** (semantic version comparison)
- Example: `1.0.0` vs `1.1.0` → `1.1.0` wins

## Project Structure

```
my-app/
├── .project           # Eclipse project metadata (commit this)
├── .classpath         # IDE classpath (auto-generated, in .gitignore)
├── jpm.lock           # Lockfile for reproducible builds (commit this)
├── .gitignore         # Git ignore rules
├── jpm.toml           # Project configuration
├── src/
│   └── Main.java     # Main class (entry point)
└── target/
    └── classes/      # Compiled classes
```

**File Guide:**
- `.project` - Required for IDE integration, should be committed
- `.classpath` - Auto-generated from `jpm.toml`, in `.gitignore`
- `jpm.lock` - Lockfile with exact dependency versions, should be committed
- `jpm.toml` - Your project configuration, should be committed
- `target/` - Build output, in `.gitignore`

## Example Usage

```bash
# Create project
jpm new hello-world
cd hello-world

# Add Guava dependency
jpm add com.google.guava:guava:32.1.3-jre

# Update Main.java to use Guava
cat > src/Main.java << 'EOF'
import com.google.common.collect.Lists;

public class Main {
    public static void main(String[] args) {
        var names = Lists.newArrayList("Alice", "Bob");
        System.out.println("Hello: " + names);
    }
}
EOF

# Build and run
jpm run
```

## Development

### Rebuilding jpm

```bash
./rebuild.sh
```

### Architecture

```
src/jpm/
├── Main.java              # CLI entry point
├── cli/                   # Commands (new, add, remove, build, run, clean, sync)
├── config/                # TOML parsing (JpmConfig, ConfigParser) - uses Records
├── deps/                  # Maven client, POM parser, dependency resolver,
│                          parent POM resolver, lockfile manager - uses Virtual Threads
├── build/                 # Compiler, runner, classpath builder, classpath generator
└── utils/                 # File utilities, version comparison
```

### Modern Java Features

JPM 0.3.0 is built with modern Java 25+ features:

- **Records** - Immutable data classes for `JpmConfig`, `PomInfo`, and `Lockfile`
- **Virtual Threads** - Concurrent artifact downloads using `Executors.newVirtualThreadPerTaskExecutor()`
- **Pattern Matching Switch** - Modern switch expressions in CLI commands
- **Compact Constructors** - Defensive copying in record constructors
- **`--release 25`** - Compiles to Java 25 bytecode for compatibility

## Differences from Maven/Gradle

| Feature         | jpm                | Maven/Gradle          |
| --------------- | ------------------ | --------------------- |
| Configuration   | TOML               | XML/Groovy/Kotlin     |
| Structure       | Simple, flat       | Complex, hierarchical |
| Learning curve  | Low                | High                  |
| Transitive deps | Automatic          | Automatic             |
| Repository      | Maven Central only | Configurable          |
| Multi-module    | No                 | Yes                   |

## Limitations

- **Maven Central only** - Cannot use other repositories
- **Single-module projects** - No workspace support
- **Java 25+ Required** - JPM requires Java 25 or later (uses virtual threads, records, pattern matching)
- **Main.java** - Entry point must be `Main` class

### System Requirements

- **Java Version:** 25 or later
- **Operating System:** Linux, macOS, Windows (with bash)
- **Disk Space:** ~100MB for jpm + dependencies
- **Network:** Internet connection for Maven Central downloads

## License

MIT - See LICENSE file for details.

## Contributing

Contributions welcome! Please ensure:

1. Code follows existing style
2. Tests pass (when we add them)
3. Documentation updated

## Acknowledgments

- Inspired by [Cargo](https://doc.rust-lang.org/cargo/)
- Uses [picocli](https://picocli.info/) for CLI
- Uses [toml4j](https://github.com/moandjiezana/toml4j) for TOML parsing
