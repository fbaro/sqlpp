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
    public void formatSimpleSelectWhere_W80() {
        assertFormatEquals(80, 2, "SELECT * FROM TBL WHERE A = B");
    }

    @Test
    public void formatSimpleSelectWhere_W15() {
        assertFormatEquals(15, 2, "SELECT *\nFROM TBL\nWHERE A = B");
    }

    private static void assertFormatEquals(int lineWidth, int indentWidth, String sql) {
        Statement statement = new SqlParser(new SqlParserOptions()).createStatement(sql, new ParsingOptions());
        String formatted = StatementLayout.format(lineWidth, indentWidth, statement);
        assertEquals(sql, formatted);
    }
}
