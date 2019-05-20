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
package com.ibaset.common.solumina;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import com.ibaset.solumina.sfcore.application.IMTNode;
import com.ibaset.solumina.sffnd.version.dao.IIBAReleaseVersionDao;

public class SoluminaInfoImpl implements ISoluminaInfo, InitializingBean, ISoluminaLifecycleListener
{
	static final Logger logger = LoggerFactory.getLogger(SoluminaInfoImpl.class);

	private static String SOLUMINA_MIDDLE_TIER_CAPABILITIES_FILE="soluminaMTCapabilities.txt";
	private String soluminaMTCapabilities = null;
	private IMTNode mtNode;
	private String soluminaMTVersion;
	private String soluminaDBVersion;
	private IIBAReleaseVersionDao dbVersionDao;

	public String getSoluminaMTCapabilities() {
		return soluminaMTCapabilities;
	}


	public void setSoluminaMTCapabilities(String soluminaMTCapabilities) {
		this.soluminaMTCapabilities = soluminaMTCapabilities;
	}


	public String getCapabilities() 
	{
		return this.getSoluminaMTCapabilities();
	}

	public IMTNode getMtNode() {
		return mtNode;
	}

	public void setMtNode(IMTNode mtNode) {
		this.mtNode = mtNode;
	}

	public String getNodeId()
	{
		return mtNode.getNodeId();
	}
	
	public void afterPropertiesSet() throws Exception 
	{
		// Get the class loader
		ClassLoader classLoader=Thread.currentThread().getContextClassLoader();
		if(classLoader==null) classLoader = getClass().getClassLoader();
		if(classLoader==null) classLoader = ClassLoader.getSystemClassLoader();
		
		// Loading Solumina Middle Tier Capabilities from the resource file
		loadCapabilities(classLoader);
		loadSoluminaMTVersion();
		loadSoluminaDBVersion();
	}
	
	private void loadCapabilities(ClassLoader classLoader)
	{
		URL url=classLoader.getResource(SOLUMINA_MIDDLE_TIER_CAPABILITIES_FILE);
		if(url!=null)
		{
			logger.info("Loading Solumina Middle Tier Capabilities from "+url);
			try 
			{
				setSoluminaMTCapabilities(convertStreamToString(url.openStream()));
			} 
			catch (IOException e) 
			{
				e.printStackTrace();
			}
			//System.out.println(getSoluminaMTCapabilities());
		}
	}
	
	private String convertStreamToString(InputStream is)
	{
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		StringBuilder sb = new StringBuilder();

		String line = null;
		try 
		{
			while ((line = reader.readLine()) != null) 
			{
				sb.append(line + "\r\n");
			}
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		} 
		finally 
		{
			try 
			{
				reader.close();
				is.close();
			} 
			catch (IOException e) 
			{
				e.printStackTrace();
			}
		}
		return sb.toString();
	}
	
	private void loadSoluminaMTVersion() {
		this.soluminaMTVersion = StringUtils.substringBetween(soluminaMTCapabilities, "SOLUMINA_MT_VERSION=", "\r\n");
	}
	
	private void loadSoluminaDBVersion() {
		this.soluminaDBVersion = dbVersionDao.getIBAReleaseVersion();
	}
	
	public String getSoluminaMTVersion() {
		return soluminaMTVersion;
	}
	
	public String getSoluminaDBVersion() {
		return soluminaDBVersion;
	}

	public String getSoluminaInfo() {
		return getCapabilities() + "SOLUMINA_DB_VERSION=" + soluminaDBVersion + "\r\n";
	}

	@Override
	public void afterStartup() throws Exception 
	{
		mtNode.registerNode();
		mtNode.setContextInitialized(true);
	}

	@Override
	public void beforeShutdown() throws Exception 
	{
		mtNode.unregisterNode();
	}

	public void setDbVersionDao(IIBAReleaseVersionDao dbVersionDao) {
		this.dbVersionDao = dbVersionDao;
	}
}
