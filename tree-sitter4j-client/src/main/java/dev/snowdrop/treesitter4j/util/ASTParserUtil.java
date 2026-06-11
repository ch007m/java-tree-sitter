package dev.snowdrop.treesitter4j.util;

import io.roastedroot.treesitter.Language;
import io.roastedroot.treesitter.TreeSitter;
import io.roastedroot.treesitter.TreeSitterException;
import io.roastedroot.treesitter.TreeSitterParser;
import io.roastedroot.treesitter.TreeSitterTree;
import io.roastedroot.treesitter.ast.ASTExporter;
import io.roastedroot.treesitter.ast.ASTJsonSerializer;
import io.roastedroot.treesitter.ast.ASTTree;
import jakarta.annotation.Nonnull;
import org.eclipse.microprofile.config.ConfigProvider;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Shared utilities for parsing source files into AST trees and managing the AST JSON store.
 */
public final class ASTParserUtil {

    public static final String STORE_DIR = ".ts4j";

    private static TreeSitter treeSitterInstance;

    private ASTParserUtil() {}

    /**
     * Returns the shared singleton {@link TreeSitter} instance, created lazily on first access.
     */
    public static synchronized TreeSitter getTreeSitter() {
        if (treeSitterInstance == null) {
            treeSitterInstance = TreeSitter.create();
        }
        return treeSitterInstance;
    }

    /**
     * Find all source files under {@code root} that are recognized by {@link LanguageDetector},
     * excluding directories matching the configured patterns ({@code ts4j.parser.exclude-dirs}).
     */
    public static List<Path> findSourceFiles(Path root) throws IOException {
        List<String> excludePatterns = ConfigProvider.getConfig()
                .getOptionalValue("ts4j.parser.exclude-dirs", String.class)
                .map(s -> Arrays.stream(s.split(","))
                        .map(String::trim)
                        .filter(p -> !p.isEmpty())
                        .toList())
                .orElse(List.of());

        List<Path> files = new ArrayList<>();
        Files.walkFileTree(root, EnumSet.of(FileVisitOption.FOLLOW_LINKS),
                Integer.MAX_VALUE, new SimpleFileVisitor<>() {

                    @Override
                    public FileVisitResult preVisitDirectory(@Nonnull Path dir, @Nonnull BasicFileAttributes attrs) {
                        String name = dir.getFileName() != null ? dir.getFileName().toString() : "";
                        if (shouldExclude(name, excludePatterns)) {
                            return FileVisitResult.SKIP_SUBTREE;
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                        if (LanguageDetector.detect(file).isPresent()) {
                            files.add(file);
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(Path file, IOException exc) {
                        return FileVisitResult.CONTINUE;
                    }
                });
        return files;
    }

    /**
     * Check whether a directory name matches any of the given exclusion patterns.
     * Patterns ending with {@code *} are treated as prefix matches.
     */
    public static boolean shouldExclude(String name, List<String> patterns) {
        for (String pattern : patterns) {
            if (pattern.endsWith("*")) {
                if (name.startsWith(pattern.substring(0, pattern.length() - 1))) {
                    return true;
                }
            } else if (name.equals(pattern)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Parse all supported source files under {@code rootDir} and return the resulting AST trees in memory.
     *
     * @param rootDir the project root directory
     * @param logger  callback for progress/warning messages (may be {@code null})
     * @return list of parsed AST trees
     */
    public static List<ASTTree> parseDirectory(Path rootDir, Consumer<String> logger) throws IOException {
        List<Path> sourceFiles = findSourceFiles(rootDir);

        if (sourceFiles.isEmpty()) {
            if (logger != null) logger.accept("No supported source files found under " + rootDir);
            return List.of();
        }

        if (logger != null) logger.accept("Found " + sourceFiles.size() + " source file(s). Parsing...");

        List<ASTTree> trees = new ArrayList<>();
        int errorCount = 0;

        TreeSitter ts = getTreeSitter();
        try (TreeSitterParser parser = ts.newParser()) {

            for (Path file : sourceFiles) {
                Optional<Language> langOpt = LanguageDetector.detect(file);
                if (langOpt.isEmpty()) {
                    continue;
                }
                Language lang = langOpt.get();

                try {
                    String source = Files.readString(file);

                    try {
                        parser.setLanguage(lang);
                    } catch (TreeSitterException e) {
                        if (logger != null)
                            logger.accept("  WARN: language " + lang + " not supported at runtime, skipping " + relativize(rootDir, file));
                        continue;
                    }

                    try (TreeSitterTree tree = parser.parseString(source)) {
                        if (tree == null) {
                            if (logger != null)
                                logger.accept("  WARN: failed to parse " + relativize(rootDir, file));
                            errorCount++;
                            continue;
                        }

                        ASTTree ast = ASTExporter.export(tree, lang, source, relativize(rootDir, file));
                        trees.add(ast);
                    }
                } catch (Exception e) {
                    if (logger != null)
                        logger.accept("  ERROR parsing " + relativize(rootDir, file) + ": " + e.getMessage());
                    errorCount++;
                }
            }
        }

        if (logger != null && errorCount > 0) {
            logger.accept(errorCount + " file(s) failed to parse.");
        }

        return trees;
    }

    /**
     * Resolve a project root directory from a user-supplied path (full or relative).
     * Falls back to the current working directory if {@code appPath} is {@code null} or blank.
     */
    public static Path resolveAppDir(String appPath) {
        if (appPath != null && !appPath.isBlank()) {
            return Paths.get(appPath).toAbsolutePath().normalize();
        }
        return Paths.get("").toAbsolutePath();
    }

    /**
     * Check whether a persisted AST store exists and contains at least one JSON file.
     */
    public static boolean hasStore(Path rootDir) {
        Path storeDir = rootDir.resolve(STORE_DIR);
        if (!Files.isDirectory(storeDir)) {
            return false;
        }
        try (Stream<Path> walk = Files.walk(storeDir)) {
            return walk.anyMatch(p -> p.toString().endsWith(".json") && Files.isRegularFile(p));
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Load all AST trees from JSON files persisted under the store directory of {@code rootDir}.
     */
    public static List<ASTTree> loadStore(Path rootDir) throws IOException {
        Path storeDir = rootDir.resolve(STORE_DIR);
        List<ASTTree> trees = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(storeDir)) {
            List<Path> jsonFiles = walk
                    .filter(p -> p.toString().endsWith(".json"))
                    .filter(Files::isRegularFile)
                    .toList();

            for (Path jsonFile : jsonFiles) {
                try {
                    trees.add(ASTJsonSerializer.fromJson(jsonFile));
                } catch (IOException e) {
                    // Skip malformed files
                }
            }
        }
        return trees;
    }

    /**
     * Save a list of AST trees to the JSON store under {@code rootDir/.ts4j/},
     * grouped by language and named by SHA-256 hash of the source file path.
     */
    public static void saveToStore(List<ASTTree> trees, Path rootDir) throws IOException {
        Path storeDir = rootDir.resolve(STORE_DIR);
        Files.createDirectories(storeDir);

        for (ASTTree ast : trees) {
            String langName = ast.getLanguage() != null ? ast.getLanguage().toLowerCase() : "unknown";
            Path langDir = storeDir.resolve(langName);
            Files.createDirectories(langDir);
            String sha = sha256(ast.getSourceFile());
            Path jsonFile = langDir.resolve(sha + ".json");
            ASTJsonSerializer.toJson(ast, jsonFile);
        }
    }

    /**
     * Safely relativize a file path against a root, falling back to the absolute path on error.
     */
    public static String relativize(Path root, Path file) {
        try {
            return root.relativize(file).toString();
        } catch (IllegalArgumentException e) {
            return file.toString();
        }
    }

    /**
     * Compute a SHA-256 hex digest for the given input string.
     */
    public static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}