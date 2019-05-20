/**
 * Proprietary and Confidential
 * Copyright 1995-2013 iBASEt, Inc.
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
package com.ibaset.web.servlet.solumina;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;

import com.ibaset.common.BinaryParameter;
import com.ibaset.common.security.ISoluminaCipherUtils;
import com.ibaset.common.security.context.ContextUtil;
import com.ibaset.common.security.context.UserContext;
import com.ibaset.common.sql.IPassThroughQuery;
import com.ibaset.common.sql.ResultObject;
import com.ibaset.common.sql.security.SQLSecurityManager;

public final class SqlRequest extends SoluminaRequest {
	
    private static SQLSecurityManager securityManager;
    private static IPassThroughQuery passThroughQuery;
    private static ISoluminaCipherUtils soluminaCipherUtils;
    
	public SqlRequest(Map<String, BinaryParameter> requestParameterMap,
			HttpServletRequest request, HttpServletResponse response) throws IOException {
		super(requestParameterMap, request, response);
	}
	
	public boolean execute() throws Exception{
		
        Map<String, String> levelOneSortKeys = getSortKeys("1:1:", requestParameterMap);
        Map<String, String> levelTwoSortKeys = getSortKeys("1:2:", requestParameterMap);
        BinaryParameter p = requestParameterMap.get(SQL);
        String charsetName = p.getCharset();
        
		BinaryParameter connectionId = requestParameterMap.get(SOLUMINA_CONNECTION_ID);
		boolean useStaticKey = (connectionId == null) ? true : StringUtils.isEmpty(connectionId.getString());
		String sql = soluminaCipherUtils.decryptSqlText(p.getString(), useStaticKey);
        
        if (logger.isDebugEnabled())
        {
        	UserContext ctx = ContextUtil.getUser().getContext();
            logger.debug("Sql request begin: sql=" + sql + 
            		", solumina_connection_id=" + ctx.getConnectionId() +
            		", locale=" + ctx.getLocale() +", sessionId="+sessionId);
        }
        ResultObject queryResult = prepareQuery(sql, levelOneSortKeys, levelTwoSortKeys);
        writeResultObject(queryResult, true, charsetName);
        return !queryResult.hasErrors();
    }
	
    /**
     * @param sql
     * @param sessionId
     * @param startRow
     * @param endRow
     * @param levelOneSortKeys
     * @param levelTwoSortKeys
     * @param bindSet
     * @param bindVariables
     * @return ResultObject
     */
    private ResultObject secureQuery(String sql,
                                      int startRow,
                                      int endRow,
                                      Map<String, String> levelOneSortKeys,
                                      Map<String, String> levelTwoSortKeys,
                                      String bindSet,
                                      Map<String, String> bindVariables)
    {
        ResultObject result = null;
        boolean flag = securityManager.isProtectQueries();
        try
        {
        	securityManager.setProtectQueries(true);        	
            result = passThroughQuery.executeQuery(sql,
                                                   startRow,
                                                   endRow,
                                                   levelOneSortKeys,
                                                   levelTwoSortKeys,
                                                   bindSet,
                                                   bindVariables);
        }
        catch (Exception e)
        {
        	logger.error("Error executing passthrough query", e);
        	result = new ResultObject();
        	result.addError(e);
        }
        finally
        {
        	securityManager.setProtectQueries(flag);        	
        }

        return result;
    }
    
    private ResultObject prepareQuery(
    					String sql, 
    					Map<String, String> levelOneSortKeys,
    					Map<String, String> levelTwoSortKeys) throws IOException
    {
    	String bindSet = getString(requestParameterMap, BIND_SET);
    	Map<String, String> bindVariables = new HashMap<String, String>();
        String startRowString = getString(requestParameterMap, START_ROW);
        String endRowString = getString(requestParameterMap, END_ROW);
        
        // Get the bind variables
        if (bindSet != null && bindSet.trim().length() > 0)
        {
            StringTokenizer tok = new StringTokenizer(bindSet, ",");

            while (tok.hasMoreTokens())
            {
            	String key = tok.nextToken();
            	bindVariables.put(key, getString(requestParameterMap, key));
            }
        }
        
        ResultObject queryResult = null;
        if (NumberUtils.isNumber(endRowString))
        {
            int startRow = Integer.parseInt(StringUtils.defaultIfEmpty(startRowString, "0"));
            int endRow = Integer.parseInt(endRowString);

            queryResult = secureQuery(sql,
                                       startRow,
                                       endRow,
                                       levelOneSortKeys,
                                       levelTwoSortKeys,
                                       bindSet,
                                       bindVariables);
        }
        else
        {
            queryResult = secureQuery(sql,
                                       -1,
                                       -1,
                                       levelOneSortKeys,
                                       levelTwoSortKeys,
                                       bindSet,
                                       bindVariables);
        }
        return queryResult;
    }
    
    private static Map<String, String> getSortKeys(String string, Map<String, BinaryParameter> request) throws IOException
    {

        Map<String, String> m = new HashMap<String, String>();
        Iterator<String> enumeration = request.keySet().iterator();
        while (enumeration.hasNext())
        {
            String paramname = enumeration.next();
            String paramVal = getString(request, paramname);

            int start = StringUtils.indexOf(paramname, string);

            if (start == 0)
            {
                paramname = StringUtils.substring(paramname, start
                        + string.length());
                m.put(paramname, paramVal);
            }
        }
        return m;
    }

	public static void setSecurityManager(SQLSecurityManager securityManager) {
		SqlRequest.securityManager = securityManager;
	}

	public static void setPassThroughQuery(IPassThroughQuery passThroughQuery) {
		SqlRequest.passThroughQuery = passThroughQuery;
	}

	public static void setSoluminaCipherUtils(ISoluminaCipherUtils soluminaCipherUtils) {
		SqlRequest.soluminaCipherUtils = soluminaCipherUtils;
	}
}
