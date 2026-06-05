package dev.snowdrop.treesitter.bonede.query;

import dev.snowdrop.treesitter.bonede.store.ParsedFile;
import org.treesitter.TSNode;
import org.treesitter.TSQuery;
import org.treesitter.TSQueryCapture;
import org.treesitter.TSQueryMatch;

public final class AnnotationNameFilter {

    private static final String ANN_NAME_CAPTURE = "ann.name";

    private AnnotationNameFilter() {}

    /**
     * Tests whether a query match's annotation name matches the given filter.
     *
     * @param match      the TSQueryMatch from the annotations query
     * @param query      the TSQuery (to resolve capture names)
     * @param parsed     the ParsedFile (to extract text from byte ranges)
     * @param nameFilter the annotation name to filter by (e.g. "Entity" or "javax.persistence.Entity")
     * @return true if the annotation name matches the filter
     */
    public static boolean matches(TSQueryMatch match,
                                  TSQuery query,
                                  ParsedFile parsed,
                                  String nameFilter) {
        TSQueryCapture[] captures = match.getCaptures();
        if (captures == null) {
            return false;
        }

        for (TSQueryCapture capture : captures) {
            String captureName = query.getCaptureNameForId(capture.getIndex());
            if (ANN_NAME_CAPTURE.equals(captureName)) {
                TSNode nameNode = capture.getNode();
                String annotationName = parsed.extractText(
                        nameNode.getStartByte(), nameNode.getEndByte());
                return nameMatches(annotationName, nameFilter);
            }
        }

        return false;
    }

    /**
     * Checks if an annotation name matches the filter.
     *
     * If the filter is a simple name (no dots), it matches as a suffix:
     *   "Entity" matches "Entity" and "javax.persistence.Entity"
     *
     * If the filter is a qualified name (has dots), it must match exactly:
     *   "javax.persistence.Entity" matches only "javax.persistence.Entity"
     */
    static boolean nameMatches(String actual, String filter) {
        if (actual.equals(filter)) {
            return true;
        }
        if (!filter.contains(".")) {
            int lastDot = actual.lastIndexOf('.');
            if (lastDot >= 0) {
                String simpleName = actual.substring(lastDot + 1);
                return simpleName.equals(filter);
            }
        }
        return false;
    }
}
