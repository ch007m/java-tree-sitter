package dev.snowdrop.treesitter.languagepack.command;

import org.aesh.command.Command;
import org.aesh.command.CommandResult;
import org.aesh.command.GroupCommandDefinition;
import org.aesh.command.invocation.CommandInvocation;

@GroupCommandDefinition(
        name = "ts",
        description = "Tree-sitter AST operations (Language Pack)",
        groupCommands = { ParseAppCommand.class, QueryStoreCommand.class}
)
public class TsGroupCommand implements Command<CommandInvocation> {

    @Override
    public CommandResult execute(CommandInvocation invocation) {
        invocation.println("Usage: ts <command>");
        invocation.println("Commands:");
        invocation.println("  parse   Scan and parse an entire directory structure");
        invocation.println("  query   Query the saved resource tree storage file");
        return CommandResult.SUCCESS;
    }
}