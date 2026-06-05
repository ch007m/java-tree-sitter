package dev.snowdrop.treesitter.languagepack;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PersistentStore implements Serializable {
    private static final long serialVersionUID = 1L;

    // Maps File Paths to their flattened Abstract Syntax Elements
    public Map<String, List<SerializableNode>> fileGraphs = new HashMap<>();

    public static class SerializableNode implements Serializable {
        private static final long serialVersionUID = 1L;
        public String type;
        public String text;
        public int startLine;
        public int endLine;

        public SerializableNode(String type, String text, int startLine, int endLine) {
            this.type = type;
            this.text = text;
            this.startLine = startLine;
            this.endLine = endLine;
        }
    }
}