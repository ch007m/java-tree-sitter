package dev.snowdrop.treesitter.jtree.languages;

import java.io.IOException;
import java.lang.foreign.*;
import java.nio.file.Path;

public final class TreeSitterJava {
    private static final ValueLayout VOID_PTR =
            ValueLayout.ADDRESS.withTargetLayout(MemoryLayout.sequenceLayout(Long.MAX_VALUE, ValueLayout.JAVA_BYTE));
    private static final FunctionDescriptor FUNC_DESC = FunctionDescriptor.of(VOID_PTR);
    private static final Linker LINKER = Linker.nativeLinker();
    private static final TreeSitterJava INSTANCE = new TreeSitterJava();

    private final Arena arena = Arena.ofAuto();
    private final SymbolLookup symbols = findLibrary();

    /**
     * {@snippet lang=c :
     * const TSLanguage *tree_sitter_java()
     * }
     */
    public static MemorySegment language() {
        return INSTANCE.call("tree_sitter_java");
    }

    private SymbolLookup findLibrary() {
        try {
            Path extensionsPath = Path.of(System.getProperty("user.home"), "Library/Java/Extensions/tree-sitter-java.dylib");

            // 2. Resolve to the absolute physical path on your Mac
            Path realPath = extensionsPath.toRealPath();
            System.out.println("Loading Java Grammar symbols explicitly from: " + realPath);

            // 3. Force Panama to open this specific file
            return SymbolLookup.libraryLookup(realPath, arena);
        } catch (IllegalArgumentException e) {
            return SymbolLookup.loaderLookup();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("SameParameterValue")
    private MemorySegment call(String name) throws UnsatisfiedLinkError {
        var address = symbols.find(name).orElseThrow(() -> unresolved(name));
        try {
            var function = LINKER.downcallHandle(address, FUNC_DESC);
            return (MemorySegment) function.invokeExact();
        } catch (Throwable e) {
            throw new RuntimeException("Call to %s failed".formatted(name), e);
        }
    }

    private static UnsatisfiedLinkError unresolved(String name) {
        return new UnsatisfiedLinkError("Unresolved symbol: %s".formatted(name));
    }
}