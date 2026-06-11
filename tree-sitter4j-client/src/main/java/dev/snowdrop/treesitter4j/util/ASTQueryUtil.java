package dev.snowdrop.treesitter4j.util;

import io.roastedroot.treesitter.Language;
import io.roastedroot.treesitter.TreeSitter;
import io.roastedroot.treesitter.TreeSitterException;
import io.roastedroot.treesitter.TreeSitterParser;
import io.roastedroot.treesitter.TreeSitterQuery;
import io.roastedroot.treesitter.TreeSitterQueryResult;
import io.roastedroot.treesitter.TreeSitterTree;
import io.roastedroot.treesitter.ast.ASTNode;
import io.roastedroot.treesitter.ast.ASTTree;

import dev.snowdrop.treesitter4j.util.lang.JavaAliases;
import dev.snowdrop.treesitter4j.util.lang.LanguageAliases;
import dev.snowdrop.treesitter4j.util.lang.PomAliases;
import dev.snowdrop.treesitter4j.util.lang.PropertiesAliases;
import dev.snowdrop.treesitter4j.util.lang.XmlAliases;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Query utility that maps human-friendly type aliases to tree-sitter S-expression
 * query patterns and executes them via the tree-sitter query API.
 * <p>
 * Each alias is associated with a specific {@link Language}, so only files belonging
 * to that language are searched. Raw tree-sitter node types are also supported and
 * the language is auto-detected from a dynamic type-to-language map built from loaded AST trees.
 * <p>
 * <b>S-expression grammar references:</b>
 * <ul>
 *   <li>Java: <a href="https://github.com/tree-sitter/tree-sitter-java/blob/master/src/node-types.json">tree-sitter-java node types</a></li>
 *   <li>Properties: <a href="https://github.com/tree-sitter-grammars/tree-sitter-properties/blob/main/src/node-types.json">tree-sitter-properties node types</a></li>
 *   <li>XML: <a href="https://github.com/tree-sitter-grammars/tree-sitter-xml/blob/master/xml/src/node-types.json">tree-sitter-xml node types</a></li>
 *   <li>S-expression query syntax: <a href="https://tree-sitter.github.io/tree-sitter/using-parsers/queries/1-syntax.html">tree-sitter query documentation</a></li>
 * </ul>
 */
public final class ASTQueryUtil {

    /**
     * Composes a filterable value from raw captured text.
     * Returns the composed value, or {@code null} to skip the match entirely.
     */
    @FunctionalInterface
    public interface ValueComposer {
        String compose(String capturedText);
    }

    /** Describes a human-friendly alias for a tree-sitter node type. */
    public record AliasInfo(String nodeType, String queryPattern, Language language, ValueComposer composer) {
        /** Convenience constructor for aliases without a composer. */
        public AliasInfo(String nodeType, String queryPattern, Language language) {
            this(nodeType, queryPattern, language, null);
        }
    }

    /** A parsed query expression (alias, optional operator and value, resolved alias info). */
    public record ParsedQuery(String alias, String operator, String value, AliasInfo aliasInfo) {}

    /** A single query match result. */
    public record QueryMatch(String sourceFile, int line, String alias, String matchedText) {}

    // ---------------------------------------------------------------------------
    // Alias dictionary: friendly name -> (node type, S-expression pattern, language)
    // Built by aggregating per-language LanguageAliases implementations.
    // ---------------------------------------------------------------------------

    private static final List<LanguageAliases> LANGUAGE_ALIASES = List.of(
            new JavaAliases(),
            new PropertiesAliases(),
            new XmlAliases(),
            new PomAliases()
    );

    private static final Map<String, AliasInfo> ALIASES;

    static {
        Map<String, AliasInfo> m = new LinkedHashMap<>();
        for (LanguageAliases la : LANGUAGE_ALIASES) {
            m.putAll(la.aliases());
        }
        ALIASES = Collections.unmodifiableMap(m);
    }

    private ASTQueryUtil() {}

    /** Returns the read-only alias dictionary. */
    public static Map<String, AliasInfo> getAliases() {
        return ALIASES;
    }

    // ---------------------------------------------------------------------------
    // Query parsing
    // ---------------------------------------------------------------------------

    /**
     * Parse a human-friendly query expression into its components.
     * <p>Supported forms:
     * <ul>
     *   <li>{@code class}                         &ndash; list all classes</li>
     *   <li>{@code class = MyApp}                 &ndash; exact match on name</li>
     *   <li>{@code property = quarkus.db.*}       &ndash; glob/wildcard match ({@code *} matches any characters)</li>
     *   <li>{@code class contains App}            &ndash; case-insensitive substring match</li>
     *   <li>{@code class_declaration}             &ndash; raw node type (auto-detect language)</li>
     * </ul>
     */
    public static ParsedQuery parseQuery(String rawInput) {
        String trimmed = rawInput.trim();

        // Check for "contains" operator (with surrounding spaces)
        int containsIdx = trimmed.toLowerCase().indexOf(" contains ");
        if (containsIdx > 0) {
            String alias = trimmed.substring(0, containsIdx).trim().toLowerCase();
            String value = normalizeQueryValue(alias, trimmed.substring(containsIdx + " contains ".length()).trim());
            return new ParsedQuery(alias, "contains", value, ALIASES.get(alias));
        }

        // Check for "=" operator
        int eqIdx = trimmed.indexOf('=');
        if (eqIdx > 0) {
            String alias = trimmed.substring(0, eqIdx).trim().toLowerCase();
            String value = normalizeQueryValue(alias, trimmed.substring(eqIdx + 1).trim());
            return new ParsedQuery(alias, "=", value, ALIASES.get(alias));
        }

        // No operator — plain type/alias
        String alias = trimmed.toLowerCase();
        return new ParsedQuery(alias, null, null, ALIASES.get(alias));
    }

    // ---------------------------------------------------------------------------
    // Query execution
    // ---------------------------------------------------------------------------

    /**
     * Execute a parsed query against the given AST trees using the tree-sitter query API.
     *
     * @param query            the parsed query expression
     * @param trees            AST trees to search (from store or freshly parsed)
     * @param fileFilter       optional file-path substring filter (may be {@code null})
     * @param languageOverride if non-null, overrides the automatic language detection.
     *                         Pass {@code EnumSet.allOf(Language.class)} to search all languages.
     * @return list of matches
     */
    public static List<QueryMatch> execute(ParsedQuery query, List<ASTTree> trees,
                                           String fileFilter, Set<Language> languageOverride) {
        List<QueryMatch> matches = new ArrayList<>();
        TreeSitter ts = ASTParserUtil.getTreeSitter();

        // Determine query pattern and target languages
        String queryPattern;
        Set<Language> targetLanguages;

        if (query.aliasInfo() != null) {
            // Known alias — use its pattern and language
            queryPattern = query.aliasInfo().queryPattern();
            targetLanguages = EnumSet.of(query.aliasInfo().language());
        } else {
            // Raw node type — generic pattern, auto-detect language from trees
            queryPattern = "(" + query.alias() + ") @name";
            Map<String, Set<Language>> typeLanguageMap = buildTypeLanguageMap(trees);
            Set<Language> fromMap = typeLanguageMap.get(query.alias());
            targetLanguages = fromMap != null ? EnumSet.copyOf(fromMap) : EnumSet.allOf(Language.class);
        }

        // Apply explicit language override
        if (languageOverride != null) {
            targetLanguages = languageOverride;
        }

        // Group trees by language, applying file filter
        Map<Language, List<ASTTree>> treesByLang = new LinkedHashMap<>();
        for (ASTTree tree : trees) {
            Language lang = resolveLanguage(tree);
            if (lang == null || !targetLanguages.contains(lang)) continue;
            if (fileFilter != null && !fileFilter.isBlank()
                    && (tree.getSourceFile() == null || !tree.getSourceFile().contains(fileFilter))) {
                continue;
            }
            treesByLang.computeIfAbsent(lang, k -> new ArrayList<>()).add(tree);
        }

        // Execute query for each language group
        try (TreeSitterParser parser = ts.newParser()) {
            for (var entry : treesByLang.entrySet()) {
                Language lang = entry.getKey();

                try {
                    parser.setLanguage(lang);
                } catch (TreeSitterException e) {
                    continue; // language not available at runtime
                }

                TreeSitterQuery tsQuery;
                try {
                    tsQuery = ts.newQuery(lang, queryPattern);
                } catch (TreeSitterException e) {
                    // Pattern not valid for this language — skip entire group
                    continue;
                }

                try {
                    for (ASTTree astTree : entry.getValue()) {
                        String source = astTree.getSourceCode();
                        if (source == null || source.isEmpty()) continue;

                        try (TreeSitterTree tree = parser.parseString(source)) {
                            if (tree == null) continue;

                            List<TreeSitterQueryResult> results = tsQuery.exec(tree.rootNode(), source);

                            for (TreeSitterQueryResult result : results) {
                                if (!"name".equals(result.name())) continue;

                                String text = source.substring(
                                        result.node().startByte(), result.node().endByte());

                                // Apply value composer if present (e.g. GAV composition for POM aliases)
                                if (query.aliasInfo() != null && query.aliasInfo().composer() != null) {
                                    text = query.aliasInfo().composer().compose(text);
                                    if (text == null) continue;
                                }

                                if (!matchesFilter(text, query.operator(), query.value())) continue;

                                int line = byteToLine(source, result.node().startByte());
                                matches.add(new QueryMatch(
                                        astTree.getSourceFile() != null ? astTree.getSourceFile() : "<unknown>",
                                        line, query.alias(), text));
                            }
                        }
                    }
                } finally {
                    tsQuery.close();
                }
            }
        }

        return matches;
    }

    // ---------------------------------------------------------------------------
    // Type-to-language dictionary (built dynamically from loaded trees)
    // ---------------------------------------------------------------------------

    /**
     * Build a map of every named node type to the set of languages in which it appears.
     * This is used to auto-detect the language when the user queries a raw node type
     * instead of a friendly alias.
     */
    public static Map<String, Set<Language>> buildTypeLanguageMap(List<ASTTree> trees) {
        Map<String, Set<Language>> map = new HashMap<>();
        for (ASTTree tree : trees) {
            Language lang = resolveLanguage(tree);
            if (lang == null) continue;
            collectNodeTypes(tree.getRoot(), lang, map);
        }
        return map;
    }

    /**
     * Resolve the {@link Language} enum from an {@link ASTTree}'s language string.
     */
    public static Language resolveLanguage(ASTTree tree) {
        if (tree.getLanguage() == null) return null;
        try {
            return Language.valueOf(tree.getLanguage().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // ---------------------------------------------------------------------------
    // Internal helpers
    // ---------------------------------------------------------------------------

    private static void collectNodeTypes(ASTNode node, Language lang,
                                         Map<String, Set<Language>> map) {
        if (node == null) return;
        if (node.getType() != null && node.isNamed()) {
            map.computeIfAbsent(node.getType(), k -> EnumSet.noneOf(Language.class)).add(lang);
        }
        if (node.getChildren() != null) {
            for (ASTNode child : node.getChildren()) {
                collectNodeTypes(child, lang, map);
            }
        }
    }

    /**
     * Normalize the query value for a given alias.
     * For annotation queries, strips the leading {@code @} since tree-sitter
     * captures only the identifier name (e.g. {@code Entity}, not {@code @Entity}).
     */
    private static String normalizeQueryValue(String alias, String value) {
        if ("annotation".equals(alias) && value != null && value.startsWith("@")) {
            return value.substring(1);
        }
        return value;
    }

    private static boolean matchesFilter(String text, String operator, String value) {
        if (operator == null || value == null) return true;
        return switch (operator) {
            case "=" -> value.contains("*") ? matchesGlob(text, value) : text.equals(value);
            case "contains" -> text.toLowerCase().contains(value.toLowerCase());
            default -> true;
        };
    }

    /**
     * Match text against a glob pattern where {@code *} matches any sequence of characters.
     * <p>Examples: {@code quarkus.db.*} matches {@code quarkus.db.kind} and {@code quarkus.db.url}.
     */
    private static boolean matchesGlob(String text, String globPattern) {
        // Convert glob to regex: escape regex-special chars, replace * with .*
        String regex = globToRegex(globPattern);
        return Pattern.matches(regex, text);
    }

    private static String globToRegex(String glob) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < glob.length(); i++) {
            char c = glob.charAt(i);
            if (c == '*') {
                sb.append(".*");
            } else if ("\\[]{}().|^$+?".indexOf(c) >= 0) {
                sb.append('\\').append(c);
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Convert a byte offset in a source string to an approximate 1-based line number.
     */
    public static int byteToLine(String source, int byteOffset) {
        if (source == null || byteOffset <= 0) return 1;
        int line = 1;
        int len = Math.min(byteOffset, source.length());
        for (int i = 0; i < len; i++) {
            if (source.charAt(i) == '\n') line++;
        }
        return line;
    }
}
