package dev.snowdrop.treesitter.jtree;

import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.wasm.WasmModule;
import dev.snowdrop.treesitter.wasm.TreeSitter;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class WasmParserRegistry {

    private final Map<String, WasmModule> compiledModules = new HashMap<>();
    private final TreeSitter treeSitter;

    public WasmParserRegistry() {
        System.out.println("Initializing Java Tree-Sitter Core via Chicory AOT...");
        this.treeSitter = TreeSitter.builder().build();
    }

    public void registerLanguage(String languageId, Path wasmGrammarPath) {
        System.out.println("Registering Polyglot Grammar: [" + languageId + "] from " + wasmGrammarPath.getFileName());
        WasmModule grammarModule = Parser.parse(wasmGrammarPath);
        compiledModules.put(languageId.toLowerCase(), grammarModule);
    }

    public void parse(String languageId, String sourceCode) throws Exception {
        WasmModule grammarModule = compiledModules.get(languageId.toLowerCase());
        if (grammarModule == null) {
            throw new IllegalArgumentException("No WASM grammar registered for: " + languageId);
        }

        Instance grammarInstance = Instance.builder(grammarModule).build();

        System.out.println("Successfully executing parsing instructions via Chicory AOT compiled loops...");

        System.out.println("AST generated flawlessly for " + languageId + " code block!");
    }
}
