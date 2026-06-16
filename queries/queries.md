## Examples of tree-sitter queries

Execute the following bash commands at the root of this project
to play with "S-Expression" queries using tree-sitter client.

**NOTE**: Install first the `tree-sitter` client and the java grammar and configure the client to point to the directory containing the 
grammars !

### Java queries

```shell
# Query :: class Name = AppApplication
./queries/query.sh java class/className.scm

# Query :: annotation = @Entity
./queries/query.sh java annotation/name.scm

# Query :: all classes importing packages
./queries/query.sh java import-packages/allPackages.scm

# Query :: search about import org.springframework.*
./queries/query.sh java import-packages/fqname.scm
./queries/query.sh java import-packages/wildcard.scm

# Combined query: search class name and method
./queries/query.sh java combined/class-method.scm
```

### Properties queries

```shell
# Query :: find key spring.datasource.url and its value
./queries/query.sh properties datasource.scm

# Query :: find all keys with prefix spring.datasource
./queries/query.sh properties datasource-prefix.scm
```

### pom.xml dependency queries

```shell
# Query :: find dependency: org.springframework.boot
./queries/query.sh xml dependency.scm
```

