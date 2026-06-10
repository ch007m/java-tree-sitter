package dev.snowdrop.treesitter.bonede.command;

import dev.snowdrop.treesitter.util.ASTStore;
import dev.snowdrop.treesitter.bonede.store.ASTStorePersistence;
import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Argument;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@CommandDefinition(
        name = "load",
        description = "Load a previously saved AST store from disk"
)
public class LoadCommand implements Command<CommandInvocation> {

    @Argument(
            description = "Path to Java project directory whose .ts4j.json should be loaded",
            required = true
    )
    private String path;

    @Override
    public CommandResult execute(CommandInvocation invocation) {
        Path projectRoot = Paths.get(path).toAbsolutePath().normalize();

        if (!Files.isDirectory(projectRoot)) {
            invocation.println("Error: '" + projectRoot + "' is not a directory.");
            return CommandResult.FAILURE;
        }

        if (!ASTStorePersistence.exists(projectRoot)) {
            invocation.println("No saved store found at " + ASTStorePersistence.storeFile(projectRoot));
            invocation.println("Run 'ts parse " + projectRoot + "' first to create one.");
            return CommandResult.FAILURE;
        }

        try {
            invocation.println("Loading AST store from " + ASTStorePersistence.storeFile(projectRoot) + " ...");

            ASTStorePersistence.LoadResult result = ASTStorePersistence.load(projectRoot);
            ASTStorePersistence.saveLastProject(projectRoot);

            invocation.println("Load complete: " + result.succeeded() + " succeeded, "
                    + result.failed() + " failed, "
                    + result.missing() + " missing (out of " + result.total() + ").");

            ASTStore store = ASTStore.getInstance();
            invocation.println("AST store now holds " + store.getFileCount() + " file(s).");
            return CommandResult.SUCCESS;

        } catch (IOException e) {
            invocation.println("Error loading store: " + e.getMessage());
            return CommandResult.FAILURE;
        }
    }
}