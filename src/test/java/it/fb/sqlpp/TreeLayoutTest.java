package it.fb.sqlpp;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

public class TreeLayoutTest {

    private TreeLayout.TreeCode simpleTreeCode = c -> {
        c
                .child("SELECT", "", ch -> {
                    ch
                            .child("", ",", ch2 -> {
                                ch2.leaf("A");
                            })
                            .child("", "", ch2 -> {
                                ch2.leaf("B");
                            });
                })
                .child("FROM", "", ch -> {
                    ch.leaf("TABLE");
                });
    };

    @Test
    public void testSimpleSelect_W80() {
        assertEquals("SELECT A, B FROM TABLE", TreeLayout.format(80, 2, simpleTreeCode));
    }

    @Test
    public void testSimpleSelect_W20() {
        assertEquals("SELECT A, B\nFROM TABLE", TreeLayout.format(20, 2, simpleTreeCode));
    }
}