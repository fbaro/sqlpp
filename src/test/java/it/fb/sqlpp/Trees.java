package it.fb.sqlpp;

import org.junit.Before;

import static it.fb.sqlpp.Tree.leaf;
import static it.fb.sqlpp.Tree.subtree;

public class Trees {

    protected Tree simpleExpr;
    protected Tree simpleExpr2;
    protected Tree simpleExpr3;

    @Before
    public void setUp() {
        simpleExpr =
                subtree(
                        subtree(
                                leaf("SELECT"),
                                leaf("*")),
                        subtree(
                                leaf("FROM"),
                                leaf("TBL")),
                        subtree(
                                leaf("WHERE"),
                                subtree(
                                        leaf("A"),
                                        subtree(
                                                leaf("="),
                                                leaf("B")))));
        simpleExpr2 =
                subtree(
                        subtree(
                                leaf("SELECT"),
                                leaf("QUITELONGCOLUMN")),
                        subtree(
                                leaf("FROM"),
                                leaf("TBL")),
                        subtree(
                                leaf("WHERE"),
                                subtree(
                                        leaf("A"),
                                        subtree(
                                                leaf("="),
                                                leaf("B")))));
        simpleExpr3 =
                subtree(
                        subtree(
                                leaf("SELECT"),
                                subtree(
                                        leaf("COL1"),
                                        subtree(",", "COL2"),
                                        subtree(",", "COL3"),
                                        subtree(",", "COL4"))),
                        subtree(
                                leaf("FROM"),
                                leaf("TBL")));
    }
}
