package it.fb.sqlpp;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

final class Tree {

    final String text;
    Tree parent;
    final List<Tree> children = new ArrayList<>();
    final FormatStatus data = new FormatStatus();

    private Tree(String text, Tree parent) {
        this.text = text;
        this.parent = parent;
    }

    public FormatStatus getData() {
        return data;
    }

    public String getText() {
        return text;
    }

    public Tree getParent() {
        return parent;
    }

    public List<? extends Tree> getChildren() {
        return children;
    }

    public boolean isFirstChild() {
        return parent == null || parent.children.get(0) == this;
    }

    public static Tree root(String text) {
        return new Tree(text, null);
    }

    public static Tree root() {
        return new Tree(null, null);
    }

    private Tree add(String text) {
        if (this.text != null) {
            throw new IllegalArgumentException("Inner nodes cannot have text");
        }
        Tree ret = new Tree(text, this);
        children.add(ret);
        return this;
    }

    private Tree add(Tree child) {
        if (this.text != null) {
            throw new IllegalArgumentException("Inner nodes cannot have text");
        }
        child.parent = this;
        this.children.add(child);
        return this;
    }

    public boolean isLeaf() {
        return children.isEmpty();
    }

    public Stream<Tree> depthFirstVisit() {
        return StreamSupport.stream(((Iterable<Tree>) this::depthFirstPreVisit).spliterator(), false);
    }

    @Override
    public String toString() {
        if (text != null) {
            return text;
        }
        return "(" + children.stream()
                .map(Tree::toString)
                .collect(Collectors.joining(" ")) + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Tree tree = (Tree) o;
        return Objects.equals(text, tree.text) &&
                Objects.equals(children, tree.children);
    }

    @Override
    public int hashCode() {
        return Objects.hash(text, children);
    }

    private Iterator<Tree> depthFirstPreVisit() {
        Stack<Tree> toVisit = new Stack<>();
        toVisit.add(this);
        return new Iterator<Tree>() {
            @Override
            public boolean hasNext() {
                return !toVisit.empty();
            }

            @Override
            public Tree next() {
                Tree ret = toVisit.pop();
                for (int i = ret.children.size() - 1; i >= 0; i--) {
                    toVisit.push(ret.children.get(i));
                }
                return ret;
            }
        };
    }

    static Tree subtree(String... children) {
        Tree root = new Tree(null, null);
        Arrays.asList(children).forEach(root::add);
        return root;
    }

    static Tree subtree(Tree... children) {
        Tree root = new Tree(null, null);
        Arrays.asList(children).forEach(root::add);
        return root;
    }

    static Tree subtree(Iterable<? extends Tree> children) {
        Tree root = new Tree(null, null);
        children.forEach(root::add);
        return root;
    }

    static Tree leaf(String text) {
        return new Tree(text, null);
    }

}
