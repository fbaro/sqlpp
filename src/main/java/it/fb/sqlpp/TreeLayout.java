package it.fb.sqlpp;

import com.google.common.base.Strings;

import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class TreeLayout {

    private static final String MANY_SPACES = Stream.generate(() -> " ").limit(1000).collect(Collectors.joining());

    public static String format(int lineWidth, int indentWidth, Consumer<? super NodeCode> formatCode) {
        TreeLayout tl = new TreeLayout(lineWidth, indentWidth);
        try {
            tl.format(formatCode, 0);
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

    private void format(Consumer<? super NodeCode> formatCode, int indentLevel) {
        int curLen = sb.length();
        try {
            formatCode.accept(straightLayout);
        } catch (ReformatException ex) {
            sb.setLength(curLen);
            formatCode.accept(new IndentingLayout(indentLevel));
        }
    }

    private void newLine(int indentLevel) {
        sb.append('\n');
        curLineStartsAt = sb.length();
        sb.append(MANY_SPACES, 0, indentLevel * indentWidth);
        curTextStartsAt = sb.length();
    }

    public interface NodeCode {
        NodeCode leaf(String text);

        NodeCode child(String preLabel, String postLabel, Consumer<? super NodeCode> code);

        NodeCode singleChild(String preLabel, String postLabel, Consumer<? super NodeCode> code);
    }

    private static final class ReformatException extends RuntimeException {
        private static final ReformatException INSTANCE = new ReformatException();

        private ReformatException() {
        }
    }

    private final class StraightLayout implements NodeCode {
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
        public NodeCode leaf(String text) throws ReformatException {
            append(text, true);
            return this;
        }

        @Override
        public NodeCode child(String preLabel, String postLabel, Consumer<? super NodeCode> code) throws ReformatException {
            append(preLabel, true);
            code.accept(this);
            append(postLabel, false);
            return this;
        }

        @Override
        public NodeCode singleChild(String preLabel, String postLabel, Consumer<? super NodeCode> code) {
            return child(preLabel, postLabel, code);
        }
    }

    private final class IndentingLayout implements NodeCode {

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
        public NodeCode leaf(String text) {
            if (++callCount > 1) {
                newLine(indentLevel);
            }
            append(text, true);
            return this;
        }

        @Override
        public NodeCode child(String preLabel, String postLabel, Consumer<? super NodeCode> code) {
            if (++callCount > 1) {
                newLine(indentLevel);
            }
            append(preLabel, true);
            format(code, indentLevel + 1);
            append(postLabel, false);
            return this;
        }

        @Override
        public NodeCode singleChild(String preLabel, String postLabel, Consumer<? super NodeCode> code) {
            if (++callCount > 1) {
                throw new IllegalStateException("Should not have called other methods before singleChild");
            }
            append(preLabel, true);
            format(code, indentLevel);
            append(postLabel, false);
            return this;
        }
    }
}
