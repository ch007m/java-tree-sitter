# Java tree-sitter client

Interactive CLI for parsing source code with [tree-sitter](https://tree-sitter.github.io/) and querying the resulting AST. Built with [Aesh](https://github.com/aeshell/aesh) and [Quarkus](https://quarkus.io/).

The project contains three modules, each using a different Java tree-sitter library to explore the trade-offs between JNI bindings, managed language packs, and pure-Java WASM runtimes.

## Prerequisites

- Maven 3.9+
- JDK version depends on the module (see below)

## Modules

| Module         | Library                                                                                                                                       | Binding             | Languages                              |
|----------------|-----------------------------------------------------------------------------------------------------------------------------------------------|---------------------|----------------------------------------|
| `bonede`       | [tree-sitter-ng](https://github.com/bonede/tree-sitter-ng) (`io.github.bonede`)                                                               | JNI                 | Java                                   |
| `languagepack` | [tree-sitter-language-pack](https://github.com/kreuzberg-dev/tree-sitter-language-pack) (`dev.kreuzberg`)                                     | JNI + auto-download | Java, YAML, JSON, XML, HTML, JS, ...   |
| `jtree`        | [jtreesitter](https://github.com/tree-sitter/java-tree-sitter) (`io.github.tree-sitter`) + [Chicory](https://github.com/dylibso/chicory) WASM | Pure Java (WASM)    | Java, YAML, JSON, Properties, HTML, JS |

### bonede

Uses the **bonede tree-sitter-ng** JNI bindings which ship pre-built native libraries for each platform. Each language grammar is a separate Maven artifact (e.g. `tree-sitter-java`).

- Parses `.java` files under a directory and builds an in-memory AST store
- Query the AST with predefined query names or raw S-expression patterns
- Persist/reload parsed projects to disk

**Requirements:**

| Item     | Version / Details                                                                                    |
|----------|------------------------------------------------------------------------------------------------------|
| JDK      | 21+                                                                                                  |
| Platform | macOS (aarch64/x86_64), Linux (x86_64/aarch64), Windows -- native libs bundled in the Maven artifact |

```bash
# Build and run
mvn clean install -pl bonede
java -jar bonede/target/aesh-tree-sitter-bonede-1.0.0-SNAPSHOT-runner.jar
```
Using jbang
```
jbang app install --force --name ng dev.snowdrop:aesh-tree-sitter-bonede:1.0.0-SNAPSHOT:runner

jbang app install --force --name lp dev.snowdrop:aesh-tree-sitter-languagepack:1.0.0-SNAPSHOT:runner
ng query classes
ng query "(method_declaration name: (identifier) @name)"
```

**Predefined queries:** `classes`, `methods`, `constructors`, `imports`, `fields`, `interfaces`, `enums`, `annotations`, `packages`, `strings`, `method-calls`

| Option           | Short          | Description                        |
|------------------|----------------|------------------------------------|
| `--limit N`      | `-l N`         | Max results to display             |
| `--file <filter>`| `-f <filter>`  | Filter by file path (substring)    |
| `--name <name>`  | `-n <name>`    | Filter annotations by name         |
| `--list-queries` | `-L`           | List predefined query names        |

### languagepack

Uses the **Kreuzberg tree-sitter-language-pack** which bundles grammars for many languages in a single dependency with the help of JDK Panama. Language detection is automatic based on file extension, and grammars are downloaded on first use.

- Polyglot: supports Java, YAML, JSON, XML, Properties, Markdown, HTML, JavaScript
- Automatic language detection from file paths
- Extracts structural items (classes, methods, imports) per language

**Requirements:**

| Item     | Version / Details                                                   |
|----------|---------------------------------------------------------------------|
| JDK      | 25+                                                                 |
| Network  | Required on first run to download grammar native libraries          |

```bash
mvn clean install -pl languagepack
java -jar languagepack/target/aesh-tree-sitter-languagepack-1.0.0-SNAPSHOT-runner.jar

jbang app install --force --name lp dev.snowdrop:aesh-tree-sitter-languagepack:1.0.0-SNAPSHOT:runner

lp parse /pat/to/project
lp query annotation
```

### jtree

Uses the official **jtreesitter** FFI bindings together with the **Chicory** WebAssembly runtime to run tree-sitter entirely in pure Java -- no native C libraries or JNI required. The tree-sitter core is compiled to WASM and then AOT-compiled to Java bytecode by the Chicory compiler plugin. Language grammars are loaded as standalone `.wasm` files and dynamically linked at runtime.

The module is split into two submodules:

- **`jtree/wasm`** -- Compiles the tree-sitter core C library (`tree-sitter-final.wasm`) into a Java class (`TreeSitterModule`) using the `chicory-compiler-maven-plugin`.
- **`jtree/jtree-parser`** -- The parser application. `WasmParserRegistry` implements an Emscripten-style dynamic linker that parses each grammar's `dylink.0` section, allocates shared memory/table space, and provides C library stubs so grammar side-modules can be instantiated via Chicory's `Store`.

**Requirements:**

| Item               | Version / Details                                                                    |
|--------------------|--------------------------------------------------------------------------------------|
| JDK                | 21+ (`jtree-parser`), 25+ (`wasm` submodule)                                        |
| Build-time tools   | [WASI SDK](https://github.com/WebAssembly/wasi-sdk), [Binaryen](https://github.com/WebAssembly/binaryen) (`wasm-opt`), [tree-sitter CLI](https://github.com/tree-sitter/tree-sitter/blob/master/cli/README.md) -- only needed to rebuild the `.wasm` files from C source |
| Runtime            | Grammar `.wasm` files in `~/Library/Java/Extensions/`                                |
| Native libraries   | None -- pure Java                                                                    |

The build scripts under `jtree/scripts/` automate WASM compilation:

- `build-tree-sitter-wasm.sh` -- compiles the tree-sitter core C library to `tree-sitter-final.wasm`
- `build-grammar-dylib-wasm.sh` -- compiles language grammars (Java, YAML, JSON, etc.) to `.wasm` side-modules using `tree-sitter build --wasm`

```bash
# Build (must build from jtree/ or root to include the wasm submodule)
mvn clean install -pl jtree/wasm,jtree/jtree-parser

# Run the demo app
mvn exec:java -pl jtree/jtree-parser
```

## Build all modules

```bash
mvn clean install
```