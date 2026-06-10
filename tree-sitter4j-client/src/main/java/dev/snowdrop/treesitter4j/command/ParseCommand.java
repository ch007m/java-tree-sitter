package dev.snowdrop.treesitter4j.command;

import dev.snowdrop.treesitter4j.util.ASTParserUtil;
import io.roastedroot.treesitter.ast.ASTTree;
import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Argument;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@CommandDefinition(name = "parse", description = "Parse files from a directory and persist AST nodes as JSON files under the store")
public class ParseCommand implements Command<CommandInvocation> {

    @Argument(description = "Path to the project directory to parse", required = true)
    private String projectPath;

    @Override
    public CommandResult execute(CommandInvocation invocation) {
        long startTime = System.nanoTime();

        Path rootDir = Paths.get(projectPath).toAbsolutePath().normalize();
        if (!Files.isDirectory(rootDir)) {
            invocation.println("Error: '" + projectPath + "' is not a valid directory.");
            return CommandResult.FAILURE;
        }

        List<ASTTree> trees;
        try {
            trees = ASTParserUtil.parseDirectory(rootDir, invocation::println);
        } catch (IOException e) {
            invocation.println("Error parsing directory: " + e.getMessage());
            return CommandResult.FAILURE;
        }

        if (trees.isEmpty()) {
            return CommandResult.SUCCESS;
        }

        try {
            ASTParserUtil.saveToStore(trees, rootDir);
        } catch (IOException e) {
            invocation.println("Error saving AST store: " + e.getMessage());
            return CommandResult.FAILURE;
        }

        long elapsedMs = (System.nanoTime() - startTime) / 1_000_000;
        invocation.println("Parsing complete: " + trees.size() + " file(s) succeeded.");
        invocation.println("AST store saved to " + rootDir.resolve(ASTParserUtil.STORE_DIR));
        invocation.println("Elapsed time: " + elapsedMs + " ms");

        return CommandResult.SUCCESS;
    }
}