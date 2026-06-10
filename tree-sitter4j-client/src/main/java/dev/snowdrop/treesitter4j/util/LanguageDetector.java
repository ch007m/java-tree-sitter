package dev.snowdrop.treesitter4j.util;

import io.roastedroot.treesitter.Language;

import java.nio.file.Path;
import java.util.Optional;

public final class LanguageDetector {

    private LanguageDetector() {}

    /**
     * Detect the tree-sitter {@link Language} from a file's extension.
     */
    public static Optional<Language> detect(Path file) {
        String name = file.getFileName().toString();
        int dot = name.lastIndexOf('.');
        if (dot < 0) {
            return Optional.empty();
        }
        String ext = name.substring(dot + 1).toLowerCase();
        return switch (ext) {
            case "java" -> Optional.of(Language.JAVA);
            case "json" -> Optional.of(Language.JSON);
            case "yaml", "yml" -> Optional.of(Language.YAML);
            case "xml" -> Optional.of(Language.XML);
            case "html", "htm" -> Optional.of(Language.HTML);
            case "properties" -> Optional.of(Language.PROPERTIES);
            case "md", "markdown" -> Optional.of(Language.MARKDOWN);
            default -> Optional.empty();
        };
    }
}
