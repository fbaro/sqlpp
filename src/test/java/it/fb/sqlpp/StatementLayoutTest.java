package it.fb.sqlpp;

import com.facebook.presto.sql.parser.ParsingOptions;
import com.facebook.presto.sql.parser.SqlParser;
import com.facebook.presto.sql.parser.SqlParserOptions;
import com.facebook.presto.sql.tree.Statement;
import org.junit.Test;

import static org.junit.Assert.*;

public class StatementLayoutTest {

    @Test
    public void formatSimpleSelect_W80() {
        assertFormatEquals(80, 2, "SELECT * FROM TBL");
    }

    @Test
    public void formatSimpleSelect_W15() {
        assertFormatEquals(15, 2, "SELECT *\nFROM TBL");
    }

    @Test
    public void formatSimpleSelect_W15_4() {
        assertFormatEquals(15, 4, "SELECT *\nFROM TBL");
    }

    @Test
    public void formatSimpleSelectWhere_W80() {
        assertFormatEquals(80, 2, "SELECT * FROM TBL WHERE A = B");
    }

    @Test
    public void formatSimpleSelectWhere_W15() {
        assertFormatEquals(15, 2, "SELECT *\nFROM TBL\nWHERE A = B");
    }

    @Test
    public void formatSelectWithLongColumns_W80() {
        assertFormatEquals(80, 2, "SELECT LONG_COLUMN_NAME_1, LONG_COLUMN_NAME_2 FROM TBL");
    }

    @Test
    public void formatSelectWithLongColumns_W50() {
        assertFormatEquals(50, 2, "SELECT LONG_COLUMN_NAME_1, LONG_COLUMN_NAME_2\nFROM TBL");
    }

    @Test
    public void formatSelectWithLongColumns_W40() {
        assertFormatEquals(40, 2, "SELECT LONG_COLUMN_NAME_1,\n  LONG_COLUMN_NAME_2\nFROM TBL");
    }

    @Test
    public void formatSelectWithLongColumns_W20() {
        assertFormatEquals(20, 2, "SELECT LONG_COLUMN_NAME_1,\n  LONG_COLUMN_NAME_2\nFROM TBL");
    }

    @Test
    public void formatInnerJoin_W80() {
        assertFormatEquals(80, 2, "SELECT * FROM TBL1 INNER JOIN TBL2 ON A = B WHERE C = D");
    }

    @Test
    public void formatInnerJoin_W50() {
        assertFormatEquals(50, 2, "SELECT *\nFROM TBL1 INNER JOIN TBL2 ON A = B\nWHERE C = D");
    }

    @Test
    public void formatInnerJoin_W30() {
        assertFormatEquals(30, 2, "SELECT *\nFROM TBL1\n  INNER JOIN TBL2 ON A = B\nWHERE C = D");
    }

    @Test
    public void formatInnerJoin_W20() {
        assertFormatEquals(20, 2, "SELECT *\nFROM TBL1\n  INNER JOIN TBL2\n    ON A = B\nWHERE C = D");
    }

    @Test
    public void formatAndCondition_W80() {
        assertFormatEquals(80, 2, "SELECT * FROM TBL1 WHERE A = B AND C = D");
    }

    @Test
    public void formatAndCondition_W30() {
        assertFormatEquals(30, 2, "SELECT *\nFROM TBL1\nWHERE A = B AND C = D");
    }

    @Test
    public void formatAndCondition_W20() {
        assertFormatEquals(20, 2, "SELECT *\nFROM TBL1\nWHERE A = B\n  AND C = D");
    }

    @Test
    public void formatMultipleAndConditions_W20() {
        assertFormatEquals(20, 2, "SELECT *\nFROM TBL1\nWHERE A = B\n  AND C = D\n  AND E = F\n  AND G = H");
    }

    private static void assertFormatEquals(int lineWidth, int indentWidth, String sql) {
        Statement statement = new SqlParser(new SqlParserOptions()).createStatement(sql, new ParsingOptions());
        String formatted = StatementLayout.format(lineWidth, indentWidth, statement);
        assertEquals(sql, formatted);
    }
}
