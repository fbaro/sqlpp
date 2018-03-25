package it.fb.sqlpp;

import com.google.common.base.Strings;

public final class TreeLayout {

    public static String format(int lineWidth, int indentWidth, TreeCode formatCode) {
        TreeLayout tl = new TreeLayout(lineWidth, indentWidth);
        try {
            tl.format(formatCode);
        } catch (ReformatException e) {
            throw new IllegalStateException("Unformattable!");
        }
        return tl.sb.toString();
    }

    private final int rowWidth;
    private final int indentWidth;
    private final StringBuilder sb = new StringBuilder();
    private int curLineStartsAt = 0;

    private TreeLayout(int rowWidth, int indentWidth) {
        this.rowWidth = rowWidth;
        this.indentWidth = indentWidth;
    }

    private void format(TreeCode formatCode) throws ReformatException {
        int curLen = sb.length();
        try {
            formatCode.accept(new StraightLayout());
        } catch (ReformatException ex) {
            sb.setLength(curLen);
            formatCode.accept(new IndentingLayout());
        }
    }

    private void newLine() {
        sb.append('\n');
        curLineStartsAt = sb.length();
    }

    public interface TreeCode {
        void accept(NodeCode layout) throws ReformatException;
    }

    public interface NodeCode {
        void leaf(String text) throws ReformatException;

        NodeCode child(String preLabel, String postLabel, TreeCode code) throws ReformatException;
    }

    public static final class ReformatException extends Exception {
        private static final ReformatException INSTANCE = new ReformatException();

        private ReformatException() {
        }
    }

    private class StraightLayout implements NodeCode {
        public StraightLayout() {
        }

        private void append(String text, boolean addSpace) throws ReformatException {
            if (Strings.isNullOrEmpty(text)) {
                return;
            }
            int curLen = sb.length();
            addSpace &= curLen > curLineStartsAt;
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
        public void leaf(String text) throws ReformatException {
            append(text, true);
        }

        @Override
        public NodeCode child(String preLabel, String postLabel, TreeCode code) throws ReformatException {
            append(preLabel, true);
            code.accept(this);
            append(postLabel, false);
            return this;
        }
    }

    private final class IndentingLayout extends StraightLayout {

        private int callCount = 0;

        private void append(String text, boolean addSpace) {
            if (Strings.isNullOrEmpty(text)) {
                return;
            }
            int curLen = sb.length();
            addSpace &= curLen > curLineStartsAt;
            if (addSpace) {
                sb.append(' ');
            }
            sb.append(text);
        }

        @Override
        public void leaf(String text) {
            if (++callCount > 1) {
                newLine();
            }
            append(text, true);
        }

        @Override
        public NodeCode child(String preLabel, String postLabel, TreeCode code) throws ReformatException {
            if (++callCount > 1) {
                newLine();
            }
            append(preLabel, true);
            format(code);
            append(postLabel, false);
            return this;
        }
    }
}
