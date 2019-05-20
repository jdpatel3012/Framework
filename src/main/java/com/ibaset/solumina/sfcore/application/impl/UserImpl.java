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
package com.ibaset.solumina.sfcore.application.impl;

import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import static org.apache.commons.lang.StringUtils.*;
import static  com.ibaset.common.util.SoluminaUtils.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.dao.SaltSource;
import org.springframework.security.authentication.encoding.PasswordEncoder;

import static com.ibaset.common.FrameworkConstants.*;
import static com.ibaset.common.DbColumnNameConstants.*;
import com.ibaset.common.security.context.ContextUtil;
import com.ibaset.solumina.sfcore.application.IMessage;
import com.ibaset.solumina.sfcore.application.IUser;
import com.ibaset.solumina.sfcore.dao.IUserRolePrivDao;
import com.ibaset.solumina.sffnd.application.IParameters;

public class UserImpl implements IUser 
{
	private IUserRolePrivDao userRolePrivDao = null;
	
	private IParameters globalParameters = null; 

	private PasswordEncoder passwordEncoder = null;
	private IMessage message = null;
	
	private SaltSource saltSource = null;
	
	static final Logger logger = LoggerFactory.getLogger(UserImpl.class);
	
	public boolean hasPrivilege(String userId, String privilege)
	{
		String userName = ContextUtil.getUsername();
		if(stringEquals(userName, privilege))
		{
			privilege = userId;
		}
		return hasPrivilege(privilege);
	}
	
    public String getLoggedInUsername()
    {
        return ContextUtil.getUsername();
    }
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ibaset.solumina.sfcore.application.IUser#hasPrivilege(java.lang.String,
	 *      java.lang.String)
	 */
	public boolean hasPrivilege(String privilege)
	{
		return ContextUtil.getUser().hasPrivilege(privilege);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ibaset.solumina.sfcore.application.IUser#createUser(java.lang.String,
	 *      java.lang.Number, java.util.Date)
	 */
	public void createUser(	String userName,
							Number passwordExpirationDays,
							Date expireAccountOnDate)
	{

		userRolePrivDao.insertUser(	userName,
									passwordExpirationDays,
									expireAccountOnDate,
									DEFAULT_ACCOUNT_TYPE);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ibaset.solumina.sfcore.application.IUser#dropUser(java.lang.String,
	 *      java.lang.String, java.lang.String)
	 */
	public void dropUser(	String userId,
							String userToDelete,
							String inDatabaseFlag)
	{
		inDatabaseFlag = defaultIfEmpty(inDatabaseFlag, globalParameters.getDatabaseUserFlag());
		
		userRolePrivDao.deleteUser(userId, userToDelete, inDatabaseFlag);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ibaset.solumina.sfcore.application.IRole#assignUserRole(java.lang.String,
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
		autoUserPrivs = defaultIfEmpty(autoUserPrivs, globalParameters.getAutomaticUserPrivilegesFlag());
		inDatabaseFlag = defaultIfEmpty(inDatabaseFlag, globalParameters.getDatabaseUserFlag());
		
		userRolePrivDao.assignUserRole(	userId,
										userToModify,
										role,
										type,
										autoUserPrivs,
										inDatabaseFlag);
	}

	public IUserRolePrivDao getPrivilegeDao()
	{
		return userRolePrivDao;
	}

	public void setPrivilegeDao(IUserRolePrivDao privilegeDao)
	{
		this.userRolePrivDao = privilegeDao;
	}
	
	public void setGlobalParameters(IParameters globalParameters)
	{
		this.globalParameters = globalParameters;
	}

	public void changePassword(String userIdToChange, String oldPassword, String newPassword)
	{

		String currentUserId = ContextUtil.getUsername();
		boolean isUserDba = hasPrivilege(SYSADMIN_EDIT_PRIV);
		if(!stringEquals(userIdToChange, currentUserId))
		{
			if(!isUserDba)
			{
				message.raiseError("WRONG_USER", userIdToChange, currentUserId);
			}
		}
		if(!isBlank(newPassword))
		{
			verifyAndInsertUpdateUserPassword(userIdToChange, newPassword, currentUserId, isUserDba);
		}
	}

	//GE-6062
	protected void verifyAndInsertUpdateUserPassword(String userIdToChange, 
									  				 String newPassword, 
									  				 String currentUserId,
									  				 boolean isUserDba) 
	{
		Map userAccountInformation = userRolePrivDao.selectUserAccountInformation(userIdToChange);
		
		String accountStatus = (String)userAccountInformation.get(ACCOUNT_STATUS);
		Number passwordExpirationDays = (Number) userAccountInformation.get(PASSWD_EXPIRE_DAYS);
		
		boolean accountIsLocked = stringEquals(accountStatus, LOCKED_STATUS);
		if(accountIsLocked && !isUserDba)
		{
			message.raiseError("ACCOUNT_LOCKED_MUSTBE_DBA");
		}
		
		boolean verifyPassword = userRolePrivDao.selectShouldVerifyPassword();
		if(verifyPassword)
		{
			userRolePrivDao.verifyUserPassword(newPassword);
		}
		
		// GE-6062
		String obsoleteRecordFlag = userRolePrivDao.getObsoleteFlag(userIdToChange);

		if (stringEquals(accountStatus, EXPIRED) || 
				(equalsIgnoreCase(accountStatus, OBSOLETE_UPPERCASE) && equalsIgnoreCase(obsoleteRecordFlag, NO)))//GE-6062 
		{
			accountStatus = OPEN;
		}
		insertPwdChangeHistoryAndUpdateUserPassword(userIdToChange, newPassword, currentUserId, accountStatus, passwordExpirationDays);
	}

	//GE-6062
	protected void insertPwdChangeHistoryAndUpdateUserPassword(String userIdToChange, 
											   				   String newPassword, 
											   				   String currentUserId,
											   				   String accountStatus, 
											   				   Number passwordExpirationDays) 
	{
		Date newExpirationDate = null;
		if(passwordExpirationDays !=null)
		{
			int numberOfDays = passwordExpirationDays.intValue();
			Calendar cal = Calendar.getInstance();
			cal.add(Calendar.DAY_OF_YEAR, numberOfDays);	
			newExpirationDate = cal.getTime();
		}
		String hashedPassword = passwordEncoder.encodePassword(newPassword, saltSource);
		userRolePrivDao.insertPasswordChangeHistory(currentUserId, userIdToChange);
		userRolePrivDao.updateUserPassword(userIdToChange, hashedPassword, currentUserId ,accountStatus, newExpirationDate);
	}

	public PasswordEncoder getPasswordEncoder()
	{
		return passwordEncoder;
	}

	public void setPasswordEncoder(PasswordEncoder passwordEncoder)
	{
		this.passwordEncoder = passwordEncoder;
	}

	public IMessage getMessage()
	{
		return message;
	}

	public void setMessage(IMessage message)
	{
		this.message = message;
	}

	public SaltSource getSaltSource()
	{
		return saltSource;
	}

	public void setSaltSource(SaltSource saltSource)
	{
		this.saltSource = saltSource;
	}
	
	/*** Start Mahendra ***/
	public void createUser( String args0,
	                        String args1,
	                        String args2,
	                        String args3,
	                        String args4 )
	{
		// Stub Used.
	}
	/*** End Mahendra ***/

	public void loadContextData() 
	{
	}
	
	public String getDefaultPreference()
	{
		return null;
	}
	
}
