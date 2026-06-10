package dev.snowdrop.treesitter4j.command;

import dev.snowdrop.treesitter4j.util.LanguageDetector;
import io.roastedroot.treesitter.Language;
import io.roastedroot.treesitter.TreeSitter;
import io.roastedroot.treesitter.TreeSitterException;
import io.roastedroot.treesitter.TreeSitterParser;
import io.roastedroot.treesitter.TreeSitterTree;
import io.roastedroot.treesitter.ast.ASTExporter;
import io.roastedroot.treesitter.ast.ASTJsonSerializer;
import io.roastedroot.treesitter.ast.ASTTree;
import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Argument;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

@CommandDefinition(name = "parse", description = "Parse files from a directory and persist AST nodes as JSON")
public class ParseCommand implements Command<CommandInvocation> {

    private static final String STORE_DIR = ".ast-store";

    @Argument(description = "Path to the project directory to parse", required = true)
    private String projectPath;

    @Override
    public CommandResult execute(CommandInvocation invocation) {
        Path rootDir = Paths.get(projectPath).toAbsolutePath().normalize();
        if (!Files.isDirectory(rootDir)) {
            invocation.println("Error: '" + projectPath + "' is not a valid directory.");
            return CommandResult.FAILURE;
        }

        // Collect supported files
        List<Path> sourceFiles;
        try {
            sourceFiles = findSourceFiles(rootDir);
        } catch (IOException e) {
            invocation.println("Error walking directory: " + e.getMessage());
            return CommandResult.FAILURE;
        }

        if (sourceFiles.isEmpty()) {
            invocation.println("No supported source files found under " + rootDir);
            return CommandResult.SUCCESS;
        }

        invocation.println("Found " + sourceFiles.size() + " source file(s). Parsing...");

        // Prepare output directory
        Path storeDir = rootDir.resolve(STORE_DIR);
        try {
            Files.createDirectories(storeDir);
        } catch (IOException e) {
            invocation.println("Error creating store directory: " + e.getMessage());
            return CommandResult.FAILURE;
        }

        int successCount = 0;
        int errorCount = 0;

        try (TreeSitter ts = TreeSitter.create();
             TreeSitterParser parser = ts.newParser()) {

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
                        invocation.println("  WARN: language " + lang + " not supported at runtime, skipping " + relativize(rootDir, file));
                        continue;
                    }

                    try (TreeSitterTree tree = parser.parseString(source)) {
                        if (tree == null) {
                            invocation.println("  WARN: failed to parse " + relativize(rootDir, file));
                            errorCount++;
                            continue;
                        }

                        ASTTree ast = ASTExporter.export(tree, lang, source, relativize(rootDir, file));

                        // Write JSON file mirroring the source path
                        Path relPath = rootDir.relativize(file);
                        Path jsonFile = storeDir.resolve(relPath + ".json");
                        Files.createDirectories(jsonFile.getParent());
                        ASTJsonSerializer.toJson(ast, jsonFile);

                        successCount++;
                    }
                } catch (Exception e) {
                    invocation.println("  ERROR parsing " + relativize(rootDir, file) + ": " + e.getMessage());
                    e.printStackTrace();
                    errorCount++;
                }
            }
        }

        invocation.println("Parsing complete: " + successCount + " succeeded, " + errorCount + " failed.");
        invocation.println("AST store saved to " + storeDir);

        return CommandResult.SUCCESS;
    }

    private List<Path> findSourceFiles(Path root) throws IOException {
        List<Path> files = new ArrayList<>();
        Files.walkFileTree(root, EnumSet.of(FileVisitOption.FOLLOW_LINKS),
                Integer.MAX_VALUE, new SimpleFileVisitor<>() {

                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                        String name = dir.getFileName() != null ? dir.getFileName().toString() : "";
                        if (name.startsWith(".") || name.equals("target") || name.equals("node_modules")) {
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

    private String relativize(Path root, Path file) {
        try {
            return root.relativize(file).toString();
        } catch (IllegalArgumentException e) {
            return file.toString();
        }
    }
}
