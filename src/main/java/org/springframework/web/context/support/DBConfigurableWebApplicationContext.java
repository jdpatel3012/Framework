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
package org.springframework.web.context.support;

import java.net.URL;
import java.util.ArrayList;

import javax.sql.DataSource;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.MetaDataAccessException;

import com.ibaset.common.context.ThreadContext;
import com.ibaset.common.dao.JdbcDaoSupport;
import com.ibaset.common.solumina.SoluminaLifecycleImpl;

public class DBConfigurableWebApplicationContext extends
												XmlWebApplicationContext
{
	
	Logger logger = LoggerFactory.getLogger(this.getClass());
	
	public DBConfigurableWebApplicationContext() {
		registerShutdownHook();
	}

	@Override
	public void refresh() throws BeansException, IllegalStateException {
	    
	    try {
	        ThreadContext.getInstance().setSessionId();
	        super.refresh();
	    }finally {
	        ThreadContext.getInstance().clear();
	    }
	}
	
	@Override
	protected DefaultListableBeanFactory createBeanFactory() 
	{
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory(getInternalParentBeanFactory());
		bf.addBeanPostProcessor(new ReferencePostProcessor(bf));
		bf.setInstantiationStrategy(new ExtensibleInstantiationStrategy());
		return bf;
	}

	@Override
	protected void finishRefresh() 
	{
		super.finishRefresh();
		SoluminaLifecycleImpl lifecycle = (SoluminaLifecycleImpl)getBean("soluminaLifecycle");
		lifecycle.contextDone(this);
	}

	public String[] getConfigLocations()
	{
		String[] defaultSpringContextList = super.getConfigLocations();
		ArrayList newConfigLocations = new ArrayList();
		CollectionUtils.addAll(newConfigLocations, defaultSpringContextList);
		String webContextFileName = defaultSpringContextList[0];

		String[] webContextActiveProfiles = getEnvironment().getActiveProfiles();
		logger.info("Activated spring profiles: " + String.join(",", webContextActiveProfiles));
		
		ClassPathXmlApplicationContext tempContext = new ClassPathXmlApplicationContext(new String[] {webContextFileName}, false, null);
        tempContext.getEnvironment().setActiveProfiles(webContextActiveProfiles);
        tempContext.refresh();
        
		DataSource ds = (DataSource) tempContext.getBean("unprotectedDataSource");
		
		String productName="";
		try
		{
			productName = (String) JdbcUtils.extractDatabaseMetaData(ds, "getDatabaseProductName");
		}
		catch (MetaDataAccessException e)
		{
			logger.error("Can't extract database product name", e);
		}
		
		String[] productNameWords = StringUtils.split(productName);
		productName="";
		for(int i=0;i<productNameWords.length;i++)
		{
			if(i>0)
			{
				productName+=StringUtils.capitalize(productNameWords[i]);
			}
			else
			{
				productName +=StringUtils.uncapitalize(productNameWords[i]);
			}
		}
		
		String databaseName = productName;
		productName ="classpath*:"+databaseName+"DaoContext.xml";
		boolean loadDao = false;
		URL url = this.getClass().getClassLoader().getResource(databaseName+"DaoContext.xml");
		if(url!=null) loadDao = true;
		else if(logger.isDebugEnabled()) logger.debug("dao not found");
		String frameworkProductName = "classpath:"+databaseName+"FrameworkContext.xml";
		if(logger.isDebugEnabled()) logger.debug("loading dao context config for "+databaseName);
		
		if(loadDao)
		{
			newConfigLocations.add(productName);
		}
		
		newConfigLocations.add(frameworkProductName);

		loadBIS(newConfigLocations, ds, databaseName);
		
		loadLTA(newConfigLocations, ds, databaseName);
		
		loadSAMLContext(newConfigLocations);
		
        newConfigLocations.add("classpath*:/soluminaComponentContext.xml");
        newConfigLocations.add("classpath*:/soluminaExtensionContext.xml");
        newConfigLocations.add("classpath*:/"+databaseName+"SoluminaExtensionDaoContext.xml");
		return (String[]) newConfigLocations.toArray(new String[newConfigLocations.size()]);
	}

	private void loadBIS(ArrayList newConfigLocations,
			DataSource publicDs, String databaseName) {
		boolean loadIntegration = false;
		try
		{
			JdbcDaoSupport jdbcDaoSupport = new JdbcDaoSupport();
			jdbcDaoSupport.setDataSource(publicDs);
			jdbcDaoSupport.getJdbcTemplate();
			
			String query = "select SFMFG.SFCORE_IS_XML_ENABLED() from DUAL";
		
			String xmlEnabled = (String)jdbcDaoSupport.getJdbcTemplate().queryForObject(query, String.class);
								
			String bisServiceQuery = "select COUNT(SERVICE_NAME) from SFMFG.SFBIS_SERVICE_DEF where (ENABLED_FLAG = 'Y' or REPLY_ENABLED_FLAG = 'Y')";
			
			int enabledBisCount = jdbcDaoSupport.getJdbcTemplate().queryForObject(bisServiceQuery, Integer.class);
			
			if (logger.isDebugEnabled())
			{
				logger.debug("SFCORE_IS_XML_ENABELD? " + xmlEnabled);
			}
			
			if (logger.isDebugEnabled())
			{
				logger.debug("Enabled BIS Count: " + enabledBisCount);
			}
			
			if ("Y".equals(xmlEnabled) && enabledBisCount > 0)
			{
		        URL url = Thread.currentThread().getContextClassLoader().getResource("integrationContext.xml");
		        if(url!=null)loadIntegration = true;	
			}
		}
		catch(Exception e)
		{
			if(logger.isDebugEnabled())
			{ 
			    logger.debug("Error loading Integration Context", e);
			
			}
			loadIntegration = false;
		}
		  
		if (loadIntegration)
		{
			newConfigLocations.add("classpath:/integrationContext.xml");
			newConfigLocations.add("classpath*:/integrationExtensionContext.xml");
		}
	}

	private void loadLTA(ArrayList newConfigLocations, DataSource publicDs,
			String databaseName) {
		boolean loadLTA = false;
		try
		{
			JdbcDaoSupport jdbcDaoSupport = new JdbcDaoSupport();
			jdbcDaoSupport.setDataSource(publicDs);
			jdbcDaoSupport.getJdbcTemplate();

			String query = "select SFMFG.SFCORE_IS_LTA_ENABLED() from DUAL";
		
			String ltaEnabled = (String)jdbcDaoSupport.getJdbcTemplate().queryForObject(query, String.class);
								
			if (logger.isDebugEnabled())
			{
				logger.debug("SFCORE_IS_LTA_ENABELD? " + ltaEnabled);
			}
			
			if ("Y".equals(ltaEnabled))
			{
				URL url = Thread.currentThread().getContextClassLoader().getResource("ltaContext.xml");  	   
		        if(url!=null) loadLTA = true;	
			}
		}
		catch(Exception e)
		{
			if(logger.isDebugEnabled()) logger.debug("Error loading LTA Context", e);
			loadLTA = false;
		}
		  
		if (loadLTA)
		{
			newConfigLocations.add("classpath:/ltaContext.xml");
		}
		
		boolean loadLdap = false;
	    URL url = Thread.currentThread().getContextClassLoader().getResource("ldapContext.xml");         
	    if(url!=null) loadLdap = true; 
		
		if(loadLdap)
		{
	          newConfigLocations.add("classpath:/ldapContext.xml");
		}
		
	}
	
	/**
	 * Load SAML context based on samlsso properties in classpath
	 * @param newConfigLocations
	 */
	private void loadSAMLContext(ArrayList newConfigLocations){
		try{
			URL url = Thread.currentThread().getContextClassLoader().getResource("samlsso.properties");  	   
	        if(url!=null){
	        	newConfigLocations.add("classpath:/samlContextConfig.xml");
	        }
		}catch(Exception e){
			logger.warn(e.getMessage());
		}
	}
	
}
