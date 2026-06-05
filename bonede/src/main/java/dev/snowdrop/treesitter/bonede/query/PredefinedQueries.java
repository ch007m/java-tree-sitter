package dev.snowdrop.treesitter.bonede.query;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PredefinedQueries {

    public record QueryEntry(String name, String description, String pattern) {}

    private static final List<QueryEntry> QUERIES = List.of(
            new QueryEntry("classes",
                    "Find all class declarations with their names",
                    "(class_declaration name: (identifier) @class.name) @class.def"),

            new QueryEntry("methods",
                    "Find all method declarations with names and return types",
                    "(method_declaration name: (identifier) @method.name) @method.def"),

            new QueryEntry("constructors",
                    "Find all constructor declarations",
                    "(constructor_declaration name: (identifier) @ctor.name) @ctor.def"),

            new QueryEntry("imports",
                    "Find all import declarations",
                    "(import_declaration) @import"),

            new QueryEntry("fields",
                    "Find all field declarations",
                    "(field_declaration declarator: (variable_declarator name: (identifier) @field.name)) @field.def"),

            new QueryEntry("interfaces",
                    "Find all interface declarations",
                    "(interface_declaration name: (identifier) @iface.name) @iface.def"),

            new QueryEntry("enums",
                    "Find all enum declarations",
                    "(enum_declaration name: (identifier) @enum.name) @enum.def"),

            new QueryEntry("annotations",
                    "Find all annotation usages (marker and parameterized)",
                    "(marker_annotation name: (_) @ann.name) @ann\n" +
                    "(annotation name: (_) @ann.name) @ann"),

            new QueryEntry("packages",
                    "Find package declarations",
                    "(package_declaration) @package"),

            new QueryEntry("strings",
                    "Find all string literals",
                    "(string_literal) @string"),

            new QueryEntry("method-calls",
                    "Find all method invocations",
                    "(method_invocation name: (identifier) @call.name) @call")
    );

    private static final Map<String, String> BY_NAME;

    static {
        Map<String, String> map = new LinkedHashMap<>();
        for (QueryEntry e : QUERIES) {
            map.put(e.name(), e.pattern());
        }
        BY_NAME = Collections.unmodifiableMap(map);
    }

    private PredefinedQueries() {}

    /**
     * If the input matches a predefined query name, return its S-expression pattern.
     * Otherwise assume the input IS a raw S-expression and return it unchanged.
     */
    public static String resolve(String nameOrPattern) {
        return BY_NAME.getOrDefault(nameOrPattern, nameOrPattern);
    }

    public static List<QueryEntry> list() {
        return QUERIES;
    }
}
