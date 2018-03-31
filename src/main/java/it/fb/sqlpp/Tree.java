package it.fb.sqlpp;

import java.util.function.Consumer;

@FunctionalInterface
public interface Tree extends Consumer<TreeVisitor> {
}
