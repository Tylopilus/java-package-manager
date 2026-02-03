# JPM - Java Package Manager

A Cargo-inspired package manager for Java with direct Maven Central integration. Written in Java, for Java developers.

## Features

- **Simple TOML Configuration** - Clean, readable project manifests
- **Direct Maven Central** - No wrapper around Maven/Gradle
- **Automatic Transitive Resolution** - Downloads dependencies and their dependencies
- **Fast Local Cache** - Dependencies cached in `~/.jpm/cache/`
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

```bash
jpm add com.google.guava:guava:32.1.3-jre
jpm add org.slf4j:slf4j-api:2.0.9
```

### Build and Run

```bash
jpm build    # Compile only
jpm run      # Build + run
jpm clean    # Remove target/
```

## Commands

| Command                 | Description                               | Example                                     |
| ----------------------- | ----------------------------------------- | ------------------------------------------- |
| `jpm new <name>`        | Create new project with template          | `jpm new my-app`                            |
| `jpm add <dep>`         | Add dependency with transitive resolution | `jpm add com.google.guava:guava:32.1.3-jre` |
| `jpm remove <artifact>` | Remove dependency                         | `jpm remove guava`                          |
| `jpm build`             | Compile src/ → target/classes/            | `jpm build`                                 |
| `jpm run`               | Build + execute Main class                | `jpm run`                                   |
| `jpm clean`             | Delete target/ directory                  | `jpm clean`                                 |

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
3. **Resolve** transitive dependencies recursively
4. **Cache** all artifacts in `~/.jpm/cache/`
5. **Build** classpath from cached JARs

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
├── jpm.toml           # Project configuration
├── src/
│   └── Main.java     # Main class (entry point)
└── target/
    └── classes/      # Compiled classes
```

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
├── cli/                   # Commands (new, add, remove, build, run, clean)
├── config/                # TOML parsing (JpmConfig, ConfigParser)
├── deps/                  # Maven client, POM parser, dependency resolver
├── build/                 # Compiler, runner, classpath builder
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
