package it.fb.sqlpp;

public interface TreeVisitor {
    TreeVisitor leaf(String text);

    TreeVisitor child(String preLabel, String postLabel, Tree subTree);

    TreeVisitor singleChild(String preLabel, String postLabel, Tree subTree);
}
