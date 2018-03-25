package it.fb.sqlpp;

import org.junit.Test;

import static org.junit.Assert.*;

public class TreeLayoutTest {

    private static TreeLayout.TreeCode xCommaY(String x, String y) {
        return r -> r
                .child("", ",", ch2 -> {
                    ch2.leaf(x);
                })
                .child("", "", ch2 -> {
                    ch2.leaf(y);
                });
    }

    private TreeLayout.TreeCode simpleCode = c -> c
            .child("SELECT", "", xCommaY("A", "B"))
            .child("FROM", "", ch -> {
                ch.leaf("TABLE");
            });

    private TreeLayout.TreeCode longTablesCode = c -> c
            .child("SELECT", "", xCommaY("A", "B"))
            .child("FROM", "", xCommaY("VERYLONGTABLE1", "VERYLONGTABLE2"));

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
}