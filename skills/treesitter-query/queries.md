## Examples of tree-sitter queries

Execute the following bash command under the folder: `treesitter-query/examples`
to play with "S-Expression" queries using tree-sitter client.

**NOTE**: Install first the `tree-sitter` client and the java grammar and configure the client to point to the directory containing the 
grammars !

### Java queries

```shell
# Query :: class Name = AppApplication
./scripts/query.sh java class/with-name.scm

# Query :: annotation = @Entity
./scripts/query.sh java annotation/name.scm

# Query :: all classes importing packages
./scripts/query.sh java import-packages/allPackages.scm

# Query :: search about import org.springframework.*
./scripts/query.sh java import-packages/fqname.scm
./scripts/query.sh java import-packages/wildcard.scm

# Combined query: search class name and method
./scripts/query.sh java combined/class-method.scm
```

### Properties queries

```shell
# Query :: find key spring.datasource.url and its value
./scripts/query.sh properties datasource.scm

# Query :: find all keys with prefix spring.datasource
./scripts/query.sh properties datasource-prefix.scm
```

### pom.xml dependency queries

```shell
# Query :: find dependency: org.springframework.boot
./scripts/query.sh xml dependency.scm
```

