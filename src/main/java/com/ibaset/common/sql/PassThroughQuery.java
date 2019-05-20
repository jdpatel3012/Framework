/**
 * Proprietary and Confidential
 * Copyright 1995-2010 iBASEt, Inc.
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
/*
 * Created on Mar 8, 2005
 */
package com.ibaset.common.sql;

import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.lang.StringUtils;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.support.JdbcUtils;

import com.ibaset.common.client.SoluminaServiceLocator;
import com.ibaset.common.dao.JdbcDaoSupport;
import com.ibaset.common.dao.SQLEvent;
import com.ibaset.common.security.context.ContextUtil;
import com.ibaset.common.sql.cache.QueryKey;
import com.ibaset.common.sql.security.SQLSecurityManager;
import com.ibaset.common.sql.security.UnauthorizedAccessException;
import com.ibaset.common.util.SoluminaUtils;
import com.ibaset.web.servlet.solumina.FileItemParameter;

/**
 * @author joes
 */
public class PassThroughQuery extends JdbcDaoSupport implements
													IPassThroughQuery
{
    
    private static final String VARBINARY = "VARBINARY";
    private static final String TIMESTAMP_NAME = "java.sql.Timestamp";
    private static final String CLOB_NAME = "java.sql.Clob";
    private final static String BLOB_NAME = "java.sql.Blob";
    private static final String STRING_NAME = "java.lang.String";
    private static final String SQL_TEXT = "SQL_TEXT";
    private static final String SQLLIB = "SQLLIB";
    private static final String SQL_ID = "TBD";

	private SQLSecurityManager securityManager = null;
	
	private final FileItemFactory fileItemFactory;

    private static String createShortText(String query)
    {
    	query = query.replaceAll("\\n\\r\\t", " ");
    	if(query.length() > 20) query = query.substring(0, 20)+"...";
    	return query;
    }
    
    public PassThroughQuery() 
    {
    	fileItemFactory = new DiskFileItemFactory();
	}

	public ResultObject executeQuery(String sql, 
    								 int startRow, 
    								 int endRow, 
    								 Map<String, String> levelOneSortKeys, 
    								 Map<String, String> levelTwoSortKeys)
    {
    	return this.executeQuery(sql, startRow, endRow, levelOneSortKeys, levelTwoSortKeys, null, null);
    }
    
	public ResultObject executeQuery(	String sql,
										int startRow,
										int endRow, 
										Map<String, String> levelOneSortKeys, 
										Map<String, String> levelTwoSortKeys,
										String bindSet,
										Map<String, String> bindVariables)
	{
		if(securityManager==null) securityManager = SoluminaServiceLocator.locateService(SQLSecurityManager.class);
		if(logger.isDebugEnabled())
		{
		    if(levelOneSortKeys!=null && levelOneSortKeys.size()>0) logger.debug("Level one"+levelOneSortKeys);
		    if(levelTwoSortKeys!=null && levelTwoSortKeys.size()>0) logger.debug("Level two"+levelTwoSortKeys);
		}
		if(logger.isDebugEnabled()) logger.debug("About to run query " + createShortText(sql));
		if(securityManager.isProtectQueries() && !securityManager.getQueryThreatAnalyzer().canExecuteQuery(sql))
		{
			ResultObject errorResult = new ResultObject();
			Exception error = new UnauthorizedAccessException("SQL query is not authorized: "+sql);
			errorResult.addError(error);
			logger.error("Unauthorized SQL query issued by user "+ContextUtil.getUsername()+": "+sql);
			return errorResult;
		}
		ParameterHolder parameters = null;
		if ((bindSet != null) && (bindSet.trim().length() > 0))
		{
			parameters = new ParameterHolder();
			StringTokenizer tok = new StringTokenizer(bindSet, ",");
            while (tok.hasMoreTokens())
            {
            	String key = tok.nextToken();
            	parameters.addParameter(bindVariables.get(key));
            }
		}
		SQLEvent event = fireBeforeEvent(SQLEvent.SELECT, sql, parameters);
		if(event!=null) sql = event.getQuery();
		ResultObject result = null;
		try{
			QueryKey key = null;
	    	QueryKey secureKey = null;
	    	QueryKey accessBitmapKey = null;
	    	String userHash = null;
	    	if(queryCache!=null && queryCache.isEnabled()){
	    		key = new QueryKey(sql, parameters, "ResultObject");
	    		StringBuffer sb=new StringBuffer();
	    		sb.append(startRow).append('-').append(endRow);
			    if(levelOneSortKeys!=null && levelOneSortKeys.size()>0) sb.append(levelOneSortKeys);
			    if(levelTwoSortKeys!=null && levelTwoSortKeys.size()>0) sb.append(levelTwoSortKeys);
			    key.setAdditionalKey(sb.toString());
		    	result = (ResultObject)queryCache.get(key);
		    	if(result !=null && event!=null) event.setCacheHit(true);
		    	if(result == null) //try to get secure result
		    	{
		    		if(securityManager.isEnabled())
		    		{
		    			userHash = securityManager.getCurrentUserSecurityHash(getDataSource());
		    			secureKey = new QueryKey(sql, parameters, "ResultObject");
		    			secureKey.setAdditionalKey(userHash+key.getAdditionalKey());
				    	result = (ResultObject)queryCache.get(secureKey);
				    	if(result !=null && event!=null) event.setCacheHit(true);
		    		}
		    	}
	    	}
			if(result == null)
			{
	    		if(secureKey!=null)
	    		{
	    			//try to find cached access bitmap
	    			accessBitmapKey = new QueryKey(sql, parameters,"AccessBitmap");
	    			accessBitmapKey.setAdditionalKey(userHash);
	    			BitSet accessBitmap = (BitSet)queryCache.get(accessBitmapKey);
	    			securityManager.setQueryAccessBitmap(accessBitmap);
	    		}
				result = doExecuteQuery(sql, startRow, endRow, levelOneSortKeys, levelTwoSortKeys, bindSet, bindVariables, parameters);
				if(result.canCache())
				{
					if(secureKey!=null && securityManager.isLastQueryProtected())
					{
						result.getHash(); // calculate hash just once if needed
						queryCache.put(secureKey, result);
						BitSet accessBitmap = securityManager.getQueryAccessBitmap();
						if(accessBitmapKey!=null && accessBitmap!=null)
						{
							queryCache.put(accessBitmapKey, accessBitmap);
							securityManager.setQueryAccessBitmap(null);
						}
					}
					else
					{
						if(key!=null) 
						{
							result.getHash(); // calculate hash just once if needed
							queryCache.put(key, result);
						}
					}
				}
			}
			if(event!=null) event.setFailure(result.hasErrors());
		} finally{
			fireAfterEvent(event);
		}
		if(logger.isDebugEnabled()) logger.debug("Done Running Query");
		return result.canCache() ? new ResultObject(result) : result;
	}
    
	private ResultObject doExecuteQuery(String sql, 
										int startRow, 
										int endRow, 
										Map<String, String> levelOneSortKeys, 
										Map<String, String> levelTwoSortKeys,
										String bindSet,
										Map<String, String> bindVariables,
										ParameterHolder parameters)
	{
		sql = fixPrefixes(sql);
		ResultObject resultObject = new ResultObject();
		resultObject.setCanCache(true);
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		boolean isMatchingEnabled = !levelOneSortKeys.isEmpty();
		try
		{
			conn = DataSourceUtils.getConnection(getDataSource());
			ps = conn.prepareStatement(	sql,
										java.sql.ResultSet.TYPE_SCROLL_INSENSITIVE,
										java.sql.ResultSet.CONCUR_READ_ONLY);
			if ((bindSet != null) && (bindSet.trim().length() > 0))
			{
				StringTokenizer tok = new StringTokenizer(bindSet, ",");
				int parameterIndex = 0;

                while (tok.hasMoreTokens())
                {
                	String key = tok.nextToken();
                	ps.setString(++parameterIndex, bindVariables.get(key));
                }
			}
			long beginTime = System.currentTimeMillis();
			ps.executeQuery();
			long endTime = System.currentTimeMillis();
    		try{
    			insertQueryTimeLog(SQLLIB, SQL_ID, sql, parameters, beginTime, endTime);
    		}
    		catch(Exception e){
    			logger.debug("insertQueryTimeLog failed : ", e);
    		}    			
			rs = getFirstResultSet(ps);			
			ResultSetMetaData data = rs.getMetaData();
			int numColumns = data.getColumnCount();
			int columnSize = 0;
			
			int levelOneSortKeyColumns[] = null;
            String levelOneSortKeyValues[] = null;
			int sortColumnsFound=0;
			if(isMatchingEnabled)
			{
			    levelOneSortKeyColumns = new int[levelOneSortKeys.size()];
			    levelOneSortKeyValues = new String[levelOneSortKeys.size()];
			}

			HashMap<String, String> columnTypes = new HashMap<String, String>();
			for (int i = 1; i < numColumns + 1; i++)
			{
				String columnName = data.getColumnName(i);
        
				String columnClassName = data.getColumnClassName(i);

                String columnTypeName = data.getColumnTypeName(i);
                String columnNameUpper = columnName.toUpperCase();
                String columnClassNameUpper = columnClassName.toUpperCase();

				if (columnClassNameUpper.indexOf("BLOB") > -1)
				{
					columnClassName = BLOB_NAME;
					
				}
				else if (columnClassNameUpper.indexOf("CLOB") > -1
						|| columnNameUpper.indexOf("INI_TEXT") > -1)
				{
					columnClassName = CLOB_NAME;
				}
				else if ( VARBINARY.equalsIgnoreCase(columnTypeName) || columnNameUpper.equals("INI_DATA_KEY"))
                {
                    columnClassName = BLOB_NAME;
                }
				columnSize = data.getColumnDisplaySize(i);
				if(columnSize == -1)
				{
					columnSize=4000;
				}
	            columnNameUpper = columnName.toUpperCase();
	            columnClassNameUpper = columnClassName.toUpperCase();
	            columnTypes.put(i+"CLASS", columnClassName);
                columnTypes.put(i+"NAME", columnName);
                columnTypes.put(i+"CLASS_UPPER", columnClassNameUpper);
                columnTypes.put(i+"NAME_UPPER", columnNameUpper);
                columnTypes.put(i+"TYPE", columnTypeName);
                columnTypes.put(i+"TYPE_UPPER", columnTypeName.toUpperCase());
                
				resultObject.addColumnHeader(new ColumnHeader(	i,
																columnName,
																columnClassName,
																columnSize));

	            if (levelOneSortKeys.containsKey(columnName))
	            {
	                levelOneSortKeyColumns[sortColumnsFound]=i;
	                levelOneSortKeyValues[sortColumnsFound++]=levelOneSortKeys.get(columnName);
	            }

			}
			if(isMatchingEnabled) isMatchingEnabled=levelOneSortKeys.size()==sortColumnsFound; //ignore levelOneSortKeys if no columns found in the result set
			int rownum = 1;
			int pageSize=(endRow - startRow) + 1;
			if (startRow > 1)
			{
				rs.absolute(startRow-1);
			}

			int matchCount=0;
			boolean continuePaging = rownum <= pageSize;
			Row fallbackRow = null;
			while (rs.next() && continuePaging)
			{
				Row row = new Row(rownum++);
				for (int i = 1; i < numColumns + 1; i++)
				{
					Object columnValue = null;
					String columnNameUpper = (String )columnTypes.get(i+"NAME_UPPER");
					String columnClass = (String)columnTypes.get(i+"CLASS");
					String columnTypeNameUpper = (String)columnTypes.get(i+"TYPE_UPPER");
					if (CLOB_NAME.equals(columnClass))
					{
						Clob clob = rs.getClob(i);
						if(clob != null && clob.length() > 0)
						{
							columnValue = clob.getSubString(1, (int) clob.length());
						}
					}
					else if(VARBINARY.equals(columnTypeNameUpper))
					{
						//VARBINARY (max size of 8000 bytes)
    					//this is needed for sqlserver
    					if (StringUtils.equalsIgnoreCase(columnNameUpper, "INI_DATA_KEY"))
                        {
    						String iniDataString = rs.getString(i);
    						
    						if(iniDataString !=null)
    						{
    							columnValue = iniDataString.getBytes();
    						}
    						else
    						{
    							columnValue = null;
    						}
                        }
    	                
    	                else
    	                {
    	                	columnValue = rs.getBytes(i);
    	                }
					}
					else if (StringUtils.equalsIgnoreCase("INI_DATA_KEY", columnNameUpper))
                    {
                        columnValue = rs.getBytes(i);
                    }
					else if (BLOB_NAME.equals(columnClass))
					{
						Blob blob = rs.getBlob(i);
						if(blob != null && blob.length() > 0)
						{
    						if(blob.length() > SoluminaUtils.getMaxMemoryRequestSize())
    						{
								FileItem item=fileItemFactory.createItem(columnNameUpper, null, false, null);
								SoluminaUtils.copyCompletely(blob.getBinaryStream(), item.getOutputStream());
    							columnValue = new FileItemParameter(item);
    							resultObject.setCanCache(false);
    						}
    						else
    						{
    							columnValue = blob.getBytes(1, (int) blob.length());
    						}
						}
					}
					else if (TIMESTAMP_NAME.equals(columnClass))
					{
						try
						{
							columnValue = rs.getTimestamp(i);
						}
						catch (IllegalArgumentException ie)
						{
							columnValue = rs.getObject(i);
						}
					}
					else if (STRING_NAME.equals(columnClass))
                    {
                        
						columnValue = rs.getString(i);   
						if (columnNameUpper.equals(SQL_TEXT))
						{
							if (columnValue != null)
							{
								columnValue = getDatabaseInformation().optimizeQuery(columnValue.toString());
							}
						}
                    }
					else
					{
						columnValue = rs.getObject(i);
					}
					
					row.addColumn(new Column(i, columnValue));

				}
				
                continuePaging = rownum <= pageSize;

                if(isMatchingEnabled)
                {
                    if(isMatching(row, levelOneSortKeyColumns, levelOneSortKeyValues))
    				{
                        matchCount++;
    				    row.setSelected(true);
    				}
    				if(matchCount==0 && !continuePaging)
    				{
    					if(fallbackRow == null) fallbackRow = row;
    					continuePaging = true;
    				}
    				else
    				{
    					row.index=resultObject.getRows().size()+1;
    				    resultObject.addRow(row);
    				}
                } else 
                    resultObject.addRow(row);
                
			}
			if(matchCount==0 && isMatchingEnabled)
			{
				if(fallbackRow != null){
					fallbackRow.index=resultObject.getRows().size()+1;
				    resultObject.addRow(fallbackRow);
				}
				if(resultObject.getRows().size() > 0)
				{
				    ((Row)resultObject.getRows().get(0)).setSelected(true);
			        if(logger.isDebugEnabled()) logger.debug("No rows were found for level one keys");
				}
			}
		}
		catch (Throwable t)
		{
			logger.error("Error in passThroughQuery", t);
			resultObject.addError(t);
		}
		finally
		{
			JdbcUtils.closeResultSet(rs);
			JdbcUtils.closeStatement(ps);
			DataSourceUtils.releaseConnection(conn, getDataSource());
		}
		return resultObject;
	}

	private boolean isMatching(Row row, int levelOneSortKeyColumns[],String levelOneSortKeyValues[])
    {
        int matchCount=0;
        for (int i = 0; i < levelOneSortKeyColumns.length; ++i)
        {
            Column c = (Column) row.getColumns().get(levelOneSortKeyColumns[i] - 1);
            Object val = c.getValue();
            if(val!=null)
            { 
                val=val.toString(); 
                if (levelOneSortKeyValues[i].equals(val))
                {
                    matchCount++;
                }
            }
        }
        return matchCount==levelOneSortKeyColumns.length;
    }

    private String fixPrefixes(String sql)
	{
		sql = StringUtils.replace(sql, " SFDB_VARCHAR_TO_DATE", " " + getSchemaPrefix()
				+ "SFDB_VARCHAR_TO_DATE");
		return sql;

	}

	private ResultSet getFirstResultSet(Statement stmt) throws SQLException
	{
		ResultSet rs = null;
		boolean hasMoreResults = true;
		while (hasMoreResults)
		{
			rs = stmt.getResultSet();

			if (rs != null)
			{
				break;
			}
			hasMoreResults = moveToNextResultSet(stmt);
		}
		return rs;
	}

	private boolean moveToNextResultSet(Statement stmt) throws SQLException
	{
		boolean moreResults;
		moreResults = !(((stmt.getMoreResults() == false) && (stmt.getUpdateCount() == -1)));
		return moreResults;
	}
}
