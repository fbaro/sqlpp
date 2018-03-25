package it.fb.sqlpp;

import org.junit.Test;

import static org.junit.Assert.*;

public class TreeLayoutTest {

    private static TreeLayout.TreeCode xCommaY(String x, String y) {
        return r -> r
                .child("", ",", ch2 -> ch2.leaf(x))
                .child("", "", ch2 -> ch2.leaf(y));
    }

    private static TreeLayout.TreeCode xEqualY(String x, String y) {
        return r -> r
                .child("", "", ch2 -> ch2.leaf(x))
                .child("=", "", ch2 -> ch2.leaf(y));
    }

    private TreeLayout.TreeCode simpleCode = c -> c
            .child("SELECT", "", xCommaY("A", "B"))
            .child("FROM", "", ch -> ch.leaf("TABLE"));

    private TreeLayout.TreeCode longTablesCode = c -> c
            .child("SELECT", "", xCommaY("A", "B"))
            .child("FROM", "", xCommaY("VERYLONGTABLE1", "VERYLONGTABLE2"));

    private TreeLayout.TreeCode innerJoinCode = c -> c
            .child("SELECT", "", c1 -> c1.leaf("*"))
            .child("FROM", "", c1 -> c1
                    .child("", "", c2 -> c2.leaf("TBL1"))
                    .child("INNER JOIN", "", c2 -> c2
                            .leaf("TBL2")
                            .child("ON", "", xEqualY("X", "Y"))))
            .child("WHERE", "", xEqualY("A", "B"));


    @Test
    public void testSimpleSelect_W80() {
        assertEquals("SELECT A, B FROM TABLE", TreeLayout.format(80, 2, simpleCode));
    }

    @Test
    public void testSimpleSelect_W15() {
        assertEquals("SELECT A, B\nFROM TABLE", TreeLayout.format(15, 2, simpleCode));
    }

    @Test
    public void testSecondLevelIndent_W80() {
        assertEquals("SELECT A, B FROM VERYLONGTABLE1, VERYLONGTABLE2", TreeLayout.format(80, 2, longTablesCode));
    }

    @Test
    public void testSecondLevelIndent_W15() {
        assertEquals("SELECT A, B\nFROM VERYLONGTABLE1,\n  VERYLONGTABLE2", TreeLayout.format(15, 2, longTablesCode));
    }

    @Test
    public void testInnerJoinIndent_W80() {
        assertEquals("SELECT * FROM TBL1 INNER JOIN TBL2 ON X = Y WHERE A = B", TreeLayout.format(80, 2, innerJoinCode));
    }

    @Test
    public void testInnerJoinIndent_W40() {
        assertEquals("SELECT *\nFROM TBL1 INNER JOIN TBL2 ON X = Y\nWHERE A = B", TreeLayout.format(40, 2, innerJoinCode));
    }

    @Test
    public void testInnerJoinIndent_W30() {
        assertEquals("SELECT *\nFROM TBL1\n  INNER JOIN TBL2 ON X = Y\nWHERE A = B", TreeLayout.format(30, 2, innerJoinCode));
    }

    @Test
    public void testInnerJoinIndent_W20() {
        assertEquals("SELECT *\nFROM TBL1\n  INNER JOIN TBL2\n    ON X = Y\nWHERE A = B", TreeLayout.format(20, 2, innerJoinCode));
    }
}
