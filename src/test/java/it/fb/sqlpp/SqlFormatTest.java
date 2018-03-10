package it.fb.sqlpp;

import org.junit.Before;
import org.junit.Test;

import static it.fb.sqlpp.Tree.leaf;
import static it.fb.sqlpp.Tree.subtree;
import static org.junit.Assert.assertEquals;

public class SqlFormatTest {

    private Tree simpleExpr;

    @Before
    public void setUp() {
        simpleExpr = subtree(
                subtree("SELECT", "*"),
                subtree("FROM", "TBL"),
                subtree(leaf("WHERE"), subtree(
                        leaf("A"), subtree("=", "B"))));
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

}