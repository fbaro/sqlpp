package it.fb.sqlpp;

import java.util.function.Consumer;

/**
 * A representation of a Tree, tailored for the needs of TreeLayout. To implement this interface, users only have to
 * call the appropriate sequence of methods of {@code Tree.Visitor} within {@code accept}.
 */
@FunctionalInterface
public interface Tree extends Consumer<Tree.Visitor> {

    /**
     * Receives a {@code Tree.Visitor}, whose methods should be called in order to provide the content of the tree.
     *
     * @param visitor The visitor instance whose methods should be called
     */
    @Override
    void accept(Visitor visitor);

    default void appendTo(String preLabel, Visitor visitor) {
        Tree.this.accept(new Visitor() {
            boolean isFirst = true;
            @Override
            public Visitor leaf(String text) {
                if (isFirst) {
                    visitor.child(preLabel, "", inner -> inner.leaf(text));
                } else {
                    visitor.leaf(text);
                }
                isFirst = false;
                return this;
            }

            @Override
            public Visitor child(String _preLabel, String postLabel, Tree subTree) {
                if (isFirst) {
                    visitor.child(preLabel + " " + _preLabel, postLabel, subTree);
                } else {
                    visitor.child(_preLabel, postLabel, subTree);
                }
                isFirst = false;
                return this;
            }

            @Override
            public void singleChild(String _preLabel, String postLabel, Tree subTree) {
                child(_preLabel, postLabel, subTree);
            }
        });
    }

    default void appendTo(Visitor visitor) {
        Tree.this.accept(new Visitor() {
            @Override
            public Visitor leaf(String text) {
                visitor.leaf(text);
                return this;
            }

            @Override
            public Visitor child(String preLabel, String postLabel, Tree subTree) {
                visitor.child(preLabel, postLabel, subTree);
                return this;
            }

            @Override
            public void singleChild(String preLabel, String postLabel, Tree subTree) {
                child(preLabel, postLabel, subTree);
            }
        });
    }

    /**
     * A tree visitor. Fluent interface: where another method can be called, the method returns the receiver itself.
     */
    interface Visitor {
        /**
         * Tree implementors should call this method if the next child is a leaf.
         *
         * @param text The text of the leaf
         * @return The receiver
         */
        Visitor leaf(String text);

        /**
         * Tree implementors should call this method if the next child is a subtree.
         *
         * @param preLabel  The text to add before the subtree, possibly empty.
         * @param postLabel The test to add after the subtree, possibly empty.
         * @param subTree   The subtree
         * @return The receiver
         */
        Visitor child(String preLabel, String postLabel, Tree subTree);

        /**
         * Tree implementors should call this method once, and only this method, if the Tree has a single child.
         *
         * @param preLabel  The text to add before the subtree, possibly empty.
         * @param postLabel The test to add after the subtree, possibly empty.
         * @param subTree   The subtree
         */
        void singleChild(String preLabel, String postLabel, Tree subTree);
    }
}
