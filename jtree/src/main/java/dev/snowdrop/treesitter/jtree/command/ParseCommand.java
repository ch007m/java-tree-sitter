package dev.snowdrop.treesitter.jtree.command;

import dev.snowdrop.treesitter.jtree.NodeStore;
import dev.snowdrop.treesitter.jtree.TreeSitterEngine;
import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Argument;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

@CommandDefinition(name = "parse", description = "Parse files from a directory and persist AST nodes as JSON")
public class ParseCommand implements Command<CommandInvocation> {

    @Argument(description = "Path to the project directory to parse", required = true)
    private String projectPath;

    @Override
    public CommandResult execute(CommandInvocation invocation) {
        Path rootDir = Paths.get(projectPath).toAbsolutePath().normalize();
        if (!Files.isDirectory(rootDir)) {
            invocation.println("Error: '" + projectPath + "' is not a valid directory.");
            return CommandResult.FAILURE;
        }

        TreeSitterEngine engine = new TreeSitterEngine();
        NodeStore.StoreData store = new NodeStore.StoreData(
                rootDir.toString(),
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        invocation.println("Parsing files under: " + rootDir);
        invocation.println("Supported languages: " + engine.supportedLanguages());
        invocation.println("");

        int[] fileCount = {0};
        int[] skippedCount = {0};
        Map<String, Integer> langCounts = new HashMap<>();

        try (Stream<Path> paths = Files.walk(rootDir)) {
            paths.filter(Files::isRegularFile)
                 .filter(p -> !isHiddenPath(rootDir, p))
                 .forEach(file -> {
                     try {
                         var detected = engine.detectLanguage(file);
                         if (detected.isEmpty() || !engine.isSupported(detected.get())) {
                             skippedCount[0]++;
                             return;
                         }

                         String lang = detected.get();
                         var nodes = engine.parseResource(file);
                         if (!nodes.isEmpty()) {
                             String relativePath = rootDir.relativize(file).toString();
                             store.addFile(relativePath, nodes);
                             fileCount[0]++;
                             langCounts.merge(lang, 1, Integer::sum);
                             invocation.println("  " + relativePath + " [" + lang + "] -> " + nodes.size() + " nodes");
                         }
                     } catch (Exception e) {
                         invocation.println("  FAILED: " + file.getFileName() + " - " + e.getMessage());
                     }
                 });

            Path storeFile = rootDir.resolve(NodeStore.STORE_FILENAME);
            NodeStore.save(store, storeFile);
            NodeStore.saveLastProject(rootDir);

            invocation.println("");
            invocation.println("--- Summary ---");
            invocation.println("Files parsed: " + fileCount[0]);
            invocation.println("Files skipped: " + skippedCount[0]);
            invocation.println("Total nodes: " + store.totalNodeCount());
            langCounts.forEach((lang, count) ->
                    invocation.println("  " + lang + ": " + count + " files"));
            invocation.println("Saved to: " + storeFile);

            return CommandResult.SUCCESS;
        } catch (Exception e) {
            invocation.println("Parsing failed: " + e.getMessage());
            return CommandResult.FAILURE;
        }
    }

    private boolean isHiddenPath(Path root, Path file) {
        Path relative = root.relativize(file);
        for (Path segment : relative) {
            if (segment.toString().startsWith(".")) {
                return true;
            }
        }
        return false;
    }
}
