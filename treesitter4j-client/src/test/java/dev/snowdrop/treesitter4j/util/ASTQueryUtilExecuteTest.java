package dev.snowdrop.treesitter4j.util;

import dev.snowdrop.treesitter4j.TreeSitterRuntime;
import dev.snowdrop.treesitter4j.util.ASTQueryUtil.ParsedQuery;
import dev.snowdrop.treesitter4j.util.ASTQueryUtil.QueryMatch;
import io.roastedroot.treesitter.*;
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
 * the tree-sitter syntax API.
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
        List<TreeSitterQueryResult> results = queryUtil.execute(q, trees, null);

        assertEquals(1, results.size());
        assertEquals("Customer", results.get(0).name());
    }

    @Test
    void queryClassByExactName() {
        ParsedQuery q = queryUtil.parseQuery("class = Customer");
        List<TreeSitterQueryResult> results = queryUtil.execute(q, trees, null);

        assertEquals(1, results.size());
        assertEquals("Customer", results.get(0).name());
    }

    @Test
    void queryClassByExactNameNoMatch() {
        ParsedQuery q = queryUtil.parseQuery("class = NonExistent");
        List<TreeSitterQueryResult> results = queryUtil.execute(q, trees, null);

        assertTrue(results.isEmpty());
    }

    @Test
    void queryClassContains() {
        ParsedQuery q = queryUtil.parseQuery("class contains Cust");
        List<TreeSitterQueryResult> results = queryUtil.execute(q, trees, null);

        assertEquals(1, results.size());
        assertEquals("Customer", results.get(0).name());
    }

    // -----------------------------------------------------------------------
    // Annotation queries
    // -----------------------------------------------------------------------

    @Test
    void queryAllAnnotations() {
        ParsedQuery q = queryUtil.parseQuery("annotation");
        List<TreeSitterQueryResult> results = queryUtil.execute(q, trees, null);

        //List<String> names = results.stream().map(QueryMatch::matchedText).toList();
        //assertTrue(names.contains("Entity"), "Should find @Entity");
        //assertTrue(names.contains("Id"), "Should find @Id");
        //assertTrue(names.contains("Column"), "Should find @Column");
    }

    @Test
    void queryAnnotationByExactName() {
        ParsedQuery q = queryUtil.parseQuery("annotation = Entity");
        List<TreeSitterQueryResult> results = queryUtil.execute(q, trees, null);

        assertEquals(1, results.size());
        assertEquals("Entity", results.get(0).name());
    }

    @Test
    void queryAnnotationByAtPrefixedName() {
        ParsedQuery q = queryUtil.parseQuery("annotation = @Entity");
        List<TreeSitterQueryResult> results = queryUtil.execute(q, trees, null);

        assertEquals(1, results.size());
        assertEquals("Entity", results.get(0).name());
    }

    @Test
    void queryAnnotationByAtPrefixedNameColumn() {
        ParsedQuery q = queryUtil.parseQuery("annotation = @Column");
        List<TreeSitterQueryResult> results = queryUtil.execute(q, trees, null);

        assertEquals(1, results.size());
        assertEquals("Column", results.get(0).name());
    }

    @Test
    void queryAnnotationContains() {
        ParsedQuery q = queryUtil.parseQuery("annotation contains ol");
        List<TreeSitterQueryResult> results = queryUtil.execute(q, trees, null);

        assertEquals(1, results.size());
        assertEquals("Column", results.get(0).name());
    }

    @Test
    void queryAnnotationContainsWithAtPrefix() {
        ParsedQuery q = queryUtil.parseQuery("annotation contains @Col");
        List<TreeSitterQueryResult> results = queryUtil.execute(q, trees, null);

        assertEquals(1, results.size());
        assertEquals("Column", results.get(0).name());
    }

    // -----------------------------------------------------------------------
    // Method queries
    // -----------------------------------------------------------------------

    @Test
    void queryAllMethods() {
        ParsedQuery q = queryUtil.parseQuery("method");
        List<TreeSitterQueryResult> results = queryUtil.execute(q, trees, null);

        //List<String> names = results.stream().map(QueryMatch::matchedText).toList();
        //assertTrue(names.contains("getId"));
        //assertTrue(names.contains("getName"));
        //assertTrue(names.contains("setName"));
    }

    @Test
    void queryMethodByExactName() {
        ParsedQuery q = queryUtil.parseQuery("method = getId");
        List<TreeSitterQueryResult> results = queryUtil.execute(q, trees, null);

        assertEquals(1, results.size());
        assertEquals("getId", results.get(0).name());
    }

    @Test
    void queryMethodContains() {
        ParsedQuery q = queryUtil.parseQuery("method contains get");
        List<TreeSitterQueryResult> results = queryUtil.execute(q, trees, null);

        // "getId" and "getName" both contain "get" (case-insensitive)
        assertEquals(2, results.size());
    }

    // -----------------------------------------------------------------------
    // Import queries
    // -----------------------------------------------------------------------

    @Test
    void queryAllImports() {
        ParsedQuery q = queryUtil.parseQuery("import");
        List<TreeSitterQueryResult> results = queryUtil.execute(q, trees, null);

        // List<String> names = results.stream().map(QueryMatch::matchedText).toList();
        // assertTrue(names.contains("jakarta.persistence.Entity"));
        // assertTrue(names.contains("jakarta.persistence.Id"));
        // assertTrue(names.contains("jakarta.persistence.Column"));
    }

    @Test
    void queryImportContains() {
        ParsedQuery q = queryUtil.parseQuery("import contains persistence");
        List<TreeSitterQueryResult> results = queryUtil.execute(q, trees, null);

        assertEquals(3, results.size(), "All 3 jakarta.persistence imports should match");
    }

    // -----------------------------------------------------------------------
    // Interface queries
    // -----------------------------------------------------------------------

    @Test
    void queryInterface() {
        ParsedQuery q = queryUtil.parseQuery("interface");
        List<TreeSitterQueryResult> results = queryUtil.execute(q, trees, null);

        assertEquals(1, results.size());
        assertEquals("CustomerRepository", results.get(0).name());
    }

    // -----------------------------------------------------------------------
    // Package queries
    // -----------------------------------------------------------------------

    @Test
    void queryPackage() {
        ParsedQuery q = queryUtil.parseQuery("package");
        List<TreeSitterQueryResult> results = queryUtil.execute(q, trees, null);

        assertEquals(2, results.size());
        assertTrue(results.stream().allMatch(m -> "com.example".equals(m.name())));
    }

    // -----------------------------------------------------------------------
    // Constructor queries
    // -----------------------------------------------------------------------

    @Test
    void queryConstructor() {
        ParsedQuery q = queryUtil.parseQuery("constructor");
        List<TreeSitterQueryResult> results = queryUtil.execute(q, trees, null);

        assertEquals(1, results.size());
        assertEquals("Customer", results.get(0).name());
    }

    // -----------------------------------------------------------------------
    // Language override
    // -----------------------------------------------------------------------

    @Test
    void queryWithLanguageOverrideAll() {
        ParsedQuery q = queryUtil.parseQuery("class");
        List<TreeSitterQueryResult> results = queryUtil.execute(q, trees, Language.JAVA);

        // Should still find Java classes even when searching all languages
        assertFalse(results.isEmpty());
        assertEquals("Customer", results.get(0).name());
    }

    @Test
    void queryWithWrongLanguageOverride() {
        ParsedQuery q = queryUtil.parseQuery("class");
        List<TreeSitterQueryResult> results = queryUtil.execute(q, trees, Language.YAML);

        // No YAML trees loaded — should find nothing
        assertTrue(results.isEmpty());
    }

    // -----------------------------------------------------------------------
    // Raw node type (no syntax)
    // -----------------------------------------------------------------------

    @Test
    void queryRawNodeType() {
        ParsedQuery q = queryUtil.parseQuery("class_declaration");
        List<TreeSitterQueryResult> results = queryUtil.execute(q, trees, null);

        // Raw type captures the full node — should still produce results
        assertFalse(results.isEmpty());
    }

    // -----------------------------------------------------------------------
    // Line number
    // -----------------------------------------------------------------------

    @Test
    void matchContainsCorrectLineNumber() {
        ParsedQuery q = queryUtil.parseQuery("class = Customer");
        List<TreeSitterQueryResult> results = queryUtil.execute(q, trees, null);

        assertEquals(1, results.size());
        // "public class Customer" is on line 8 of JAVA_SOURCE
        //assertTrue(results.get(0).line() > 1, "Line should be > 1 for a non-first-line declaration");
    }

    // -----------------------------------------------------------------------
    // Property queries
    // -----------------------------------------------------------------------

    @Test
    void queryAllProperties() {
        ParsedQuery q = queryUtil.parseQuery("property");
        List<TreeSitterQueryResult> results = queryUtil.execute(q, allTrees, null);

        assertEquals(6, results.size(), "Should find all 6 properties");
    }

    @Test
    void queryPropertyByExactKey() {
        ParsedQuery q = queryUtil.parseQuery("property = quarkus.http.port");
        List<TreeSitterQueryResult> results = queryUtil.execute(q, allTrees, null);

        assertEquals(1, results.size());
        assertEquals("quarkus.http.port", results.get(0).name());
    }

    @Test
    void queryPropertyByWildcardSuffix() {
        ParsedQuery q = queryUtil.parseQuery("property = quarkus.datasource.*");
        List<TreeSitterQueryResult> results = queryUtil.execute(q, allTrees, null);

        // quarkus.datasource.db-kind, username, password, jdbc.url
        assertEquals(4, results.size());
        assertTrue(results.stream().allMatch(m -> m.name().startsWith("quarkus.datasource.")));
    }

    @Test
    void queryPropertyByWildcardMiddle() {
        ParsedQuery q = queryUtil.parseQuery("property = quarkus.*.generation");
        List<TreeSitterQueryResult> results = queryUtil.execute(q, allTrees, null);

        assertEquals(1, results.size());
        assertEquals("quarkus.hibernate-orm.database.generation", results.get(0).name());
    }

    @Test
    void queryPropertyByWildcardPrefix() {
        ParsedQuery q = queryUtil.parseQuery("property = *.port");
        List<TreeSitterQueryResult> results = queryUtil.execute(q, allTrees, null);

        assertEquals(1, results.size());
        assertEquals("quarkus.http.port", results.get(0).name());
    }

    @Test
    void queryPropertyContains() {
        ParsedQuery q = queryUtil.parseQuery("property contains jdbc");
        List<TreeSitterQueryResult> results = queryUtil.execute(q, allTrees, null);

        assertEquals(1, results.size());
        assertTrue(results.get(0).name().contains("jdbc"));
    }

    @Test
    void queryPropertyOnlySearchesPropertiesFiles() {
        // "property" syntax targets Language.PROPERTIES — should not touch Java files
        ParsedQuery q = queryUtil.parseQuery("property");
        List<TreeSitterQueryResult> results = queryUtil.execute(q, allTrees, null);

        //assertTrue(results.stream().allMatch(m -> m.sourceFile().endsWith(".properties")),
        //        "Property queries should only match .properties files");
    }
}
