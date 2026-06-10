package dev.snowdrop.treesitter4j.command;

import dev.snowdrop.treesitter4j.util.ASTParserUtil;
import io.roastedroot.treesitter.ast.ASTNode;
import io.roastedroot.treesitter.ast.ASTTree;
import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Argument;
import org.aesh.command.option.Option;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@CommandDefinition(name = "query", description = "Query AST nodes by type, file, or text")
public class QueryCommand implements Command<CommandInvocation> {

    @Argument(description = "Node type to search for (e.g., class_declaration, method_declaration, import_declaration)", required = false)
    private String nodeType;

    @Option(name = "file", shortName = 'f', description = "Filter results by file path substring", hasValue = true)
    private String fileFilter;

    @Option(name = "text", shortName = 't', description = "Filter by node text (case-insensitive contains)", hasValue = true)
    private String textFilter;

    @Option(name = "app", shortName = 'a', description = "Path to the application directory (full or relative, defaults to current directory)", hasValue = true)
    private String appPath;

    @Option(name = "reload", shortName = 'r', description = "Force re-parse of source files, save AST to the store, then query", hasValue = false)
    private boolean reload;

    @Override
    public CommandResult execute(CommandInvocation invocation) {
        long startTime = System.nanoTime();

        if (nodeType == null || nodeType.isBlank()) {
            invocation.println("Usage: ts4j query <node-type> [--file filter] [--text filter] [--app path] [--reload]");
            return CommandResult.FAILURE;
        }

        Path rootDir = ASTParserUtil.resolveAppDir(appPath);
        if (!Files.isDirectory(rootDir)) {
            invocation.println("Error: '" + (appPath != null ? appPath : rootDir) + "' is not a valid directory.");
            return CommandResult.FAILURE;
        }

        List<ASTTree> trees;

        if (reload) {
            // Force re-parse, save to store, then query
            try {
                trees = ASTParserUtil.parseDirectory(rootDir, invocation::println);
                if (!trees.isEmpty()) {
                    ASTParserUtil.saveToStore(trees, rootDir);
                    invocation.println("AST store saved to " + rootDir.resolve(ASTParserUtil.STORE_DIR));
                }
            } catch (IOException e) {
                invocation.println("Error during reload: " + e.getMessage());
                return CommandResult.FAILURE;
            }
        } else if (ASTParserUtil.hasStore(rootDir)) {
            // Existing store found — load from JSON
            try {
                trees = ASTParserUtil.loadStore(rootDir);
            } catch (IOException e) {
                invocation.println("Error loading AST store: " + e.getMessage());
                return CommandResult.FAILURE;
            }
        } else {
            // No store — parse on the fly, keep in memory only
            try {
                trees = ASTParserUtil.parseDirectory(rootDir, invocation::println);
            } catch (IOException e) {
                invocation.println("Error parsing application: " + e.getMessage());
                return CommandResult.FAILURE;
            }
        }

        if (trees.isEmpty()) {
            invocation.println("No AST trees available.");
            return CommandResult.SUCCESS;
        }

        long elapsedMs = (System.nanoTime() - startTime) / 1_000_000;
        invocation.println("Elapsed Query time: " + elapsedMs + " ms");

        return queryNodes(invocation, trees);
    }

    private CommandResult queryNodes(CommandInvocation invocation, List<ASTTree> trees) {
        int totalMatches = 0;

        for (ASTTree tree : trees) {
            if (fileFilter != null && !matchesFileFilter(tree)) {
                continue;
            }

            List<ASTNode> matches = new ArrayList<>();
            findNodes(tree.getRoot(), nodeType, matches);

            for (ASTNode node : matches) {
                if (textFilter != null && !matchesTextFilter(node)) {
                    continue;
                }

                String sourceFile = tree.getSourceFile() != null ? tree.getSourceFile() : "<unknown>";
                String text = node.getText() != null ? truncate(node.getText(), 120) : "";
                int startLine = byteToApproxLine(tree.getSourceCode(), node.getStartByte());

                invocation.println("  " + sourceFile + ":" + startLine
                        + "  [" + node.getType() + "] = " + text);
                totalMatches++;
            }
        }
        return CommandResult.SUCCESS;
    }

    private boolean matchesFileFilter(ASTTree tree) {
        String sourceFile = tree.getSourceFile();
        return sourceFile != null && sourceFile.contains(fileFilter);
    }

    private boolean matchesTextFilter(ASTNode node) {
        String text = nodeFullText(node);
        return text != null && text.toLowerCase().contains(textFilter.toLowerCase());
    }

    private String nodeFullText(ASTNode node) {
        if (node.getText() != null) {
            return node.getText();
        }
        if (node.getChildren() != null) {
            StringBuilder sb = new StringBuilder();
            for (ASTNode child : node.getChildren()) {
                String childText = nodeFullText(child);
                if (childText != null) {
                    if (sb.length() > 0) sb.append(" ");
                    sb.append(childText);
                }
            }
            return sb.length() > 0 ? sb.toString() : null;
        }
        return null;
    }

    private void findNodes(ASTNode node, String type, List<ASTNode> results) {
        if (node == null) return;
        if (node.getType() != null && node.getType().equals(type)) {
            results.add(node);
        }
        if (node.getChildren() != null) {
            for (ASTNode child : node.getChildren()) {
                findNodes(child, type, results);
            }
        }
    }

    private int byteToApproxLine(String source, int byteOffset) {
        if (source == null || byteOffset <= 0) return 1;
        int line = 1;
        int len = Math.min(byteOffset, source.length());
        for (int i = 0; i < len; i++) {
            if (source.charAt(i) == '\n') line++;
        }
        return line;
    }

    private String truncate(String text, int maxLen) {
        int newline = text.indexOf('\n');
        if (newline >= 0) {
            text = text.substring(0, newline) + " ...";
        }
        if (text.length() > maxLen) {
            text = text.substring(0, maxLen - 3) + "...";
        }
        return text;
    }
}