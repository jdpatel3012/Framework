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
package com.ibaset.solumina.sfcore.application.impl;

import com.ibaset.common.Reference;
import com.ibaset.common.solumina.IValidator;
import com.ibaset.solumina.sfcore.application.IBootLib;
import com.ibaset.solumina.sfcore.application.IMessage;
import com.ibaset.solumina.sfcore.dao.IBootLibDao;

public class BootLibImpl implements IBootLib {

	private IBootLibDao bootLibDao;
	private IValidator validator = null;
	
	@Reference
	private IMessage message = null;
	
	public int deleteBootId(String bootID) 
	{
        // Checks the user has privilege or not
        validator.checkUserPrivilege("CONFIG_UPDATE");

		return bootLibDao.deleteBootId(bootID);
	}

	public void insertBootId(String bootID, String description, String bootText) 
	{
	    
        // Checks the user has privilege or not
        validator.checkUserPrivilege("CONFIG_UPDATE");
        
        // Check the Boot Id already exists.
        boolean bootIdExists = bootLibDao.selectBootIdExists(bootID);
        if(bootIdExists)
        {
        	// GE-5511
        	// INI BootId already Exists.
        	message.raiseError("MFI_E397156BD360468F96B6BEC29B803343");
        }

		bootLibDao.insertBootId(bootID, description, bootText);
	}

	public String selectBootId(String bootID) 
	{
		return bootLibDao.selectBootId(bootID);
	}

	public int updateBootId(String bootID, String description, String bootText) 
	{
        // Checks the user has privilege or not
        validator.checkUserPrivilege("CONFIG_UPDATE");
        
	    return bootLibDao.updateBootId(bootID, description, bootText);
	}

	public void setBootLibDao(IBootLibDao bootLibDao) 
	{
		this.bootLibDao = bootLibDao;
	}
    public void setValidator(IValidator validator)
    {
        this.validator = validator;
    }

}
