package dev.snowdrop.treesitter.jtree;

import dev.snowdrop.treesitter.jtree.languages.TreeSitterJava;
import io.github.treesitter.jtreesitter.Language;
import io.github.treesitter.jtreesitter.Node;
import io.github.treesitter.jtreesitter.Parser;
import io.github.treesitter.jtreesitter.Tree;

public class JTreeApp {
    public static void main(String[] args) {
        String sourceCode = """
                package com.example;

                public class HelloWorld {
                    public static void main(String[] args) {
                        System.out.println("Hello, World!");
                    }
                }
                """;

        Language java = new Language(TreeSitterJava.language());

        try (var parser = new Parser(java)) {
            try (var tree = parser.parse(sourceCode).orElseThrow()) {
                Node root = tree.getRootNode();

                System.out.println("=== S-expression ===");
                System.out.println(root.toSexp());
                System.out.println();

                System.out.println("=== AST ===");
                printTree(root, 0);
            }
        }
    }

    private static void printTree(Node node, int depth) {
        if (!node.isNamed()) return;
        String indent = "  ".repeat(depth);
        System.out.printf("%s%s [%d:%d - %d:%d]%n",
                indent, node.getType(),
                node.getStartPoint().row(), node.getStartPoint().column(),
                node.getEndPoint().row(), node.getEndPoint().column());
        for (var child : node.getChildren()) {
            printTree(child, depth + 1);
        }
    }
}
