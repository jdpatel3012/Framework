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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibaset.common.util.Parameters;
import com.ibaset.solumina.sfcore.application.IConfiguration;
import com.ibaset.solumina.sfcore.application.IMessage;
import com.ibaset.solumina.sfcore.dao.ICfgLibDao;
import com.ibaset.solumina.sfcore.dao.IConfigurationDao;

public class ConfigurationImpl implements IConfiguration {

    private Logger logger = LoggerFactory.getLogger(this.getClass());
	private IConfigurationDao configurationDao;
	private ICfgLibDao cfgLibDao;
	private Map<String, Parameters> moduleCache=Collections.synchronizedMap(new HashMap<String, Parameters>());
	private String cfgId = "DEFAULT";
	private IMessage message = null;
	public List getSqlLib()
	{		 
		return configurationDao.selectSqlLib();
	}	
	public List getSqlIDs() 
	{
			return configurationDao.selectSqlIDs();
	}
	
	public List getSqlIDDisplay()
	{
	   return configurationDao.selectSqlIDDisplay();
	}

	public List getSqlID(String sqlID) {
		
		return configurationDao.selectSqlID(sqlID);
	}
	
	public List getIniIDs() {
		return configurationDao.selectIniIDs();
	}

	public String getIniID(String iniID) {
		return configurationDao.selectIniID(iniID);
	}

	public List getUdvIDs() {
		return configurationDao.selectUdvIDs();
	}

	public String getUdvID(String udvID) {
		return configurationDao.selectUdvID(udvID);
	}
	
	private Parameters findParameters(String moduleName)
	{
		String key= cfgId+"."+moduleName;
		Parameters p=moduleCache.get(key);
		if(p==null)
		{
			try 
			{
				String text = cfgLibDao.selectCfgText(cfgId, moduleName);
				p = new Parameters(text);
			} 
			catch (Exception e) 
			{
				logger.error("Unable to load cfgId="+cfgId+" module="+moduleName, e);
				p = new Parameters();
			}
			moduleCache.put(key, p);
		}
		return p;
	}
	public List<String> getStringList(String moduleName, String path) 
	{
		return findParameters(moduleName).getStringList(path);
	}

	public String getStringValue(String moduleName, String path,
			String parameterName, String defaultValue) 
	{
		return findParameters(moduleName).getStringValue(path, parameterName, defaultValue);
	}
	
	public String getStringValue(String moduleName, String path, String defaultValue) 
	{
		return findParameters(moduleName).getStringValue(path, defaultValue);
	}
	public int deleteCfg(
			String cfgId, 
			String cfgModuleName) 
	{
		String key= cfgId+"."+cfgModuleName;
		moduleCache.remove(key);
		return cfgLibDao.deleteCfg(cfgId, cfgModuleName);
	}

	public void insertCfg(
			String cfgId, 
			String cfgModuleName,
			String description, 
			String cfgText) 
	{
	    
	    boolean moduleNameExists = cfgLibDao.selectGroupNameExists(cfgModuleName);
        if (!moduleNameExists)
        {
            message.raiseError("INVALIDMODULE", cfgModuleName);
        }
		cfgLibDao.insertCfg(cfgId, cfgModuleName, description, cfgText);
	}

	public int updateCfg(
			String cfgId, 
			String cfgModuleName,
			String description, 
			String cfgText) 
	{
		String key= cfgId+"."+cfgModuleName;
		moduleCache.remove(key);
		return cfgLibDao.updateCfg(cfgId, cfgModuleName, description, cfgText);
	}
	
	public void setConfigurationDao(IConfigurationDao configurationDao) 
	{
		this.configurationDao = configurationDao;
	}
	
	public void setCfgLibDao(ICfgLibDao cfgLibDao) 
	{
		this.cfgLibDao = cfgLibDao;
	}
	public void setMessage(IMessage message)
    {
        this.message = message;
    }
}
