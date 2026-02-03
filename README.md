# JPM - Java Package Manager

A Cargo-inspired package manager for Java with direct Maven Central integration. Written in Java, for Java developers.

## Features

- **Simple TOML Configuration** - Clean, readable project manifests
- **Direct Maven Central** - No wrapper around Maven/Gradle
- **Automatic Transitive Resolution** - Downloads dependencies and their dependencies
- **Parent POM Resolution** - Follows Maven parent POMs for correct transitive dependencies
- **Fast Local Cache** - Dependencies cached in `~/.jpm/cache/`
- **Lockfile Support** - `jpm.lock` for fast, reproducible builds
- **Zero External Runtime Dependencies** - Just JDK + bootstrap libraries
- **Self-Hosting** - jpm builds itself

## Quick Start

### Installation

```bash
# Clone or download jpm
cd jpm
./bootstrap.sh

# Add to your PATH
export PATH="$HOME/.jpm/bin:$PATH"
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
jpm build              # Compile only
jpm build --force-resolve   # Force dependency re-resolution
jpm run                # Build + run
jpm run --force-resolve     # Force dependency re-resolution
jpm clean              # Remove target/
```

## Commands

| Command                 | Description                               | Example                                     |
| ----------------------- | ----------------------------------------- | ------------------------------------------- |
| `jpm new <name>`        | Create new project with template          | `jpm new my-app`                            |
| `jpm add <dep>`         | Add dependency with transitive resolution | `jpm add com.google.guava:guava:32.1.3-jre` |
| `jpm remove <artifact>` | Remove dependency                         | `jpm remove guava`                          |
| `jpm build`             | Compile src/ → target/classes/            | `jpm build`                                 |
| `jpm build --force-resolve` | Compile with fresh dependency resolution | `jpm build --force-resolve`          |
| `jpm run`               | Build + execute Main class                | `jpm run`                                   |
| `jpm run --force-resolve` | Build + run with fresh resolution      | `jpm run --force-resolve`             |
| `jpm clean`             | Delete target/ directory                  | `jpm clean`                                 |
| `jpm sync`              | Sync IDE configuration (`.classpath`, `.project`) | `jpm sync`                          |

## IDE Integration

jpm automatically generates Eclipse project files (`.project` and `.classpath`) that are recognized by most Java IDEs and LSP servers (jdtls, VSCode, Eclipse).

### Project Files

**`.project`** - Eclipse project metadata (committed to git)
- Defines the project name and Java nature
- Required for jdtls to recognize the folder as a Java project
- Generated automatically by `jpm new` and `jpm add`
- **Should be committed** to version control

**`.classpath`** - IDE classpath configuration (generated, not committed)
- Lists source directories, output directories, JRE, and all dependency JARs
- Includes **transitive dependencies** automatically
- Regenerated when you add/remove dependencies
- Listed in `.gitignore` by default

### nvim-jdtls / VSCode / Eclipse

When you create or work with a project, jpm generates both files:

```bash
jpm new my-app              # Creates .project and .classpath
jpm add com.google.guava:guava:32.1.3-jre  # Updates .classpath with transitive deps
# Open your IDE - imports and autocompletion work immediately!
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
java-version = "21"

[dependencies]
"com.google.guava:guava" = "32.1.3-jre"
"org.slf4j:slf4j-api" = "2.0.9"
```

### Dependency Format

Dependencies use Maven coordinates: `groupId:artifactId:version`

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
├── config/                # TOML parsing (JpmConfig, ConfigParser)
├── deps/                  # Maven client, POM parser, dependency resolver,
│                          parent POM resolver, lockfile manager
├── build/                 # Compiler, runner, classpath builder, classpath generator
└── utils/                 # File utilities, version comparison
```

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
- **Java 21+** - Requires modern JDK
- **Main.java** - Entry point must be `Main` class

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
