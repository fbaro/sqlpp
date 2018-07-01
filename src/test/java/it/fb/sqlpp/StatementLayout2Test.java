package it.fb.sqlpp;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class StatementLayout2Test {

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
    public void formatSimpleSelectTwoTables_W80() {
        assertFormatEquals(80, 2, "SELECT * FROM TBL1 , TBL2");
    }

    @Test
    public void formatSimpleSelectTwoTables_W20() {
        assertFormatEquals(20, 2, "SELECT *\nFROM TBL1 , TBL2");
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
    public void formatFullSelectNoOrderBy_W80() {
        assertFormatEquals(80, 2, "SELECT * FROM TBL WHERE A = B GROUP BY C, D HAVING D > 1");
    }

    @Test
    public void formatFullSelect_W90() {
        assertFormatEquals(90, 2, "SELECT * FROM TBL WHERE A = B GROUP BY C, D HAVING D > 1 ORDER BY E DESC NULLS LAST");
    }

    @Test
    public void formatFullSelect_W15() {
        assertFormatEquals(15, 2, "SELECT *\nFROM TBL\nWHERE A = B\nGROUP BY C, D\nHAVING D > 1\nORDER BY E DESC NULLS LAST");
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
        assertFormatEquals(80, 2, "SELECT T.*, T.A FROM LONG_TABLE_1 T WHERE T.A = T.B");
    }

    @Test
    public void formatAliases_W40() {
        assertFormatEquals(40, 2, "SELECT T.*, T.A\nFROM LONG_TABLE_1 T\nWHERE T.A = T.B");
    }

    @Test
    public void formatAliases_W20() {
        assertFormatEquals(20, 2, "SELECT T.*, T.A\nFROM LONG_TABLE_1 T\nWHERE T.A = T.B");
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
    public void formatSimpleCase_W80() {
        assertFormatEquals(80, 2, "SELECT X, CASE A WHEN B THEN 1 WHEN C THEN 2 ELSE 3 END FROM TBL");
    }

    @Test
    public void formatSimpleCase_W60() {
        assertFormatEquals(60, 2, "SELECT X, CASE A WHEN B THEN 1 WHEN C THEN 2 ELSE 3 END\nFROM TBL");
    }

    @Test
    public void formatSimpleCase_W25() {
        assertFormatEquals(25, 2, "SELECT X,\n  CASE A\n    WHEN B THEN 1\n    WHEN C THEN 2\n    WHEN D THEN 3\n    ELSE 4 END\nFROM TBL");
    }

    @Test
    public void formatFunction_W80() {
        assertFormatEquals(80, 2, "SELECT X, FUNC( COL1, COL2, COL3) FROM TBL");
    }

    @Test
    public void formatFunction_W40() {
        assertFormatEquals(40, 2, "SELECT X, FUNC( COL1, COL2, COL3)\nFROM TBL");
    }

    @Test
    public void formatFunction_W30() {
        assertFormatEquals(30, 2, "SELECT X,\n  FUNC( COL1, COL2, COL3)\nFROM TBL");
    }

    @Test
    public void formatFunction_W20() {
        assertFormatEquals(20, 2, "SELECT X,\n  FUNC( COL1,\n    COL2,\n    COL3)\nFROM TBL");
    }

    @Test
    public void formatWindowFunction_W100() {
        assertFormatEquals(100, 2, "SELECT X, LEAD( COL1, COL2, COL3) OVER( PARTITION BY X, Y ORDER BY Z, K DESC) FROM TBL");
    }

    @Test
    public void formatWindowFunction_W80() {
        assertFormatEquals(80, 2, "SELECT X, LEAD( COL1, COL2, COL3) OVER( PARTITION BY X, Y ORDER BY Z, K DESC)\nFROM TBL");
    }

    @Test
    public void formatWindowFunction_W70() {
        assertFormatEquals(70, 2, "SELECT X,\n  LEAD( COL1, COL2, COL3) OVER( PARTITION BY X, Y ORDER BY Z, K DESC)\nFROM TBL");
    }

    @Test
    public void formatWindowFunction_W60() {
        assertFormatEquals(60, 2, "SELECT X,\n  LEAD( COL1, COL2, COL3)\n    OVER( PARTITION BY X, Y ORDER BY Z, K DESC)\nFROM TBL");
    }

    @Test
    public void formatWindowFunction_W40() {
        assertFormatEquals(40, 2, "SELECT X,\n  LEAD( COL1, COL2, COL3)\n    OVER( PARTITION BY X, Y\n      ORDER BY Z, K DESC)\nFROM TBL");
    }

    @Test
    public void formatOperatorPrecedence_1_W80() {
        assertFormatEquals(80, 2, "SELECT X + ( Y * Z) + K, W");
    }

    @Test
    public void formatOperatorPrecedence_1_W20() {
        assertFormatEquals(20, 2, "SELECT X\n    + ( Y * Z)\n    + K,\n  W");
    }

    @Test
    public void formatOperatorPrecedence_2_W80() {
        assertFormatEquals(80, 2, "SELECT ( X + Y) * ( Z + K)");
    }

    @Test
    public void formatOperatorPrecedence_2_W20() {
        assertFormatEquals(20, 2, "SELECT ( X + Y)\n    * ( Z + K),\n  W");
    }

    @Test
    public void formatOperatorPrecedence_3_W80() {
        assertFormatEquals(80, 2, "SELECT 1 FROM TBL WHERE ( A = B OR C = D OR E = G) AND G = H");
    }

    @Test
    public void formatOperatorPrecedence_3_W35() {
        assertFormatEquals(35, 2, "SELECT 1\nFROM TBL\nWHERE ( A = B OR C = D OR E = G)\n  AND G = H");
    }

    @Test
    public void formatOperatorPrecedence_4_W80() {
        assertFormatEquals(80, 2, "SELECT 1 FROM TBL WHERE ( A = B AND C = D AND E = G) OR G = H");
    }

    @Test
    public void formatOperatorPrecedence_4_W35() {
        assertFormatEquals(35, 2, "SELECT 1\nFROM TBL\nWHERE ( A = B AND C = D AND E = G)\n  OR G = H");
    }

    @Test
    public void formatComplexQuery_W300() {
        assertFormatEquals(300, 2, "" +
                "SELECT FIRMID, EVENTTIME_EVTDATE, EVENTTIME_EVTTIMESEC, NEWSCATEGORY, NEWSID, NEWSPAGE, NEWSSUBJECT, " +
                "( SELECT NI.ISINCODE FROM MDB_NEWS_INSTRUMENT NI WHERE NI.NEWSID = mdb_news.NEWSID AND NI.FIRMID = mdb_news.FIRMID AND rownum = 1 ) AS ISINCODE " +
                "FROM mdb_news");
    }

    @Test
    public void formatComplexQuery_W250() {
        assertFormatEquals(250, 2, "" +
                "SELECT FIRMID, EVENTTIME_EVTDATE, EVENTTIME_EVTTIMESEC, NEWSCATEGORY, NEWSID, NEWSPAGE, NEWSSUBJECT, " +
                "( SELECT NI.ISINCODE FROM MDB_NEWS_INSTRUMENT NI WHERE NI.NEWSID = mdb_news.NEWSID AND NI.FIRMID = mdb_news.FIRMID AND rownum = 1 ) AS ISINCODE\n" +
                "FROM mdb_news");
    }

    @Test
    public void formatComplexQuery_W200() {
        assertFormatEquals(200, 2, "" +
                "SELECT FIRMID,\n  EVENTTIME_EVTDATE,\n  EVENTTIME_EVTTIMESEC,\n  NEWSCATEGORY,\n  NEWSID,\n  NEWSPAGE,\n  NEWSSUBJECT," +
                "\n  ( SELECT NI.ISINCODE FROM MDB_NEWS_INSTRUMENT NI WHERE NI.NEWSID = mdb_news.NEWSID AND NI.FIRMID = mdb_news.FIRMID AND rownum = 1 ) AS ISINCODE\n" +
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
                "\n  ( SELECT NI.ISINCODE FROM MDB_NEWS_INSTRUMENT NI WHERE NI.NEWSID = mdb_news.NEWSID AND NI.FIRMID = mdb_news.FIRMID AND rownum = 1 ) AS ISINCODE\n" +
                "FROM mdb_news");
    }

    @Test
    public void formatComplexQuery_W80() {
        assertFormatEquals(80, 2, "" +
                "SELECT FIRMID,\n" +
                "  ( SELECT NI.ISINCODE\n" +
                "    FROM MDB_NEWS_INSTRUMENT NI\n" +
                "    WHERE NI.NEWSID = mdb_news.NEWSID\n" +
                "      AND NI.FIRMID = mdb_news.FIRMID\n" +
                "      AND rownum = 1 ) AS ISINCODE\n" +
                "FROM mdb_news");
    }

    @Test
    public void formatInsert_W80() {
        assertFormatEquals(80, 2, "INSERT INTO TBL ( A, B, C) VALUES ( 1, 2, 3)");
    }

    @Test
    public void formatInsert_W40() {
        assertFormatEquals(40, 2, "INSERT INTO TBL ( A, B, C)\nVALUES ( 1, 2, 3)");
    }

    @Test
    public void formatInsert_W20() {
        assertFormatEquals(20, 2, "INSERT INTO TBL ( A,\n  B,\n  C)\nVALUES ( 1, 2, 3)");
    }

    @Test
    public void formatInsert_W15() {
        assertFormatEquals(15, 2, "INSERT INTO TBL ( A,\n  B,\n  C)\nVALUES ( 1,\n  2,\n  3)");
    }

    @Test
    public void formatInsert_2_W80() {
        assertFormatEquals(80, 2, "INSERT INTO TBL VALUES ( ( 1, 2, 3), ( 4, 5, 6))");
    }

    @Test
    public void formatInsert_2_W40() {
        assertFormatEquals(40, 2, "INSERT INTO TBL\nVALUES ( ( 1, 2, 3), ( 4, 5, 6))");
    }

    @Test
    public void formatInsert_2_W30() {
        assertFormatEquals(30, 2, "INSERT INTO TBL\nVALUES ( ( 1, 2, 3),\n  ( 4, 5, 6))");
    }

    // TODO: Looks uglyish
    @Test
    public void formatInsert_2_W15() {
        assertFormatEquals(15, 2, "INSERT INTO TBL\nVALUES ( ( 1,\n    2,\n    3),\n  ( 4, 5, 6))");
    }

    @Test
    public void formatInsert_3_W80() {
        assertFormatEquals(80, 2, "INSERT INTO TBL SELECT * FROM TBL");
    }

    @Test
    public void formatInsert_3_W30() {
        assertFormatEquals(30, 2, "INSERT INTO TBL\nSELECT * FROM TBL");
    }

    @Test
    public void formatDelete_W80() {
        assertFormatEquals(80, 2, "DELETE FROM TBL WHERE A = B");
    }

    @Test
    public void formatDelete_W25() {
        assertFormatEquals(25, 2, "DELETE FROM TBL\nWHERE A = B");
    }

    @Test
    public void formatInsert_3_W05() {
        assertFormatEquals(5, 2, "INSERT INTO TBL\nSELECT *\n  FROM TBL");
    }

    @Test
    public void formatParentheses_W80() {
        assertFormatEquals(80, 2, "SELECT 1 + ( 2 * ( 3 + ( 4 * ( 5 + 6)))) FROM DUAL");
    }

    @Test
    public void formatIsNull_W80() {
        assertFormatEquals(80, 2, "SELECT * FROM TBL WHERE COL1 IS NULL");
        assertFormatEquals(80, 2, "SELECT * FROM TBL WHERE COL1 IS NOT NULL");
    }

    @Test
    public void formatIsNull_W30() {
        assertFormatEquals(30, 2, "SELECT *\nFROM TBL\nWHERE COL1 IS NULL");
        assertFormatEquals(30, 2, "SELECT *\nFROM TBL\nWHERE COL1 IS NOT NULL");
    }

    @Test
    public void formatLiterals_W80() {
        assertFormatEquals(80, 2, "SELECT 'a', 1, NULL FROM TBL");
    }

    @Test
    public void magnumTest() {
        assertFormatEquals(80, 2, "" +
                "SELECT contr.isincode,\n" +
                "  contr.INSTRUMENTFULLNAME,\n" +
                "  contr2.currency1,\n" +
                "  contr.pricemultiplier,\n" +
                "  c.pricenotation,\n" +
                "  c.qtynotation,\n" +
                "  t.price,\n" +
                "  t.qty,\n" +
                "  t.CMN_PRICECURRENCY,\n" +
                "  ft.LOTSIZE,\n" +
                "  t.CMN_EVTTM_EVTDATE,\n" +
                "  contr.DELIVERYTYPE,\n" +
                "  contr.UNDERLYINGISINCODE,\n" +
                "  contr.UNDERLYINGINDEXNAME,\n" +
                "  contr.UNDERLYINGINDEXTERM,\n" +
                "  t.Fees,\n" +
                "  t.MktFees,\n" +
                "  t.CMN_INSTRKEY_INTSECURITYTYPE AS ASSETCLASS,\n" +
                "  t.CMN_INSTRKEY_MIC AS MIC,\n" +
                "  t.CMN_INSTRKEY_STRIKEPRICESTR AS STRIKEPRICE,\n" +
                "  t.CMN_CONTRACTID AS CONTRACTID\n" +
                "FROM MDB_TRADE_EVENT t\n" +
                "  INNER JOIN MDB_TRADE trade\n" +
                "    ON trade.LASTFLOWID = t.CMN_FLOWID\n" +
                "      AND trade.LASTFLOWEVENTKEY = t.CMN_FLOWEVENTKEY\n" +
                "      AND t.TRADESTATUS = 0\n" +
                "  INNER JOIN MDB_CONTRACT_INST_MIFID2 c\n" +
                "    ON c.CONTRACTID = t.cmn_contractid\n" +
                "      AND c.MIC = t.CMN_INSTRKEY_MIC\n" +
                "      AND t.CMN_EVTTM_EVTDATE\n" +
                "        BETWEEN c.MNGSD_VALIDITYFROM\n" +
                "        AND c.MNGSD_VALIDITYTO - 1\n" +
                "      AND c.MNG_DEL = 0\n" +
                "      AND c.MNGSD_DELETED = 0\n" +
                "  INNER JOIN FT_C_SECURITY ft\n" +
                "    ON c.CONTRACTID = ft.FTPRODUCTID AND c.MIC = ft.MARKETID AND ft.DEL = 0\n" +
                "  INNER JOIN MDB_MARKET m\n" +
                "    ON m.MIC = t.cmn_instrkey_mic\n" +
                "      AND m.mic\n" +
                "        IN ( SELECT mic\n" +
                "          FROM mdb_market m2\n" +
                "          WHERE t.CMN_EVTTM_EVTDATE\n" +
                "              BETWEEN m2.MNGSD_VALIDITYFROM\n" +
                "              AND m2.MNGSD_VALIDITYTO - 1\n" +
                "            AND m2.MktType = ?\n" +
                "            AND EXISTS ( SELECT *\n" +
                "              FROM mdb_trade_event t2\n" +
                "              WHERE t2.TRADETYPE = 1\n" +
                "                AND t2.CMN_TRADINGCAPACITY = 0\n" +
                "                AND t.cmn_flowid = t2.cmn_flowid\n" +
                "                AND t.cmn_floweventkey = t2.cmn_floweventkey )\n" +
                "            AND m2.LEI\n" +
                "              IN ( SELECT LEI\n" +
                "                FROM MDB_ENTITY e\n" +
                "                WHERE e.EntityID IN ( ?, ? )\n" +
                "                  AND e.mngsd_validityfrom <= ?\n" +
                "                  AND e.mngsd_validityto > ? )\n" +
                "            AND segmentMic = ? )\n" +
                "      AND t.CMN_EVTTM_EVTDATE\n" +
                "        BETWEEN m.MNGSD_VALIDITYFROM\n" +
                "        AND m.MNGSD_VALIDITYTO - 1\n" +
                "      AND m.MNGSD_DELETED = 0\n" +
                "      AND m.MNG_DEL = 0\n" +
                "  INNER JOIN MDB_CONTRACT_MIFID2 contr\n" +
                "    ON c.CONTRACTID = contr.CONTRACTID\n" +
                "      AND contr.MNG_DEL = 0\n" +
                "      AND contr.MNGSD_DELETED = 0\n" +
                "      AND t.CMN_EVTTM_EVTDATE\n" +
                "        BETWEEN contr.MNGSD_VALIDITYFROM\n" +
                "        AND contr.MNGSD_VALIDITYTO - 1\n" +
                "  LEFT JOIN MDB_CONTRACT_MIFID2 contr2\n" +
                "    ON contr2.ISINCODE = contr.UNDERLYINGISINCODE\n" +
                "      AND c.MNG_DEL = 0\n" +
                "      AND c.MNGSD_DELETED = 0\n" +
                "      AND t.CMN_EVTTM_EVTDATE\n" +
                "        BETWEEN contr2.MNGSD_VALIDITYFROM\n" +
                "        AND contr2.MNGSD_VALIDITYTO - 1\n" +
                "WHERE t.CMN_FIRMID IN ( ?, ? )\n" +
                "  AND t.MNG_DEL = 0\n" +
                "  AND t.CMN_CLIENTID <> 'Test'\n" +
                "  AND t.CMN_EVTTM_EVTDATE BETWEEN ? AND ?\n" +
                "ORDER BY t.CMN_CONTRACTID, t.CMN_EVTTM_EVTDATE DESC NULLS LAST");
    }

    private static void assertFormatEquals(int lineWidth, int indentWidth, String sql) {
        String formatted2 = StatementLayout2.format(lineWidth, indentWidth, sql);
        if (!sql.equals(formatted2)) {
            System.out.println(TreePrint.print(StatementLayout2.toTree(sql)));
        }
        assertEquals(sql, formatted2);
    }
}
