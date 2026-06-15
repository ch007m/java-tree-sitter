## Examples of tree-sitter queries

Execute the following bash commands at the root of this project
to play with "S-Expression" queries using tree-sitter client

```shell
# Query :: class Name = AppApplication
./queries/query.sh class/className.txt

# Query :: annotation = @Entity
./queries/query.sh annotation/name.txt

# Query :: all classes importing packages
./queries/query.sh import-packages/allPackages.txt

# Query :: search about import org.springframework.*
./queries/query.sh import-packages/fqname.txt
./queries/query.sh import-packages/wildcard.txt

# Combined query: search class name and method
./queries/query.sh combined/class-method.txt
```



