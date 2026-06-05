## Instructions

Install the tree-sitter lib using brew:
```shell
brew install tree-sitter-cli
brew install tree-sitter
```

Create a symlink link between the lib and ~/Library/Java/Extensions
```shell
 mkdir -p ~/Library/Java/Extensions
ln -s /opt/homebrew/Cellar/tree-sitter/0.26.9/lib/libtree-sitter.0.26.dylib ~/Library/Java/Extensions/libtree-sitter.dylib
```

Git clone the java grammar project and build the lib
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


Run the TreeSitterParser class
```shell
mvn exec:java
```
