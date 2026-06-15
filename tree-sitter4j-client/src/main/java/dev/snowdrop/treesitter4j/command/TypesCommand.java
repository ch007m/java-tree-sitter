package dev.snowdrop.treesitter4j.command;

import dev.snowdrop.treesitter4j.util.ASTParserUtil;
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
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

@CommandDefinition(name = "types", description = "List distinct AST node types, grouped by language")
public class TypesCommand implements Command<CommandInvocation> {

    @Option(name = "language", shortName = 'L', description = "Filter by language (e.g., java, yaml, json)", hasValue = true)
    private String languageFilter;

    @Option(name = "app", shortName = 'a', description = "Path to the application directory (full or relative, defaults to current directory)", hasValue = true)
    private String appPath;

    @Override
    public CommandResult execute(CommandInvocation invocation) {
        ASTParserUtil parserUtil = new ASTParserUtil();
        Path rootDir = parserUtil.resolveAppDir(appPath);
        if (!parserUtil.hasStore(rootDir)) {
            invocation.println("No AST store found. Run 'ts4j parse <path>' first.");
            invocation.println("Looked in: " + rootDir.resolve(parserUtil.STORE_DIR));
            return CommandResult.FAILURE;
        }

        List<ASTTree> trees;
        try {
            trees = parserUtil.loadStore(rootDir);
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