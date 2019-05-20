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
package com.ibaset.solumina.sfcore.dao.impl;

import java.util.List;

import com.ibaset.common.dao.JdbcDaoSupport;
import com.ibaset.common.sql.ParameterHolder;
import com.ibaset.solumina.sfcore.dao.IConfigurationDao;

public class ConfigurationDaoImpl extends JdbcDaoSupport implements
														IConfigurationDao
{
	public List selectSqlLib()
	{
		String select = "SELECT SQL_ID, SQL_ID_DISPL ,UPDT_USERID,TIME_STAMP, " +
                         this.getSchemaPrefix()+"SFDB_NVL_NUMBER(READ_ONLY,0) AS READ_ONLY, "+
                         "DATASOURCE,STYPE,DESCRIPTION,SQL_TEXT "+
                         "FROM SFCORE_SQL_LIB ORDER BY SQL_ID";
		return this.queryForList(select);
	}

	public List selectSqlIDs()
	{
		String select = "SELECT SQL_ID, SQL_ID_DISPL FROM SFCORE_SQL_LIB  ORDER BY SQL_ID";
		return this.queryForList(select);
	}
	
	public List selectSqlIDDisplay()
	{
		String select = "SELECT SQL_ID_DISPL, DESCRIPTION from SFCORE_SQL_LIB ORDER BY SQL_ID_DISPL";
		return this.queryForList(select);
	}

	public List selectSqlID(String sqlID)
	{
		String select = "SELECT SQL_ID, SQL_ID_DISPL ,UPDT_USERID,TIME_STAMP, " +
		                 this.getSchemaPrefix()+" SFDB_NVL_NUMBER(READ_ONLY,0) AS READ_ONLY, "+
		                 " DATASOURCE,STYPE,DESCRIPTION,SQL_TEXT "+
		                 " FROM SFCORE_SQL_LIB WHERE upper(SQL_ID) = upper(?)";
		ParameterHolder params = new ParameterHolder();
		params.addParameter(sqlID);
		return this.queryForList(select, params);
	}

	public List selectIniIDs()
	{
		String select = "SELECT INI_ID FROM SFCORE_INI_LIB ORDER BY INI_ID";
		return this.queryForList(select);
	}

	public String selectIniID(String iniID)
	{
		String select = "SELECT INI_TEXT FROM SFCORE_INI_LIB WHERE upper(INI_ID) = upper(?)";
		ParameterHolder params = new ParameterHolder();
		params.addParameter(iniID);
		return queryForString(select, params);
	}

	public List selectUdvIDs()
	{
		String select = "SELECT UDV_ID,UDV_TAG,UDV_DESC FROM SFCORE_UDV_LIB ORDER BY UDV_ID";
		return this.queryForList(select);
	}

	public String selectUdvID(String udvID)
	{
		String select = "SELECT UDV_ID, UDV_TAG,UPDT_USERID,TIME_STAMP,"+    
		                 "      UDV_TYPE, UDV_DESC,STATE,LOAD_REF,"+      
		                 "      TOOL_VERSION,OBJECT_REV,OWNER_GROUP,"+   
		                 "      UDV_DEFINITION "+
		                 " FROM SFCORE_UDV_LIB WHERE upper(UDV_ID) = upper(?)";
		ParameterHolder params = new ParameterHolder();
		params.addParameter(udvID);
		return queryForString(select, params);
	}



}

