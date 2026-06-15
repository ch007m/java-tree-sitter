package dev.snowdrop.treesitter4j.util.lang;

import dev.snowdrop.treesitter4j.util.ASTQueryUtil.QueryInfo;
import io.roastedroot.treesitter.Language;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Alias dictionary for the XML language.
 * <p>
 * Grammar reference:
 * <a href="https://github.com/tree-sitter-grammars/tree-sitter-xml/blob/master/xml/src/node-types.json">
 *   tree-sitter-xml node types</a>
 */
public final class XmlDictionnary implements LanguageDictionary {

    private static final Map<String, QueryInfo> ALIASES;

    static {
        Map<String, QueryInfo> m = new LinkedHashMap<>();
        m.put("element", new QueryInfo("element",
                "(element (start_tag (tag_name) @name))", Language.XML));
        m.put("attribute", new QueryInfo("attribute",
                "(attribute (attribute_name) @name)", Language.XML));
        ALIASES = Collections.unmodifiableMap(m);
    }

    @Override
    public Language language() {
        return Language.XML;
    }

    @Override
    public Map<String, QueryInfo> getTypeAndQueryExpression() {
        return ALIASES;
    }
}
