# Properties Query Examples

### Find a specific property key and its value

- **File:** [properties/datasource.scm](properties/datasource.scm)
- **Description:** Matches a property entry by exact key (e.g., `spring.datasource.url`) and captures both the key and its value.

### Find all properties with a key prefix

- **File:** [properties/datasource-prefix.scm](properties/datasource-prefix.scm)
- **Description:** Matches all property entries whose key starts with a given prefix (e.g., `spring.datasource`).