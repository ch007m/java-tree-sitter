package dev.snowdrop.treesitter4j.command;

import dev.snowdrop.treesitter4j.util.LanguageDetector;
import io.roastedroot.treesitter.Language;
import io.roastedroot.treesitter.TreeSitterPool;
import io.roastedroot.treesitter.ast.ASTExporter;
import io.roastedroot.treesitter.ast.ASTJsonSerializer;
import io.roastedroot.treesitter.ast.ASTTree;
import jakarta.annotation.Nonnull;
import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Argument;
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
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

@CommandDefinition(name = "parse", description = "Parse files from a directory and persist AST nodes as JSON files under the store")
public class ParseCommand implements Command<CommandInvocation> {

    private static final String STORE_DIR = ".ts4j";

    @Argument(description = "Path to the project directory to parse", required = true)
    private String projectPath;

    @Override
    public CommandResult execute(CommandInvocation invocation) {
        long startTime = System.nanoTime();

        Path rootDir = Paths.get(projectPath).toAbsolutePath().normalize();
        if (!Files.isDirectory(rootDir)) {
            invocation.println("Error: '" + projectPath + "' is not a valid directory.");
            return CommandResult.FAILURE;
        }

        // Collect supported files grouped by language
        Map<Language, List<Path>> sourceFilesByLanguage;
        try {
            sourceFilesByLanguage = findSourceFiles(rootDir);
        } catch (IOException e) {
            invocation.println("Error walking directory: " + e.getMessage());
            return CommandResult.FAILURE;
        }

        if (sourceFilesByLanguage.isEmpty()) {
            invocation.println("No supported source files found under " + rootDir);
            return CommandResult.SUCCESS;
        }

        int totalFiles = sourceFilesByLanguage.values().stream().mapToInt(List::size).sum();
        invocation.println("Found " + totalFiles + " source file(s) in " + sourceFilesByLanguage.size() + " language(s). Parsing...");

        // Prepare output directory
        Path storeDir = rootDir.resolve(STORE_DIR);
        try {
            Files.createDirectories(storeDir);
        } catch (IOException e) {
            invocation.println("Error creating store directory: " + e.getMessage());
            return CommandResult.FAILURE;
        }

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger errorCount = new AtomicInteger();

        // Create language directories upfront (single-threaded, cheap)
        for (Language lang : sourceFilesByLanguage.keySet()) {
            try {
                Files.createDirectories(storeDir.resolve(lang.name().toLowerCase()));
            } catch (IOException e) {
                invocation.println("  ERROR creating language directory for " + lang + ": " + e.getMessage());
            }
        }

        // Flatten all (language, file) pairs into a list of tasks
        record ParseTask(Language lang, Path file) {}
        List<ParseTask> tasks = new ArrayList<>();
        for (var entry : sourceFilesByLanguage.entrySet()) {
            for (Path file : entry.getValue()) {
                tasks.add(new ParseTask(entry.getKey(), file));
            }
        }

        int poolSize = Runtime.getRuntime().availableProcessors();
        try (var pool = new TreeSitterPool(poolSize);
             ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {

            List<Future<?>> futures = new ArrayList<>(tasks.size());
            for (ParseTask task : tasks) {
                futures.add(executor.submit(() -> {
                    try {
                        pool.execute(ts -> {
                            try (var parser = ts.newParser()) {
                                parser.setLanguage(task.lang());
                                String source = Files.readString(task.file());

                                try (var tree = parser.parseString(source)) {
                                    if (tree == null) {
                                        invocation.println("  WARN: failed to parse " + relativize(rootDir, task.file()));
                                        errorCount.incrementAndGet();
                                        return;
                                    }

                                    ASTTree ast = ASTExporter.export(tree, task.lang(), source, relativize(rootDir, task.file()));

                                    String relPath = rootDir.relativize(task.file()).toString();
                                    String sha = sha256(relPath);
                                    Path langDir = storeDir.resolve(task.lang().name().toLowerCase());
                                    Path jsonFile = langDir.resolve(sha + ".json");
                                    ASTJsonSerializer.toJson(ast, jsonFile);

                                    successCount.incrementAndGet();
                                }
                            } catch (Exception e) {
                                invocation.println("  ERROR parsing " + relativize(rootDir, task.file()) + ": " + e.getMessage());
                                errorCount.incrementAndGet();
                            }
                        });
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        invocation.println("  ERROR: interrupted while parsing " + relativize(rootDir, task.file()));
                        errorCount.incrementAndGet();
                    }
                }));
            }

            // Wait for all tasks to complete
            for (Future<?> future : futures) {
                try {
                    future.get();
                } catch (Exception e) {
                    // Errors already counted inside the task
                }
            }
        }

        long elapsedMs = (System.nanoTime() - startTime) / 1_000_000;
        invocation.println("Parsing complete: " + successCount.get() + " succeeded, " + errorCount.get() + " failed.");
        invocation.println("AST store saved to " + storeDir);
        invocation.println("Elapsed time: " + elapsedMs + " ms");

        return CommandResult.SUCCESS;
    }

    private Map<Language, List<Path>> findSourceFiles(Path root) throws IOException {
        List<String> excludePatterns = ConfigProvider.getConfig()
                .getOptionalValue("ts4j.parser.exclude-dirs", String.class)
                .map(s -> Arrays.stream(s.split(","))
                        .map(String::trim)
                        .filter(p -> !p.isEmpty())
                        .toList())
                .orElse(List.of());

        Map<Language, List<Path>> filesByLanguage = new HashMap<>();
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
                        LanguageDetector.detect(file).ifPresent(lang ->
                                filesByLanguage.computeIfAbsent(lang, k -> new ArrayList<>()).add(file));
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(Path file, IOException exc) {
                        return FileVisitResult.CONTINUE;
                    }
                });
        return filesByLanguage;
    }

    private boolean shouldExclude(String name, List<String> patterns) {
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

    private String relativize(Path root, Path file) {
        try {
            return root.relativize(file).toString();
        } catch (IllegalArgumentException e) {
            return file.toString();
        }
    }

    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
