package it.fb.sqlpp;

import java.io.PrintWriter;
import java.io.StringWriter;

public class TreePrint implements Tree.Visitor {

    public static String print(Tree tree) {
        TreePrint tp = new TreePrint();
        tree.accept(tp);
        return tp.sw.toString();
    }

    private int indent = 0;
    private StringWriter sw = new StringWriter();
    private PrintWriter pw = new PrintWriter(sw);

    private TreePrint() {
    }

    @Override
    public Tree.Visitor leaf(String text) {
        indent(text);
        return this;
    }

    private void indent(String text) {
        for (int i = 0; i < indent; i++) {
            pw.print("    ");
        }
        pw.println(text);
    }

    @Override
    public Tree.Visitor child(String preLabel, String postLabel, Tree subTree) {
        indent(preLabel + "-" + postLabel);
        indent++;
        subTree.accept(this);
        indent--;
        return this;
    }

    @Override
    public void singleChild(String preLabel, String postLabel, Tree subTree) {
        indent(preLabel + "." + postLabel);
        indent++;
        subTree.accept(this);
        indent--;
    }
}
