package dev.snowdrop.treesitter4j.util.lang;

import dev.snowdrop.treesitter4j.util.ASTQueryUtil.AliasInfo;
import io.roastedroot.treesitter.Language;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Alias dictionary for the Properties language.
 * <p>
 * Grammar reference:
 * <a href="https://github.com/tree-sitter-grammars/tree-sitter-properties/blob/main/src/node-types.json">
 *   tree-sitter-properties node types</a>
 */
public final class PropertiesAliases implements LanguageAliases {

    private static final Map<String, AliasInfo> ALIASES;

    static {
        Map<String, AliasInfo> m = new LinkedHashMap<>();
        m.put("property", new AliasInfo("property",
                "(property (key) @name)", Language.PROPERTIES));
        ALIASES = Collections.unmodifiableMap(m);
    }

    @Override
    public Language language() {
        return Language.PROPERTIES;
    }

    @Override
    public Map<String, AliasInfo> aliases() {
        return ALIASES;
    }
}
