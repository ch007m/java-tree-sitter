package dev.snowdrop.treesitter.bonede.command;

import dev.snowdrop.treesitter.bonede.query.AnnotationNameFilter;
import dev.snowdrop.treesitter.bonede.query.PredefinedQueries;
import dev.snowdrop.treesitter.bonede.query.QueryResultPrinter;
import dev.snowdrop.treesitter.util.ASTStore;
import dev.snowdrop.treesitter.bonede.store.ASTStorePersistence;
import dev.snowdrop.treesitter.bonede.store.ParsedFile;
import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Argument;
import org.aesh.command.option.Option;

import java.io.IOException;
import java.nio.file.Path;

import org.treesitter.TSNode;
import org.treesitter.TSQuery;
import org.treesitter.TSQueryCursor;
import org.treesitter.TSQueryMatch;
import org.treesitter.TreeSitterJava;

@CommandDefinition(
        name = "query",
        description = "Query the parsed AST using tree-sitter S-expression patterns or predefined queries"
)
public class QueryCommand implements Command<CommandInvocation> {

    @Argument(
            description = "Tree-sitter query pattern or predefined name: "
                    + "classes, methods, imports, fields, interfaces, enums, annotations, "
                    + "constructors, packages, strings, method-calls. "
                    + "When using --name, defaults to 'annotations' if omitted."
    )
    private String pattern;

    @Option(
            name = "limit",
            shortName = 'l',
            description = "Maximum number of results to display (0 = unlimited)",
            defaultValue = "0"
    )
    private int limit;

    @Option(
            name = "file",
            shortName = 'f',
            description = "Filter results to a specific file (substring match on path)"
    )
    private String fileFilter;

    @Option(
            name = "name",
            shortName = 'n',
            description = "Filter annotations by name (e.g. 'Entity' or 'javax.persistence.Entity'). " +
                    "Simple names match as suffix: 'Entity' matches both @Entity and @javax.persistence.Entity"
    )
    private String nameFilter;

    @Option(
            name = "list-queries",
            shortName = 'L',
            description = "List all predefined query names",
            hasValue = false
    )
    private boolean listQueries;

    @Override
    public CommandResult execute(CommandInvocation invocation) {
        if (listQueries) {
            invocation.println("Predefined queries:");
            PredefinedQueries.list().forEach(entry ->
                    invocation.println("  " + padRight(entry.name(), 14) + " - " + entry.description())
            );
            return CommandResult.SUCCESS;
        }

        // Auto-select 'annotations' query when --name is specified without a pattern
        if ((pattern == null || pattern.isBlank()) && nameFilter != null && !nameFilter.isBlank()) {
            pattern = "annotations";
        }

        if (pattern == null || pattern.isBlank()) {
            invocation.println("Usage: query <pattern|predefined-name> [--limit N] [--file filter] [--name annotation-name]");
            invocation.println("       query --list-queries");
            return CommandResult.FAILURE;
        }

        ASTStore store = ASTStore.getInstance();
        if (store.isEmpty()) {
            // Try to auto-load from the last parsed/loaded project
            Path lastProject = ASTStorePersistence.getLastProject();
            if (lastProject == null) {
                invocation.println("No AST data loaded. Run 'ts parse <path>' first.");
                return CommandResult.FAILURE;
            }
            try {
                invocation.println("Loading AST store from " + ASTStorePersistence.storeFile(lastProject) + " ...");
                ASTStorePersistence.LoadResult result = ASTStorePersistence.load(lastProject);
                invocation.println("Loaded " + result.succeeded() + " file(s) from saved store.\n");
            } catch (IOException e) {
                invocation.println("Failed to load saved store: " + e.getMessage());
                invocation.println("Run 'ts parse <path>' to create a new one.");
                return CommandResult.FAILURE;
            }
        }

        String queryPattern = PredefinedQueries.resolve(pattern);

        TSQuery query;
        try {
            query = new TSQuery(new TreeSitterJava(), queryPattern);
        } catch (Exception e) {
            invocation.println("Invalid query pattern: " + e.getMessage());
            invocation.println("Pattern was: " + queryPattern);
            return CommandResult.FAILURE;
        }

        int totalMatches = 0;

        for (ParsedFile parsed : store.getAllFiles()) {
            if (fileFilter != null && !parsed.getFilePath().toString().contains(fileFilter)) {
                continue;
            }

            TSQueryCursor cursor = new TSQueryCursor();
            TSNode rootNode = parsed.getTree().getRootNode();
            cursor.exec(query, rootNode);

            TSQueryMatch match = new TSQueryMatch();
            while (cursor.nextMatch(match)) {
                if (limit > 0 && totalMatches >= limit) {
                    invocation.println("... (limit of " + limit + " reached)");
                    return CommandResult.SUCCESS;
                }

                // Apply name filter if --name was provided
                if (nameFilter != null && !nameFilter.isBlank()) {
                    if (!AnnotationNameFilter.matches(match, query, parsed, nameFilter)) {
                        continue;
                    }
                }

                QueryResultPrinter.printMatch(invocation, parsed, match, query);
                totalMatches++;
            }
        }

        invocation.println("\n" + totalMatches + " match(es) found across "
                + store.getFileCount() + " file(s).");
        return CommandResult.SUCCESS;
    }

    private static String padRight(String text, int width) {
        if (text.length() >= width) {
            return text;
        }
        return text + " ".repeat(width - text.length());
    }
}
