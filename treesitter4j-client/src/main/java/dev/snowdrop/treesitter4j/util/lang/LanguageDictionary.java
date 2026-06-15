package dev.snowdrop.treesitter4j.util.lang;

import dev.snowdrop.treesitter4j.util.ASTQueryUtil.QueryInfo;
import io.roastedroot.treesitter.Language;

import java.util.Map;

/**
 * Provides a dictionary of Query or S-Expression for a tree-sitter language.
 * <p>
 * Each implementation owns the friendly-name-to-{@link QueryInfo} mappings
 * for one language and can house language-specific helper methods.
 */
public interface LanguageDictionary {

    /** The tree-sitter language this dictionary set belongs to. */
    Language language();

    /**
     * Returns an unmodifiable map of friendly alias names to their
     * {@link QueryInfo} descriptors.
     */
    Map<String, QueryInfo> getTypeAndQueryExpression();
}
