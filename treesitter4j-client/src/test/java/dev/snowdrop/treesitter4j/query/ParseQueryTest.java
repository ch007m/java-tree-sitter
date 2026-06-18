package dev.snowdrop.treesitter4j.query;

import dev.snowdrop.treesitter4j.util.ASTQueryUtil;
import dev.snowdrop.treesitter4j.util.ASTQueryUtil.ParsedQuery;
import io.roastedroot.treesitter.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ASTQueryUtil#parseQuery(String)}.
 */
class ParseQueryTest {

    final ASTQueryUtil queryUtil = new ASTQueryUtil();

    @Test
    void parseAliasOnly() {
        ParsedQuery q = queryUtil.parseQuery("class");
        assertEquals("class", q.syntax());
        assertNull(q.operator());
        assertNull(q.value());
        assertNotNull(q.queryInfo());
        assertEquals(Language.JAVA, q.queryInfo().language());
    }

    @Test
    void parseAliasWithEquals() {
        ParsedQuery q = queryUtil.parseQuery("class = AppApplication");
        assertEquals("class", q.syntax());
        assertEquals("=", q.operator());
        assertEquals("AppApplication", q.value());
        assertNotNull(q.queryInfo());
    }

    @Test
    void parseAliasWithContains() {
        ParsedQuery q = queryUtil.parseQuery("annotation contains Entity");
        assertEquals("annotation", q.syntax());
        assertEquals("contains", q.operator());
        assertEquals("Entity", q.value());
        assertNotNull(q.queryInfo());
    }

    @Test
    void parseIsCaseInsensitive() {
        ParsedQuery q = queryUtil.parseQuery("CLASS = Foo");
        assertEquals("class", q.syntax());
        assertEquals("=", q.operator());
        assertEquals("Foo", q.value());
        assertNotNull(q.queryInfo());
    }

    @Test
    void parseRawNodeType() {
        ParsedQuery q = queryUtil.parseQuery("class_declaration");
        assertEquals("class_declaration", q.syntax());
        assertNull(q.operator());
        assertNull(q.value());
        assertNull(q.queryInfo(), "Raw node types should not resolve to an syntax");
    }

    @Test
    void parseRawNodeTypeWithOperator() {
        ParsedQuery q = queryUtil.parseQuery("class_declaration = MyClass");
        assertEquals("class_declaration", q.syntax());
        assertEquals("=", q.operator());
        assertEquals("MyClass", q.value());
    }

    @Test
    void parsePropertyAlias() {
        ParsedQuery q = queryUtil.parseQuery("property");
        assertEquals("property", q.syntax());
        assertNotNull(q.queryInfo());
        assertEquals(Language.PROPERTIES, q.queryInfo().language());
    }

    @Test
    void parseElementAlias() {
        ParsedQuery q = queryUtil.parseQuery("element = dependency");
        assertEquals("element", q.syntax());
        assertEquals("=", q.operator());
        assertEquals("dependency", q.value());
        assertNotNull(q.queryInfo());
        assertEquals(Language.XML, q.queryInfo().language());
    }

    @Test
    void parseContainsPreservesValueCase() {
        ParsedQuery q = queryUtil.parseQuery("method contains getData");
        assertEquals("method", q.syntax());
        assertEquals("contains", q.operator());
        assertEquals("getData", q.value(), "Value must preserve original case");
    }

    @Test
    void parseTrimsWhitespace() {
        ParsedQuery q = queryUtil.parseQuery("  class  =  MyApp  ");
        assertEquals("class", q.syntax());
        assertEquals("=", q.operator());
        assertEquals("MyApp", q.value());
    }

    @Test
    void parsePomDependencyAlias() {
        ParsedQuery q = queryUtil.parseQuery("pom-dependency = io.quarkus:quarkus-rest");
        assertEquals("pom-dependency", q.syntax());
        assertEquals("=", q.operator());
        assertEquals("io.quarkus:quarkus-rest", q.value());
        assertNotNull(q.queryInfo());
        assertEquals(Language.XML, q.queryInfo().language());
        assertNotNull(q.queryInfo().composer(), "POM aliases must have a composer");
    }

    @Test
    void parsePomPluginAlias() {
        ParsedQuery q = queryUtil.parseQuery("pom-plugin");
        assertEquals("pom-plugin", q.syntax());
        assertNull(q.operator());
        assertNull(q.value());
        assertNotNull(q.queryInfo());
        assertEquals(Language.XML, q.queryInfo().language());
        assertNotNull(q.queryInfo().composer());
    }
}
