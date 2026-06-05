# Instructions

Install the tree-sitter lib and client using brew
```shell
brew install tree-sitter-cli
brew install tree-sitter
```

## Using dylib

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

- As we cannot build the `tree-sitter` project like the languages, then we will use a skill:
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
**Remark**: The tree-sitter core lib has been installed using the brew command !

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
