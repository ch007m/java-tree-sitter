# Java tree-sitter client

Interactive CLI for parsing source code with [tree-sitter](https://tree-sitter.github.io/) and querying the resulting AST. Built with [Aesh](https://github.com/aeshell/aesh) and [Quarkus](https://quarkus.io/).

The project contains three modules, each using a different Java tree-sitter library to explore the trade-offs between JNI bindings, managed language packs, and pure-Java WASM runtimes.

## Prerequisites

- Maven 3.9+
- JDK version depends on the module (see below)

## Modules

| Module              | Library                                                                                                        | Binding             | Languages                                       |
|---------------------|----------------------------------------------------------------------------------------------------------------|---------------------|--------------------------------------------------|
| `tree-sitter4j-client` | [tree-sitter4j](https://github.com/roastedroot/tree-sitter4j) (`io.roastedroot`) + [Chicory](https://github.com/dylibso/chicory) WASM | Pure Java (WASM)    | Java, YAML, JSON, Properties, HTML, XML, Markdown |
| `bonede`            | [tree-sitter-ng](https://github.com/bonede/tree-sitter-ng) (`io.github.bonede`)                                | JNI                 | Java                                             |
| `languagepack`      | [tree-sitter-language-pack](https://github.com/kreuzberg-dev/tree-sitter-language-pack) (`dev.kreuzberg`)      | JNI + auto-download | Java, YAML, JSON, XML, HTML, JS, ...             |

### tree-sitter4j client

Uses the **[tree-sitter4j](https://github.com/roastedroot/tree-sitter4j)** library which runs tree-sitter entirely in pure Java -- no native C libraries or JNI required. The tree-sitter core and language grammars are compiled to WASM and executed via the [Chicory](https://github.com/dylibso/chicory) WebAssembly runtime.

- Polyglot: supports Java, YAML, JSON, XML, Properties, HTML, Markdown
- Automatic language detection from file extension
- Parses source files and exports AST trees as JSON (using `ASTExporter` / `ASTJsonSerializer`)
- Query persisted AST nodes by type, file path, or text content

**Requirements:**

| Item     | Version / Details  |
|----------|--------------------|
| JDK      | 21+                |
| Runtime  | None -- pure Java  |

```bash
# Build and run
mvn clean install -pl tree-sitter4j-client
java -jar tree-sitter4j-client/target/tree-sitter4j-client-1.0.0-SNAPSHOT-runner.jar
```

Using jbang
```
jbang app install --force --name ts4j dev.snowdrop:tree-sitter4j-client:1.0.0-SNAPSHOT:runner

ts4j parse /path/to/project
ts4j query class_declaration --store /path/to/project
ts4j query import_declaration --file MyClass --store /path/to/project
ts4j types
ts4j types -L java
```

| Option             | Short          | Description                              |
|--------------------|----------------|------------------------------------------|
| `--store <path>`   | `-s <path>`    | Path to the project root with .ast-store |
| `--file <filter>`  | `-f <filter>`  | Filter by file path (substring)          |
| `--text <text>`    | `-t <text>`    | Filter by node text (case-insensitive)   |
| `--list-types`     | `-l`           | List all distinct node types             |

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

ng parse /path/to/project
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

## Build all modules

```bash
mvn clean install
```

## To be reviewed


Install the wasm, tree-sitter lib and client using brew
```shell
brew install tree-sitter-cli
brew install tree-sitter
brew install binaryen
```

### Using dylib

- Create the following path `mkdir -p ~/Library/Java/Extensions`
- Add a symlink link between the tree-sitter lib and ~/Library/Java/Extensions
```shell
ln -s /opt/homebrew/Cellar/tree-sitter/0.26.9/lib/libtree-sitter.0.26.dylib ~/Library/Java/Extensions/libtree-sitter.dylib
```
- For the grammar/language to be used on your machine, git clone and build the dylib
- Execute the following bash script able to git clone and build the needed grammars dylib and wasm
```shell
./scripts/build-grammar-lib.sh
# The dylib will be created under: lib/wasm_dylib_output
# cp them to: ~/Library/Java/Extensions
cp ./lib/wasm_dylib_output/*.dylib ~/Library/Java/Extensions
cp ./lib/wasm_dylib_output/*.wasm ~/Library/Java/Extensions
```

### Build tree-sitter wasm

**Remark**: The tree-sitter core lib has been installed using the brew command !

As we cannot build the `tree-sitter` project like the languages, then we can build/install it using a skill:
```shell
git clone https://github.com/tree-sitter/tree-sitter.git "lib/tree-sitter"
cd "lib/tree-sitter/lib"

# To build the wasm file, we need the help of a skill
git clone https://github.com/andreaTP/skill-compile-to-wasm.git
cp -r skill-compile-to-wasm ~/.claude/skills/compile-to-wasm
claude "Build this tree-sitter project to wasm"

# When done
cp *.wasm ~/Library/Java/Extensions/
```
OR

using the following bash script
```shell
./scripts/build-tree-sitter-wasm.sh
```

- If you prefer to do it manually, following these instructions
```shell
git clone https://github.com/tree-sitter/tree-sitter-java & cd tree-sitter-java
make all
cp libtree-sitter-java.* ~/Library/Java/Extensions/
```
and many more
```shell
git clone https://github.com/tree-sitter-grammars/tree-sitter-yaml.git
cd tree-sitter-yaml
make all
cp libtree-sitter-yaml.* ~/Library/Java/Extensions/

git clone https://github.com/tree-sitter-grammars/tree-sitter-properties.git
cd tree-sitter-properties
make
cp libtree-sitter-properties.* ~/Library/Java/Extensions/

git clone https://github.com/tree-sitter-grammars/tree-sitter-markdown.git
cd tree-sitter-markdown/
make
cp tree-sitter-markdown-inline/libtree-sitter-markdown-inline.* ~/Library/Java/Extensions/
cp tree-sitter-markdown/libtree-sitter-markdown.* ~/Library/Java/Extensions/

git clone https://github.com/tree-sitter/tree-sitter-html.git
cd tree-sitter-html
make all
cp libtree-sitter-html.* ~/Library/Java/Extensions/
```
