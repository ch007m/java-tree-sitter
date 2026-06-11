package dev.snowdrop.treesitter4j.util.lang;

import dev.snowdrop.treesitter4j.util.ASTQueryUtil.AliasInfo;
import io.roastedroot.treesitter.Language;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Alias dictionary for the Java language.
 * <p>
 * Grammar reference:
 * <a href="https://github.com/tree-sitter/tree-sitter-java/blob/master/src/node-types.json">
 *   tree-sitter-java node types</a>
 */
public final class JavaAliases implements LanguageAliases {

    private static final Map<String, AliasInfo> ALIASES;

    static {
        Map<String, AliasInfo> m = new LinkedHashMap<>();
        m.put("class", new AliasInfo("class_declaration",
                "(class_declaration name: (identifier) @name)", Language.JAVA));
        m.put("annotation", new AliasInfo("annotation",
                "(marker_annotation name: (identifier) @name)\n(annotation name: (identifier) @name)", Language.JAVA));
        m.put("method", new AliasInfo("method_declaration",
                "(method_declaration name: (identifier) @name)", Language.JAVA));
        m.put("import", new AliasInfo("import_declaration",
                "(import_declaration (scoped_identifier) @name)", Language.JAVA));
        m.put("interface", new AliasInfo("interface_declaration",
                "(interface_declaration name: (identifier) @name)", Language.JAVA));
        m.put("field", new AliasInfo("field_declaration",
                "(field_declaration declarator: (variable_declarator name: (identifier) @name))", Language.JAVA));
        m.put("enum", new AliasInfo("enum_declaration",
                "(enum_declaration name: (identifier) @name)", Language.JAVA));
        m.put("package", new AliasInfo("package_declaration",
                "(package_declaration (scoped_identifier) @name)", Language.JAVA));
        m.put("constructor", new AliasInfo("constructor_declaration",
                "(constructor_declaration name: (identifier) @name)", Language.JAVA));
        ALIASES = Collections.unmodifiableMap(m);
    }

    @Override
    public Language language() {
        return Language.JAVA;
    }

    @Override
    public Map<String, AliasInfo> aliases() {
        return ALIASES;
    }
}
