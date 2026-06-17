# XML Query Examples

These examples target Maven `pom.xml` files. Two grammar variants are covered:

- **tree-sitter-xml** (`tree-sitter-grammars/tree-sitter-xml`) — uses `STag`, `Name`, `content`, `CharData` node types.
- **panicinc/tree-sitter-xml** (deprecated) — uses `start_tag`, `tag_name`, `text` node types.

## tree-sitter-grammars/tree-sitter-xml

### Find dependency elements

- **File:** [xml/dependency.scm](xml/dependency.scm)
- **Description:** Matches `<dependency>` elements and captures `groupId` and `artifactId`.

### Find parent or dependency version by groupId

- **File:** [xml/project-version.scm](xml/project-version.scm)
- **Description:** Matches `<parent>` or `<dependency>` elements with a specific `groupId` and captures the optional `<version>`.

## panicinc/tree-sitter-xml (deprecated)

### Find dependency elements with a specific groupId

- **File:** [xml/panincinc/dependency.scm](xml/panincinc/dependency.scm)
- **Description:** Matches `<dependency>` elements filtered by `groupId` (e.g., `org.springframework.boot`), with optional `<version>`.

### Find parent or dependency version by groupId

- **File:** [xml/panincinc/project-version.scm](xml/panincinc/project-version.scm)
- **Description:** Matches `<parent>` or `<dependency>` elements for a specific `groupId` and captures `artifactId` and optional `version`.