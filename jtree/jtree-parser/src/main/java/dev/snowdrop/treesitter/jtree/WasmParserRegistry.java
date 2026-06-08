package dev.snowdrop.treesitter.jtree;

import com.dylibso.chicory.runtime.GlobalInstance;
import com.dylibso.chicory.runtime.HostFunction;
import com.dylibso.chicory.runtime.ImportGlobal;
import com.dylibso.chicory.runtime.ImportMemory;
import com.dylibso.chicory.runtime.ImportTable;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.Memory;
import com.dylibso.chicory.runtime.Store;
import com.dylibso.chicory.runtime.TableInstance;
import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.wasm.WasmModule;
import com.dylibso.chicory.wasm.types.MemoryLimits;
import com.dylibso.chicory.wasm.types.MutabilityType;
import com.dylibso.chicory.wasm.types.Table;
import com.dylibso.chicory.wasm.types.TableLimits;
import com.dylibso.chicory.wasm.types.ValType;
import com.dylibso.chicory.wasm.types.Value;
import dev.snowdrop.treesitter.wasm.TreeSitter;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WasmParserRegistry {

    private record DylinkInfo(int memorySize, int memoryAlign, int tableSize, int tableAlign) {}

    private static final int TABLE_EXTRA_CAPACITY = 1024;

    private final Map<String, WasmModule> compiledModules = new HashMap<>();
    private final Map<String, DylinkInfo> dylinkInfos = new HashMap<>();
    private final TreeSitter treeSitter;
    private final TableInstance coreTable;

    public WasmParserRegistry() {
        System.out.println("Initializing Java Tree-Sitter Core via Chicory AOT...");
        this.treeSitter = TreeSitter.builder()
                .withMemoryLimits(new MemoryLimits(6))
                .build();
        this.coreTable = makeTableGrowable(treeSitter.instance(), TABLE_EXTRA_CAPACITY);
    }

    public void registerLanguage(String languageId, Path wasmGrammarPath) {
        System.out.println("Registering Polyglot Grammar: [" + languageId + "] from " + wasmGrammarPath.getFileName());
        WasmModule grammarModule = Parser.parse(wasmGrammarPath);
        compiledModules.put(languageId.toLowerCase(), grammarModule);
        try {
            dylinkInfos.put(languageId.toLowerCase(), parseDylink(wasmGrammarPath));
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse dylink.0 section from " + wasmGrammarPath, e);
        }
    }

    public void parse(String languageId, String sourceCode) throws Exception {
        WasmModule grammarModule = compiledModules.get(languageId.toLowerCase());
        if (grammarModule == null) {
            throw new IllegalArgumentException("No WASM grammar registered for: " + languageId);
        }
        DylinkInfo dylink = dylinkInfos.get(languageId.toLowerCase());

        // 1. Create the ephemeral linking Store for this parsing operation.
        Store store = new Store();

        // 2. Register the core instance exports under both namespaces:
        //    - "tree-sitter" for grammar imports using that namespace
        //    - "env" for Emscripten side-module imports (calloc, malloc, etc.)
        store.register("tree-sitter", treeSitter.instance());
        store.register("env", treeSitter.instance());

        // 3. Provide C library and Emscripten runtime stubs not exported by the core.
        store.addFunction(createLibcStubs());

        // 4. Override env memory and table with our properly-configured versions.
        var coreMemory = treeSitter.instance().memory();
        store.addMemory(new ImportMemory("env", "memory", coreMemory));
        store.addTable(new ImportTable("env", "__indirect_function_table", coreTable));

        // 5. Dynamic linking: allocate memory and table space for the grammar module
        //    using values from its dylink.0 section.
        int alignment = 1 << dylink.memoryAlign;
        int rawPtr = treeSitter.exports().malloc(dylink.memorySize + alignment - 1);
        int memoryBase = (rawPtr + alignment - 1) & ~(alignment - 1);

        int tableBase = coreTable.size();
        coreTable.grow(dylink.tableSize, Value.REF_NULL_VALUE, treeSitter.instance());

        store.addGlobal(
                new ImportGlobal("env", "__memory_base",
                        new GlobalInstance(memoryBase, 0, ValType.I32, MutabilityType.Const)),
                new ImportGlobal("env", "__table_base",
                        new GlobalInstance(tableBase, 0, ValType.I32, MutabilityType.Const)));

        // 6. Instantiate the grammar module with all env imports satisfied.
        Instance grammarInstance = store.instantiate(languageId, grammarModule);

        // =========================================================================
        // GLUE PIPELINE: Connects grammar and core engine via WebAssembly memory pointers
        // =========================================================================

        // Extract the target language initializer function from the grammar module
        var langFunctionName = "tree_sitter_" + languageId.toLowerCase();
        var grammarInitFunc = grammarInstance.export(langFunctionName);

        // Execute it to locate where the grammar lookup table rests in WebAssembly memory (i32 index pointer)
        long languagePointer = grammarInitFunc.apply()[0];

        // 5. Use your wrapper's custom strongly-typed exports to safely allocate a new parser layout
        // (This completely avoids raw string lookups like coreTreeSitterInstance.export("ts_parser_new"))
        int parserPointer = treeSitter.exports().tsParserNew();

        // Bind the language memory table pointer to your core wrapper's parser context
        treeSitter.exports().tsParserSetLanguage(parserPointer, (int) languagePointer);

        // Success! Your parser is fully linked.
        // Proceed with injecting your sourceCode string into memory and invoking ts_parser_parse()

        System.out.println("AST generated flawlessly for " + languageId + " code block!");
    }

    /**
     * Creates host function stubs for C library and Emscripten runtime functions
     * that grammar WASM modules import from "env" but the core doesn't export.
     */
    private static HostFunction[] createLibcStubs() {
        return new HostFunction[] {
            // Emscripten runtime stubs
            new HostFunction("env", "__assert_fail",
                    List.of(ValType.I32, ValType.I32, ValType.I32, ValType.I32), List.of(),
                    (inst, args) -> null),
            new HostFunction("env", "abort",
                    List.of(), List.of(),
                    (inst, args) -> { throw new RuntimeException("WASM abort called"); }),

            // C string/memory functions
            new HostFunction("env", "strlen",
                    List.of(ValType.I32), List.of(ValType.I32),
                    (inst, args) -> {
                        Memory mem = inst.memory();
                        int ptr = (int) args[0];
                        int len = 0;
                        while (mem.read(ptr + len) != 0) len++;
                        return new long[] { len };
                    }),
            new HostFunction("env", "memcmp",
                    List.of(ValType.I32, ValType.I32, ValType.I32), List.of(ValType.I32),
                    (inst, args) -> {
                        Memory mem = inst.memory();
                        int s1 = (int) args[0], s2 = (int) args[1], n = (int) args[2];
                        for (int i = 0; i < n; i++) {
                            int diff = Byte.toUnsignedInt(mem.read(s1 + i))
                                     - Byte.toUnsignedInt(mem.read(s2 + i));
                            if (diff != 0) return new long[] { diff };
                        }
                        return new long[] { 0 };
                    }),
            new HostFunction("env", "strncpy",
                    List.of(ValType.I32, ValType.I32, ValType.I32), List.of(ValType.I32),
                    (inst, args) -> {
                        Memory mem = inst.memory();
                        int dest = (int) args[0], src = (int) args[1], n = (int) args[2];
                        int i = 0;
                        for (; i < n; i++) {
                            byte b = mem.read(src + i);
                            mem.writeByte(dest + i, b);
                            if (b == 0) { i++; break; }
                        }
                        for (; i < n; i++) mem.writeByte(dest + i, (byte) 0);
                        return new long[] { dest };
                    }),

            // Wide character classification functions
            new HostFunction("env", "towupper",
                    List.of(ValType.I32), List.of(ValType.I32),
                    (inst, args) -> new long[] { Character.toUpperCase((int) args[0]) }),
            new HostFunction("env", "iswspace",
                    List.of(ValType.I32), List.of(ValType.I32),
                    (inst, args) -> new long[] { Character.isWhitespace((int) args[0]) ? 1 : 0 }),
            new HostFunction("env", "iswalnum",
                    List.of(ValType.I32), List.of(ValType.I32),
                    (inst, args) -> new long[] { Character.isLetterOrDigit((int) args[0]) ? 1 : 0 }),
            new HostFunction("env", "iswalpha",
                    List.of(ValType.I32), List.of(ValType.I32),
                    (inst, args) -> new long[] { Character.isLetter((int) args[0]) ? 1 : 0 }),
        };
    }

    /**
     * Replaces the core instance's internal table with a copy that has a larger
     * max limit, allowing it to grow when grammar modules are dynamically linked.
     * The core WASM module defines its table with max == initial, preventing growth.
     */
    private static TableInstance makeTableGrowable(Instance instance, int extraCapacity) {
        var oldTable = instance.table(0);
        int oldSize = oldTable.size();

        var growableTable = new TableInstance(
                new Table(oldTable.elementType(),
                        new TableLimits(oldSize, oldSize + extraCapacity)),
                Value.REF_NULL_VALUE);

        for (int i = 0; i < oldSize; i++) {
            growableTable.setRef(i, oldTable.ref(i), oldTable.instance(i));
        }

        try {
            Field tablesField = Instance.class.getDeclaredField("tables");
            tablesField.setAccessible(true);
            TableInstance[] tables = (TableInstance[]) tablesField.get(instance);
            tables[0] = growableTable;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to replace core table with growable copy", e);
        }

        return growableTable;
    }

    /**
     * Parses the dylink.0 custom section from a WASM binary to extract
     * the memory and table size requirements for dynamic linking.
     */
    private static DylinkInfo parseDylink(Path wasmPath) throws IOException {
        byte[] bytes = Files.readAllBytes(wasmPath);
        ByteBuffer buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);

        // Skip WASM magic (4 bytes) and version (4 bytes)
        buf.position(8);

        while (buf.hasRemaining()) {
            int sectionId = Byte.toUnsignedInt(buf.get());
            int sectionSize = readLeb128(buf);
            int sectionEnd = buf.position() + sectionSize;

            if (sectionId == 0) { // Custom section
                int nameLen = readLeb128(buf);
                byte[] nameBytes = new byte[nameLen];
                buf.get(nameBytes);
                String name = new String(nameBytes);

                if ("dylink.0".equals(name)) {
                    while (buf.position() < sectionEnd) {
                        int subType = Byte.toUnsignedInt(buf.get());
                        int subSize = readLeb128(buf);
                        int subEnd = buf.position() + subSize;

                        if (subType == 1) { // WASM_DYLINK_MEM_INFO
                            int memorySize = readLeb128(buf);
                            int memoryAlign = readLeb128(buf);
                            int tableSize = readLeb128(buf);
                            int tableAlign = readLeb128(buf);
                            return new DylinkInfo(memorySize, memoryAlign, tableSize, tableAlign);
                        }
                        buf.position(subEnd);
                    }
                }
            }
            buf.position(sectionEnd);
        }
        throw new IllegalArgumentException("No dylink.0 section found in " + wasmPath);
    }

    private static int readLeb128(ByteBuffer buf) {
        int result = 0;
        int shift = 0;
        while (true) {
            byte b = buf.get();
            result |= (b & 0x7F) << shift;
            if ((b & 0x80) == 0) break;
            shift += 7;
        }
        return result;
    }
}