package dev.snowdrop.treesitter.bonede.command;

import dev.snowdrop.treesitter.util.ASTStore;
import dev.snowdrop.treesitter.bonede.store.ASTStorePersistence;
import dev.snowdrop.treesitter.bonede.store.ParsedFile;
import dev.snowdrop.treesitter.util.JavaFileWalker;
import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Argument;
import org.treesitter.TSParser;
import org.treesitter.TSTree;
import org.treesitter.TreeSitterJava;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@CommandDefinition(
        name = "parse",
        description = "Parse all .java files under a directory and build an in-memory AST graph"
)
public class ParseCommand implements Command<CommandInvocation> {

    @Argument(
            description = "Path to Java project directory (relative or absolute)",
            required = true
    )
    private String path;

    @Override
    public CommandResult execute(CommandInvocation invocation) {
        try {
            Path rootPath = Paths.get(path).toAbsolutePath().normalize();

            if (!Files.isDirectory(rootPath)) {
                invocation.println("Error: '" + rootPath + "' is not a directory.");
                return CommandResult.FAILURE;
            }

            ASTStore store = ASTStore.getInstance();
            store.clear();
            store.setRootPath(rootPath);

            List<Path> javaFiles = JavaFileWalker.findJavaFiles(rootPath);
            if (javaFiles.isEmpty()) {
                invocation.println("No .java files found under " + rootPath);
                return CommandResult.SUCCESS;
            }

            invocation.println("Found " + javaFiles.size() + " Java file(s). Parsing...");

            TSParser parser = new TSParser();
            parser.setLanguage(new TreeSitterJava());

            int successCount = 0;
            int errorCount = 0;

            for (Path javaFile : javaFiles) {
                try {
                    String source = Files.readString(javaFile);
                    TSTree tree = parser.parseString(null, source);

                    if (tree == null) {
                        invocation.println("  WARN: failed to parse " + relativize(rootPath, javaFile));
                        errorCount++;
                        continue;
                    }

                    store.addFile(javaFile, new ParsedFile(javaFile, source, tree));
                    successCount++;
                } catch (IOException e) {
                    invocation.println("  ERROR reading " + javaFile + ": " + e.getMessage());
                    errorCount++;
                }
            }

            invocation.println("Parsing complete: " + successCount + " succeeded, "
                    + errorCount + " failed.");
            invocation.println("AST store now holds " + store.getFileCount() + " file(s).");

            // Persist the store to disk
            try {
                Path storeFile = ASTStorePersistence.save(store);
                ASTStorePersistence.saveLastProject(rootPath);
                invocation.println("Store saved to " + storeFile);
            } catch (IOException e) {
                invocation.println("WARN: could not save store: " + e.getMessage());
            }

            return CommandResult.SUCCESS;

        } catch (IOException e) {
            invocation.println("Error walking directory: " + e.getMessage());
            return CommandResult.FAILURE;
        }
    }

    private String relativize(Path root, Path file) {
        try {
            return root.relativize(file).toString();
        } catch (IllegalArgumentException e) {
            return file.toString();
        }
    }
}
