/**
 * Proprietary and Confidential
 * Copyright 1995-2015 iBASEt, Inc.
 * Unpublished-rights reserved under the Copyright Laws of the United States
 * US Government Procurements:
 * Commercial Software licensed with Restricted Rights.
 * Use, reproduction, or disclosure is subject to restrictions set forth in
 * license agreement and purchase contract.
 * iBASEt, Inc. 27442 Portola Parkway, Suite 300, Foothill Ranch, CA 92610
 *
 * Solumina software may be subject to United States Dept of Commerce Export Controls.
 * Contact iBASEt for specific Expert Control Classification information.
 */
package com.ibaset.common.sql;

import java.io.IOException;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class TestSQLStatement
{
    protected void verifySQLStatementForTableName(String sqlQuery, String expectedTableName) throws IOException
    {
        SQLStatement stat = SQLStatement.parseSQL(sqlQuery);
        assertEquals(expectedTableName, stat.table);
    }
    
    @Test
    public void testParseSQL_UpdateWithSubQuery() throws IOException
    {
        String sqlQuery = " UPDATE " +
                          "    SFSQA_INSP_ORDER_DESC " +
                          "  SET " +
                          "    DELIVERY_NO = ?, " +
                          "    SHIP_TO_ADDRESS = (SELECT " +
                          "                         DS.SHIP_TO_ADDRESS FROM SFSQA_PO_DELIVERY_SCHED DS   " +
                          "                       WHERE DS.PO_ID = SFSQA_INSP_ORDER_DESC.PO_ID  " +
                          "                         AND DS.PO_LINE_ITEM = SFSQA_INSP_ORDER_DESC.PO_LINE_ITEM  " +
                          "                         AND DS.DELIVERY_NO = ?   +,  " +
                          "    UPDT_USERID = ?, " +
                          "    TIME_STAMP = NULL, " +
                          "    LAST_ACTIVITY_TIME_STAMP = NULL, " +
                          "    LAST_ACTION = ? " +
                          "  WHERE INSP_ORDER_ID = ? ";
        
        verifySQLStatementForTableName(sqlQuery, "SFSQA_INSP_ORDER_DESC");
    }

    // Samples Queries - For Oracle
    @Test
    public void testParseSQL_Update() throws IOException
    {
        String sqlQuery = " UPDATE EMPLOYEES " +
                          "    SET COMMISSION_PCT = NULL " + 
                          "  WHERE JOB_ID = 'SH_CLERK'";
        
        verifySQLStatementForTableName(sqlQuery, "EMPLOYEES");
    }
    
    @Test
    public void testParseSQL_UpdateQuery_MixedCase() throws IOException
    {
        String sqlQuery = " UPDATE employees " +
                          "    SET commission_pct = NULL " + 
                          "  WHERE job_id = 'SH_CLERK'";

        verifySQLStatementForTableName(sqlQuery, "EMPLOYEES");
    }
    
    @Test
    public void testParseSQL_UpdateWithDifferentColumnDataJoin() throws IOException
    {
        String sqlQuery = " UPDATE EMPLOYEES " +
                          "    SET JOB_ID = 'SA_MAN', SALARY = SALARY + 1000, DEPARTMENT_ID = 120" +
                          "  WHERE FIRST_NAME||' '||LAST_NAME = 'DOUGLAS GRANT' ";

        verifySQLStatementForTableName(sqlQuery, "EMPLOYEES");
    }
    
    @Test
    public void testParseSQL_UpdateOnRemoteDatabase() throws IOException
    {
        String sqlQuery = "UPDATE EMPLOYEES@REMOTE  SET SALARY = SALARY*1.1 WHERE LAST_NAME = 'BAER'";
        verifySQLStatementForTableName(sqlQuery, "EMPLOYEES@REMOTE");
    }
    
    @Test
    public void testParseSQL_UpdateWithCoRelatedSubQuery() throws IOException
    {
        String sqlQuery = " UPDATE EMPLOYEES A " +
                         "     SET DEPARTMENT_ID = " +
                         "         (SELECT DEPARTMENT_ID " +
                         "             FROM DEPARTMENTS " +
                         "             WHERE LOCATION_ID = '2100'), " +
                         "         (SALARY, COMMISSION_PCT) = " +
                         "         (SELECT 1.1*AVG(SALARY), 1.5*AVG(COMMISSION_PCT) " +
                         "           FROM EMPLOYEES_SALARY B " +
                         "           WHERE A.DEPARTMENT_ID = B.DEPARTMENT_ID) " +
                         "     WHERE DEPARTMENT_ID IN " +
                         "         (SELECT DEPARTMENT_ID " +
                         "           FROM DEPARTMENTS " +
                         "           WHERE LOCATION_ID = 2900 " +
                         "               OR LOCATION_ID = 2700) ";
        
        verifySQLStatementForTableName(sqlQuery, "EMPLOYEES");
    }
    
    @Test
    public void testParseSQL_UpdateAnObjectTable() throws IOException
    {
        String sqlQuery = " UPDATE PEOPLE_DEMO1 P SET VALUE(P) = " + 
                          "    (SELECT VALUE(Q) FROM PEOPLE_DEMO2 Q " + 
                          "     WHERE P.DEPARTMENT_ID = Q.DEPARTMENT_ID) " + 
                          "    WHERE P.DEPARTMENT_ID = 10 ";
        
        verifySQLStatementForTableName(sqlQuery, "PEOPLE_DEMO1");
    }
    
    @Test
    public void testParseSQL_UpdateTableWithAlias() throws IOException
    {
        String sqlQuery = "UPDATE A SET COL = ? FROM TABLE A ";
        verifySQLStatementForTableName(sqlQuery, "TABLE");
    }   
    
    // Samples Queries - For SS
    @Test
    public void testParseSQL_SS_BasicUpdate() throws IOException
    {
        String sqlQuery = " UPDATE PERSON.ADDRESS SET MODIFIEDDATE = GETDATE() ";
        verifySQLStatementForTableName(sqlQuery, "PERSON.ADDRESS");
    }
    
    @Test
    public void testParseSQL_SS_MultipleColumn() throws IOException
    {
        String sqlQuery = " UPDATE Sales.SalesPerson " +
                          "    SET Bonus = 6000, CommissionPct = .10, SalesQuota = NULL ";
        verifySQLStatementForTableName(sqlQuery, "SALES.SALESPERSON");
    }
    
    @Test
    public void testParseSQL_SS_updateUsingTopClause() throws IOException
    {
        String sqlQuery = " UPDATE EMPLOYEE " + 
                          "    SET VACATION_HOURS = VACATION_HOURS + 8 " + 
                          "   FROM (SELECT TOP 10 BUSINESS_ENTITYID FROM EMPLOYEE_MASTER " + 
                          "          ORDER BY HIRE_DATE ASC) AS ALIAS " + 
                          "  WHERE EMPLOYEE.BUSINESS_ENTITYID = ALIAS.BUSINESS_ENTITYID ";
        verifySQLStatementForTableName(sqlQuery, "EMPLOYEE");
    }
    
    @Test
    public void testParseSQL_SS_updateWithSubQueryInSETClause() throws IOException
    {
        String sqlQuery = " UPDATE SALESPERSON " + 
                          " SET SALESYTD = SALESYTD + " + 
                          "     (SELECT SUM(SO.SUBTOTAL) " + 
                          "      FROM SALESORDERHEADER AS SO " + 
                          "      WHERE SO.ORDERDATE = (SELECT MAX(ORDERDATE) " + 
                          "                            FROM SALESORDERHEADER AS SO2 " + 
                          "                            WHERE SO2.SALES_PERSONID = SO.SALES_PERSONID) " + 
                          "      AND SALESPERSON.BUSINESS_ENTITYID = SO.SALES_PERSONID " + 
                          "      GROUP BY SO.SALES_PERSONID) ";
        
        verifySQLStatementForTableName(sqlQuery, "SALESPERSON");
    }
    
    @Test
    public void testParseSQL_SS_updateInvokeMethod() throws IOException
    {
        String sqlQuery = " UPDATE CITIES " +
                          "    SET LOCATION.SETXY(23.5, 23.5) " +
                          "  WHERE NAME = 'ANCHORAGE' ";
        
        verifySQLStatementForTableName(sqlQuery, "CITIES");
    }
    
    @Test
    public void testParseSQL_SS_updateWithSubQueryAndAlias() throws IOException
    {
        String sqlQuery = " UPDATE A " + 
                          "    SET A.MSG_TEXT = (SELECT 'TEST' FROM DUAL) " + 
                          "   FROM SFCORE_MESSAGES A " + 
                          "  WHERE ID IN (SELECT ID FROM XYZ WHERE TEXT LIKE '%Test%') ";
        
        verifySQLStatementForTableName(sqlQuery, "SFCORE_MESSAGES");
    }
    
    @Test
    public void testParseSQL_SS_updateUsingMultipleSubQueryAlias() throws IOException
    {
        String sqlQuery = " UPDATE EMPLOYEE " + 
                          "    SET VACATION_HOURS = VACATION_HOURS + 8 " + 
                          "   FROM (SELECT TOP 10 BUSINESS_ENTITYID FROM EMPLOYEE_MASTER " + 
                          "          ORDER BY HIRE_DATE ASC) AS ALIAS_1, " + 
                          "        (SELECT EMPLOYEE_ID FROM EMPLOYEE_INDIA ) AS ALIAS_2, " + 
                          "  WHERE EMPLOYEE.BUSINESS_ENTITYID = ALIAS_1.BUSINESS_ENTITYID " +
                          "    AND ALIAS_1.BUSINESS_ENTITYID = ALIAS_2.EMPLOYEE_ID ";
        
        verifySQLStatementForTableName(sqlQuery, "EMPLOYEE");
    }
    
    @Test
    public void testParseInsert_sqlStatementHavingAppendHint() throws IOException
    {
        String insertSql = "INSERT /*+ APPEND */ INTO SFSQA_INSP_ORDER_HOLDS( "
                            +"    HOLD_ID, "
                            +"    INSP_ORDER_ID, "
                            +"    INSP_STEP_ID, "
                            +"    INSP_ITEM_ID, "
                            +"    ITEM_ID, "
                            +"    HOLD_TYPE, "
                            +"    HOLD_STATUS, "
                            +"    STOP_TYPE, "
                            +"    SCHED_END_DATE, "
                            +"    DATE_CREATED, "
                            +"    NOTES, "
                            +"    DISC_ID, "
                            +"    DISC_LINE_NO, "
                            +"    RELATED_INSP_ORDER_ID, "
                            +"    SUPPLIER_CODE, "
                            +"    HOLD_REF1, " 
                            +"    UPDT_USERID, "
                            +"    TIME_STAMP, "
                            +"    LAST_ACTION ) "
                            +" WITH INSP_EXCLUDE AS "
                            +"  (SELECT X.INSP_ORDER_ID, "
                            +"         X.HOLD_ID, "
                            +"         X.HOLD_STATUS, "
                            +"         HOLD_TYPE "
                            +"     FROM  "
                            +"         SFSQA_INSP_ORDER_HOLDS X,  "
                            +"         SFSQA_INSP_ORDER_DESC Y  "
                            +"   WHERE "
                            +"          X.INSP_ORDER_ID = Y.INSP_ORDER_ID "
                            +"      AND X.HOLD_TYPE = ? "
                            +"      AND X.HOLD_STATUS = ? )  "
                            +" SELECT "
                            +"    SFMFG.SFDB_GUID, "
                            +"     INSP_ORDER_ID, "
                            +"    ?, "
                            +"    ?, "
                            +"     ITEM_ID, "
                            +"     ?, "
                            +"     ?, "
                            +"     ?, "
                            +"     ?, "
                            +"     ?, "
                            +"     ?, "
                            +"     ?, "
                            +"     ?, "
                            +"     ?, "
                            +"     ?, "
                            +"     ?, "
                            +"     ?, "
                            +"     ?, "
                            +"     ?  "
                            +" FROM "
                            +"     SFSQA_INSP_ORDER_DESC A "
                            +" WHERE "
                            +"     A.RESP_LOCATION_ID = ? AND " 
                            +"     EXISTS (SELECT "
                            +"                   ? "
                            +"               FROM "
                            +"                   SFSQA_INSP_ORDER_TYPE_DEF "
                            +"               WHERE "
                            +"                   INSP_ORDER_TYPE = A.INSP_ORDER_TYPE AND "
                            +"                   INSP_ORDER_TYPE NOT IN (?) AND "
                            +"               INSP_ORDER_CATEGORY = ?) AND "
                            +"     NOT EXISTS (SELECT "
                            +"                      ? "
                            +"                     FROM "
                            +"                         INSP_EXCLUDE E "
                            +"                    WHERE "
                            +"                         INSP_ORDER_ID = A.INSP_ORDER_ID ) "
                            +"     AND STATUS != ? "
                            +"     AND INSPECTION_TYPE != ? ";
        
        verifySQLStatementForTableName(insertSql, "SFSQA_INSP_ORDER_HOLDS");
    }
}
