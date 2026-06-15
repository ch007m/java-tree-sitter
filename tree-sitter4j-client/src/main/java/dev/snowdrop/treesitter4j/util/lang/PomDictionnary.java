package dev.snowdrop.treesitter4j.util.lang;

import dev.snowdrop.treesitter4j.util.ASTQueryUtil.QueryInfo;
import dev.snowdrop.treesitter4j.util.ASTQueryUtil.ValueComposer;
import io.roastedroot.treesitter.Language;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Alias dictionary for Maven POM files (XML language).
 * <p>
 * Provides aliases that compose GAV (groupId:artifactId[:version]) coordinates
 * from the child elements of {@code <dependency>}, {@code <plugin>},
 * {@code <parent>}, and {@code <extension>} XML elements.
 * <p>
 * Each alias captures the entire {@code (element) @name} node, then uses a
 * {@link ValueComposer} to extract the GAV string. Elements whose tag name
 * does not match the expected type are skipped (composer returns {@code null}).
 */
public final class PomDictionnary implements LanguageDictionary {

    /** Pattern to capture entire XML elements. */
    private static final String ELEMENT_PATTERN = "(element) @name";

    private static final Map<String, QueryInfo> ALIASES;

    static {
        Map<String, QueryInfo> m = new LinkedHashMap<>();
        m.put("pom-dependency", new QueryInfo("element", ELEMENT_PATTERN, Language.XML,
                gavComposer("dependency")));
        m.put("pom-plugin", new QueryInfo("element", ELEMENT_PATTERN, Language.XML,
                gavComposer("plugin")));
        m.put("pom-parent", new QueryInfo("element", ELEMENT_PATTERN, Language.XML,
                gavComposer("parent")));
        m.put("pom-extension", new QueryInfo("element", ELEMENT_PATTERN, Language.XML,
                gavComposer("extension")));
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

    // -----------------------------------------------------------------------
    // GAV composer factory
    // -----------------------------------------------------------------------

    // Regex to extract the tag name from the opening tag of the captured element.
    // Matches e.g. "<dependency>" or "<dependency " at the start of the text.
    private static final Pattern TAG_NAME_PATTERN = Pattern.compile("^\\s*<(\\w+)[\\s>]");

    // Regex patterns to extract child element text content.
    private static final Pattern GROUP_ID_PATTERN =
            Pattern.compile("<groupId>\\s*([^<]+?)\\s*</groupId>");
    private static final Pattern ARTIFACT_ID_PATTERN =
            Pattern.compile("<artifactId>\\s*([^<]+?)\\s*</artifactId>");
    private static final Pattern VERSION_PATTERN =
            Pattern.compile("<version>\\s*([^<]+?)\\s*</version>");

    /**
     * Creates a {@link ValueComposer} that checks whether the captured XML element
     * has the expected tag name, then extracts groupId, artifactId, and optionally
     * version to produce a GAV string like {@code groupId:artifactId[:version]}.
     *
     * @param expectedTag the XML tag name to match (e.g. "dependency", "plugin")
     * @return a composer that returns the GAV string, or {@code null} to skip
     */
    static ValueComposer gavComposer(String expectedTag) {
        return capturedText -> {
            // Check if this element's tag matches the expected type
            Matcher tagMatcher = TAG_NAME_PATTERN.matcher(capturedText);
            if (!tagMatcher.find() || !expectedTag.equals(tagMatcher.group(1))) {
                return null;
            }

            // Extract GAV components
            Matcher groupMatcher = GROUP_ID_PATTERN.matcher(capturedText);
            Matcher artifactMatcher = ARTIFACT_ID_PATTERN.matcher(capturedText);
            if (!groupMatcher.find() || !artifactMatcher.find()) {
                return null; // not a valid GAV element
            }

            String groupId = groupMatcher.group(1);
            String artifactId = artifactMatcher.group(1);

            Matcher versionMatcher = VERSION_PATTERN.matcher(capturedText);
            if (versionMatcher.find()) {
                return groupId + ":" + artifactId + ":" + versionMatcher.group(1);
            }
            return groupId + ":" + artifactId;
        };
    }
}
