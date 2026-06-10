package dev.snowdrop.treesitter4j.util;

import io.roastedroot.treesitter.Language;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;

public final class LanguageDetector {

    private LanguageDetector() {}

    /**
     * Detect the tree-sitter {@link Language} from a file's extension.
     */
    public static Optional<Language> detect(Path file) {
        String filename = file.getFileName().toString();
        String ext = filename.substring(filename.lastIndexOf(".") + 1)
                .toLowerCase(Locale.ROOT);
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
