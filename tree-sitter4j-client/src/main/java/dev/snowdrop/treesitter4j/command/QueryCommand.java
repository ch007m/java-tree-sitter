package dev.snowdrop.treesitter4j.command;

import dev.snowdrop.treesitter4j.util.ASTParserUtil;
import dev.snowdrop.treesitter4j.util.ASTQueryUtil;
import dev.snowdrop.treesitter4j.util.ASTQueryUtil.QueryInfo;
import dev.snowdrop.treesitter4j.util.ASTQueryUtil.ParsedQuery;
import dev.snowdrop.treesitter4j.util.ASTQueryUtil.QueryMatch;
import io.roastedroot.treesitter.Language;
import io.roastedroot.treesitter.ast.ASTTree;
import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Arguments;
import org.aesh.command.option.Option;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@CommandDefinition(name = "query", description = "Query AST nodes by type, file, or text")
public class QueryCommand implements Command<CommandInvocation> {

    @Arguments(description = "Query expression: <type> [= | contains <value>]  (e.g., class, 'class = MyApp', 'annotation contains Entity')")
    private List<String> queryArgs;

    @Option(name = "file", shortName = 'f', description = "Filter results by file path substring", hasValue = true)
    private String fileFilter;

    @Option(name = "text", shortName = 't', description = "Filter by node text (case-insensitive contains)", hasValue = true)
    private String textFilter;

    @Option(name = "app", shortName = 'a', description = "Path to the application directory (full or relative, defaults to current directory)", hasValue = true)
    private String appPath;

    @Option(name = "language", shortName = 'L',
            description = "Filter by language (java, yaml, json, xml, html, properties, markdown) or 'all' to search every language. Default: auto-detect from query type.",
            hasValue = true)
    private String languageOption;

    @Option(name = "reload", shortName = 'r', description = "Force re-parse of source files, save AST to the store, then query", hasValue = false)
    private boolean reload;

    @Override
    public CommandResult execute(CommandInvocation invocation) {
        ASTQueryUtil queryUtil = new ASTQueryUtil();
        ASTParserUtil parserUtil = new ASTParserUtil();

        if (queryArgs == null || queryArgs.isEmpty()) {
            printUsage(invocation, queryUtil.getAliases());
            return CommandResult.FAILURE;
        }

        // Parse query expression
        String rawQuery = String.join(" ", queryArgs);
        ParsedQuery parsed = queryUtil.parseQuery(rawQuery);

        // Apply --text as a "contains" filter when the expression has no inline operator
        if (textFilter != null && !textFilter.isBlank() && parsed.operator() == null) {
            parsed = new ParsedQuery(parsed.alias(), "contains", textFilter, parsed.queryInfo());
        }

        // Resolve language override
        Set<Language> langOverride = null;
        if (languageOption != null) {
            if ("all".equalsIgnoreCase(languageOption)) {
                langOverride = EnumSet.allOf(Language.class);
            } else {
                try {
                    langOverride = EnumSet.of(Language.valueOf(languageOption.toUpperCase()));
                } catch (IllegalArgumentException e) {
                    invocation.println("Unknown language: " + languageOption
                            + ". Valid values: java, yaml, json, xml, html, properties, markdown, all");
                    return CommandResult.FAILURE;
                }
            }
        }

        Path rootDir = parserUtil.resolveAppDir(appPath);
        if (!Files.isDirectory(rootDir)) {
            invocation.println("Error: '" + (appPath != null ? appPath : rootDir) + "' is not a valid directory.");
            return CommandResult.FAILURE;
        }

        // Load AST trees
        List<ASTTree> trees;
        long startTime = System.nanoTime();

        if (reload) {
            try {
                trees = parserUtil.parseDirectory(rootDir, invocation::println);
                if (!trees.isEmpty()) {
                    parserUtil.saveToStore(trees, rootDir);
                    invocation.println("AST store saved to " + rootDir.resolve(parserUtil.STORE_DIR));
                }
            } catch (IOException e) {
                invocation.println("Error during reload: " + e.getMessage());
                return CommandResult.FAILURE;
            }
        } else if (parserUtil.hasStore(rootDir)) {
            try {
                trees = parserUtil.loadStore(rootDir);
            } catch (IOException e) {
                invocation.println("Error loading AST store: " + e.getMessage());
                return CommandResult.FAILURE;
            }
        } else {
            try {
                trees = parserUtil.parseDirectory(rootDir, invocation::println);
            } catch (IOException e) {
                invocation.println("Error parsing application: " + e.getMessage());
                return CommandResult.FAILURE;
            }
        }

        if (trees.isEmpty()) {
            invocation.println("No AST trees available.");
            return CommandResult.SUCCESS;
        }

        // Execute query
        List<QueryMatch> matches = queryUtil.execute(parsed, trees, fileFilter, langOverride);

        for (QueryMatch match : matches) {
            invocation.println("  " + match.sourceFile() + ":" + match.line()
                    + "  [" + match.alias() + "] = " + truncate(match.matchedText(), 120));
        }

        long elapsedMs = (System.nanoTime() - startTime) / 1_000_000;
        invocation.println(matches.size() + " match(es). Elapsed: " + elapsedMs + " ms");

        return CommandResult.SUCCESS;
    }

    private void printUsage(CommandInvocation invocation, Map<String, QueryInfo> aliases) {
        invocation.println("Usage: ts4j query <expression> [--file filter] [--text filter] [--app path] [--language lang] [--reload]");
        invocation.println("");
        invocation.println("Query expression:");
        invocation.println("  <type>                    List all nodes of the given type");
        invocation.println("  <type> = <value>          Find nodes where name equals value");
        invocation.println("  <type> = <pattern*>       Glob/wildcard match (* matches any characters)");
        invocation.println("  <type> contains <value>   Find nodes where name contains value (case-insensitive)");
        invocation.println("");
        invocation.println("Available aliases:");
        String currentLang = null;
        for (Map.Entry<String, QueryInfo> e : aliases.entrySet()) {
            String lang = e.getValue().language().name().toLowerCase();
            if (!lang.equals(currentLang)) {
                currentLang = lang;
                invocation.println(" " + lang + ":");
            }
            invocation.println("  " + String.format("%-14s", e.getKey()) + " -> " + e.getValue().nodeType());
        }
        invocation.println("");
        invocation.println("Raw tree-sitter node types (e.g., class_declaration) are also supported.");
        invocation.println("Language is auto-detected from the query type. Use --language to override.");
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
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
