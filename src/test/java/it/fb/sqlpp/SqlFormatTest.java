package it.fb.sqlpp;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

public class SqlFormatTest extends Trees {

    @Test
    public void verifyToTree() {
        assertEquals(simpleExpr, SqlFormat.toTree("SELECT * FROM TBL WHERE A=B"));
    }

    @Test
    public void verifyToTree2() {
        assertEquals(simpleExpr2, SqlFormat.toTree("SELECT QUITELONGCOLUMN FROM TBL WHERE A=B"));
    }

    @Test
    public void verifyToTree3() {
        assertEquals(simpleExpr3, SqlFormat.toTree("SELECT COL1, COL2, COL3, COL4 FROM TBL"));
    }

    @Test
    public void verifyToTreeX40() {
        assertEquals("SELECT COL1\n  FROM TBL\n  WHERE A = B AND C = D",
                SqlFormat.format("SELECT COL1 FROM TBL WHERE A=B AND C=D", 40, 2));
    }

    @Test
    @Ignore
    public void verifyToTreeX15() {
        assertEquals("SELECT COL1\n  FROM TBL\n  WHERE\n    A = B\n    AND C = D",
                SqlFormat.format("SELECT COL1 FROM TBL WHERE A=B AND C=D", 15, 2));
    }
}
