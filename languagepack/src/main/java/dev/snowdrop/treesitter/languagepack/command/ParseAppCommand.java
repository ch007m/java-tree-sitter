package dev.snowdrop.treesitter.languagepack.command;

import dev.snowdrop.treesitter.languagepack.PersistentStore;
import dev.snowdrop.treesitter.languagepack.TreeSitterEngine;
import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Argument;

import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

@CommandDefinition(name = "parse", description = "Scan and parse an entire directory structure via WASM")
public class ParseAppCommand implements Command<CommandInvocation> {

    @Argument(description = "Path to the local project target application directory", required = true)
    private String projectPath;

    @Override
    public CommandResult execute(CommandInvocation invocation) {
        Path rootDir = Paths.get(projectPath);
        if (!Files.isDirectory(rootDir)) {
            invocation.getShell().writeln("Error: Provided path is not a valid directory.");
            return CommandResult.FAILURE;
        }

        PersistentStore store = new PersistentStore();
        TreeSitterEngine engine = new TreeSitterEngine();
        invocation.getShell().writeln("Scanning codebase via Tree-sitter Language Pack bindings...");

        try (Stream<Path> paths = Files.walk(rootDir)) {
            paths.filter(Files::isRegularFile).forEach(file -> {
                try {
                    var extractedNodes = engine.parseResource(file);
                    invocation.getShell().writeln("Parsed & extracted resource: " + file.getFileName());
                    invocation.getShell().writeln("ExtractNodes: " + extractedNodes);
                    if (!extractedNodes.isEmpty()) {
                        store.fileGraphs.put(file.toAbsolutePath().toString(), extractedNodes);
                        invocation.getShell().writeln("Parsed Resource: " + file.getFileName());
                    }
                } catch (Exception e) {
                    invocation.getShell().writeln("Failed to parse: " + file.getFileName() + " - " + e.getMessage());
                }
            });

            // Persist the computed Graph Store metadata cache structure onto local disk
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("graph_store.dat"))) {
                oos.writeObject(store);
            }

            invocation.getShell().writeln("\nSuccess! AST graph serialization completed and saved to 'graph_store.dat'.");
            return CommandResult.SUCCESS;
        } catch (Exception e) {
            invocation.getShell().writeln("Parsing failed: " + e.getMessage());
            return CommandResult.FAILURE;
        }
    }
}