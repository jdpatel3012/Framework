/**
 * Proprietary and Confidential
 * Copyright 1995-2017 iBASEt, Inc.
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

import static org.apache.commons.lang.StringUtils.defaultIfEmpty;
import static org.apache.commons.lang.StringUtils.defaultString;
import static org.apache.commons.lang.StringUtils.split;
import static org.apache.commons.lang.math.NumberUtils.createNumber;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.collections.map.ListOrderedMap;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.support.SqlLobValue;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.object.StoredProcedure;

import com.ibaset.common.BinaryParameter;
import com.ibaset.common.dao.JdbcDaoSupport;
import com.ibaset.common.dao.SQLEvent;
import com.ibaset.solumina.sfcore.application.ILicenseInfo;
import com.ibaset.solumina.sfcore.application.IMessage;


public class PassThroughProc extends JdbcDaoSupport implements IPassThroughProc
{

	private List autocommitProcList = new ArrayList();
	private ILicenseInfo licenseInfo = null;
	private IMessage message = null;
	private final Map<String, ArrayList<SqlParameter>> cachedProcs = Collections.synchronizedMap(new HashMap<String, ArrayList<SqlParameter>>());
	private final Map<String, Boolean> cachedFunctions = Collections.synchronizedMap(new HashMap<String, Boolean>());

    public Map<String, Object> execute(String procedureName, Map<String, BinaryParameter> binaryParameters) {
        try {
            return doExecute(procedureName, binaryParameters);
        } catch (DuplicateKeyException e) {
            message.raiseError("ORA-00001");
            return null;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
	
	protected Map<String, Object> doExecute(String procedureName, Map<String, BinaryParameter> binaryParameters) throws Exception 
	{

		ArrayList<SqlParameter> parameterList = null;
		ListOrderedMap parameters = new ListOrderedMap();
		boolean isFunction = false;
		String[] splitNames = null;
		boolean isPackage = false;
		if (procedureName.indexOf(".") > -1)
		{
			splitNames = split(procedureName, ".", 2);
			procedureName = splitNames[1];
			isPackage = true;
		}
		procedureName = procedureName.toUpperCase();

		if (cachedProcs.get(procedureName) == null)
		{
			parameterList = new ArrayList<SqlParameter>();
			if(logger.isDebugEnabled()) logger.debug("Calling " + procedureName);

			if(logger.isDebugEnabled()) logger.debug("PARAMETERS " + binaryParameters);

			Connection conn = null;
			ResultSet rs = null;
			try
			{

				conn = DataSourceUtils.getConnection(this	.getJdbcTemplate()
															.getDataSource());

				if (isPackage)
				{
					if(logger.isDebugEnabled()) logger.debug("This is a package...setting catalog to "
							+ splitNames[0]);
					conn.setCatalog(splitNames[0]);
				}
				

				DatabaseMetaData dbmd = conn.getMetaData();
				rs = dbmd.getProcedureColumns(	conn.getCatalog(),
												null,
												procedureName,
												"%");
				if(logger.isDebugEnabled()) logger.debug("Got result set for " + procedureName);
				String parameterName = "RESULT_NOT_FOUND";

				Iterator<String> it = binaryParameters.keySet().iterator();
				if (it.hasNext())
				{
					parameterName = it.next();
				}
				while (rs.next())
				{

					if(logger.isDebugEnabled()) logger.debug("Processing result set");
					short dbColumnReturn = rs.getShort(5);
					int dbSqlType = rs.getInt(6);
					String typeName = rs.getString(7);
					String columnName = rs.getString(4);
					if(logger.isDebugEnabled()) logger.debug("switching on " + dbColumnReturn);
					
					BinaryParameter binaryParameter = binaryParameters.get(parameterName);
					
					if (dbColumnReturn == DatabaseMetaData.procedureColumnIn)
					{
						if (dbSqlType == Types.DECIMAL || dbSqlType == Types.BIGINT || dbSqlType == Types.NUMERIC)
						{
							parameterList.add(new SqlParameter(	parameterName, Types.NUMERIC));
							try
							{
								if (binaryParameter.getSize() != 0)
								{
									String value = binaryParameter.getString();
									if("null".equals(value))
									{
										parameters.put(parameterName, null);
									}
									else
									{
										parameters.put(	parameterName,
													createNumber(value));
									}
								}
								else
								{
									parameters.put(parameterName, null);
								}
							}
							catch (NumberFormatException nfe)
							{
								parameters.put(parameterName, null);
							}
						}
						else if (typeName.equals("CLOB") /**|| dbSqlType == Types.LONGVARCHAR**/)
						{
							parameterList.add(new SqlParameter(	parameterName, Types.CLOB));
							dbSqlType = Types.CLOB;
							if(binaryParameter.isInMemory())
							{
								String value = binaryParameter.getString();
								parameters.put(	parameterName,
												new SqlLobValue(defaultString(value),
																getLobHandler()));
							}
							else
							{
								parameters.put(	parameterName,
										new SqlLobValue(binaryParameter.getInputStream(),
														(int)binaryParameter.getSize(),
														getLobHandler()));
							}
						}
						else if (typeName.equals("BLOB") || dbSqlType == Types.LONGVARBINARY
								|| dbSqlType == Types.VARBINARY)
						{
							parameterList.add(new SqlParameter(	parameterName,
																Types.BLOB));
							if(binaryParameter.isInMemory())
							{
								parameters.put(parameterName,
										   new SqlLobValue(binaryParameter.getByteArray(),
										   getLobHandler()));
							}
							else
							{
								parameters.put(parameterName,
											   new SqlLobValue(binaryParameter.getInputStream(),
											   (int)binaryParameter.getSize(),
											   getLobHandler()));
							}
						}
						else if (typeName.equals("RAW"))
						{
							byte[] value = binaryParameter.getByteArray();
							parameters.put(	parameterName,
							               	value);
							parameterList.add(new SqlParameter(	parameterName,
																Types.BINARY));
						}
						else
						{
							parameterList.add(new SqlParameter(	parameterName,
																dbSqlType));
							final String val = defaultIfEmpty(binaryParameter.getString(), null);
							if("null".equals(val))
							{
								parameters.put(parameterName, null);
							}
							else
							{
								parameters.put(	parameterName,val);
							}
						}
						if(logger.isDebugEnabled()) logger.debug("new INPUT PARAMETER::"
								+ parameterName.toString() + " dbSqlType "
								+ dbSqlType);

						if (it.hasNext())
						{
							parameterName = it.next();
						}

					}
					else if (dbColumnReturn == DatabaseMetaData.procedureColumnOut
							|| dbColumnReturn == DatabaseMetaData.procedureColumnInOut
							|| dbColumnReturn == DatabaseMetaData.procedureColumnReturn)
					{

						if ("PL/SQL BOOLEAN".equals(typeName))
						{
							throw new RuntimeException("you cannot use a PL/SQL boolean as a return type in java");
						}
						if (dbColumnReturn == DatabaseMetaData.procedureColumnReturn)
						{
							isFunction = true;
							parameterList.add(new SqlOutParameter(columnName, dbSqlType));
						}
						else
						{
							parameterList.add(new SqlOutParameter(	parameterName.toString(),
																	dbSqlType));
	
							parameters.put(parameterName, null);
							if(logger.isDebugEnabled()) logger.debug("new OUTPUT OR IN/OUT PARAMETER::"
									+ parameterName);
							if (it.hasNext())
							{
								parameterName = it.next();
							}
						}
					}

				}
			}
			finally
			{
				try
				{
					rs.close();
				}
				catch (Exception e)
				{
					logger.error(e);
				}
				DataSourceUtils.releaseConnection(conn, getJdbcTemplate()	.getDataSource());
			}
			cachedProcs.put(procedureName, parameterList);
			cachedFunctions.put(procedureName, new Boolean(isFunction));
		}
		else
		{
			if(logger.isDebugEnabled()) logger.debug("GETTING CACHED...."+procedureName);
			parameterList = cachedProcs.get(procedureName);
			
			Iterator<SqlParameter> it = parameterList.iterator();
			while(it.hasNext())
			{
				final SqlParameter param = (SqlParameter) it.next();
				final int sqlType = param.getSqlType();
				final String parameterName = param.getName();
				BinaryParameter binaryParameter = binaryParameters.get(parameterName);
				if(binaryParameter == null)
				{
					parameters.put(parameterName, null);	
				}
				else if(sqlType == Types.BLOB || sqlType == Types.LONGVARBINARY
						|| sqlType == Types.VARBINARY)
				{
					if(binaryParameter.isInMemory())
					{
						parameters.put(parameterName,
								   new SqlLobValue(binaryParameter.getByteArray(),
								   getLobHandler()));
					}
					else
					{
						parameters.put(parameterName,
									   new SqlLobValue(binaryParameter.getInputStream(),
									   (int)binaryParameter.getSize(),
									   getLobHandler()));
					}
				}
				else if (sqlType == Types.CLOB)
				{
					if(binaryParameter.isInMemory())
					{
						String value = binaryParameter.getString();
						parameters.put(	parameterName,
										new SqlLobValue(defaultString(value),
														getLobHandler()));
					}
					else
					{
						parameters.put(	parameterName,
								new SqlLobValue(binaryParameter.getInputStream(),
												(int)binaryParameter.getSize(),
												getLobHandler()));
					}
				}
				else if (sqlType == Types.BINARY)
				{
					parameters.put(parameterName, binaryParameter.getByteArray());
				}
				else if(sqlType == Types.NUMERIC)
				{
					try
					{
						if (binaryParameter.getSize() != 0)
						{
							String value = binaryParameter.getString();
							if("null".equals(value))
							{
								parameters.put(parameterName, null);
							}
							else
							{
								parameters.put(	parameterName,
											createNumber(value));
							}
						}
						else
						{
							parameters.put(parameterName, null);
						}
					}
					catch (NumberFormatException nfe)
					{
						parameters.put(parameterName, null);
					}
				}
				else 
				{
					final String val = defaultIfEmpty(binaryParameter.getString(), null);
					if("null".equals(val))
					{
						parameters.put(parameterName, null);
					}
					else
					{
						parameters.put(	parameterName,val);
					}
				}
			}
			isFunction = ((Boolean) cachedFunctions.get(procedureName)).booleanValue();

		}

		if (isPackage)
		{
			procedureName = splitNames[0] + "." + splitNames[1];
		}
		
		if ("SFCORE_LICENSE_LOADER".equals(procedureName))
		{
			licenseInfo.clearInMemoryLicenseData();
		}
		
		SQLEvent event = fireBeforeEvent(SQLEvent.CALL, procedureName, (ParameterHolder)null);
		try{
			SqlParameter[] paramInfos=(SqlParameter[]) parameterList.toArray(new SqlParameter[parameterList.size()]);
			DynamicProc proc = new DynamicProc(	getDataSource(),
											paramInfos,
											procedureName,
											isFunction);
			//replace empty strings with nulls for MS SQL Server
			for(SqlParameter p:paramInfos)
			{
				int type = p.getSqlType();
				if(type==Types.VARCHAR || type==Types.CHAR)
				{
					String name = p.getName();
					Object value = parameters.get(name);
					if("".equals(value)) 
					{
						parameters.put(name, null);
					}
				}
			}
			Map result = proc.execute(parameters);
			if(event!=null) event.setFailure(false);
			return result;
		} 
		finally
		{
			fireAfterEvent(event);
		}

	}

	private class DynamicProc extends StoredProcedure
	{
		
		private String procName = null;
		public DynamicProc(	DataSource ds,
							SqlParameter[] params,
							String sql,
							boolean isFunction)
		{
			if(logger.isDebugEnabled()) logger.debug("IS FUNCTION " + isFunction);
			for (int i = 0; params != null && i < params.length; i++)
			{
				declareParameter(params[i]);
			}
			procName=sql;
			setDataSource(ds);
			setSql(sql);
			setFunction(isFunction);
			setLobHandler(getLobHandler());
			compile();
		}

		public Map execute(Map params)
		{
			boolean currentAutoCommit=false;
			if (autocommitProcList.contains(procName))
			{
			Connection conn = DataSourceUtils.getConnection(getDataSource());
			
			

				try
				{
					currentAutoCommit = conn.getAutoCommit();
					conn.setAutoCommit(true);
					DataSourceUtils.releaseConnection(conn, getDataSource());
				}
				catch (SQLException e)
				{
				}
			}
			try 
			{
			    return super.execute(params);
			} 
			finally 
			{
    			if (autocommitProcList.contains(procName))
    			{
    				Connection conn = DataSourceUtils.getConnection(getDataSource());
    			
    				try
    				{
    					conn.setAutoCommit(currentAutoCommit);
    					DataSourceUtils.releaseConnection(conn, getDataSource());
    				}
    				catch (SQLException e)
    				{
    				}
    			}
			}
		}
	}

	public List getAutocommitProcList()
	{
		return autocommitProcList;
	}

	public void setAutocommitProcList(List autocommitProcList)
	{
		this.autocommitProcList = autocommitProcList;
	}

	public ILicenseInfo getLicenseInfo() {
		return licenseInfo;
	}

	public void setLicenseInfo(ILicenseInfo licenseInfo) {
		this.licenseInfo = licenseInfo;
	}

	public void setMessage(IMessage message) {
		this.message = message;
	}

}
