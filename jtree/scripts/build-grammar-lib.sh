#!/bin/bash
set -e

# Define the languages you want to compile
LANGUAGES=("java" "yaml" "json" "properties" "html" "javascript" "markdown")

# CRITICAL: Capture the absolute starting path right away so pwd shifts do not break paths later
ROOT_DIR="$(pwd)"
OUTPUT_DIR="$ROOT_DIR/lib/wasm_dylib_output"
BUILD_TEMP="$ROOT_DIR/lib/build_temp"

# Ensure output folder exists
mkdir -p "$OUTPUT_DIR"

# Clean and recreate temporary build space
rm -rf "$BUILD_TEMP"
mkdir -p "$BUILD_TEMP"
cd "$BUILD_TEMP"

for LANG in "${LANGUAGES[@]}"; do
    echo "========================================="
    echo "Processing Grammar: tree-sitter-$LANG"
    echo "========================================="

    # Handle naming quirks for repositories hosted under different GitHub organizations
    if [[ "$LANG" == "yaml" || "$LANG" == "properties" || "$LANG" == "markdown" ]]; then
        REPO_URL="https://github.com/tree-sitter-grammars/tree-sitter-$LANG.git"
    else
        REPO_URL="https://github.com/tree-sitter/tree-sitter-$LANG.git"
    fi

    # Clone the target repository cleanly
    git clone --depth=1 "$REPO_URL" "repo-$LANG"
    cd "repo-$LANG"

    # Fixed syntax spacing error [[ ... ]]
    if [[ "$LANG" == "markdown" ]]; then

        cd tree-sitter-markdown
        tree-sitter build
        tree-sitter build --wasm

        # Rename generically named artifacts so they don't overwrite each other
        mv tree-sitter-markdown.dylib "$OUTPUT_DIR/tree-sitter-markdown.dylib" 2>/dev/null || mv *.dylib "$OUTPUT_DIR/tree-sitter-markdown.dylib"
        mv tree-sitter-markdown.wasm "$OUTPUT_DIR/tree-sitter-markdown.wasm" 2>/dev/null || mv *.wasm "$OUTPUT_DIR/tree-sitter-markdown.wasm"
        cd ..

        # 2. Compile inline markdown inline text parser
        cd tree-sitter-markdown-inline
        tree-sitter build
        tree-sitter build --wasm

        # Prevent files from overwriting the main block markdown outputs
        mv tree-sitter-markdown-inline.dylib "$OUTPUT_DIR/tree-sitter-markdown-inline.dylib" 2>/dev/null || mv *.dylib "$OUTPUT_DIR/tree-sitter-markdown-inline.dylib"
        mv tree-sitter-markdown-inline.wasm "$OUTPUT_DIR/tree-sitter-markdown-inline.wasm" 2>/dev/null || mv *.wasm "$OUTPUT_DIR/tree-sitter-markdown-inline.wasm"
        cd ..

    else
        # For standard languages, compile directly
        tree-sitter build
        tree-sitter build --wasm

        # Safely move the output dylib and name it appropriately
        mv *.dylib "$OUTPUT_DIR/tree-sitter-$LANG.dylib"

        # Find the output .wasm file and give it its correct language prefix name
        # (This standardizes the files so tree-sitter.wasm becomes tree-sitter-java.wasm)
        if [ -f tree-sitter-$LANG.wasm ]; then
            mv tree-sitter-$LANG.wasm "$OUTPUT_DIR/"
        else
            mv *.wasm "$OUTPUT_DIR/tree-sitter-$LANG.wasm"
        fi
    fi

    # Step back out to the base build directory and flush the current repository cache
    cd "$BUILD_TEMP"
    rm -rf "repo-$LANG"
done

# Clean up and return to where the script was triggered from
cd "$ROOT_DIR"
rm -rf "$BUILD_TEMP"

echo "========================================="
echo "Success! All compiled assets are located in: $OUTPUT_DIR"
ls -la "$OUTPUT_DIR"