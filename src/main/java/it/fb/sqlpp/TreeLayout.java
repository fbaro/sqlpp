package it.fb.sqlpp;

import com.google.common.base.Strings;

import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class TreeLayout {

    private static final String MANY_SPACES = Stream.generate(() -> " ").limit(1000).collect(Collectors.joining());

    public static String format(int lineWidth, int indentWidth, Tree tree) {
        TreeLayout tl = new TreeLayout(lineWidth, indentWidth);
        try {
            tl.format(tree, 0);
        } catch (ReformatException ex) {
            throw new IllegalStateException("Should not get a ReformatException at this level", ex);
        }
        return tl.sb.toString();
    }

    private final int rowWidth;
    private final int indentWidth;
    private final StringBuilder sb = new StringBuilder();
    private final StraightLayout straightLayout = new StraightLayout();
    private int curLineStartsAt = 0;
    private int curTextStartsAt = 0;

    private TreeLayout(int rowWidth, int indentWidth) {
        this.rowWidth = rowWidth;
        this.indentWidth = indentWidth;
    }

    private void format(Tree tree, int indentLevel) {
        int curLen = sb.length();
        try {
            tree.accept(straightLayout);
        } catch (ReformatException ex) {
            sb.setLength(curLen);
            tree.accept(new IndentingLayout(indentLevel));
        }
    }

    private void newLine(int indentLevel) {
        sb.append('\n');
        curLineStartsAt = sb.length();
        sb.append(MANY_SPACES, 0, indentLevel * indentWidth);
        curTextStartsAt = sb.length();
    }

    private static final class ReformatException extends RuntimeException {
        private static final ReformatException INSTANCE = new ReformatException();

        private ReformatException() {
        }
    }

    private final class StraightLayout implements TreeVisitor {
        private StraightLayout() {
        }

        private void append(String text, boolean addSpace) throws ReformatException {
            if (Strings.isNullOrEmpty(text)) {
                return;
            }
            int curLen = sb.length();
            addSpace &= curLen > curTextStartsAt;
            int newLen = curLen + text.length() + (addSpace ? 1 : 0);
            if (newLen - curLineStartsAt > rowWidth) {
                throw ReformatException.INSTANCE;
            }
            if (addSpace) {
                sb.append(' ');
            }
            sb.append(text);
        }

        @Override
        public TreeVisitor leaf(String text) throws ReformatException {
            append(text, true);
            return this;
        }

        @Override
        public TreeVisitor child(String preLabel, String postLabel, Tree subTree) throws ReformatException {
            append(preLabel, true);
            subTree.accept(this);
            append(postLabel, false);
            return this;
        }

        @Override
        public TreeVisitor singleChild(String preLabel, String postLabel, Tree subTree) {
            return child(preLabel, postLabel, subTree);
        }
    }

    private final class IndentingLayout implements TreeVisitor {

        private final int indentLevel;
        private int callCount = 0;

        private IndentingLayout(int indentLevel) {
            this.indentLevel = indentLevel;
        }

        private void append(String text, boolean addSpace) {
            if (Strings.isNullOrEmpty(text)) {
                return;
            }
            int curLen = sb.length();
            addSpace &= curLen > curTextStartsAt;
            if (addSpace) {
                sb.append(' ');
            }
            sb.append(text);
        }

        @Override
        public TreeVisitor leaf(String text) {
            if (++callCount > 1) {
                newLine(indentLevel);
            }
            append(text, true);
            return this;
        }

        @Override
        public TreeVisitor child(String preLabel, String postLabel, Tree subTree) {
            if (++callCount > 1) {
                newLine(indentLevel);
            }
            append(preLabel, true);
            format(subTree, indentLevel + 1);
            append(postLabel, false);
            return this;
        }

        @Override
        public TreeVisitor singleChild(String preLabel, String postLabel, Tree subTree) {
            if (++callCount > 1) {
                throw new IllegalStateException("Should not have called other methods before singleChild");
            }
            append(preLabel, true);
            format(subTree, indentLevel);
            append(postLabel, false);
            return this;
        }
    }
}
