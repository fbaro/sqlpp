package it.fb.sqlpp;

import org.junit.Before;
import org.junit.Test;

import static it.fb.sqlpp.Tree.leaf;
import static it.fb.sqlpp.Tree.subtree;
import static org.junit.Assert.assertEquals;

public class SqlFormatTest {

    private Tree simpleExpr;
    private Tree simpleExpr2;

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
    }

    @Test
    public void verifyNoFormattingForShortStringAndLongLine() {
        assertEquals("SELECT * FROM TBL WHERE A = B",
                SqlFormat.format(simpleExpr, 80, 4));
    }

    @Test
    public void verifyFormattingForShortStringAndShortLine() {
        assertEquals("SELECT *\n  FROM TBL\n  WHERE A = B",
                SqlFormat.format(simpleExpr, 20, 2));
    }
    @Test
    public void verifyFormattingForShortStringAndShortLine2() {
        assertEquals("SELECT\n    QUITELONGCOLUMN\n  FROM TBL\n  WHERE A = B",
                SqlFormat.format(simpleExpr2, 20, 2));
    }

}