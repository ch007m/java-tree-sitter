package dev.snowdrop.treesitter.languagepack.command;

import dev.snowdrop.treesitter.languagepack.PersistentStore;
import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Argument;

import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

@CommandDefinition(name = "query", description = "Query the saved resource tree storage file")
public class QueryStoreCommand implements Command<CommandInvocation> {

    @Argument(description = "Target node type criteria to query (e.g., annotation, class_declaration)", required = true)
    private String queryType;

    @Override
    public CommandResult execute(CommandInvocation invocation) {
        if (!Files.exists(Paths.get("graph_store.dat"))) {
            invocation.getShell().writeln("Error: No database storage found. Run 'parse <directory>' first.");
            return CommandResult.FAILURE;
        }

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream("graph_store.dat"))) {
            PersistentStore store = (PersistentStore) ois.readObject();
            invocation.getShell().writeln("Searching for nodes matching type matching: " + queryType);
            invocation.getShell().writeln("---------------------------------------------------------");

            store.fileGraphs.forEach((filePath, nodes) -> {
                nodes.stream()
                        .filter(node -> node.type.equalsIgnoreCase(queryType))
                        .forEach(node -> invocation.getShell().writeln(
                                String.format("File: %s\n Match -> Type: %s | Asset: %s [Lines %d-%d]\n",
                                        filePath, node.type, node.text, node.startLine, node.endLine)
                        ));
            });

            return CommandResult.SUCCESS;
        } catch (Exception e) {
            invocation.getShell().writeln("Query operational fault: " + e.getMessage());
            return CommandResult.FAILURE;
        }
    }
}