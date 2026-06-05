package dev.snowdrop.treesitter.bonede.store;

import org.treesitter.TSParser;
import org.treesitter.TSTree;
import org.treesitter.TreeSitterJava;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Persists and restores the {@link ASTStore} to/from a JSON file.
 *
 * <p>Since tree-sitter {@code TSTree} objects are native and cannot be serialized,
 * only the root path and file paths are saved. On load, source files are re-read
 * from disk and re-parsed with tree-sitter.</p>
 */
public final class ASTStorePersistence {

    private static final String STORE_FILE_NAME = ".ast-store.json";
    private static final Path LAST_PROJECT_FILE =
            Path.of(System.getProperty("user.home"), ".aesh-tree-sitter", "last-project");

    private ASTStorePersistence() {}

    /**
     * Returns the store file path for a given project root.
     */
    public static Path storeFile(Path rootPath) {
        return rootPath.resolve(STORE_FILE_NAME);
    }

    /**
     * Save the current store to {@code <rootPath>/.ast-store.json}.
     *
     * @return the path to the written file
     */
    public static Path save(ASTStore store) throws IOException {
        Path rootPath = store.getRootPath();
        if (rootPath == null) {
            throw new IOException("Store has no root path set");
        }

        Path outFile = storeFile(rootPath);

        try (BufferedWriter writer = Files.newBufferedWriter(outFile)) {
            writer.write("{\n");
            writer.write("  \"rootPath\": \"" + jsonEscape(rootPath.toString()) + "\",\n");
            writer.write("  \"files\": [\n");

            List<ParsedFile> allFiles = new ArrayList<>(store.getAllFiles());
            for (int i = 0; i < allFiles.size(); i++) {
                String path = allFiles.get(i).getFilePath().toString();
                writer.write("    \"" + jsonEscape(path) + "\"");
                if (i < allFiles.size() - 1) {
                    writer.write(",");
                }
                writer.write("\n");
            }

            writer.write("  ]\n");
            writer.write("}\n");
        }

        return outFile;
    }

    /**
     * Load a store from {@code <projectRoot>/.ast-store.json}.
     *
     * <p>Re-reads source files from disk and re-parses them with tree-sitter.
     * Files that no longer exist or fail to parse are skipped.</p>
     *
     * @return a {@link LoadResult} with counts of succeeded/failed/missing files
     */
    public static LoadResult load(Path projectRoot) throws IOException {
        Path storeFile = storeFile(projectRoot);
        if (!Files.isRegularFile(storeFile)) {
            throw new IOException("No saved store found at " + storeFile);
        }

        String rootPathStr = null;
        List<String> filePaths = new ArrayList<>();

        // Simple JSON parsing — the format is controlled by save()
        try (BufferedReader reader = Files.newBufferedReader(storeFile)) {
            String line;
            boolean inFilesArray = false;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("\"rootPath\"")) {
                    rootPathStr = extractJsonStringValue(line);
                } else if (line.startsWith("\"files\"")) {
                    inFilesArray = true;
                } else if (inFilesArray && line.startsWith("\"")) {
                    String value = line;
                    if (value.endsWith(",")) {
                        value = value.substring(0, value.length() - 1);
                    }
                    // Strip surrounding quotes
                    if (value.startsWith("\"") && value.endsWith("\"")) {
                        value = jsonUnescape(value.substring(1, value.length() - 1));
                    }
                    filePaths.add(value);
                } else if (inFilesArray && line.equals("]")) {
                    inFilesArray = false;
                }
            }
        }

        if (rootPathStr == null) {
            throw new IOException("Malformed store file: missing rootPath");
        }

        Path rootPath = Path.of(rootPathStr);
        ASTStore store = ASTStore.getInstance();
        store.clear();
        store.setRootPath(rootPath);

        TSParser parser = new TSParser();
        parser.setLanguage(new TreeSitterJava());

        int succeeded = 0;
        int failed = 0;
        int missing = 0;

        for (String fp : filePaths) {
            Path javaFile = Path.of(fp);
            if (!Files.isRegularFile(javaFile)) {
                missing++;
                continue;
            }
            try {
                String source = Files.readString(javaFile);
                TSTree tree = parser.parseString(null, source);
                if (tree == null) {
                    failed++;
                    continue;
                }
                store.addFile(javaFile, new ParsedFile(javaFile, source, tree));
                succeeded++;
            } catch (IOException e) {
                failed++;
            }
        }

        return new LoadResult(succeeded, failed, missing, filePaths.size());
    }

    /**
     * Checks whether a saved store exists for the given project root.
     */
    public static boolean exists(Path projectRoot) {
        return Files.isRegularFile(storeFile(projectRoot));
    }

    /**
     * Remember the last project root so that {@code ts query} can auto-load it.
     * Writes to {@code ~/.aesh-tree-sitter/last-project}.
     */
    public static void saveLastProject(Path projectRoot) throws IOException {
        Files.createDirectories(LAST_PROJECT_FILE.getParent());
        Files.writeString(LAST_PROJECT_FILE, projectRoot.toAbsolutePath().normalize().toString());
    }

    /**
     * Returns the last project root that was parsed/loaded, or {@code null} if none.
     */
    public static Path getLastProject() {
        if (!Files.isRegularFile(LAST_PROJECT_FILE)) {
            return null;
        }
        try {
            String content = Files.readString(LAST_PROJECT_FILE).trim();
            if (content.isEmpty()) {
                return null;
            }
            Path project = Path.of(content);
            return exists(project) ? project : null;
        } catch (IOException e) {
            return null;
        }
    }

    private static String extractJsonStringValue(String line) {
        // Format: "key": "value"  or  "key": "value",
        int colonIdx = line.indexOf(':');
        if (colonIdx < 0) return null;
        String value = line.substring(colonIdx + 1).trim();
        if (value.endsWith(",")) {
            value = value.substring(0, value.length() - 1).trim();
        }
        if (value.startsWith("\"") && value.endsWith("\"")) {
            return jsonUnescape(value.substring(1, value.length() - 1));
        }
        return value;
    }

    private static String jsonEscape(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private static String jsonUnescape(String s) {
        return s.replace("\\t", "\t")
                .replace("\\r", "\r")
                .replace("\\n", "\n")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }

    public record LoadResult(int succeeded, int failed, int missing, int total) {}
}