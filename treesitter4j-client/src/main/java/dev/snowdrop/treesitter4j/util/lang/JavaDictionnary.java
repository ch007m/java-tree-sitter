package dev.snowdrop.treesitter4j.util.lang;

import dev.snowdrop.treesitter4j.util.ASTQueryUtil.QueryInfo;
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
public final class JavaDictionnary implements LanguageDictionary {

    private static final Map<String, QueryInfo> TYPE_QUERY_EXPRESSIONS;

    static {
        Map<String, QueryInfo> m = new LinkedHashMap<>();
        m.put("class", new QueryInfo("class_declaration",
                "(class_declaration name: (identifier) @name)", Language.JAVA));
        m.put("annotation", new QueryInfo("annotation",
                "(marker_annotation name: (identifier) @name)\n(annotation name: (identifier) @name)", Language.JAVA));
        m.put("method", new QueryInfo("method_declaration",
                "(method_declaration name: (identifier) @name)", Language.JAVA));
        m.put("import", new QueryInfo("import_declaration",
                "(import_declaration (scoped_identifier) @name)", Language.JAVA));
        m.put("interface", new QueryInfo("interface_declaration",
                "(interface_declaration name: (identifier) @name)", Language.JAVA));
        m.put("field", new QueryInfo("field_declaration",
                "(field_declaration declarator: (variable_declarator name: (identifier) @name))", Language.JAVA));
        m.put("enum", new QueryInfo("enum_declaration",
                "(enum_declaration name: (identifier) @name)", Language.JAVA));
        m.put("package", new QueryInfo("package_declaration",
                "(package_declaration (scoped_identifier) @name)", Language.JAVA));
        m.put("constructor", new QueryInfo("constructor_declaration",
                "(constructor_declaration name: (identifier) @name)", Language.JAVA));
        TYPE_QUERY_EXPRESSIONS = Collections.unmodifiableMap(m);
    }

    @Override
    public Language language() {
        return Language.JAVA;
    }

    @Override
    public Map<String, QueryInfo> getTypeAndQueryExpression() {
        return TYPE_QUERY_EXPRESSIONS;
    }
}
