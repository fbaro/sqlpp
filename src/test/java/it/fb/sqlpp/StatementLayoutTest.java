package it.fb.sqlpp;

import com.facebook.presto.sql.parser.ParsingOptions;
import com.facebook.presto.sql.parser.SqlParser;
import com.facebook.presto.sql.parser.SqlParserOptions;
import com.facebook.presto.sql.tree.Statement;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

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
    public void formatFullSelect_W80() {
        assertFormatEquals(80, 2, "SELECT * FROM TBL WHERE A = B GROUP BY C HAVING D > 1 ORDER BY E");
    }

    @Test
    public void formatFullSelect_W15() {
        assertFormatEquals(15, 2, "SELECT *\nFROM TBL\nWHERE A = B\nGROUP BY C\nHAVING D > 1\nORDER BY E");
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

    @Test
    public void formatMultipleFromTables_W20() {
        assertFormatEquals(20, 2, "SELECT *\nFROM LONG_TABLE_1\n  , LONG_TABLE_2\n  , LONG_TABLE_3");
    }

    @Test
    public void formatMultipleJoins_W80() {
        assertFormatEquals(80, 2, "SELECT * FROM TBL1 INNER JOIN TBL2 ON A = B LEFT JOIN TBL3 ON C < D");
    }

    @Test
    public void formatMultipleJoins_W60() {
        assertFormatEquals(60, 2, "SELECT *\nFROM TBL1 INNER JOIN TBL2 ON A = B LEFT JOIN TBL3 ON C < D");
    }

    @Test
    public void formatMultipleJoins_W40() {
        assertFormatEquals(40, 2, "SELECT *\nFROM TBL1\n  INNER JOIN TBL2 ON A = B\n  LEFT JOIN TBL3 ON C < D");
    }

    @Test
    public void formatMultipleJoins_W20() {
        assertFormatEquals(20, 2, "SELECT *\nFROM TBL1\n  INNER JOIN TBL2\n    ON A = B\n  LEFT JOIN TBL3\n    ON C < D");
    }

    @Test
    public void formatMultipleArithmeticOperations_W80() {
        assertFormatEquals(80, 2, "SELECT COL_1 + COL_2 - COL_3, COL_4 FROM DUAL");
    }

    @Test
    public void formatMultipleArithmeticOperations_W40() {
        assertFormatEquals(40, 2, "SELECT COL_1 + COL_2 - COL_3, COL_4\nFROM DUAL");
    }

    @Test
    public void formatMultipleArithmeticOperations_W30() {
        assertFormatEquals(30, 2, "SELECT COL_1 + COL_2 - COL_3,\n  COL_4\nFROM DUAL");
    }

    @Test
    public void formatMultipleArithmeticOperations_W20() {
        assertFormatEquals(20, 2, "SELECT COL_1\n    + COL_2\n    - COL_3,\n  COL_4\nFROM DUAL");
    }

    @Test
    public void formatMultipleArithmeticOperations_W20_2() {
        assertFormatEquals(20, 2, "SELECT COL_4,\n  COL_1\n    - COL_2\n    + COL_3\nFROM DUAL");
    }

    @Test
    public void formatAliases_W80() {
        assertFormatEquals(80, 2, "SELECT T.*, T.A FROM LONG_TABLE_1 AS T WHERE T.A = T.B");
    }

    @Test
    public void formatAliases_W40() {
        assertFormatEquals(40, 2, "SELECT T.*, T.A\nFROM LONG_TABLE_1 AS T\nWHERE T.A = T.B");
    }

    @Test
    public void formatAliases_W20() {
        assertFormatEquals(20, 2, "SELECT T.*, T.A\nFROM LONG_TABLE_1 AS T\nWHERE T.A = T.B");
    }

    @Test
    public void formatInList_W80() {
        assertFormatEquals(80, 2, "SELECT * FROM TBL WHERE A IN ( X, Y )");
    }

    @Test
    public void formatInList_W20() {
        assertFormatEquals(20, 2, "SELECT *\nFROM TBL\nWHERE A IN ( X, Y )");
    }

    @Test
    public void formatInListWithLongIdentifiers_W35() {
        assertFormatEquals(35, 2, "SELECT *\nFROM TBL\nWHERE A\n  IN ( LONG_COL_1, LONG_COL_2 )");
    }

    @Test
    public void formatInListWithLongIdentifiers_W30() {
        assertFormatEquals(30, 2, "SELECT *\nFROM TBL\nWHERE A\n  IN ( LONG_COL_1,\n    LONG_COL_2 )");
    }

    @Test
    public void formatLike_W80() {
        assertFormatEquals(80, 2, "SELECT * FROM TBL WHERE COL LIKE '%' ESCAPE 'e'");
    }

    @Test
    public void formatLike_W40() {
        assertFormatEquals(40, 2, "SELECT *\nFROM TBL\nWHERE COL LIKE '%' ESCAPE 'e'");
    }

    @Test
    public void formatLike_W20() {
        assertFormatEquals(20, 2, "SELECT *\nFROM TBL\nWHERE COL\n  LIKE '%'\n  ESCAPE 'e'");
    }

    @Test
    public void formatCase_W80() {
        assertFormatEquals(80, 2, "SELECT X, CASE WHEN A = B THEN 1 WHEN C = D THEN 2 ELSE 3 END FROM TBL");
    }

    @Test
    public void formatCase_W60() {
        assertFormatEquals(60, 2, "SELECT X,\n  CASE WHEN A = B THEN 1 WHEN C = D THEN 2 ELSE 3 END\nFROM TBL");
    }

    @Test
    public void formatCase_W40() {
        assertFormatEquals(40, 2, "SELECT X,\n  CASE WHEN A = B THEN 1\n    WHEN C = D THEN 2\n    ELSE 3\n    END\nFROM TBL");
    }

    @Test
    public void formatCase_W20() {
        assertFormatEquals(20, 2, "SELECT X,\n  CASE WHEN A = B\n      THEN 1\n    WHEN C = D\n      THEN 2\n    ELSE 3\n    END\nFROM TBL");
    }

    @Test
    public void formatComplexQuery_W300() {
        assertFormatEquals(300, 2, "" +
                "SELECT FIRMID, EVENTTIME_EVTDATE, EVENTTIME_EVTTIMESEC, NEWSCATEGORY, NEWSID, NEWSPAGE, NEWSSUBJECT, " +
                "( SELECT NI.ISINCODE FROM MDB_NEWS_INSTRUMENT AS NI WHERE NI.NEWSID = mdb_news.NEWSID AND NI.FIRMID = mdb_news.FIRMID AND rownum = 1 ) AS ISINCODE " +
                "FROM mdb_news");
    }

    @Test
    public void formatComplesQuery_W250() {
        assertFormatEquals(250, 2, "" +
                "SELECT FIRMID, EVENTTIME_EVTDATE, EVENTTIME_EVTTIMESEC, NEWSCATEGORY, NEWSID, NEWSPAGE, NEWSSUBJECT, " +
                "( SELECT NI.ISINCODE FROM MDB_NEWS_INSTRUMENT AS NI WHERE NI.NEWSID = mdb_news.NEWSID AND NI.FIRMID = mdb_news.FIRMID AND rownum = 1 ) AS ISINCODE\n" +
                "FROM mdb_news");
    }

    @Test
    public void formatComplexQuery_W200() {
        assertFormatEquals(200, 2, "" +
                "SELECT FIRMID,\n  EVENTTIME_EVTDATE,\n  EVENTTIME_EVTTIMESEC,\n  NEWSCATEGORY,\n  NEWSID,\n  NEWSPAGE,\n  NEWSSUBJECT," +
                "\n  ( SELECT NI.ISINCODE FROM MDB_NEWS_INSTRUMENT AS NI WHERE NI.NEWSID = mdb_news.NEWSID AND NI.FIRMID = mdb_news.FIRMID AND rownum = 1 ) AS ISINCODE\n" +
                "FROM mdb_news");
    }

    /**
     * Notice this test appears to be wrong: the second line is longer than 140 characters.
     * Splitting however does not occur because the line length is exceeded with "AS ISINCODE", which is treated as
     * a trailer, and because of this is not subject to line length testing.
     * This might change in future, in case it appears to produce consistently wrong results.
     */
    @Test
    public void formatComplexQuery_W140() {
        assertFormatEquals(140, 2, "" +
                "SELECT FIRMID," +
                "\n  ( SELECT NI.ISINCODE FROM MDB_NEWS_INSTRUMENT AS NI WHERE NI.NEWSID = mdb_news.NEWSID AND NI.FIRMID = mdb_news.FIRMID AND rownum = 1 ) AS ISINCODE\n" +
                "FROM mdb_news");
    }

    @Test
    public void formatComplexQuery_W80() {
        assertFormatEquals(80, 2, "" +
                "SELECT FIRMID,\n" +
                "  ( SELECT NI.ISINCODE\n" +
                "      FROM MDB_NEWS_INSTRUMENT AS NI\n" +
                "      WHERE NI.NEWSID = mdb_news.NEWSID\n" +
                "        AND NI.FIRMID = mdb_news.FIRMID\n" +
                "        AND rownum = 1 ) AS ISINCODE\n" +
                "FROM mdb_news");
    }

    private static void assertFormatEquals(int lineWidth, int indentWidth, String sql) {
        Statement statement = new SqlParser(new SqlParserOptions()).createStatement(sql, new ParsingOptions());
        String formatted = StatementLayout.format(lineWidth, indentWidth, statement);
        assertEquals(sql, formatted);
    }
}
