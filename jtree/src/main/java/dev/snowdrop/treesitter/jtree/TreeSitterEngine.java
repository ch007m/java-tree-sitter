package dev.snowdrop.treesitter.jtree;

import dev.kreuzberg.treesitterlanguagepack.PackConfig;
import dev.kreuzberg.treesitterlanguagepack.ProcessConfig;
import dev.kreuzberg.treesitterlanguagepack.ProcessResult;
import dev.kreuzberg.treesitterlanguagepack.StructureItem;
import dev.kreuzberg.treesitterlanguagepack.TreeSitterLanguagePack;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TreeSitterEngine {

    private static final List<String> SUPPORTED_LANGUAGES =
            List.of("java", "yaml", "json", "properties", "markdown");

    public TreeSitterEngine() {
        try {
            PackConfig config = PackConfig.builder()
                    .withLanguages(SUPPORTED_LANGUAGES)
                    .build();
            TreeSitterLanguagePack.init(config);
            TreeSitterLanguagePack.download(SUPPORTED_LANGUAGES);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize TreeSitterLanguagePack", e);
        }
    }

    public List<String> supportedLanguages() {
        return SUPPORTED_LANGUAGES;
    }

    public Optional<String> detectLanguage(Path filePath) {
        try {
            return Optional.ofNullable(
                    TreeSitterLanguagePack.detectLanguageFromPath(filePath.toString()));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public boolean isSupported(String language) {
        return SUPPORTED_LANGUAGES.contains(language);
    }

    public List<NodeStore.NodeEntry> parseResource(Path filePath) throws Exception {
        List<NodeStore.NodeEntry> nodes = new ArrayList<>();

        Optional<String> detected = detectLanguage(filePath);
        if (detected.isEmpty()) {
            return nodes;
        }
        String language = detected.get();

        if (!isSupported(language)) {
            return nodes;
        }

        String sourceContent = Files.readString(filePath);

        ProcessConfig.Builder builder = ProcessConfig.builder()
                .withLanguage(language);

        switch (language) {
            case "java"       -> builder.withStructure(true).withImports(true);
            case "yaml", "json" -> builder.withStructure(true).withImports(false);
            case "properties" -> builder.withStructure(true).withImports(false);
            case "markdown"   -> builder.withStructure(false).withImports(false);
        }

        ProcessConfig config = builder.build();
        ProcessResult result = TreeSitterLanguagePack.process(sourceContent, config);

        if (result.structure() != null) {
            collectStructureItems(result.structure(), nodes);
        }

        if (result.imports() != null) {
            result.imports().forEach(imp -> {
                long line = imp.span() != null ? imp.span().startLine() + 1 : 0;
                long endLine = imp.span() != null ? imp.span().endLine() + 1 : line;
                nodes.add(new NodeStore.NodeEntry(
                        "import", imp.source(), (int) line, (int) endLine));
            });
        }

        return nodes;
    }

    private void collectStructureItems(List<StructureItem> items, List<NodeStore.NodeEntry> nodes) {
        for (StructureItem item : items) {
            String kind = item.kind() != null ? item.kind().getValue() : "other";
            String name = item.name() != null ? item.name() : "";
            int startLine = item.span() != null ? (int) item.span().startLine() + 1 : 0;
            int endLine = item.span() != null ? (int) item.span().endLine() + 1 : startLine;

            nodes.add(new NodeStore.NodeEntry(kind, name, startLine, endLine));

            if (item.children() != null) {
                collectStructureItems(item.children(), nodes);
            }
        }
    }
}
