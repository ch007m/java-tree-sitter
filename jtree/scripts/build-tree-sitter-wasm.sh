#!/bin/bash
set -euo pipefail

# Clones the tree-sitter repository, installs the required WASM tooling on macOS
# via Homebrew (if not already present), and builds the tree-sitter core library
# as a WASM module using the Makefile from tree-sitter-asm/.

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
MAKEFILE_SRC="$PROJECT_ROOT/scripts/Makefile"
CLONE_DIR="$PROJECT_ROOT/lib/tree-sitter"
BUILD_DIR="$CLONE_DIR/lib/wasm"

# ---------------------------------------------------------------------------
# 1. Verify the source Makefile exists
# ---------------------------------------------------------------------------
if [ ! -f "$MAKEFILE_SRC" ]; then
    echo "ERROR: Makefile not found at $MAKEFILE_SRC"
    exit 1
fi

# ---------------------------------------------------------------------------
# 2. Install WASM tools via Homebrew (macOS only, skip if already installed)
# ---------------------------------------------------------------------------
if [ "$(uname -s)" = "Darwin" ]; then
    echo "--- Checking Homebrew dependencies ---"

    # Ensure Homebrew itself is available
    if ! command -v brew &>/dev/null; then
        echo "ERROR: Homebrew is not installed. Install it from https://brew.sh"
        exit 1
    fi

    BREW_PACKAGES=("wasm-tools" "wabt")

    for pkg in "${BREW_PACKAGES[@]}"; do
        if brew list "$pkg" &>/dev/null; then
            echo "  $pkg: already installed"
        else
            echo "  $pkg: installing..."
            brew install "$pkg"
        fi
    done
else
    echo "WARNING: Not running on macOS — skipping Homebrew dependency check."
    echo "         Ensure wasm-tools and wabt are available on your PATH."
fi

# ---------------------------------------------------------------------------
# 3. Clone tree-sitter if not already present
# ---------------------------------------------------------------------------
if [ -d "$CLONE_DIR" ]; then
    echo "--- tree-sitter already cloned at $CLONE_DIR, pulling latest ---"
    git -C "$CLONE_DIR" pull --ff-only || echo "  (pull skipped — not on a tracking branch)"
else
    echo "--- Cloning tree-sitter ---"
    git clone https://github.com/tree-sitter/tree-sitter.git "$CLONE_DIR"
fi

# Verify the expected source layout exists
if [ ! -f "$CLONE_DIR/lib/src/lib.c" ]; then
    echo "ERROR: Expected source file not found at $CLONE_DIR/lib/src/lib.c"
    exit 1
fi

# ---------------------------------------------------------------------------
# 4. Set up the build directory and copy the Makefile
# ---------------------------------------------------------------------------
echo "--- Preparing build directory at $BUILD_DIR ---"
mkdir -p "$BUILD_DIR"
cp "$MAKEFILE_SRC" "$BUILD_DIR/Makefile"

# ---------------------------------------------------------------------------
# 5. Run the build
# ---------------------------------------------------------------------------
echo "--- Building tree-sitter WASM module ---"
make -C "$BUILD_DIR" release

echo ""
echo "--- Verifying the WASM module ---"
make -C "$BUILD_DIR" test

echo ""
echo "=== Build complete ==="
echo "Output: $BUILD_DIR/target/tree-sitter-final.wasm"
ls -lh "$BUILD_DIR/target/tree-sitter-final.wasm"

# ---------------------------------------------------------------------------
# 6. Copy WASM files to ~/Library/Java/Extensions/
# ---------------------------------------------------------------------------
JAVA_EXT_DIR="$HOME/Library/Java/Extensions"
echo ""
echo "--- Copying WASM files to $JAVA_EXT_DIR ---"
mkdir -p "$JAVA_EXT_DIR"
cp "$BUILD_DIR/target/"*.wasm "$JAVA_EXT_DIR/"
echo "Installed:"
ls -lh "$JAVA_EXT_DIR/"*.wasm