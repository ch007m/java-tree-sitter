package dev.snowdrop.treesitter.bonede.query;

import dev.snowdrop.treesitter.util.ASTStore;
import dev.snowdrop.treesitter.bonede.store.ParsedFile;
import org.aesh.command.invocation.CommandInvocation;
import org.treesitter.TSNode;
import org.treesitter.TSPoint;
import org.treesitter.TSQuery;
import org.treesitter.TSQueryCapture;
import org.treesitter.TSQueryMatch;

import java.nio.file.Path;

public final class QueryResultPrinter {

    private QueryResultPrinter() {}

    public static void printMatch(CommandInvocation invocation,
                                  ParsedFile parsed,
                                  TSQueryMatch match,
                                  TSQuery query) {

        Path rootPath = ASTStore.getInstance().getRootPath();
        String relPath = relativize(rootPath, parsed.getFilePath());

        TSQueryCapture[] captures = match.getCaptures();
        if (captures == null || captures.length == 0) {
            return;
        }

        for (TSQueryCapture capture : captures) {
            TSNode node = capture.getNode();
            TSPoint start = node.getStartPoint();
            TSPoint end = node.getEndPoint();

            String captureName = query.getCaptureNameForId(capture.getIndex());
            String matchedText = parsed.extractText(node.getStartByte(), node.getEndByte());
            String displayText = truncateForDisplay(matchedText, 120);

            int line = start.getRow() + 1;
            int col = start.getColumn() + 1;
            int endLine = end.getRow() + 1;

            String location = relPath + ":" + line + ":" + col;

            if (line == endLine) {
                invocation.println("  " + location + "  @" + captureName
                        + " [" + node.getType() + "] = " + displayText);
            } else {
                invocation.println("  " + location + "-" + endLine
                        + "  @" + captureName
                        + " [" + node.getType() + "] = " + displayText);
            }
        }
    }

    private static String truncateForDisplay(String text, int maxLen) {
        int newline = text.indexOf('\n');
        if (newline >= 0) {
            text = text.substring(0, newline) + " ...";
        }
        if (text.length() > maxLen) {
            text = text.substring(0, maxLen - 3) + "...";
        }
        return text;
    }

    private static String relativize(Path root, Path file) {
        if (root == null) {
            return file.toString();
        }
        try {
            return root.relativize(file).toString();
        } catch (IllegalArgumentException e) {
            return file.toString();
        }
    }
}
