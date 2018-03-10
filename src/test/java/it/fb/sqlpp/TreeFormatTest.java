package it.fb.sqlpp;

import org.junit.Before;
import org.junit.Test;

import static it.fb.sqlpp.Tree.leaf;
import static it.fb.sqlpp.Tree.subtree;
import static org.junit.Assert.assertEquals;

public class TreeFormatTest {

    private Tree simpleExpr;
    private Tree simpleExpr2;
    private Tree simpleExpr3;

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

    @Test
    public void verifyNoFormattingForShortStringAndLongLine() {
        assertEquals("SELECT * FROM TBL WHERE A = B",
                TreeFormat.format(simpleExpr, 80, 4));
    }

    @Test
    public void verifyFormattingForShortStringAndShortLine() {
        assertEquals("SELECT *\n  FROM TBL\n  WHERE A = B",
                TreeFormat.format(simpleExpr, 20, 2));
    }

    @Test
    public void verifyFormattingForShortStringAndShortLine2() {
        assertEquals("SELECT\n    QUITELONGCOLUMN\n  FROM TBL\n  WHERE A = B",
                TreeFormat.format(simpleExpr2, 20, 2));
    }

    @Test
    public void verifyFormattingForShortStringAndShortLine80() {
        assertEquals("SELECT COL1 , COL2 , COL3 , COL4 FROM TBL",
                TreeFormat.format(simpleExpr3, 80, 2));
    }

    @Test
    public void verifyFormattingForShortStringAndShortLine40() {
        assertEquals("SELECT COL1 , COL2 , COL3 , COL4\n  FROM TBL",
                TreeFormat.format(simpleExpr3, 40, 2));
    }

    @Test
    public void verifyFormattingForShortStringAndShortLine30() {
        assertEquals("SELECT\n    COL1 , COL2 , COL3 , COL4\n  FROM TBL",
                TreeFormat.format(simpleExpr3, 30, 2));
    }

    /**
     * <pre>
     * SELECT COL1 , COL2 , COL3 , COL4 FROM TBL
     *
     * SELECT COL1 , COL2 , COL3 , COL4
     *   FROM TBL
     *
     * SELECT
     *     COL1 , COL2 , COL3 , COL4
     *   FROM TBL
     *
     * SELECT
     *       COL1
     *       , COL2
     *       , COL3
     *       , COL4
     *   FROM TBL
     * </pre>
     */
    @Test
    public void verifyFormattingForShortStringAndShortLine20() {
        assertEquals("SELECT\n      COL1\n      , COL2\n      , COL3\n      , COL4\n  FROM TBL",
                TreeFormat.format(simpleExpr3, 20, 2));
    }
}