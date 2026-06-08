package dev.snowdrop.treesitter.languagepack;

import dev.snowdrop.treesitter.languagepack.command.ParseAppCommand;
import dev.snowdrop.treesitter.languagepack.command.QueryStoreCommand;
import org.aesh.command.AeshCommandRuntimeBuilder;
import org.aesh.command.CommandRuntime;
import org.aesh.command.impl.registry.AeshCommandRegistryBuilder;
import org.aesh.command.registry.CommandRegistry;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class MainApp {
    public static void main(String[] args) throws Exception {
        CommandRegistry registry = AeshCommandRegistryBuilder.builder()
                .command(ParseAppCommand.class)
                .command(QueryStoreCommand.class)
                .create();

        CommandRuntime runtime = AeshCommandRuntimeBuilder.builder()
                .commandRegistry(registry)
                .build();

        System.out.println("=================================================");
        System.out.println("  Polyglot WASM Tree-sitter Codebase Indexer     ");
        System.out.println("=================================================");
        System.out.println("Commands: parse <dir_path>   |   query <node_type>");

        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        String line;
        System.out.print("> ");
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.equalsIgnoreCase("exit") || line.equalsIgnoreCase("quit")) {
                break;
            }
            if (!line.isEmpty()) {
                try {
                    runtime.executeCommand(line);
                } catch (Exception e) {
                    System.err.println("Error: " + e.getMessage());
                }
            }
            System.out.print("> ");
        }
    }
}