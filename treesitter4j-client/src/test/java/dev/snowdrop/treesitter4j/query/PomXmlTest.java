package dev.snowdrop.treesitter4j.query;

import dev.snowdrop.treesitter4j.TreeSitterRuntime;
import dev.snowdrop.treesitter4j.util.ASTQueryUtil;
import dev.snowdrop.treesitter4j.util.ASTQueryUtil.ParsedQuery;
import io.roastedroot.treesitter.*;
import io.roastedroot.treesitter.ast.ASTExporter;
import io.roastedroot.treesitter.ast.ASTTree;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for POM-specific aliases ({@code pom-dependency}, {@code pom-plugin},
 * {@code pom-parent}, {@code pom-extension}) with GAV composition.
 */
@Disabled
class PomXmlTest {

    final ASTQueryUtil queryUtil = new ASTQueryUtil();

    static final String POM_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0">
                <modelVersion>4.0.0</modelVersion>

                <parent>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-starter-parent</artifactId>
                    <version>3.2.0</version>
                </parent>

                <groupId>com.example</groupId>
                <artifactId>my-app</artifactId>
                <version>1.0.0</version>

                <dependencies>
                    <dependency>
                        <groupId>io.quarkus</groupId>
                        <artifactId>quarkus-rest</artifactId>
                    </dependency>
                    <dependency>
                        <groupId>io.quarkus</groupId>
                        <artifactId>quarkus-hibernate-orm-panache</artifactId>
                        <version>3.0.0</version>
                    </dependency>
                    <dependency>
                        <groupId>org.apache.commons</groupId>
                        <artifactId>commons-lang3</artifactId>
                        <version>3.14.0</version>
                    </dependency>
                    <dependency>
                        <groupId>io.quarkus</groupId>
                        <artifactId>quarkus-junit5</artifactId>
                        <scope>test</scope>
                    </dependency>
                </dependencies>

                <build>
                    <plugins>
                        <plugin>
                            <groupId>io.quarkus</groupId>
                            <artifactId>quarkus-maven-plugin</artifactId>
                            <version>3.0.0</version>
                        </plugin>
                        <plugin>
                            <groupId>org.apache.maven.plugins</groupId>
                            <artifactId>maven-compiler-plugin</artifactId>
                            <version>3.11.0</version>
                        </plugin>
                    </plugins>
                    <extensions>
                        <extension>
                            <groupId>io.opentelemetry.contrib</groupId>
                            <artifactId>opentelemetry-maven-extension</artifactId>
                            <version>1.0.0</version>
                        </extension>
                    </extensions>
                </build>
            </project>
            """;

    static List<ASTTree> pomTrees;

    @BeforeAll
    static void parsePom() {
        TreeSitter ts = TreeSitterRuntime.get();
        try (TreeSitterParser parser = ts.newParser()) {
            parser.setLanguage(Language.XML);
            try (TreeSitterTree tree = parser.parseString(POM_XML)) {
                ASTTree ast = ASTExporter.export(tree, Language.XML, POM_XML, "pom.xml");
                pomTrees = List.of(ast);
            }
        }
    }


    // -----------------------------------------------------------------------
    // Alias registration
    // -----------------------------------------------------------------------

    @Test
    void pomGetTypeAndQueryExpressionAreRegistered() {
        var aliases = queryUtil.getAliases();
        assertTrue(aliases.containsKey("pom-dependency"));
        assertTrue(aliases.containsKey("pom-plugin"));
        assertTrue(aliases.containsKey("pom-parent"));
        assertTrue(aliases.containsKey("pom-extension"));
    }

    @Test
    void pomGetTypeAndQueryExpressionTargetXmlLanguage() {
        var aliases = queryUtil.getAliases();
        assertEquals(Language.XML, aliases.get("pom-dependency").language());
        assertEquals(Language.XML, aliases.get("pom-plugin").language());
        assertEquals(Language.XML, aliases.get("pom-parent").language());
        assertEquals(Language.XML, aliases.get("pom-extension").language());
    }

    @Test
    void pomGetTypeAndQueryExpressionHaveComposer() {
        var aliases = queryUtil.getAliases();
        assertNotNull(aliases.get("pom-dependency").composer());
        assertNotNull(aliases.get("pom-plugin").composer());
        assertNotNull(aliases.get("pom-parent").composer());
        assertNotNull(aliases.get("pom-extension").composer());
    }

    // -----------------------------------------------------------------------
    // pom-dependency queries
    // -----------------------------------------------------------------------

    @Test
    void queryAllDependencies() {
        ParsedQuery q = queryUtil.parseQuery("pom-dependency");
        Map<String, List<TreeSitterQueryResult>> results = queryUtil.execute(q, pomTrees, Language.XML);

        assertEquals(4, results.size());
        // assertTrue(gavs.contains("io.quarkus:quarkus-rest"));
        // assertTrue(gavs.contains("io.quarkus:quarkus-hibernate-orm-panache:3.0.0"));
        // assertTrue(gavs.contains("org.apache.commons:commons-lang3:3.14.0"));
        // assertTrue(gavs.contains("io.quarkus:quarkus-junit5"));
    }

    @Test
    void queryDependencyExactMatchWithoutVersion() {
        ParsedQuery q = queryUtil.parseQuery("pom-dependency = io.quarkus:quarkus-rest");
        Map<String, List<TreeSitterQueryResult>> results = queryUtil.execute(q, pomTrees, Language.XML);

        assertEquals(1, results.size());
        //assertEquals("io.quarkus:quarkus-rest", results.get(0).name());
    }

    @Test
    void queryDependencyExactMatchWithVersion() {
        ParsedQuery q = queryUtil.parseQuery("pom-dependency = io.quarkus:quarkus-hibernate-orm-panache:3.0.0");
        Map<String, List<TreeSitterQueryResult>> results = queryUtil.execute(q, pomTrees, Language.XML);

        assertEquals(1, results.size());
        //assertEquals("io.quarkus:quarkus-hibernate-orm-panache:3.0.0", results.get(0).name());
    }

    @Test
    void queryDependencyWildcardOnGroupId() {
        ParsedQuery q = queryUtil.parseQuery("pom-dependency = io.quarkus:*");
        Map<String, List<TreeSitterQueryResult>> results = queryUtil.execute(q, pomTrees, Language.XML);

        assertEquals(3, results.size());
        //assertTrue(results.stream().allMatch(m -> m.name().startsWith("io.quarkus:")));
    }

    @Test
    void queryDependencyContains() {
        ParsedQuery q = queryUtil.parseQuery("pom-dependency contains panache");
        Map<String, List<TreeSitterQueryResult>> results = queryUtil.execute(q, pomTrees, Language.XML);

        assertEquals(1, results.size());
        //assertTrue(results.get(0).name().contains("panache"));
    }

    @Test
    void queryDependencyNoMatch() {
        ParsedQuery q = queryUtil.parseQuery("pom-dependency = com.nonexistent:no-such-lib");
        Map<String, List<TreeSitterQueryResult>> results = queryUtil.execute(q, pomTrees, Language.XML);

        assertTrue(results.isEmpty());
    }

    // -----------------------------------------------------------------------
    // pom-plugin queries
    // -----------------------------------------------------------------------

    @Test
    void queryAllPlugins() {
        ParsedQuery q = queryUtil.parseQuery("pom-plugin");
        Map<String, List<TreeSitterQueryResult>> results = queryUtil.execute(q, pomTrees, Language.XML);

        assertEquals(2, results.size());
        // List<String> gavs = results.stream().map(QueryMatch::matchedText).toList();
        // assertTrue(gavs.contains("io.quarkus:quarkus-maven-plugin:3.0.0"));
        // assertTrue(gavs.contains("org.apache.maven.plugins:maven-compiler-plugin:3.11.0"));
    }

    @Test
    void queryPluginExactMatch() {
        ParsedQuery q = queryUtil.parseQuery("pom-plugin = io.quarkus:quarkus-maven-plugin:3.0.0");
        Map<String, List<TreeSitterQueryResult>> results = queryUtil.execute(q, pomTrees, Language.XML);

        assertEquals(1, results.size());
        //assertEquals("io.quarkus:quarkus-maven-plugin:3.0.0", results.get(0).name());
    }

    @Test
    void queryPluginWildcard() {
        ParsedQuery q = queryUtil.parseQuery("pom-plugin = org.apache.maven.plugins:*");
        Map<String, List<TreeSitterQueryResult>> results = queryUtil.execute(q, pomTrees, Language.XML);

        assertEquals(1, results.size());
        //assertTrue(results.get(0).name().startsWith("org.apache.maven.plugins:"));
    }

    // -----------------------------------------------------------------------
    // pom-parent queries
    // -----------------------------------------------------------------------

    @Test
    void queryParent() {
        ParsedQuery q = queryUtil.parseQuery("pom-parent");
        Map<String, List<TreeSitterQueryResult>> results = queryUtil.execute(q, pomTrees, Language.XML);

        assertEquals(1, results.size());
        //assertEquals("org.springframework.boot:spring-boot-starter-parent:3.2.0",
        //        results.get(0).name());
    }

    @Test
    void queryParentExactMatch() {
        ParsedQuery q = queryUtil.parseQuery("pom-parent = org.springframework.boot:spring-boot-starter-parent:3.2.0");
        Map<String, List<TreeSitterQueryResult>> results = queryUtil.execute(q, pomTrees, Language.XML);

        assertEquals(1, results.size());
    }

    // -----------------------------------------------------------------------
    // pom-extension queries
    // -----------------------------------------------------------------------

    @Test
    void queryExtension() {
        ParsedQuery q = queryUtil.parseQuery("pom-extension");
        Map<String, List<TreeSitterQueryResult>> results = queryUtil.execute(q, pomTrees, Language.XML);

        assertEquals(1, results.size());
        //assertEquals("io.opentelemetry.contrib:opentelemetry-maven-extension:1.0.0",
        //        results.get(0).name());
    }

    @Test
    void queryExtensionContains() {
        ParsedQuery q = queryUtil.parseQuery("pom-extension contains opentelemetry");
        Map<String, List<TreeSitterQueryResult>> results = queryUtil.execute(q, pomTrees, Language.XML);

        assertEquals(1, results.size());
    }
}
