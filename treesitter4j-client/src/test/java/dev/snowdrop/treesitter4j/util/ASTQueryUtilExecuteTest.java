package dev.snowdrop.treesitter4j.util;

import dev.snowdrop.treesitter4j.TreeSitterRuntime;
import dev.snowdrop.treesitter4j.util.ASTQueryUtil.ParsedQuery;
import dev.snowdrop.treesitter4j.util.ASTQueryUtil.QueryMatch;
import io.roastedroot.treesitter.Language;
import io.roastedroot.treesitter.TreeSitter;
import io.roastedroot.treesitter.TreeSitterParser;
import io.roastedroot.treesitter.TreeSitterTree;
import io.roastedroot.treesitter.ast.ASTExporter;
import io.roastedroot.treesitter.ast.ASTTree;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ASTQueryUtil#execute(ParsedQuery, List, String, Set)}.
 * Verifies that human-friendly queries produce correct results using
 * the tree-sitter query API.
 */
class ASTQueryUtilExecuteTest {

    final ASTQueryUtil queryUtil = new ASTQueryUtil();

    static final String JAVA_SOURCE = """
            package com.example;

            import jakarta.persistence.Entity;
            import jakarta.persistence.Id;
            import jakarta.persistence.Column;

            @Entity
            public class Customer {

                @Id
                private Long id;

                @Column(name = "full_name")
                private String name;

                public Customer() {}

                public Long getId() {
                    return id;
                }

                public String getName() {
                    return name;
                }

                public void setName(String name) {
                    this.name = name;
                }
            }
            """;

    static final String JAVA_SOURCE_2 = """
            package com.example;

            public interface CustomerRepository {
                Customer findById(Long id);
            }
            """;

    static final String PROPERTIES_SOURCE = """
            quarkus.datasource.db-kind=postgresql
            quarkus.datasource.username=admin
            quarkus.datasource.password=secret
            quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/mydb
            quarkus.hibernate-orm.database.generation=drop-and-create
            quarkus.http.port=8080
            """;

    static List<ASTTree> trees;
    static List<ASTTree> allTrees;

    @BeforeAll
    static void parseSource() {
        TreeSitter ts = TreeSitterRuntime.get();
        try (TreeSitterParser parser = ts.newParser()) {
            // Parse Java sources
            parser.setLanguage(Language.JAVA);
            ASTTree ast1;
            ASTTree ast2;
            try (TreeSitterTree tree1 = parser.parseString(JAVA_SOURCE)) {
                ast1 = ASTExporter.export(tree1, Language.JAVA, JAVA_SOURCE, "src/main/java/com/example/Customer.java");
            }
            try (TreeSitterTree tree2 = parser.parseString(JAVA_SOURCE_2)) {
                ast2 = ASTExporter.export(tree2, Language.JAVA, JAVA_SOURCE_2, "src/main/java/com/example/CustomerRepository.java");
            }
            trees = List.of(ast1, ast2);

            // Parse Properties source
            parser.setLanguage(Language.PROPERTIES);
            ASTTree ast3;
            try (TreeSitterTree tree3 = parser.parseString(PROPERTIES_SOURCE)) {
                ast3 = ASTExporter.export(tree3, Language.PROPERTIES, PROPERTIES_SOURCE, "src/main/resources/application.properties");
            }

            allTrees = List.of(ast1, ast2, ast3);
        }
    }


    // -----------------------------------------------------------------------
    // Class queries
    // -----------------------------------------------------------------------

    @Test
    void queryAllClasses() {
        ParsedQuery q = queryUtil.parseQuery("class");
        List<QueryMatch> matches = queryUtil.execute(q, trees, null, null);

        assertEquals(1, matches.size());
        assertEquals("Customer", matches.get(0).matchedText());
    }

    @Test
    void queryClassByExactName() {
        ParsedQuery q = queryUtil.parseQuery("class = Customer");
        List<QueryMatch> matches = queryUtil.execute(q, trees, null, null);

        assertEquals(1, matches.size());
        assertEquals("Customer", matches.get(0).matchedText());
        assertTrue(matches.get(0).sourceFile().contains("Customer.java"));
    }

    @Test
    void queryClassByExactNameNoMatch() {
        ParsedQuery q = queryUtil.parseQuery("class = NonExistent");
        List<QueryMatch> matches = queryUtil.execute(q, trees, null, null);

        assertTrue(matches.isEmpty());
    }

    @Test
    void queryClassContains() {
        ParsedQuery q = queryUtil.parseQuery("class contains Cust");
        List<QueryMatch> matches = queryUtil.execute(q, trees, null, null);

        assertEquals(1, matches.size());
        assertEquals("Customer", matches.get(0).matchedText());
    }

    // -----------------------------------------------------------------------
    // Annotation queries
    // -----------------------------------------------------------------------

    @Test
    void queryAllAnnotations() {
        ParsedQuery q = queryUtil.parseQuery("annotation");
        List<QueryMatch> matches = queryUtil.execute(q, trees, null, null);

        List<String> names = matches.stream().map(QueryMatch::matchedText).toList();
        assertTrue(names.contains("Entity"), "Should find @Entity");
        assertTrue(names.contains("Id"), "Should find @Id");
        assertTrue(names.contains("Column"), "Should find @Column");
    }

    @Test
    void queryAnnotationByExactName() {
        ParsedQuery q = queryUtil.parseQuery("annotation = Entity");
        List<QueryMatch> matches = queryUtil.execute(q, trees, null, null);

        assertEquals(1, matches.size());
        assertEquals("Entity", matches.get(0).matchedText());
    }

    @Test
    void queryAnnotationByAtPrefixedName() {
        ParsedQuery q = queryUtil.parseQuery("annotation = @Entity");
        List<QueryMatch> matches = queryUtil.execute(q, trees, null, null);

        assertEquals(1, matches.size());
        assertEquals("Entity", matches.get(0).matchedText());
    }

    @Test
    void queryAnnotationByAtPrefixedNameColumn() {
        ParsedQuery q = queryUtil.parseQuery("annotation = @Column");
        List<QueryMatch> matches = queryUtil.execute(q, trees, null, null);

        assertEquals(1, matches.size());
        assertEquals("Column", matches.get(0).matchedText());
    }

    @Test
    void queryAnnotationContains() {
        ParsedQuery q = queryUtil.parseQuery("annotation contains ol");
        List<QueryMatch> matches = queryUtil.execute(q, trees, null, null);

        assertEquals(1, matches.size());
        assertEquals("Column", matches.get(0).matchedText());
    }

    @Test
    void queryAnnotationContainsWithAtPrefix() {
        ParsedQuery q = queryUtil.parseQuery("annotation contains @Col");
        List<QueryMatch> matches = queryUtil.execute(q, trees, null, null);

        assertEquals(1, matches.size());
        assertEquals("Column", matches.get(0).matchedText());
    }

    // -----------------------------------------------------------------------
    // Method queries
    // -----------------------------------------------------------------------

    @Test
    void queryAllMethods() {
        ParsedQuery q = queryUtil.parseQuery("method");
        List<QueryMatch> matches = queryUtil.execute(q, trees, null, null);

        List<String> names = matches.stream().map(QueryMatch::matchedText).toList();
        assertTrue(names.contains("getId"));
        assertTrue(names.contains("getName"));
        assertTrue(names.contains("setName"));
    }

    @Test
    void queryMethodByExactName() {
        ParsedQuery q = queryUtil.parseQuery("method = getId");
        List<QueryMatch> matches = queryUtil.execute(q, trees, null, null);

        assertEquals(1, matches.size());
        assertEquals("getId", matches.get(0).matchedText());
    }

    @Test
    void queryMethodContains() {
        ParsedQuery q = queryUtil.parseQuery("method contains get");
        List<QueryMatch> matches = queryUtil.execute(q, trees, null, null);

        // "getId" and "getName" both contain "get" (case-insensitive)
        assertEquals(2, matches.size());
    }

    // -----------------------------------------------------------------------
    // Import queries
    // -----------------------------------------------------------------------

    @Test
    void queryAllImports() {
        ParsedQuery q = queryUtil.parseQuery("import");
        List<QueryMatch> matches = queryUtil.execute(q, trees, null, null);

        List<String> names = matches.stream().map(QueryMatch::matchedText).toList();
        assertTrue(names.contains("jakarta.persistence.Entity"));
        assertTrue(names.contains("jakarta.persistence.Id"));
        assertTrue(names.contains("jakarta.persistence.Column"));
    }

    @Test
    void queryImportContains() {
        ParsedQuery q = queryUtil.parseQuery("import contains persistence");
        List<QueryMatch> matches = queryUtil.execute(q, trees, null, null);

        assertEquals(3, matches.size(), "All 3 jakarta.persistence imports should match");
    }

    // -----------------------------------------------------------------------
    // Interface queries
    // -----------------------------------------------------------------------

    @Test
    void queryInterface() {
        ParsedQuery q = queryUtil.parseQuery("interface");
        List<QueryMatch> matches = queryUtil.execute(q, trees, null, null);

        assertEquals(1, matches.size());
        assertEquals("CustomerRepository", matches.get(0).matchedText());
    }

    // -----------------------------------------------------------------------
    // Package queries
    // -----------------------------------------------------------------------

    @Test
    void queryPackage() {
        ParsedQuery q = queryUtil.parseQuery("package");
        List<QueryMatch> matches = queryUtil.execute(q, trees, null, null);

        assertEquals(2, matches.size());
        assertTrue(matches.stream().allMatch(m -> "com.example".equals(m.matchedText())));
    }

    // -----------------------------------------------------------------------
    // Constructor queries
    // -----------------------------------------------------------------------

    @Test
    void queryConstructor() {
        ParsedQuery q = queryUtil.parseQuery("constructor");
        List<QueryMatch> matches = queryUtil.execute(q, trees, null, null);

        assertEquals(1, matches.size());
        assertEquals("Customer", matches.get(0).matchedText());
    }

    // -----------------------------------------------------------------------
    // File filter
    // -----------------------------------------------------------------------

    @Test
    void queryWithFileFilter() {
        ParsedQuery q = queryUtil.parseQuery("method");
        List<QueryMatch> matches = queryUtil.execute(q, trees, "Repository", null);

        // CustomerRepository has no methods, only Customer.java does
        // The file filter "Repository" should exclude Customer.java
        List<QueryMatch> fromRepo = matches.stream()
                .filter(m -> m.sourceFile().contains("Repository"))
                .toList();
        assertEquals(matches.size(), fromRepo.size(), "All matches should be from Repository file");
    }

    // -----------------------------------------------------------------------
    // Language override
    // -----------------------------------------------------------------------

    @Test
    void queryWithLanguageOverrideAll() {
        ParsedQuery q = queryUtil.parseQuery("class");
        List<QueryMatch> matches = queryUtil.execute(q, trees, null, EnumSet.allOf(Language.class));

        // Should still find Java classes even when searching all languages
        assertFalse(matches.isEmpty());
        assertEquals("Customer", matches.get(0).matchedText());
    }

    @Test
    void queryWithWrongLanguageOverride() {
        ParsedQuery q = queryUtil.parseQuery("class");
        List<QueryMatch> matches = queryUtil.execute(q, trees, null, EnumSet.of(Language.YAML));

        // No YAML trees loaded — should find nothing
        assertTrue(matches.isEmpty());
    }

    // -----------------------------------------------------------------------
    // Raw node type (no alias)
    // -----------------------------------------------------------------------

    @Test
    void queryRawNodeType() {
        ParsedQuery q = queryUtil.parseQuery("class_declaration");
        List<QueryMatch> matches = queryUtil.execute(q, trees, null, null);

        // Raw type captures the full node — should still produce results
        assertFalse(matches.isEmpty());
    }

    // -----------------------------------------------------------------------
    // Line number
    // -----------------------------------------------------------------------

    @Test
    void matchContainsCorrectLineNumber() {
        ParsedQuery q = queryUtil.parseQuery("class = Customer");
        List<QueryMatch> matches = queryUtil.execute(q, trees, null, null);

        assertEquals(1, matches.size());
        // "public class Customer" is on line 8 of JAVA_SOURCE
        assertTrue(matches.get(0).line() > 1, "Line should be > 1 for a non-first-line declaration");
    }

    // -----------------------------------------------------------------------
    // Property queries
    // -----------------------------------------------------------------------

    @Test
    void queryAllProperties() {
        ParsedQuery q = queryUtil.parseQuery("property");
        List<QueryMatch> matches = queryUtil.execute(q, allTrees, null, null);

        assertEquals(6, matches.size(), "Should find all 6 properties");
    }

    @Test
    void queryPropertyByExactKey() {
        ParsedQuery q = queryUtil.parseQuery("property = quarkus.http.port");
        List<QueryMatch> matches = queryUtil.execute(q, allTrees, null, null);

        assertEquals(1, matches.size());
        assertEquals("quarkus.http.port", matches.get(0).matchedText());
    }

    @Test
    void queryPropertyByWildcardSuffix() {
        ParsedQuery q = queryUtil.parseQuery("property = quarkus.datasource.*");
        List<QueryMatch> matches = queryUtil.execute(q, allTrees, null, null);

        // quarkus.datasource.db-kind, username, password, jdbc.url
        assertEquals(4, matches.size());
        assertTrue(matches.stream().allMatch(m -> m.matchedText().startsWith("quarkus.datasource.")));
    }

    @Test
    void queryPropertyByWildcardMiddle() {
        ParsedQuery q = queryUtil.parseQuery("property = quarkus.*.generation");
        List<QueryMatch> matches = queryUtil.execute(q, allTrees, null, null);

        assertEquals(1, matches.size());
        assertEquals("quarkus.hibernate-orm.database.generation", matches.get(0).matchedText());
    }

    @Test
    void queryPropertyByWildcardPrefix() {
        ParsedQuery q = queryUtil.parseQuery("property = *.port");
        List<QueryMatch> matches = queryUtil.execute(q, allTrees, null, null);

        assertEquals(1, matches.size());
        assertEquals("quarkus.http.port", matches.get(0).matchedText());
    }

    @Test
    void queryPropertyContains() {
        ParsedQuery q = queryUtil.parseQuery("property contains jdbc");
        List<QueryMatch> matches = queryUtil.execute(q, allTrees, null, null);

        assertEquals(1, matches.size());
        assertTrue(matches.get(0).matchedText().contains("jdbc"));
    }

    @Test
    void queryPropertyOnlySearchesPropertiesFiles() {
        // "property" alias targets Language.PROPERTIES — should not touch Java files
        ParsedQuery q = queryUtil.parseQuery("property");
        List<QueryMatch> matches = queryUtil.execute(q, allTrees, null, null);

        assertTrue(matches.stream().allMatch(m -> m.sourceFile().endsWith(".properties")),
                "Property queries should only match .properties files");
    }
}
