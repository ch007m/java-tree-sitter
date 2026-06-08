package dev.snowdrop.treesitter.util;

import dev.snowdrop.treesitter.bonede.store.ParsedFile;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ASTStore {

    private static final ASTStore INSTANCE = new ASTStore();

    private final Map<Path, ParsedFile> files = new LinkedHashMap<>();
    private Path rootPath;

    private ASTStore() {}

    public static ASTStore getInstance() {
        return INSTANCE;
    }

    public void clear() {
        files.clear();
        rootPath = null;
    }

    public void setRootPath(Path rootPath) {
        this.rootPath = rootPath;
    }

    public Path getRootPath() {
        return rootPath;
    }

    public void addFile(Path path, ParsedFile parsed) {
        files.put(path, parsed);
    }

    public Collection<ParsedFile> getAllFiles() {
        return Collections.unmodifiableCollection(files.values());
    }

    public ParsedFile getFile(Path path) {
        return files.get(path);
    }

    public int getFileCount() {
        return files.size();
    }

    public boolean isEmpty() {
        return files.isEmpty();
    }
}
