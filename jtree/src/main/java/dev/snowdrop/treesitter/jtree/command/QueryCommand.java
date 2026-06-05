package dev.snowdrop.treesitter.jtree.command;

import dev.snowdrop.treesitter.jtree.NodeStore;
import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Argument;
import org.aesh.command.option.Option;

import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

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
        Path projectRoot = resolveProjectRoot(invocation);
        if (projectRoot == null) {
            return CommandResult.FAILURE;
        }

        Optional<Path> storeFile = NodeStore.findStoreFile(projectRoot);
        if (storeFile.isEmpty()) {
            invocation.println("Error: No " + NodeStore.STORE_FILENAME + " found in " + projectRoot);
            invocation.println("Run 'jt parse <directory>' first to create the store.");
            return CommandResult.FAILURE;
        }

        NodeStore.StoreData store;
        try {
            store = NodeStore.load(storeFile.get());
        } catch (Exception e) {
            invocation.println("Error loading store: " + e.getMessage());
            return CommandResult.FAILURE;
        }

        invocation.println("Store: " + storeFile.get());
        invocation.println("Root: " + store.rootPath);
        invocation.println("Parsed at: " + store.parsedAt);
        invocation.println("Files: " + store.files.size() + " | Nodes: " + store.totalNodeCount());
        invocation.println("");

        if (listTypes) {
            return listDistinctTypes(invocation, store);
        }

        if (nodeType == null || nodeType.isBlank()) {
            invocation.println("Usage: jt query <node_type> [-f file_filter] [-t text_filter]");
            invocation.println("       jt query --list-types");
            return CommandResult.SUCCESS;
        }

        return queryNodes(invocation, store);
    }

    private CommandResult listDistinctTypes(CommandInvocation invocation, NodeStore.StoreData store) {
        Set<String> types = new TreeSet<>();
        store.files.values().forEach(nodes ->
                nodes.forEach(n -> types.add(n.type())));

        invocation.println("Distinct node types (" + types.size() + "):");
        types.forEach(type -> invocation.println("  " + type));
        return CommandResult.SUCCESS;
    }

    private CommandResult queryNodes(CommandInvocation invocation, NodeStore.StoreData store) {
        int[] matchCount = {0};

        store.files.forEach((filePath, nodes) -> {
            if (fileFilter != null && !filePath.toLowerCase().contains(fileFilter.toLowerCase())) {
                return;
            }

            nodes.stream()
                 .filter(node -> node.type().equalsIgnoreCase(nodeType))
                 .filter(node -> textFilter == null ||
                         node.text().toLowerCase().contains(textFilter.toLowerCase()))
                 .forEach(node -> {
                     matchCount[0]++;
                     invocation.println(String.format("%s:%d  [%s] %s",
                             filePath, node.startLine(), node.type(),
                             truncate(node.text(), 120)));
                 });
        });

        invocation.println("");
        invocation.println("Matches: " + matchCount[0]);
        return CommandResult.SUCCESS;
    }

    private Path resolveProjectRoot(CommandInvocation invocation) {
        if (storePath != null) {
            Path p = Path.of(storePath).toAbsolutePath().normalize();
            if (!java.nio.file.Files.isDirectory(p)) {
                invocation.println("Error: '" + storePath + "' is not a valid directory.");
                return null;
            }
            return p;
        }

        Optional<Path> lastProject = NodeStore.getLastProject();
        if (lastProject.isPresent()) {
            return lastProject.get();
        }

        invocation.println("Error: No project specified. Use -s <path> or run 'jt parse <directory>' first.");
        return null;
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        int newline = text.indexOf('\n');
        if (newline >= 0) {
            text = text.substring(0, newline);
        }
        if (text.length() > maxLen) {
            return text.substring(0, maxLen) + "...";
        }
        return text;
    }
}
