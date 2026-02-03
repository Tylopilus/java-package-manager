# JPM - Java Package Manager Implementation Plan

## Overview

A Cargo-inspired package manager for Java with direct Maven Central integration, written in Java itself.

## Core Principles

- **Self-hosting**: jpm builds itself using its own build system
- **Zero external runtime dependencies**: Uses only the JDK + pre-downloaded libraries for bootstrapping
- **Simplicity**: Single-module projects, Maven Central only, TOML configuration
- **Fast feedback**: Direct javac/java integration without wrapper overhead

---

## Phase 0: Bootstrap

**Goal**: Create a mechanism to build jpm without jpm existing yet

### Deliverables

- `bootstrap.sh` - Shell script that:
    1. Downloads picocli and toml4j JARs to `~/.jpm/bootstrap-libs/`
    2. Compiles all jpm source files
    3. Packages into `~/.jpm/bin/jpm.jar`
    4. Creates `~/.jpm/bin/jpm` wrapper script
- Initial project structure
- Self-hosting `jpm.toml`

### Bootstrap Script Requirements

```bash
#!/bin/bash
# Downloads dependencies, compiles jpm, creates executable
# Usage: ./bootstrap.sh
```

---

## Phase 1: Project Structure & Configuration

### Deliverables

```
jpm/
├── jpm.toml                    # Self-hosting manifest
├── src/jpm/
│   ├── Main.java              # CLI entry point
│   ├── cli/
│   │   ├── NewCommand.java
│   │   ├── AddCommand.java
│   │   ├── RemoveCommand.java
│   │   ├── BuildCommand.java
│   │   ├── RunCommand.java
│   │   └── CleanCommand.java
│   ├── config/
│   │   ├── JpmConfig.java       # TOML representation
│   │   └── ConfigParser.java    # TOML reader
│   ├── deps/
│   │   ├── MavenClient.java     # HTTP client for Maven Central
│   │   ├── PomParser.java       # Parse pom.xml
│   │   ├── DependencyResolver.java  # Transitive resolution
│   │   └── CacheManager.java    # ~/.jpm/cache/ management
│   ├── build/
│   │   ├── Compiler.java        # javac wrapper
│   │   ├── Runner.java          # java wrapper
│   │   └── ClasspathBuilder.java
│   └── utils/
│       ├── Version.java         # Semver comparison
│       └── FileUtils.java
└── bootstrap.sh
```

### Configuration Format (jpm.toml)

```toml
[package]
name = "jpm"
version = "0.1.0"
java-version = "21"

[dependencies]
"info.picocli:picocli" = "4.7.5"
"com.moandjiezana.toml:toml4j" = "0.7.2"
```

---

## Phase 2: CLI Framework

### Commands to Implement

| Command                 | Description                               | Example                                     |
| ----------------------- | ----------------------------------------- | ------------------------------------------- |
| `jpm new <name>`        | Create new project with template          | `jpm new my-app`                            |
| `jpm add <dep>`         | Add dependency with transitive resolution | `jpm add com.google.guava:guava:32.1.3-jre` |
| `jpm remove <artifact>` | Remove dependency                         | `jpm remove guava`                          |
| `jpm build`             | Compile src/ → target/classes/            | `jpm build`                                 |
| `jpm run`               | Build + execute Main class                | `jpm run`                                   |
| `jpm clean`             | Delete target/                            | `jpm clean`                                 |

### CLI Requirements

- Use picocli for argument parsing
- Consistent output format (Cargo-style)
- Exit codes: 0 = success, 1 = error

---

## Phase 3: Maven Integration

### Maven Client Requirements

#### HTTP Operations

- Download JAR/POM from `https://repo1.maven.org/maven2/`
- Follow Maven repository path structure: `{group}/{artifact}/{version}/`
- Handle redirects, connection timeouts (30s default)

#### POM Parsing

- Parse `pom.xml` for:
    - Direct dependencies from `<dependencies>`
    - Parent POM references
    - Properties substitution
    - Dependency management (versions)
- Handle `<scope>` tags: compile (keep), test/provided (skip)
- Handle `<optional>true</optional>` (skip)

#### Transitive Resolution Algorithm

```java
resolve(groupId, artifactId, version):
    1. Download POM from Maven Central
    2. Parse direct dependencies (filter scope/optional)
    3. For each dependency:
        a. Check cache in ~/.jpm/cache/
        b. If not cached, download JAR + POM
        c. Recursively resolve that dependency's dependencies
    4. Collect all artifacts into resolution graph
    5. Detect version conflicts
    6. Resolve conflicts: newest version wins (semver comparison)
    7. Return final classpath list
```

### Cache Structure

```
~/.jpm/
├── cache/
│   ├── com/google/guava/guava/32.1.3-jre/
│   │   ├── guava-32.1.3-jre.jar
│   │   └── guava-32.1.3-jre.pom
│   └── ...
└── bootstrap-libs/
    ├── picocli-4.7.5.jar
    └── toml4j-0.7.2.jar
```

---

## Phase 4: Build System

### Compilation

- Command: `javac -d target/classes -cp "dependencies:src" src/**/*.java`
- Source: All `.java` files in `src/` (recursive)
- Output: `target/classes/`
- Classpath: All resolved dependencies + `src/`

### Execution

- Command: `java -cp "target/classes:dependencies" Main`
- Main class: First class with `main()` method, or explicitly configured
- Classpath: Compiled classes + all dependencies

---

## Phase 5: jpm new Command

### Project Template

```
new-project/
├── jpm.toml
└── src/
    └── Main.java
```

### Generated jpm.toml

```toml
[package]
name = "{project-name}"
version = "0.1.0"
java-version = "21"

[dependencies]
```

### Generated Main.java

```java
public class Main {

    public static void main(String[] args) {
        System.out.println("Hello, {project-name}!");
    }
}
```

---

## Implementation Order

1. **Phase 0**: Write `bootstrap.sh`
2. **Phase 1**: Create directory structure, self-hosting `jpm.toml`
3. **Phase 2**: Implement CLI entry point with picocli
4. **Phase 3**: Implement `new` command (simplest, no dependencies)
5. **Phase 4**: Implement Maven client (HTTP + XML parsing)
6. **Phase 5**: Implement `add` command with transitive resolution
7. **Phase 6**: Implement `build` command
8. **Phase 7**: Implement `run` command
9. **Phase 8**: Implement `remove` and `clean` commands
10. **Phase 9**: Bootstrap jpm using itself
11. **Phase 10**: Test with real Maven dependencies

---

## Testing Checklist

- [ ] Bootstrap script successfully builds jpm
- [ ] `jpm new` creates valid project structure
- [ ] `jpm add com.google.guava:guava:32.1.3-jre` downloads and caches
- [ ] Transitive dependencies resolved automatically
- [ ] Version conflicts resolved correctly (newest wins)
- [ ] `jpm build` compiles project with dependencies
- [ ] `jpm run` executes compiled code
- [ ] `jpm clean` removes target/ directory
- [ ] `jpm remove` updates jpm.toml and cleans cache entry

---

## Success Criteria

1. Can create new Java project with `jpm new`
2. Can add dependencies from Maven Central
3. Dependencies are cached locally
4. Transitive dependencies resolved automatically
5. Can build and run projects with single commands
6. jpm can build itself (`./bootstrap.sh` → `jpm build`)
