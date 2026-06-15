#!/bin/bash

QUERY=$1
TARGET_DIR=~/code/quarkus/rewrite-mtool/_others/treesitter/aesh-tree-sitter
JAVA_SOURCE_PATH="$TARGET_DIR/examples/spring-boot-todo-app/src/main/java/"

find $JAVA_SOURCE_PATH -type f -name "*.java" | while read -r file; do
    echo "Processing: $file"
    if [ -f "$file" ]; then
        echo "Processing Java File: $file"
        cat ./examples/queries/$QUERY | tree-sitter query /dev/stdin "$file"
    fi
done