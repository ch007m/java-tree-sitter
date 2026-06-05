package dev.snowdrop.treesitter.util;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public final class JavaFileWalker {

    private JavaFileWalker() {}

    /**
     * Recursively find all .java files under the given root.
     * Skips hidden directories (starting with '.').
     */
    public static List<Path> findJavaFiles(Path root) throws IOException {
        List<Path> javaFiles = new ArrayList<>();
        Files.walkFileTree(root, EnumSet.of(FileVisitOption.FOLLOW_LINKS),
                Integer.MAX_VALUE, new SimpleFileVisitor<>() {

                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                        if (dir.getFileName() != null
                                && dir.getFileName().toString().startsWith(".")) {
                            return FileVisitResult.SKIP_SUBTREE;
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                        if (file.toString().endsWith(".java")) {
                            javaFiles.add(file);
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(Path file, IOException exc) {
                        System.err.println("Warning: cannot read " + file + ": " + exc.getMessage());
                        return FileVisitResult.CONTINUE;
                    }
                });
        return javaFiles;
    }
}
