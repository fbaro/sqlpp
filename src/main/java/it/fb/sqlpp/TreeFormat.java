package it.fb.sqlpp;

import java.util.List;

public class TreeFormat {

    public static String format(Tree tree, int lineWidth, int indentSize) {
        new TreeSplit(lineWidth, indentSize).accept(tree, 0, 0);
        TreePrint pc = new TreePrint(getIndentString(indentSize));
        pc.accept(tree, 0);
        return pc.result.toString();
    }

    private static String getIndentString(int indentSize) {
        StringBuilder sb = new StringBuilder();
        while (indentSize > 0) {
            sb.append(' ');
            indentSize--;
        }
        return sb.toString();
    }

    private static final class TreeSplit {

        private final int lineWidth;
        private final int indentSize;

        TreeSplit(int lineWidth, int indentSize) {
            this.lineWidth = lineWidth;
            this.indentSize = indentSize;
        }

        private void accept(Tree tree, int indentLevel, int curPosInRow) {
            if (tree.isLeaf()) {
                return;
            }
            int subtreeLen = tree.depthFirstVisit()
                    .mapToInt(n -> n.text == null ? 0 : n.text.length() + 1)
                    .sum() - 1;
            int rowLen = curPosInRow + subtreeLen;
            if (curPosInRow > indentLevel * indentSize) {
                rowLen++; // Aggiungo uno spazio
            }
            if (rowLen <= lineWidth) {
                return;
            }
            tree.data.splitChildren = true;
            List<Tree> children = tree.children;
            accept(children.get(0), indentLevel + 1, curPosInRow);
            for (int c = 1; c < children.size(); c++) {
                accept(children.get(c), indentLevel + 1, (indentLevel + 1) * indentSize);
            }
        }
    }

    private static final class TreePrint {
        private final String indentString;
        private final StringBuilder result = new StringBuilder();

        TreePrint(String indentString) {
            this.indentString = indentString;
        }

        private void accept(Tree tree, int indentLevel) {
            if (tree.isLeaf()) {
                if (result.length() > 0 && result.charAt(result.length() - 1) == '\n') {
                    for (int i = 0; i < indentLevel; i++) {
                        result.append(indentString);
                    }
                }
                result.append(tree.text);
            } else {
                int recursiveIndentLevel = indentLevel + (tree.data.splitChildren ? 1 : 0);
                List<Tree> children = tree.children;
                for (int c = 0; c < children.size() - 1; c++) {
                    Tree child = children.get(c);
                    accept(child, recursiveIndentLevel);
                    if (tree.data.splitChildren) {
                        result.append('\n');
                    } else {
                        result.append(' ');
                    }
                }
                accept(children.get(children.size() - 1), recursiveIndentLevel);
            }
        }
    }
}
