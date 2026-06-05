package dev.snowdrop.treesitter.jtree;

import java.nio.file.Path;

public class WasmApp {
    public static void main(String[] args) throws Exception {
        // Core WebAssembly runtime module path
        String rootWasmPath = "/Users/cmoullia/code/quarkus/rewrite-mtool/_others/treesitter/aesh-tree-sitter/lib/wasm_dylib_output/";
        Path coreEngine = Path.of(rootWasmPath,"tree-sitter.wasm");

        // Initialize our registry
        WasmParserRegistry registry = new WasmParserRegistry(coreEngine);

        // Map and register your distinct language definitions
        registry.registerLanguage("java",       Path.of(rootWasmPath,"tree-sitter-java.wasm"));
        registry.registerLanguage("yaml",       Path.of(rootWasmPath,"tree-sitter-yaml.wasm"));
        registry.registerLanguage("json",       Path.of(rootWasmPath,"tree-sitter-json.wasm"));
        registry.registerLanguage("properties", Path.of(rootWasmPath,"tree-sitter-properties.wasm"));
        registry.registerLanguage("html",       Path.of(rootWasmPath,"tree-sitter-html.wasm"));
        registry.registerLanguage("javascript", Path.of(rootWasmPath,"tree-sitter-javascript.wasm"));

        // Test multi-language parsing sequences sequentially!
        String javaCode = "public class App {}";
        registry.parse("java", javaCode);

        String yamlConfig = "server:\n  port: 8080";
        registry.parse("yaml", yamlConfig);

        String jsScript = "console.log('Hello World');";
        registry.parse("javascript", jsScript);

        System.out.println("\nAll languages parsed successfully with zero native libraries!");
    }
}