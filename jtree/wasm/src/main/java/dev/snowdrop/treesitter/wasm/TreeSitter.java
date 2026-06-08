package dev.snowdrop.treesitter.wasm;

import com.dylibso.chicory.runtime.ImportValues;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.wasi.WasiOptions;
import com.dylibso.chicory.wasi.WasiPreview1;
import com.dylibso.chicory.wasm.types.MemoryLimits;

public final class TreeSitter implements AutoCloseable {
    private final Instance instance;
    private final WasiPreview1 wasi;
    private final TreeSitter_ModuleExports exports;

    private TreeSitter(MemoryLimits memoryLimits) {
        var wasiOpts = WasiOptions.builder().build();
        this.wasi = WasiPreview1.builder().withOptions(wasiOpts).build();
        var imports = ImportValues.builder().addFunction(wasi.toHostFunctions()).build();
        var builder = Instance.builder(TreeSitterModule.load())
                        .withImportValues(imports)
                        .withMachineFactory(TreeSitterModule::create);
        if (memoryLimits != null) {
            builder.withMemoryLimits(memoryLimits);
        }
        this.instance = builder.build();
        this.exports = new TreeSitter_ModuleExports(this.instance);
    }

    public static Builder builder() {
        return new Builder();
    }

    public Instance instance() {
        return instance;
    }

    public TreeSitter_ModuleExports exports() {
        return exports;
    }

    @Override
    public void close() {
        if (wasi != null) {
            wasi.close();
        }
    }

    public static final class Builder {
        private MemoryLimits memoryLimits;

        private Builder() {}

        public Builder withMemoryLimits(MemoryLimits memoryLimits) {
            this.memoryLimits = memoryLimits;
            return this;
        }

        public TreeSitter build() {
            return new TreeSitter(memoryLimits);
        }
    }
}
