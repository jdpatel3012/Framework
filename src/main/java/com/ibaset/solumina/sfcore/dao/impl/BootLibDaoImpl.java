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
import com.ibaset.solumina.sfcore.dao.IBootLibDao;

public class BootLibDaoImpl extends JdbcDaoSupport implements IBootLibDao {

	public String selectBootId(String bootID) {
        StringBuffer selectSql = new StringBuffer().append("SELECT ")
        	.append( " BOOT_TEXT FROM SFCORE_BOOT_LIB WHERE BOOT_ID=?  ");

        ParameterHolder parameters = new ParameterHolder();
        parameters.addParameter(bootID);

        return queryForString(selectSql.toString(), parameters);
	}
	public int deleteBootId(String bootID) 
	{
		StringBuffer deleteSql = new StringBuffer().append("  DELETE FROM ")
		   .append("    SFCORE_BOOT_LIB ")
		   .append(" WHERE ")
		   .append("     BOOT_ID = ? ");

		ParameterHolder parameters = new ParameterHolder();
		parameters.addParameter( bootID );

		return delete( deleteSql.toString(), parameters );
	}
	public void insertBootId(
			String bootID, 
			String description, 
			String bootText) 
	{
		String userName = ContextUtil.getUsername();
		StringBuffer insertSql = new StringBuffer()
		   .append(" INSERT INTO SFCORE_BOOT_LIB ( ")
		   .append("    BOOT_ID, ")
		   .append("    DESCRIPTION, ")
		   .append("    BOOT_TEXT, ")
		   .append("    UPDT_USERID, ")
		   .append("    TIME_STAMP, ")
		   .append("    LAST_ACTION ) ")
		   .append(" VALUES ( ")
		   .append("    ?, ")
		   .append("    ?, ")
		   .append("    ?, ")
		   .append("    ?, ")
		   .append("    " + getTimestampFunction() + ", ")
		   .append("    ? ) ");

		ParameterHolder parameters = new ParameterHolder();
		parameters.addParameter( bootID );
		parameters.addParameter( description );
		parameters.addParameter( bootText );
		parameters.addParameter( userName );
		parameters.addParameter( FrameworkConstants.INSERTED );


		insert(insertSql.toString(), parameters);
	}
	public int updateBootId(
			String bootID, 
			String description, 
			String bootText) 
	{
		String userName = ContextUtil.getUsername();
		StringBuffer updateSql = new StringBuffer().append(" UPDATE ")
		   .append("     SFCORE_BOOT_LIB ")
		   .append(" SET ")
		   .append("     DESCRIPTION = ?, ")
		   .append("     BOOT_TEXT = ?, ")
		   .append("     UPDT_USERID = ?, ")
		   .append("     TIME_STAMP = " + getTimestampFunction() + ", ")
		   .append("     LAST_ACTION = ? ")
		   .append(" WHERE ")
		   .append("     BOOT_ID = ? ");

		ParameterHolder parameters = new ParameterHolder();
		parameters.addParameter(description);
		parameters.addParameter(bootText);
		parameters.addParameter(userName);
		parameters.addParameter(FrameworkConstants.UPDATED);
		parameters.addParameter(bootID);
		
		return update(updateSql.toString(), parameters);
	}
	
	public boolean selectBootIdExists(String bootId)
	{
		boolean bootIdExists = false;

		StringBuilder selectSql = new StringBuilder().append("SELECT ? ")
													 .append(getDualTable())
													 .append("WHERE EXISTS ( ")
													 .append("    SELECT ? ")
													 .append("    FROM ")
													 .append("        SFCORE_BOOT_LIB ")
													 .append("    WHERE ")
													 .append("        BOOT_ID = ? ) ");

		ParameterHolder parameters = new ParameterHolder();
		parameters.addParameter(FrameworkConstants.X);
		parameters.addParameter(FrameworkConstants.X);
		parameters.addParameter(bootId);

		List bootIdList = queryForList(selectSql.toString(), parameters);

		if (bootIdList.size() > 0)
		{
			bootIdExists = true;
		}
		
		return bootIdExists;
	}

}
