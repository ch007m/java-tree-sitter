package dev.snowdrop.treesitter4j.util.lang;

import dev.snowdrop.treesitter4j.util.ASTQueryUtil.AliasInfo;
import io.roastedroot.treesitter.Language;

import java.util.Map;

/**
 * Provides the alias dictionary for a single tree-sitter language.
 * <p>
 * Each implementation owns the friendly-name-to-{@link AliasInfo} mappings
 * for one language and can house language-specific helper methods.
 */
public interface LanguageAliases {

    /** The tree-sitter language this alias set belongs to. */
    Language language();

    /**
     * Returns an unmodifiable map of friendly alias names to their
     * {@link AliasInfo} descriptors.
     */
    Map<String, AliasInfo> aliases();
}
