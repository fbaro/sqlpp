package it.fb.sqlpp;

import com.google.common.base.Strings;

public final class TreeLayout {

    public static String format(int lineWidth, int indentWidth, TreeCode formatCode) {
        TreeLayout tl = new TreeLayout(lineWidth, indentWidth);
        tl.format(formatCode);
        return tl.sb.toString();
    }

    private final int rowWidth;
    private final int indentWidth;
    private final StringBuilder sb = new StringBuilder();

    private TreeLayout(int rowWidth, int indentWidth) {
        this.rowWidth = rowWidth;
        this.indentWidth = indentWidth;
    }

    private void format(TreeCode formatCode) {
        try {
            formatCode.accept(new StraightLayout());
        } catch (ReformatException ex) {
            try {
                formatCode.accept(new IndentingLayout());
            } catch (ReformatException e) {
                throw new IllegalStateException("Unable to reformat");
            }
        }
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

    private final class StraightLayout implements NodeCode {
        public StraightLayout() {
        }

        @Override
        public void leaf(String text) throws ReformatException {
            if (text != null && !text.isEmpty()) {
                if (sb.length() > 0) {
                    sb.append(' ');
                }
                sb.append(text);
            }
        }

        @Override
        public NodeCode child(String preLabel, String postLabel, TreeCode code) throws ReformatException {
            leaf(preLabel);
            code.accept(this);
            if (!Strings.isNullOrEmpty(postLabel)) {
                sb.append(postLabel);
            }
            return this;
        }
    }

    private class IndentingLayout implements NodeCode {

        @Override
        public void leaf(String text) throws ReformatException {

        }

        @Override
        public NodeCode child(String preLabel, String postLabel, TreeCode code) throws ReformatException {
            return null;
        }
    }
}
