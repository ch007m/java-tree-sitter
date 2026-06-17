# Java Query Examples

## Class queries

### Find a class by exact name or name prefix

- **File:** [java/class/with-name.scm](java/class/with-name.scm)
- **Description:** Matches a class declaration by exact name (`#eq?`) or by prefix (`#match?`).

## Annotation queries

### Find an annotation by name

- **File:** [java/annotation/with-name.scm](java/annotation/with-name.scm)
- **Description:** Matches a marker annotation by its identifier (e.g., `@Entity`).

### Find an import used by an annotation

- **File:** [java/annotation/using-import.scm](java/annotation/using-import.scm)
- **Description:** Matches an import declaration for a specific fully-qualified annotation class (e.g., `jakarta.persistence.Entity`).

## Import queries

### Find all imported packages

- **File:** [java/import-packages/allPackages.scm](java/import-packages/allPackages.scm)
- **Description:** Captures the package scope and class name from every import declaration.

### Find an import by fully-qualified name

- **File:** [java/import-packages/fqname.scm](java/import-packages/fqname.scm)
- **Description:** Matches an import for an exact fully-qualified name (e.g., `org.springframework.ui.Model`).

### Find imports by wildcard prefix

- **File:** [java/import-packages/wildcard.scm](java/import-packages/wildcard.scm)
- **Description:** Matches imports whose path starts with a given prefix (e.g., `org.springframework.beans`).

## Combined queries

### Find all class names and method names

- **File:** [java/combined/class-method.scm](java/combined/class-method.scm)
- **Description:** Captures class and method identifiers in a single query file.