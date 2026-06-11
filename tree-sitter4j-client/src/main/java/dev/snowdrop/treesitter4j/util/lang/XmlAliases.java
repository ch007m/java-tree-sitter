package dev.snowdrop.treesitter4j.util.lang;

import dev.snowdrop.treesitter4j.util.ASTQueryUtil.AliasInfo;
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
public final class XmlAliases implements LanguageAliases {

    private static final Map<String, AliasInfo> ALIASES;

    static {
        Map<String, AliasInfo> m = new LinkedHashMap<>();
        m.put("element", new AliasInfo("element",
                "(element (start_tag (tag_name) @name))", Language.XML));
        m.put("attribute", new AliasInfo("attribute",
                "(attribute (attribute_name) @name)", Language.XML));
        ALIASES = Collections.unmodifiableMap(m);
    }

    @Override
    public Language language() {
        return Language.XML;
    }

    @Override
    public Map<String, AliasInfo> aliases() {
        return ALIASES;
    }
}
