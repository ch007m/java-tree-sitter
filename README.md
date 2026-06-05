# aesh-tree-sitter

Interactive CLI tool for parsing Java source code with [tree-sitter](https://tree-sitter.github.io/) and querying the resulting AST graph. Built with [Aesh](https://github.com/aeshell/aesh) and [tree-sitter-ng](https://github.com/bonede/tree-sitter-ng).

## Prerequisites

- Java 21+
- Maven 3.9+

## Build

```bash
mvn clean package
```

This produces a fat JAR at `target/aesh-tree-sitter-1.0.0-SNAPSHOT.jar`.

## Run

```bash
java -jar target/aesh-tree-sitter-1.0.0-SNAPSHOT.jar
```

You will see an interactive prompt:

```
[ast]$
```

### Run with JBang

Install the CLI as a JBang app using the local Maven snapshot:

```bash
jbang app install --name ts dev.snowdrop:aesh-tree-sitter:1.0.0-SNAPSHOT:runner
```

Then run it:

```bash
ast
```

## Commands

### parse

Parse all `.java` files under a directory and build an in-memory AST graph.

```
[ast]$ parse /path/to/java-project
Found 42 Java file(s). Parsing...
Parsing complete: 42 succeeded, 0 failed.
AST store now holds 42 file(s).
```

The path can be relative or absolute. Hidden directories (`.git`, `.idea`, etc.) are skipped automatically.

### query

Query the parsed AST using predefined query names or raw tree-sitter S-expression patterns.

**Using a predefined query:**

```
[ast]$ query classes
  src/main/java/com/example/App.java:5:1  @class.name [identifier] = App
  src/main/java/com/example/Service.java:3:1  @class.name [identifier] = Service

2 match(es) found across 42 file(s).
```

**Using a raw S-expression pattern:**

```
[ast]$ query "(method_declaration name: (identifier) @name)"
  src/main/java/com/example/App.java:8:5  @name [identifier] = main

1 match(es) found across 42 file(s).
```

**List all predefined queries:**

```
[ast]$ query --list-queries
Predefined queries:
  classes        - Find all class declarations with their names
  methods        - Find all method declarations with names and return types
  constructors   - Find all constructor declarations
  imports        - Find all import declarations
  fields         - Find all field declarations
  interfaces     - Find all interface declarations
  enums          - Find all enum declarations
  annotations    - Find all annotation usages
  packages       - Find package declarations
  strings        - Find all string literals
  method-calls   - Find all method invocations
```

**Options:**

| Option | Short | Description |
|--------|-------|-------------|
| `--limit N` | `-l N` | Maximum number of results to display |
| `--file <filter>` | `-f <filter>` | Filter results by file path (substring match) |
| `--name <name>` | `-n <name>` | Filter annotations by name (simple name matches as suffix) |
| `--list-queries` | `-L` | List all predefined query names |

**Examples:**

```
[ast]$ query -f Service methods
[ast]$ query -l 10 classes
[ast]$ query "(annotation name: (identifier) @ann.name) @ann"
```

**Find annotations by name:**

```
[ast]$ query --name Entity annotations
[ast]$ query -n Entity
[ast]$ query -n javax.persistence.Entity
[ast]$ query -n RestController -f Controller
```

When using `--name`, the query pattern defaults to `annotations` if omitted.

### exit

Exit the interactive shell.

```
[ast]$ exit
```
