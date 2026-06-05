package dev.snowdrop.treesitter.jtree.command;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Argument;
import org.aesh.command.option.Option;

@CommandDefinition(name = "query", description = "Query persisted AST nodes by type, file, or text")
public class QueryCommand implements Command<CommandInvocation> {

    @Argument(description = "Node type to search for (e.g., class, method, import)", required = false)
    private String nodeType;

    @Option(name = "file", shortName = 'f', description = "Filter results by file path substring", hasValue = true)
    private String fileFilter;

    @Option(name = "text", shortName = 't', description = "Filter by node text (case-insensitive contains)", hasValue = true)
    private String textFilter;

    @Option(name = "list-types", shortName = 'l', description = "List all distinct node types in the store", hasValue = false)
    private boolean listTypes;

    @Option(name = "store", shortName = 's', description = "Path to the project root containing ast-store.json", hasValue = true)
    private String storePath;

    @Override
    public CommandResult execute(CommandInvocation invocation) {
        //TODO
        return null;
    }
}
