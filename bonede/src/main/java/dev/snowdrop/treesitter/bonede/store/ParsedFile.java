package dev.snowdrop.treesitter.bonede.store;

import org.treesitter.TSTree;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

public class ParsedFile {

    private final Path filePath;
    private final String sourceCode;
    private final TSTree tree;

    public ParsedFile(Path filePath, String sourceCode, TSTree tree) {
        this.filePath = filePath;
        this.sourceCode = sourceCode;
        this.tree = tree;
    }

    public Path getFilePath() {
        return filePath;
    }

    public String getSourceCode() {
        return sourceCode;
    }

    public TSTree getTree() {
        return tree;
    }

    /**
     * Extract the substring corresponding to a node's byte range.
     * Tree-sitter uses byte offsets into UTF-8 encoded source.
     */
    public String extractText(int startByte, int endByte) {
        byte[] bytes = sourceCode.getBytes(StandardCharsets.UTF_8);
        return new String(bytes, startByte, endByte - startByte, StandardCharsets.UTF_8);
    }
}
