package dev.snowdrop.treesitter4j.command;

import io.roastedroot.treesitter.ast.ASTJsonSerializer;
import io.roastedroot.treesitter.ast.ASTNode;
import io.roastedroot.treesitter.ast.ASTTree;
import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Option;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Stream;

@CommandDefinition(name = "types", description = "List distinct AST node types, grouped by language")
public class TypesCommand implements Command<CommandInvocation> {

    private static final String STORE_DIR = ".ast-store";

    @Option(name = "language", shortName = 'L', description = "Filter by language (e.g., java, yaml, json)", hasValue = true)
    private String languageFilter;

    @Option(name = "store", shortName = 's', description = "Path to the project root containing .ast-store/", hasValue = true)
    private String storePath;

    @Override
    public CommandResult execute(CommandInvocation invocation) {
        Path storeDir = resolveStoreDir();
        if (storeDir == null || !Files.isDirectory(storeDir)) {
            invocation.println("No AST store found. Run 'ts4j parse <path>' first.");
            if (storeDir != null) {
                invocation.println("Looked in: " + storeDir);
            }
            return CommandResult.FAILURE;
        }

        List<ASTTree> trees;
        try {
            trees = loadStore(storeDir);
        } catch (IOException e) {
            invocation.println("Error loading AST store: " + e.getMessage());
            return CommandResult.FAILURE;
        }

        if (trees.isEmpty()) {
            invocation.println("AST store is empty.");
            return CommandResult.SUCCESS;
        }

        // Group node types by language
        Map<String, TreeSet<String>> typesByLanguage = new TreeMap<>();

        for (ASTTree tree : trees) {
            String lang = tree.getLanguage() != null ? tree.getLanguage().toLowerCase() : "unknown";

            if (languageFilter != null && !lang.equalsIgnoreCase(languageFilter)) {
                continue;
            }

            TreeSet<String> types = typesByLanguage.computeIfAbsent(lang, k -> new TreeSet<>());
            collectTypes(tree.getRoot(), types);
        }

        if (typesByLanguage.isEmpty()) {
            invocation.println("No node types found matching the given filters.");
            return CommandResult.SUCCESS;
        }

        for (Map.Entry<String, TreeSet<String>> entry : typesByLanguage.entrySet()) {
            TreeSet<String> types = entry.getValue();
            invocation.println(entry.getKey() + " (" + types.size() + " types):");
            for (String type : types) {
                invocation.println("  " + type);
            }
        }

        return CommandResult.SUCCESS;
    }

    private Path resolveStoreDir() {
        if (storePath != null && !storePath.isBlank()) {
            return Paths.get(storePath).toAbsolutePath().normalize().resolve(STORE_DIR);
        }
        return Paths.get("").toAbsolutePath().resolve(STORE_DIR);
    }

    private List<ASTTree> loadStore(Path storeDir) throws IOException {
        List<ASTTree> trees = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(storeDir)) {
            List<Path> jsonFiles = walk
                    .filter(p -> p.toString().endsWith(".json"))
                    .filter(Files::isRegularFile)
                    .toList();

            for (Path jsonFile : jsonFiles) {
                try {
                    trees.add(ASTJsonSerializer.fromJson(jsonFile));
                } catch (IOException e) {
                    // Skip malformed files
                }
            }
        }
        return trees;
    }

    private void collectTypes(ASTNode node, TreeSet<String> types) {
        if (node == null) return;
        if (node.getType() != null && node.isNamed()) {
            types.add(node.getType());
        }
        if (node.getChildren() != null) {
            for (ASTNode child : node.getChildren()) {
                collectTypes(child, types);
            }
        }
    }
}