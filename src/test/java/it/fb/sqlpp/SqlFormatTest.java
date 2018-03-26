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
    public void verifyCommaIsNotSeparatedFromColumnName() {
        assertEquals("SELECT\n    A\n    , VERYLONGCOLUMNNAME\n  FROM TBL",
                SqlFormat.format("SELECT A, VERYLONGCOLUMNNAME FROM TBL", 15, 2));
    }

    @Test
    public void verifyJoinIsFormattedAsExpected80() {
        assertEquals("SELECT * FROM TBL LEFT JOIN TBL2 ON A = B",
                SqlFormat.format("SELECT * FROM TBL LEFT JOIN TBL2 ON A = B", 80, 2));
    }

    @Test
    public void verifyJoinIsFormattedAsExpected40() {
        assertEquals("SELECT *\n  FROM TBL LEFT JOIN TBL2 ON A = B",
                SqlFormat.format("SELECT * FROM TBL LEFT JOIN TBL2 ON A = B", 40, 2));
    }

    @Test
    @Ignore
    public void verifyJoinIsFormattedAsExpected30() {
        assertEquals("SELECT *\n  FROM\n    TBL\n    LEFT JOIN TBL2 ON A = B",
                SqlFormat.format("SELECT * FROM TBL LEFT JOIN TBL2 ON A = B", 30, 2));
    }

    @Test
    @Ignore
    public void verifyJoinIsFormattedAsExpected20() {
        assertEquals("SELECT *\n  FROM\n    TBL\n    LEFT JOIN TBL2\n      ON A = B",
                SqlFormat.format("SELECT * FROM TBL LEFT JOIN TBL2 ON A = B", 20, 2));
    }

    @Test
    @Ignore
    public void verifyToTreeX15() {
        assertEquals("SELECT COL1\n  FROM TBL\n  WHERE\n    A = B\n    AND C = D",
                SqlFormat.format("SELECT COL1 FROM TBL WHERE A=B AND C=D", 15, 2));
    }
}
