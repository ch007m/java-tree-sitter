package dev.snowdrop.treesitter.jtree;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class NodeStore {

    public static final String STORE_FILENAME = "ast-store.json";
    private static final Path CONFIG_DIR = Path.of(System.getProperty("user.home"), ".aesh-tree-sitter");
    private static final Path LAST_PROJECT_FILE = CONFIG_DIR.resolve("jt-last-project");

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    public record NodeEntry(
            @JsonProperty("type") String type,
            @JsonProperty("text") String text,
            @JsonProperty("startLine") int startLine,
            @JsonProperty("endLine") int endLine
    ) {}

    public static class StoreData {
        @JsonProperty("rootPath")
        public String rootPath;

        @JsonProperty("parsedAt")
        public String parsedAt;

        @JsonProperty("files")
        public Map<String, List<NodeEntry>> files = new LinkedHashMap<>();

        public StoreData() {}

        public StoreData(String rootPath, String parsedAt) {
            this.rootPath = rootPath;
            this.parsedAt = parsedAt;
        }

        public void addFile(String relativePath, List<NodeEntry> nodes) {
            files.put(relativePath, nodes);
        }

        public int totalNodeCount() {
            return files.values().stream().mapToInt(List::size).sum();
        }
    }

    public static void save(StoreData data, Path outputFile) throws IOException {
        MAPPER.writeValue(outputFile.toFile(), data);
    }

    public static StoreData load(Path jsonFile) throws IOException {
        return MAPPER.readValue(jsonFile.toFile(), StoreData.class);
    }

    public static void saveLastProject(Path projectRoot) throws IOException {
        Files.createDirectories(CONFIG_DIR);
        Files.writeString(LAST_PROJECT_FILE, projectRoot.toAbsolutePath().toString());
    }

    public static Optional<Path> getLastProject() {
        try {
            if (Files.exists(LAST_PROJECT_FILE)) {
                String path = Files.readString(LAST_PROJECT_FILE).trim();
                if (!path.isEmpty()) {
                    return Optional.of(Path.of(path));
                }
            }
        } catch (IOException ignored) {
        }
        return Optional.empty();
    }

    public static Optional<Path> findStoreFile(Path projectRoot) {
        Path storeFile = projectRoot.resolve(STORE_FILENAME);
        return Files.exists(storeFile) ? Optional.of(storeFile) : Optional.empty();
    }
}
