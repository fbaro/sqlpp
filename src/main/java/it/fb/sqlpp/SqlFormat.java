package it.fb.sqlpp;

import java.util.List;

public class SqlFormat {

    public static String format(Tree tree, int lineWidth, int indentSize) {
        int fullLen = tree.depthFirstVisit()
                .mapToInt(n -> n.text == null ? 0 : n.text.length() + 1)
                .sum() - 1;
        if (fullLen > lineWidth) {
            tree.data.splitChildren = true;
        }
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

    private static final class TreePrint {
        private final String indentString;
        private final StringBuilder result = new StringBuilder();

        TreePrint(String indentString) {
            this.indentString = indentString;
        }

        private void accept(Tree tree, int indentLevel) {
            if (tree.isLeaf()) {
                result.append(tree.text);
            } else {
                List<Tree> children = tree.children;
                for (int c = 0; c < children.size(); c++) {
                    Tree child = children.get(c);
                    accept(child, indentLevel);
                    if (c != children.size() - 1) {
                        if (tree.data.splitChildren) {
                            result.append('\n');
                            for (int i = 0; i < indentLevel + 1; i++) {
                                result.append(indentString);
                            }
                        } else {
                            result.append(' ');
                        }
                    }
                }
            }
        }
    }
}
