# AGENTS.md - Guidelines for AI Coding Agents

## Code changing rules
- always expand to the whole project scope
- always check for refactoring. Aim for the DRY principle where possible
- If a change makes no change - push back before implemeting it
- Question yourself if a change now is good or should it be done elsewhere
- Always clean up after yourself - don't leave old code behind. If it can be
  refactored do so.
- always update the readme if necessary
- alwyas commit changes using semantic commit messages
## Build Commands

### Development Build
```bash
./rebuild.sh
```
Quick rebuild during development. Requires dependencies to already be downloaded.

### Full Bootstrap
```bash
./bootstrap.sh
```
Complete build from scratch including dependency downloads.

### Manual Compile (if needed)
```bash
JPM_DIR="$HOME/.jpm"
LIB_DIR="$JPM_DIR/lib"
CLASSPATH="$LIB_DIR/picocli-4.7.6.jar:$LIB_DIR/toml4j-0.7.2.jar:$LIB_DIR/gson-2.10.1.jar"
javac -cp "$CLASSPATH" -d target/classes src/**/*.java
```

### Run jpm
```bash
~/.jpm/bin/jpm --help
```

## Testing

**Note:** This project does not currently have automated tests. When adding tests:
- Create a `src/test/java` directory structure
- Use JUnit 5 (JUnit Jupiter) for new tests
- Run individual test class: `java -cp <classpath> org.junit.platform.console.ConsoleLauncher -c <TestClass>`
- Tests should be placed in packages matching the source (e.g., `jpm.deps` tests in `test/jpm/deps`)

## Code Style Guidelines

### Language Version
- **Java 25** is required
- Target modern Java features (records, var keyword, text blocks, switch expressions)

### Imports
```java
// Group by: java.*, third-party libs, project imports
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

import picocli.CommandLine;

import jpm.config.JpmConfig;
import jpm.utils.FileUtils;
```
- No wildcard imports (`import java.util.*;` is OK, but not `import static jpm.deps.*;`)
- Always specify StandardCharsets for encoding operations

### Formatting
- **Indentation**: 4 spaces (no tabs)
- **Line length**: 120 characters max
- **Braces**: Opening brace on same line, closing on new line
- **No trailing whitespace**
- **One blank line** between methods
- **Two blank lines** between classes

### Types and Declarations
```java
// Use 'var' for local variables where type is obvious
var resolver = new DependencyResolver();
var files = new ArrayList<String>();

// Use records for immutable data classes
public record Dependency(String groupId, String artifactId, String version) {
    public boolean shouldInclude() {
        return !"test".equals(scope);
    }
}

// Use text blocks for multi-line strings
String xml = """
    <?xml version="1.0" encoding="UTF-8"?>
    <projectDescription>
        <name>%s</name>
    </projectDescription>
    """.formatted(projectName);
```

### Naming Conventions
- **Classes**: PascalCase (e.g., `DependencyResolver`, `MavenClient`)
- **Interfaces**: PascalCase with -able/-er suffix when applicable
- **Methods**: camelCase, verbs (e.g., `resolve()`, `downloadArtifact()`)
- **Variables**: camelCase (e.g., `resolvedDeps`, `cacheDir`)
- **Constants**: UPPER_SNAKE_CASE (e.g., `MAX_PARENT_DEPTH`, `MAVEN_CENTRAL`)
- **Packages**: lowercase, no underscores (e.g., `jpm.deps`, `jpm.utils`)

### Package Structure
```
jpm/
├── cli/          # CLI commands (picocli-based)
├── config/       # Configuration parsing
├── deps/         # Dependency resolution
├── build/        # Build system
├── net/          # HTTP/network utilities
└── utils/        # General utilities
```

### Error Handling
```java
// Use checked exceptions for recoverable errors
try {
    var content = downloadPom(groupId, artifactId, version);
} catch (IOException e) {
    System.err.println("  Error: Failed to download POM: " + e.getMessage());
    return null;
}

// Use unchecked exceptions for programming errors
if (parts.length != 2) {
    throw new IllegalArgumentException("Invalid coordinate format: " + input);
}

// Always handle InterruptedException properly
try {
    process.waitFor();
} catch (InterruptedException e) {
    Thread.currentThread().interrupt();
    return new CompileResult(false, "Interrupted", 1);
}
```

### String Operations
- **Always** use `StandardCharsets.UTF_8` for byte conversions
- Use `String.isBlank()` instead of `trim().isEmpty()`
- Use `String.strip()` for Unicode-aware trimming (not `trim()`)

### Thread Safety
- Use `HttpClientManager.getClient()` for shared HTTP clients
- Mark shared collections as `final` and use thread-safe types when needed
- Use concurrent collections (`ConcurrentHashMap`) for multi-threaded access

### Comments
```java
// Single line for implementation details

/**
 * Javadoc for public APIs.
 * Use @param and @return for parameters and return values.
 */
public List<Dependency> resolveDependencies(String pomContent) throws Exception

// Inline comments for complex logic
// Sort by longest key first to avoid partial substitutions
```

### Prohibited
- ❌ Wildcard static imports
- ❌ `System.out.print` in library code (use `System.err` for errors)
- ❌ `printStackTrace()` (use proper logging)
- ❌ `null` returns without documentation
- ❌ Raw types (use generics)
- ❌ Tabs for indentation

### Dependencies
- picocli (CLI framework)
- toml4j (TOML parsing)
- gson (JSON, required by toml4j)

## Testing Guidelines

When adding tests:
1. Place in `src/test/java` with package matching source
2. Use JUnit 5 with `@Test` annotations
3. Name test methods descriptively: `shouldResolveDependencies()`
4. Use `assertThrows()` for exception testing
5. Use temp directories via `@TempDir` annotation
6. Test edge cases: empty inputs, nulls, malformed data
