Can # Tree-sitter S-Expression Query Generator

## Trigger

Use this skill when the user asks to **create**, **generate**, or **write** a Tree-sitter query (`.scm` file) for a supported language.

## Objective

Convert a user-friendly, natural-language query description into a valid Tree-sitter **S-Expression** query that can be saved as a `.scm` file and executed by a Tree-sitter client.

---

## Step 1 — Ask the user

Before generating anything, ask the user the following two questions using the `AskUserQuestion` tool:

1. **Grammar language** — Which language grammar should the query target?
   Options: `java`, `properties`, `xml`, `html`, `json`, `yaml`, `markdown`

2. **User-friendly query** — Describe in plain English what you want to find.
   Examples:
   - "Find all classes annotated with @RestController"
   - "Find the property `quarkus.datasource.db-kind` and its value"
   - "Find all `<dependency>` elements with groupId `io.quarkus`"
   - "Find all headings in the markdown document"
   - "Find all keys in a JSON object"

---

## Step 2 — Look up the grammar

Before writing the S-Expression, you **must** read the `grammar.js` file for the chosen language to understand the exact node types, field names, and tree structure. Use `WebFetch` to retrieve the raw `grammar.js` content from the URL in the table below.

### Supported grammars

| Grammar      | Repository                                                        | `grammar.js` URL (raw)                                                                                              |
|--------------|-------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------|
| **java**     | [tree-sitter-java](https://github.com/tree-sitter/tree-sitter-java) | https://raw.githubusercontent.com/tree-sitter/tree-sitter-java/master/grammar.js                                     |
| **properties** | [tree-sitter-properties](https://github.com/tree-sitter-grammars/tree-sitter-properties) | https://raw.githubusercontent.com/tree-sitter-grammars/tree-sitter-properties/master/grammar.js                      |
| **xml**      | [tree-sitter-xml (panicinc)](https://github.com/panicinc/tree-sitter-xml) | https://raw.githubusercontent.com/panicinc/tree-sitter-xml/main/tree-sitter-xml/grammar.js                           |
| **html**     | [tree-sitter-html](https://github.com/tree-sitter/tree-sitter-html) | https://raw.githubusercontent.com/tree-sitter/tree-sitter-html/master/grammar.js                                     |
| **json**     | [tree-sitter-json](https://github.com/tree-sitter/tree-sitter-json) | https://raw.githubusercontent.com/tree-sitter/tree-sitter-json/master/grammar.js                                     |
| **yaml**     | [tree-sitter-yaml](https://github.com/tree-sitter-grammars/tree-sitter-yaml) | https://raw.githubusercontent.com/tree-sitter-grammars/tree-sitter-yaml/master/grammar.js                            |
| **markdown** | [tree-sitter-markdown](https://github.com/tree-sitter-grammars/tree-sitter-markdown) | https://raw.githubusercontent.com/tree-sitter-grammars/tree-sitter-markdown/split_parser/tree-sitter-markdown/grammar.js |

---

## Step 3 — Query syntax reference

The Tree-sitter query language uses **S-Expressions** (Lisp-like patterns) to match nodes in the syntax tree. Consult these references for the full specification:

- **Basic syntax**: https://tree-sitter.github.io/tree-sitter/using-parsers/queries/1-syntax.html
- **Operators**: https://tree-sitter.github.io/tree-sitter/using-parsers/queries/2-operators.html
- **Predicates & directives**: https://tree-sitter.github.io/tree-sitter/using-parsers/queries/3-predicates-and-directives.html

### Quick syntax cheat sheet

| Concept              | Syntax                                                | Description                                          |
|----------------------|-------------------------------------------------------|------------------------------------------------------|
| Match a node type    | `(class_declaration)`                                 | Matches any `class_declaration` node                 |
| Named field          | `(class_declaration name: (identifier) @class.name)`  | Matches the `name` field and captures it             |
| Capture              | `@capture.name`                                       | Binds a node to a name for extraction                |
| Exact match          | `(#eq? @name "Foo")`                                  | Predicate: captured text must equal `"Foo"`          |
| Regex match          | `(#match? @name "^Foo")`                              | Predicate: captured text must match the regex        |
| Optional             | `(node)?`                                             | The node is optional (zero or one)                   |
| Wildcard child       | `(_)`                                                 | Matches any single named node                        |
| Anchor first child   | `(parent . (child))`                                  | Child must be the first named child                  |
| Anchor last child    | `(parent (child) .)`                                  | Child must be the last named child                   |
| Anchor sibling       | `(parent (child1) . (child2))`                        | child2 must immediately follow child1                |
| Alternation          | `[(node_a) (node_b)]`                                 | Match either node_a or node_b                        |
| Negation             | `(#not-eq? @name "Bar")`                              | Predicate: captured text must NOT equal `"Bar"`      |
| Outer capture        | `(class_declaration ...) @whole.class`                 | Captures the entire enclosing node                   |

---

## Step 4 — Generate the query

Using the grammar node types from `grammar.js` and the syntax rules above, produce a valid `.scm` query. Follow these conventions:

### Output format

```scheme
; <plain-English description of what this query does>
; Grammar: <grammar name> — <grammar repository URL>
<S-Expression query>
```

### Conventions

1. **Always add a comment header** describing the query purpose and the grammar used.
2. **Use meaningful capture names** with dotted notation (e.g., `@class.name`, `@import.fqn`, `@tag.dep`).
3. **Use `#eq?` for exact matches** and `#match?` for regex/prefix/suffix patterns.
4. **Use `?` for optional elements** (e.g., an optional `<version>` inside a `<dependency>`).
5. **Use outer captures** (e.g., `@target.dependency`) when the user needs the entire matched block.
6. **Test the query mentally** against a sample AST to verify correctness before presenting it.

---

## Examples

Below are real-world examples from this project to illustrate the expected output format and patterns.

### Java — Find a class by name

```scheme
; Search a class having as name: AppApplication
(class_declaration
  name: (identifier) @class.name
  (#eq? @class.name "AppApplication"))
```

### Java — Find a class name starting with a prefix

```scheme
; Search a class name starting with "App"
(class_declaration
  name: (identifier) @class.name
  (#match? @class.name "^App")
) @Class.name
```

### Java — Find all class names and method names (combined)

```scheme
(class_declaration
  name: (identifier) @class.name)

(method_declaration
  name: (identifier) @method.name)
```

### Java — Find an annotation by name

```scheme
; Search about Entity annotation => @Entity
(marker_annotation
  name: (identifier) @annotation.name
  (#eq? @annotation.name "Entity")
)
```

### Java — Find an import by fully-qualified name

```scheme
(import_declaration
  (scoped_identifier) @import.fqn
  (#eq? @import.fqn "org.springframework.ui.Model")
) @fqn.import
```

### Properties — Find a specific property key and its value

```scheme
(property
  (key) @key
  (value) @value
  (#eq? @key "spring.datasource.url"))
```

### Properties — Find all properties with a key prefix

```scheme
(property
  (key) @key
  (value) @value
  (#match? @key "^spring\\.datasource"))
```

### XML — Find dependency elements with a specific groupId

```scheme
; Grammar used: https://github.com/panicinc/tree-sitter-xml
; Search about Spring Boot dependencies
((element
   (start_tag (tag_name) @tag.dep (#eq? @tag.dep "dependency"))
   (element
     (start_tag (tag_name) @tag.g (#eq? @tag.g "groupId"))
     (text) @group.id)
   (element
     (start_tag (tag_name) @tag.a (#eq? @tag.a "artifactId"))
     (text) @artifact.id)

   ;; Optional modifier (?) applied to the whole version element block
   (element
     (start_tag (tag_name) @tag.v (#eq? @tag.v "version"))
     (text) @version.value)?
   )
  (#eq? @group.id "org.springframework.boot")
  ) @target.dependency
```

---

## Step 5 — Present the result

1. Show the generated `.scm` query to the user in a fenced code block with `scheme` syntax highlighting.
2. If the user wants to save it, write it to the appropriate path under the project's `queries/<language>/` directory.
3. Suggest a descriptive filename based on the query purpose (e.g., `annotation/with-rest-controller.scm`).