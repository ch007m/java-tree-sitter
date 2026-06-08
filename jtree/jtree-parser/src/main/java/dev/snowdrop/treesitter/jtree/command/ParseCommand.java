package dev.snowdrop.treesitter.jtree.command;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Argument;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@CommandDefinition(name = "parse", description = "Parse files from a directory and persist AST nodes as JSON")
public class ParseCommand implements Command<CommandInvocation> {

    @Argument(description = "Path to the project directory to parse", required = true)
    private String projectPath;

    @Override
    public CommandResult execute(CommandInvocation invocation) {
        Path rootDir = Paths.get(projectPath).toAbsolutePath().normalize();
        if (!Files.isDirectory(rootDir)) {
            invocation.println("Error: '" + projectPath + "' is not a valid directory.");
            return CommandResult.FAILURE;
        }
        // TODO
        return CommandResult.SUCCESS;
    }
}
