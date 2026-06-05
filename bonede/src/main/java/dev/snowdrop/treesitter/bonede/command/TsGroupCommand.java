package dev.snowdrop.treesitter.bonede.command;

import org.aesh.command.Command;
import org.aesh.command.CommandResult;
import org.aesh.command.GroupCommandDefinition;
import org.aesh.command.invocation.CommandInvocation;

@GroupCommandDefinition(
        name = "ts",
        description = "Tree-sitter AST operations",
        groupCommands = {ParseCommand.class, QueryCommand.class, LoadCommand.class}
)
public class TsGroupCommand implements Command<CommandInvocation> {

    @Override
    public CommandResult execute(CommandInvocation invocation) {
        invocation.println("Usage: ts <command>");
        invocation.println("Commands:");
        invocation.println("  parse   Parse all .java files under a directory and build an AST graph");
        invocation.println("  load    Load a previously saved AST store from disk");
        invocation.println("  query   Query the parsed AST using tree-sitter S-expression patterns or predefined queries");
        return CommandResult.SUCCESS;
    }
}