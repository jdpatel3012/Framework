/**
 * Proprietary and Confidential
 * Copyright 1995-2011 iBASEt, Inc.
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

import com.ibaset.common.FrameworkConstants;
import com.ibaset.common.dao.JdbcDaoSupport;
import com.ibaset.common.security.context.ContextUtil;
import com.ibaset.common.sql.ParameterHolder;
import com.ibaset.solumina.sfcore.dao.ICfgLibDao;

public class CfgLibDaoImpl extends JdbcDaoSupport implements ICfgLibDao {

	public int deleteAll(String cfgId) 
	{
		StringBuffer deleteSql = new StringBuffer().append("  DELETE FROM ")
		   .append("    SFCORE_CFG_LIB ")
		   .append(" WHERE ")
		   .append("     CFG_ID = ? ");

		ParameterHolder parameters = new ParameterHolder();
		parameters.addParameter( cfgId );

		return delete( deleteSql.toString(), parameters );
	}

	public int deleteCfg(
			String cfgId, 
			String cfgModuleName) 
	{
		StringBuffer deleteSql = new StringBuffer().append("  DELETE FROM ")
		   .append("    SFCORE_CFG_LIB ")
		   .append(" WHERE ")
		   .append("     CFG_ID = ? AND ")
		   .append("     STYPE = ? ");

		ParameterHolder parameters = new ParameterHolder();
		parameters.addParameter( cfgId );
		parameters.addParameter( cfgModuleName );

		return delete( deleteSql.toString(), parameters );
	}

	public void insertCfg(
			String cfgId, 
			String cfgModuleName,
			String description, 
			String cfgText) 
	{
		String userName = ContextUtil.getUsername();
		StringBuffer insertSql = new StringBuffer().append(" INSERT INTO SFCORE_CFG_LIB ( ")
		   .append("    CFG_ID, ")
		   .append("    STYPE, ")
		   .append("    DESCRIPTION, ")
		   .append("    CFG_TEXT, ")
		   .append("    UPDT_USERID, ")
		   .append("    TIME_STAMP, ")
		   .append("    LAST_ACTION ) ")
		   .append(" VALUES ( ")
		   .append("    ?, ")
		   .append("    ?, ")
		   .append("    ?, ")
		   .append("    ?, ")
		   .append("    ?, ")
		   .append("    " + getTimestampFunction() + ", ")
		   .append("    ? ) ");

		ParameterHolder parameters = new ParameterHolder();
		parameters.addParameter( cfgId );
		parameters.addParameter( cfgModuleName );
		parameters.addParameter( description );
		parameters.addParameter( cfgText );
		parameters.addParameter( userName );
		parameters.addParameter( FrameworkConstants.INSERTED );


		insert(insertSql.toString(), parameters);
	}

	public String selectCfgText(
			String cfgId, 
			String cfgModuleName) 
	{
        StringBuffer selectSql = new StringBuffer().
        append("SELECT CFG_TEXT ")
    	.append(" FROM SFCORE_CFG_LIB WHERE CFG_ID=? AND STYPE=? ");

        ParameterHolder parameters = new ParameterHolder();
        parameters.addParameter(cfgId);
        parameters.addParameter(cfgModuleName);

        return queryForString(selectSql.toString(), parameters);
	}

	public int updateCfg(
			String cfgId, 
			String cfgModuleName,
			String description, 
			String cfgText) 
	{
		String userName = ContextUtil.getUsername();
		StringBuffer updateSql = new StringBuffer().append(" UPDATE ")
		   .append("     SFCORE_CFG_LIB ")
		   .append(" SET ")
		   .append("     DESCRIPTION = ?, ")
		   .append("     CFG_TEXT = ?, ")
		   .append("     UPDT_USERID = ?, ")
		   .append("     TIME_STAMP = " + getTimestampFunction() + ", ")
		   .append("     LAST_ACTION = ? ")
		   .append(" WHERE ")
		   .append("     CFG_ID = ? AND STYPE=? ");

		ParameterHolder parameters = new ParameterHolder();
		parameters.addParameter(description);
		parameters.addParameter(cfgText);
		parameters.addParameter(userName);
		parameters.addParameter(FrameworkConstants.UPDATED);
		parameters.addParameter(cfgId);
		parameters.addParameter(cfgModuleName);
		
		return update(updateSql.toString(), parameters);
	}
    public boolean selectGroupNameExists(String groupName)
    {
        boolean groupNameExists = false;

        StringBuffer selectSql = new StringBuffer().append("SELECT ")
                                                   .append("  GROUP_NAME ")
                                                   .append("FROM ")
                                                   .append("    SFCORE_MODULES ")
                                                   .append("WHERE ")
                                                   .append("    GROUP_NAME = ? ");

        ParameterHolder parameters = new ParameterHolder();
        parameters.addParameter(groupName);
        

        List groupNameList = queryForList(selectSql.toString(), parameters);

        if (groupNameList.size() > 0)
        {
            groupNameExists = true;
        }

        return groupNameExists;
    }
}
