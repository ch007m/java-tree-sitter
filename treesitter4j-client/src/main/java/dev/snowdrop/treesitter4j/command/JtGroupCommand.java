package dev.snowdrop.treesitter4j.command;

import org.aesh.command.Command;
import org.aesh.command.CommandResult;
import org.aesh.command.GroupCommandDefinition;
import org.aesh.command.invocation.CommandInvocation;

@GroupCommandDefinition(
        name = "ts4j",
        description = "Tree-sitter polyglot AST CLI (treesitter4j)",
        groupCommands = { ParseCommand.class, QueryCommand.class, TypesCommand.class}
)
public class JtGroupCommand implements Command<CommandInvocation> {

    @Override
    public CommandResult execute(CommandInvocation invocation) {
        invocation.println("Usage: ts4j <command>");
        invocation.println("Commands:");
        invocation.println("  parse   Parse files from an application and persist AST nodes as JSON");
        invocation.println("  syntax   Query persisted AST nodes");
        invocation.println("  types   List distinct AST node types, grouped by language");
        return CommandResult.SUCCESS;
    }
}
