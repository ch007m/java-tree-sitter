#!/bin/bash

# Exit immediately if a command exits with a non-zero status
set -e

# --- Pre-flight Verification Check ---
# 'command -v' is the standard, shell-builtin way to check for binaries (safer than 'which')
if ! command -v tree-sitter &> /dev/null; then
    echo "❌ Error: The 'tree-sitter' CLI client is not installed or not in your PATH."
    echo "💡 To install it on macOS, please run: brew install tree-sitter"
    exit 1
fi

# --- Configuration & Arguments ---
APP_PATH="$(pwd)/examples/spring-boot-todo-app"
LANGUAGE="${1:-java}"          # Defaults to 'java' if not provided
QUERY_FILE_NAME="${2:-query.scm}" # Defaults to 'query.scm' if not provided

# Resolve the absolute path to your query selector
QUERY_PATH="$(pwd)/queries/$LANGUAGE/$QUERY_FILE_NAME"

echo "=========================================================="
echo "🚀 Initializing macOS-Safe Tree-Sitter Workspace Scan"
echo "Target Application: $APP_PATH"
echo "Language Engine:   $LANGUAGE"
echo "Query Matrix File: $QUERY_PATH"
echo "=========================================================="

# Validate that the targets exist
if [ ! -f "$QUERY_PATH" ]; then
    echo "❌ Error: Query file not found at '$QUERY_PATH'"
    exit 1
fi

if [ ! -d "$APP_PATH" ]; then
    echo "❌ Error: Application path '$APP_PATH' does not exist."
    exit 1
fi

# --- Step 1 & 2: Discover and Group Files via native 'find' ---
declare -a JAVA_FILES=()
declare -a PROPERTY_FILES=()
declare -a XML_FILES=()
declare -a YAML_FILES=()
declare -a MARKDOWN_FILES=()
declare -a HTML_FILES=()
declare -a MISC_FILES=()

while IFS= read -r file; do
    if [ -f "$file" ]; then
        # Extract file extension in lowercase
        ext="${file##*.}"
        ext=$(echo "$ext" | tr '[:upper:]' '[:lower:]')

        case "$ext" in
            java)             JAVA_FILES+=("$file") ;;
            properties)       PROPERTY_FILES+=("$file") ;;
            xml)              XML_FILES+=("$file") ;;
            yaml|yml)         YAML_FILES+=("$file") ;;
            md|markdown)      MARKDOWN_FILES+=("$file") ;;
            html|htm)         HTML_FILES+=("$file") ;;
            *)                MISC_FILES+=("$file") ;;
        esac
    fi
done < <(find "$APP_PATH" -type f)

# --- Step 3 & 4: Select and Iterate over the chosen group ---
# Replacing 'declare -n' pointer logic with safe manual copies
# to protect backward compatibility with macOS Bash 3.2.
declare -a ACTIVE_SELECTION=()

case "$LANGUAGE" in
    java)               ACTIVE_SELECTION=("${JAVA_FILES[@]}") ;;
    properties)         ACTIVE_SELECTION=("${PROPERTY_FILES[@]}") ;;
    xml)                ACTIVE_SELECTION=("${XML_FILES[@]}") ;;
    yaml|yml)           ACTIVE_SELECTION=("${YAML_FILES[@]}") ;;
    markdown|md)        ACTIVE_SELECTION=("${MARKDOWN_FILES[@]}") ;;
    html)               ACTIVE_SELECTION=("${HTML_FILES[@]}") ;;
    *)
        echo "⚠️ Warning: Language '$LANGUAGE' unknown. Defaulting sweep to un-grouped misc files."
        ACTIVE_SELECTION=("${MISC_FILES[@]}")
        ;;
esac

echo "🔍 Categorization complete. Detected file count in active bucket: ${#ACTIVE_SELECTION[@]}"

if [ ${#ACTIVE_SELECTION[@]} -eq 0 ]; then
    echo "ℹ️ No source files found matching the category criteria for '$LANGUAGE'."
    exit 0
fi

echo "🧠 Executing parser evaluations..."
echo "----------------------------------------------------------"

for source_file in "${ACTIVE_SELECTION[@]}"; do
    echo "📖 Processing: $source_file"
    set -x
    # Run tree-sitter query (suppressing path configuration warnings)
    tree-sitter query "$QUERY_PATH" "$source_file" 2>/dev/null || true
    set +x
    echo "----------------------------------------------------------"
done

echo "✅ Scan complete."