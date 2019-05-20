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
package com.ibaset.solumina.sfcore.dao;

import java.util.Date;
import java.util.List;
import java.util.Map;

public interface IUserRolePrivDao
{
	/**
	 * Select a list of privileges for a user
	 * @deprecated use ILogin.getUserAppRolePrivs();
	 * @param userId
	 *            The user id related to a list of privileges
	 * @return A list of maps with key "PRIVILEGE" containing a string
	 *         representation of a user priv
	 */
	public List selectPriviligeList(String userId);

	/**
	 * Insert a user into the database
	 * 
	 * @param userId
	 *            The userid for this user
	 * @param password
	 *            The users password (unencrypted)
	 * @param tablespace
	 *            Optional, only used if an actual database user (IE oracle
	 *            user) is being created
	 * @param tempTablespace
	 *            Optional, only used if an actual database user (IE oracle
	 *            user) is being created
	 * @param forcePasswordChange
	 *            A flag Y/N if the user should change thier password upon first
	 *            login
	 * @param accountType
	 *            EXTERNAL, EXTERNAL2 if User is just external (identified
	 *            externally) but should not have ANONYMOUS role granted, use
	 *            EXTERNAL if User is just external (identified externally) but
	 *            should have ANONYMOUS role granted, use EXTERNAL2 else, use
	 *            blank/null
	 */
	public void insertUser(	String userId,
							Number passwordExpirationDays,
							Date expireAccountOnDate,
							String accountType);

	/**
	 * 
	 * @param userId
	 * @param userToModify
	 * @param role
	 * @param type
	 * @param autoUserPrivs
	 * @param inDatabaseFlag
	 */
	public void assignUserRole(	String userId,
								String userToModify,
								String role,
								String type,
								String autoUserPrivs,
								String inDatabaseFlag);
	
	/**
	 * 
	 * @param userId
	 * @param userToDelete
	 * @param inDatabaseFlag
	 */
	public void deleteUser(String userId,
	                       String userToDelete,
	                       String inDatabaseFlag);
	
	public Map selectUserAccountInformation(String userId);
	
	public void insertPasswordChangeHistory(String currentUserId, String userIdToChange);

	public void updateUserPassword(String userIdToChange, String hashedPassword, String currentUserId, String accountStatus, Date newExpirationDate);
	
	public void verifyUserPassword(String newPassword);

	public boolean selectShouldVerifyPassword();
	
	/**@since 5070
	 * 
	 * @param userId
	 */
	public String getObsoleteFlag(String userId);
	
	public void unObsoleteUser(String userId);
}
