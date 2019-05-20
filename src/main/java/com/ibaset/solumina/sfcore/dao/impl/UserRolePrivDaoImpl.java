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

import static com.ibaset.common.FrameworkConstants.NO;
import static com.ibaset.common.FrameworkConstants.YES;
import static com.ibaset.common.util.SoluminaUtils.stringEquals;
import static org.apache.commons.lang.StringUtils.defaultIfEmpty;

import java.sql.CallableStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.collections.map.ListOrderedMap;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.CallableStatementCallback;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.object.StoredProcedure;

import com.ibaset.common.FrameworkConstants;
import com.ibaset.common.dao.JdbcDaoSupport;
import com.ibaset.common.security.context.ContextUtil;
import com.ibaset.common.sql.ParameterHolder;
import com.ibaset.solumina.sfcore.dao.IUserRolePrivDao;
import com.ibaset.solumina.sffnd.application.IParameters;

public class UserRolePrivDaoImpl extends JdbcDaoSupport	implements
																IUserRolePrivDao
{
	private IParameters globalParameters = null;

	public List selectPriviligeList(String userId)
	{
		StringBuffer query = new StringBuffer().append(" SELECT ? AS PRIVILEGE "
				+ getDualTable());
		ParameterHolder params = new ParameterHolder();
		params.addParameter(userId);

		return queryForList(query.toString(), params);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ibaset.solumina.sfcore.dao.IUserRolePrivDao#insertUser(java.lang.String,
	 *      java.lang.Number, java.util.Date, java.lang.String)
	 */
	public void insertUser(	String userId,
							Number passwordExpirationDays,
							Date expireAccountOnDate,
							String accountType)
	{

		CreateUserProcedure proc = new CreateUserProcedure(getDataSource());
		
		Map m = new HashMap();
		m.put("CREATED_BY", ContextUtil.getUsername());
		m.put("NEW_USER_NAME", userId);
		m.put("PASSWORD_EXIPRATION_DAYS", passwordExpirationDays);
		m.put("ACCOUNT_EXIPRATION_DATE", expireAccountOnDate);
		m.put("ACCOUNT_TYPE", accountType);
		proc.execute(m);


	}

	public void verifyUserPassword(String newPassword)
	{

		VerifyPasswordProcedure proc = new VerifyPasswordProcedure(getDataSource());
		
		Map m = new HashMap();
		m.put("NEW_PASSWORD", newPassword);
		proc.execute(m);


	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ibaset.solumina.sfcore.dao.IUserRolePrivDao#assignUserRole(java.lang.String,
	 *      java.lang.String, java.lang.String, java.lang.String,
	 *      java.lang.String, java.lang.String)
	 */
	public void assignUserRole(	String userId,
								String userToModify,
								String role,
								String type,
								String autoUserPrivs,
								String inDatabaseFlag)
	{
		final String userIdF = userId;
		final String userToModifyF = userToModify;
		final String roleF = role;
		final String typeF = type;
		final String autoUserPrivsF = defaultIfEmpty(	autoUserPrivs,
																	globalParameters.getAutomaticUserPrivilegesFlag());
		final String inDatabaseFlagF = defaultIfEmpty(	inDatabaseFlag,
																	globalParameters.getDatabaseUserFlag());

		this.getJdbcTemplate()
			.execute(	"{call SFCORE_USER_ASSIGNROLE(?, ?, ?, ?, ?, ?)}",
						new CallableStatementCallback()
						{

							public Object doInCallableStatement(CallableStatement cs)	throws SQLException,
																						DataAccessException
							{
								cs.setString(1, userIdF);
								cs.setString(2, userToModifyF);
								cs.setObject(3, roleF);
								cs.setObject(4, typeF);
								cs.setString(5, autoUserPrivsF);
								cs.setString(6, inDatabaseFlagF);
								cs.execute();
								return null;
							}
						});
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ibaset.solumina.sfcore.dao.IUserRolePrivDao#deleteUser(java.lang.String,
	 *      java.lang.String, java.lang.String)
	 */
	public void deleteUser(	String userId,
							String userToDelete,
							String inDatabaseFlag)
	{
		final String userIdF = userId;
		final String userToDeleteF = userToDelete;
		final String inDatabaseFlagF = inDatabaseFlag;

		this.getJdbcTemplate().execute(	"{call SFCORE_DROP_USER(?, ?, ?)}",
										new CallableStatementCallback()
										{

											public Object doInCallableStatement(CallableStatement cs)	throws SQLException,
																										DataAccessException
											{
												cs.setString(1, userIdF);
												cs.setString(2, userToDeleteF);
												cs.setObject(3, inDatabaseFlagF);
												cs.execute();
												return null;
											}
										});
	}
	
	public void setGlobalParameters(IParameters globalParameters)
	{
		this.globalParameters = globalParameters;
	}
	
	
	private class CreateUserProcedure extends StoredProcedure
	{
		public CreateUserProcedure(DataSource ds)
		{
			SqlParameter createdBy= new SqlParameter("CREATED_BY", Types.VARCHAR);
			SqlParameter newUserName = new SqlParameter("NEW_USER_NAME", Types.VARCHAR);
			SqlParameter passwordExpirationDays = new SqlParameter("PASSWORD_EXIPRATION_DAYS", Types.NUMERIC);
			SqlParameter accountExpirationDate= new SqlParameter("ACCOUNT_EXIPRATION_DATE", Types.DATE);
			SqlParameter accountType= new SqlParameter("ACCOUNT_TYPE", Types.VARCHAR);
			
						String SQL = "SFCORE_CREATE_USER";
			setSql(SQL);
			declareParameter(createdBy);
			declareParameter(newUserName);
			declareParameter(passwordExpirationDays);
			declareParameter(accountExpirationDate);
			declareParameter(accountType);
        	this.setJdbcTemplate(createJdbcTemplate(ds));
			this.compile();
		}



	}


	private class VerifyPasswordProcedure extends StoredProcedure
	{
		public VerifyPasswordProcedure(DataSource ds)
		{
			SqlParameter newPassword= new SqlParameter("NEW_PASSWORD", Types.VARCHAR);
			
			String SQL = "SFCORE_VERIFY_PWD";
			setSql(SQL);
			declareParameter(newPassword);
        	this.setJdbcTemplate(createJdbcTemplate(ds));
			this.compile();
		}



	}
	
	public void insertPasswordChangeHistory(String currentUserId, String userIdToChange)
	{
		
		
		StringBuffer sql = new StringBuffer().
		append(" INSERT INTO SFCORE_PASSWD_HIST ").
		append(" ( ").
		append(" HIST_ID, ").
		append(" HIST_TIME_STAMP, ").
		append(" HIST_USERID, ").
		append(" USERID, ").
		append(" PASSWORD, ").
		append(" TIME_STAMP ").
		append(" ) ").
		append(" SELECT ").
		append(" "+getSchemaPrefix()+"SFDB_GUID(), ").
		append(" "+getTimestampFunction()+", ").
		append(" ? , ").
		append(" USERID, ").
		append(" PASSWORD, ").
		append(" TIME_STAMP ").
		append(" FROM SFCORE_USER where USERID=? ");
		
		ParameterHolder params = new ParameterHolder();
		params.addParameter(currentUserId);
		params.addParameter(userIdToChange);
		
		update(sql.toString(), params);
		
		
	}

	public Map selectUserAccountInformation(String userId)
	{
		ParameterHolder params = new ParameterHolder();
		params.addParameter(userId);

		return queryForMap("select ACCOUNT_TYPE, ACCOUNT_STATUS,PASSWD_EXPIRE_DAYS from SFCORE_USER WHERE USERID=?", params);

	}
	
	public void updateUserPassword(String userIdToChange, String hashedPassword, String currentUserId, String accountStatus, Date newExpirationDate)
	{
		ParameterHolder params = new ParameterHolder();
		StringBuffer sql = new StringBuffer().
		append(" UPDATE SFCORE_USER ").
		append(" SET PASSWORD = ? ").
		append(" ,UPDT_USERID = ? ").
		append(" ,TIME_STAMP="+getTimestampFunction()+" ").
		append(" ,PASSWD_CHANGE_DATE = ? ").
		append(" ,ACCOUNT_STATUS = ? ").
		append(" WHERE USERID = ? ");
		
		params.addParameter(hashedPassword);
		params.addParameter(currentUserId);
		params.addParameter(newExpirationDate);
		params.addParameter(accountStatus);
		params.addParameter(userIdToChange);
		
		update(sql.toString(), params);
		
	}

    @Override
    public void unObsoleteUser(String userId) {

        String updateSQL = "UPDATE SFCORE_USER SET UPDT_USERID = ?, TIME_STAMP = " + getTimestampFunction() + ", ACCOUNT_STATUS = ? WHERE USERID = ? ";

        ParameterHolder params = new ParameterHolder();

        params.addParameter(ContextUtil.getUsername());
        params.addParameter(FrameworkConstants.OPEN);
        params.addParameter(userId);

        update(updateSQL, params);
    }
	
	public boolean selectShouldVerifyPassword()
	{
		String verifyValue= null;
		
		try
		{
		 verifyValue =queryForString("SELECT "+this.getSchemaPrefix()+"SFCORE_RUN_PASSWD_VERIFICATION() "+this.getDualTable());
		}
		catch(IncorrectResultSizeDataAccessException ex)
		{
			if(logger.isDebugEnabled()) logger.debug(" SFCORE_RUN_PASSWD_VERIFICATION RETURNED 0 ROWS ");
		}
		return stringEquals(YES, verifyValue);
	}

	//GE-6062
    /**@since 5070
     * Returns the value of the OBSOLETE_RECORD_FLAG if the userid is found. If
     * not found, this method returns a null
     * 
     * @param userId
     *            The user id for which to obtain the obsolete flag
     * @return The value of the flag as a String
     */
	public String getObsoleteFlag(String userId)
    {
        String returnValue = null;
        String selectSQL = " SELECT "+
                    	   " CASE "+
                    	   "       WHEN OBSOLETE_RECORD_FLAG IS NULL THEN ? "+
                    	   "       ELSE OBSOLETE_RECORD_FLAG "+
                    	   "  END AS OBSOLETE_RECORD_FLAG "+
                    	   " FROM SFFND_USER "+
                    	   " WHERE  UPPER ( USERID ) = UPPER ( ? ) ";
        ParameterHolder params = new ParameterHolder();
        params.addParameter(NO);
        params.addParameter(userId);

        List list = queryForList(selectSQL, params);
        Iterator iter = list.listIterator();
        if (iter.hasNext())
        {
            ListOrderedMap row = (ListOrderedMap) iter.next();
            returnValue = (String) row.get("OBSOLETE_RECORD_FLAG");
        }

        return returnValue;
    }
	
}

