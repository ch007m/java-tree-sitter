package dev.snowdrop.treesitter4j;

import io.roastedroot.treesitter.TreeSitter;

/**
 * Manages the singleton {@link TreeSitter} runtime instance.
 */
public class TreeSitterRuntime {

    private static TreeSitter instance;

    private TreeSitterRuntime() {}

    /**
     * Returns the shared {@link TreeSitter} singleton, creating it on first call.
     */
    public static TreeSitter get() {
        TreeSitter ts = instance;
        if (ts == null) {
            ts = TreeSitter.create();
            instance = ts;
        }
        return ts;
    }
}