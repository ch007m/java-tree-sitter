package dev.snowdrop.treesitter4j.util;

import dev.snowdrop.treesitter4j.TreeSitterRuntime;
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
import java.io.UncheckedIOException;
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
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Shared utilities for parsing source files into AST trees and managing the AST JSON store.
 */
public final class ASTParserUtil {

    public final String STORE_DIR = ".ts4j";
    private TreeSitter ts;

    /**
     * Holds a source file's path, detected language, pre-loaded content, and relative path.
     * Used to bulk-load file data during the directory walk so that parsing tasks
     * receive content directly without additional I/O.
     */
    record SourceFile(Path path, Language language, String content, String relativePath) {}

    public ASTParserUtil() {
        ts = TreeSitterRuntime.get();
    }

    /**
     * Find all source files under {@code root} that are recognized by {@link LanguageDetector},
     * excluding directories matching the configured patterns ({@code ts4j.parser.exclude-dirs}).
     */
    public List<Path> findSourceFiles(Path root) throws IOException {
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
     * Walk the directory tree and bulk-load all recognized source files in a single pass.
     * Each file's content is read during the walk so that downstream parsing tasks
     * receive pre-loaded data and perform no additional file I/O.
     */
    List<SourceFile> loadSourceFiles(Path root) throws IOException {
        List<String> excludePatterns = ConfigProvider.getConfig()
                .getOptionalValue("ts4j.parser.exclude-dirs", String.class)
                .map(s -> Arrays.stream(s.split(","))
                        .map(String::trim)
                        .filter(p -> !p.isEmpty())
                        .toList())
                .orElse(List.of());

        List<SourceFile> files = new ArrayList<>();
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
                        LanguageDetector.detect(file).ifPresent(lang -> {
                            try {
                                String content = Files.readString(file);
                                files.add(new SourceFile(file, lang, content, relativize(root, file)));
                            } catch (IOException e) {
                                // Skip unreadable files
                            }
                        });
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
    public boolean shouldExclude(String name, List<String> patterns) {
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
     * Parse all supported source files under {@code rootDir} in parallel using a fixed thread pool.
     * <p>
     * File contents are bulk-loaded during the directory walk so that parsing tasks
     * receive pre-loaded data and perform no additional file I/O.
     *
     * @param rootDir  the project root directory
     * @param logger   callback for progress/warning messages (may be {@code null})
     * @return list of parsed AST trees
     */
    public List<ASTTree> parseDirectory(Path rootDir, Consumer<String> logger) throws IOException {
        List<SourceFile> sourceFiles = loadSourceFiles(rootDir);

        if (sourceFiles.isEmpty()) {
            if (logger != null) logger.accept("No supported source files found under " + rootDir);
            return List.of();
        }

        if (logger != null) {
            logger.accept("Found " + sourceFiles.size() + " source file(s). Parsing in parallel ...");
        }

        AtomicInteger errorCount = new AtomicInteger();

        List<ASTTree> trees;

        int poolSize = Runtime.getRuntime().availableProcessors();
        try (ExecutorService executor = Executors.newFixedThreadPool(poolSize)) {
            // Submit all parsing tasks up front via stream, collecting futures eagerly
            List<Future<ASTTree>> futures = sourceFiles.stream()
                    .map(sf -> executor.submit(() -> {
                        try (TreeSitterParser parser = ts.newParser(sf.language());
                             TreeSitterTree tree = parser.parseString(sf.content())) {

                            if (tree == null) {
                                if (logger != null)
                                    logger.accept("  WARN: failed to parse " + sf.relativePath());
                                errorCount.incrementAndGet();
                                return null;
                            }

                            return ASTExporter.export(tree, sf.language(), sf.content(), sf.relativePath());
                        } catch (TreeSitterException e) {
                            if (logger != null)
                                logger.accept("  WARN: language " + sf.language() + " not supported at runtime, skipping " + sf.relativePath());
                            return null;
                        } catch (Exception e) {
                            e.printStackTrace();
                            if (logger != null)
                                logger.accept("  ERROR parsing " + sf.relativePath() + ": " + e.getMessage());
                            errorCount.incrementAndGet();
                            return null;
                        }
                    }))
                    .toList();

            // Collect results, filtering out nulls (failed parses)
            trees = futures.stream()
                    .map(future -> {
                        try {
                            return future.get();
                        } catch (ExecutionException | InterruptedException e) {
                            errorCount.incrementAndGet();
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .toList();
        }

        if (logger != null && errorCount.get() > 0) {
            logger.accept(errorCount.get() + " file(s) failed to parse.");
        }

        return trees;
    }

    /**
     * Resolve a project root directory from a user-supplied path (full or relative).
     * Falls back to the current working directory if {@code appPath} is {@code null} or blank.
     */
    public Path resolveAppDir(String appPath) {
        if (appPath != null && !appPath.isBlank()) {
            return Paths.get(appPath).toAbsolutePath().normalize();
        }
        return Paths.get("").toAbsolutePath();
    }

    /**
     * Check whether a persisted AST store exists and contains at least one JSON file.
     */
    public boolean hasStore(Path rootDir) {
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
    public List<ASTTree> loadStore(Path rootDir) throws IOException {
        Path storeDir = rootDir.resolve(STORE_DIR);
        int poolSize = Runtime.getRuntime().availableProcessors();
        try (Stream<Path> walk = Files.walk(storeDir);
             ExecutorService executor = Executors.newFixedThreadPool(poolSize)) {
            List<Future<ASTTree>> futures = walk
                    .filter(p -> p.toString().endsWith(".json"))
                    .filter(Files::isRegularFile)
                    .map(jsonFile -> executor.submit(() -> ASTJsonSerializer.fromJson(jsonFile)))
                    .toList();

            List<ASTTree> trees = new ArrayList<>(futures.size());
            for (Future<ASTTree> future : futures) {
                try {
                    trees.add(future.get());
                } catch (ExecutionException e) {
                    // Skip malformed files
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted while loading AST store", e);
                }
            }
            return trees;
        }
    }

    /**
     * Save a list of AST trees to the JSON store under {@code rootDir/.ts4j/},
     * grouped by language and named by SHA-256 hash of the source file path.
     * <p>
     * Language directories are pre-created in one batch, then all JSON files
     * are written in parallel using virtual threads (I/O-bound work).
     */
    public void saveToStore(List<ASTTree> trees, Path rootDir) throws IOException {
        if (trees.isEmpty()) {
            return;
        }

        Path storeDir = rootDir.resolve(STORE_DIR);
        Files.createDirectories(storeDir);

        // Pre-create all needed language directories in one pass
        Set<String> languages = trees.stream()
                .map(ast -> ast.getLanguage() != null ? ast.getLanguage().toLowerCase() : "unknown")
                .collect(Collectors.toSet());
        for (String lang : languages) {
            Files.createDirectories(storeDir.resolve(lang));
        }

        // Write all JSON files in parallel using virtual threads
        int poolSize = Runtime.getRuntime().availableProcessors();
        try (ExecutorService executor = Executors.newFixedThreadPool(poolSize)) {
            List<Future<?>> futures = new ArrayList<>();
            for (ASTTree ast : trees) {
                futures.add(executor.submit(() -> {
                    String langName = ast.getLanguage() != null ? ast.getLanguage().toLowerCase() : "unknown";
                    Path langDir = storeDir.resolve(langName);
                    String sha = sha256(ast.getSourceFile());
                    Path jsonFile = langDir.resolve(sha + ".json");
                    try {
                        ASTJsonSerializer.toJson(ast, jsonFile);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }));
            }

            for (Future<?> future : futures) {
                try {
                    future.get();
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause();
                    if (cause instanceof UncheckedIOException uio) {
                        throw uio.getCause();
                    }
                    throw new IOException("Failed to save AST JSON", cause);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted while saving AST store", e);
                }
            }
        }
    }

    /**
     * Safely relativize a file path against a root, falling back to the absolute path on error.
     */
    public String relativize(Path root, Path file) {
        try {
            return root.relativize(file).toString();
        } catch (IllegalArgumentException e) {
            return file.toString();
        }
    }

    /**
     * Compute a SHA-256 hex digest for the given input string.
     */
    public String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}