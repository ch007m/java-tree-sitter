package dev.snowdrop.treesitter.jtree.command;

import org.aesh.command.Command;
import org.aesh.command.CommandResult;
import org.aesh.command.GroupCommandDefinition;
import org.aesh.command.invocation.CommandInvocation;

@GroupCommandDefinition(
        name = "jt",
        description = "Tree-sitter polyglot AST CLI (jtree)",
        groupCommands = { ParseCommand.class, QueryCommand.class}
)
public class JtGroupCommand implements Command<CommandInvocation> {

    @Override
    public CommandResult execute(CommandInvocation invocation) {
        invocation.println("Usage: jt <command>");
        invocation.println("Commands:");
        invocation.println("  parse   Parse files from a directory and persist AST nodes as JSON");
        invocation.println("  query   Query persisted AST nodes by type, file, or text");
        return CommandResult.SUCCESS;
    }
}
