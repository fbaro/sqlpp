package it.fb.sqlpp;

import java.util.function.Consumer;

@FunctionalInterface
public interface Tree extends Consumer<Tree.Visitor> {

    interface Visitor {
        Visitor leaf(String text);

        Visitor child(String preLabel, String postLabel, Tree subTree);

        Visitor singleChild(String preLabel, String postLabel, Tree subTree);
    }

}
