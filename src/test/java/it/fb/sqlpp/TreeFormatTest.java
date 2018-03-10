package it.fb.sqlpp;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TreeFormatTest extends Trees {

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