package dev.snowdrop.treesitter4j;

import dev.snowdrop.treesitter4j.util.LanguageDetector;
import io.roastedroot.treesitter.Language;
import io.roastedroot.treesitter.TreeSitter;
import io.roastedroot.treesitter.TreeSitterException;
import io.roastedroot.treesitter.TreeSitterParser;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Manages the singleton {@link TreeSitter} runtime instance and pre-created parsers
 * for all supported languages.
 */
public class TreeSitterRuntime {

    private static TreeSitter instance;
    private static Map<Language, TreeSitterParser> parsers;

    private TreeSitterRuntime() {}

    /**
     * Returns the shared {@link TreeSitter} singleton, creating it on first call.
     * Also pre-creates parsers for all supported languages.
     */
    public static TreeSitter get() {
        TreeSitter ts = instance;
        if (ts == null) {
            ts = TreeSitter.create();
            instance = ts;
            parsers = createParserMap(ts);
        }
        return ts;
    }

    /**
     * Returns the pre-created parser for the given language, or {@code null}
     * if the language is not supported at runtime.
     */
    public static TreeSitterParser getParser(Language language) {
        get(); // ensure singleton is initialized
        return parsers.get(language);
    }

    /**
     * Returns the pre-created parser map (read-only).
     */
    public static Map<Language, TreeSitterParser> getParsers() {
        get(); // ensure singleton is initialized
        return parsers;
    }

    private static Map<Language, TreeSitterParser> createParserMap(TreeSitter ts) {
        Map<Language, TreeSitterParser> map = new EnumMap<>(Language.class);
        for (Language lang : LanguageDetector.supportedLanguages()) {
            try {
                map.put(lang, ts.newParser(lang));
            } catch (TreeSitterException e) {
                // Language not available at runtime, skip
            }
        }
        return map;
    }
}