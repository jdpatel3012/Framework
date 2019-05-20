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
package com.ibaset.common;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.sql.DataSource;

import junit.framework.TestCase;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.lob.LobHandler;
import org.springframework.jdbc.support.nativejdbc.NativeJdbcExtractor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.test.AbstractTransactionalSpringContextTests;
import org.springframework.web.context.support.ExtensibleInstantiationStrategy;
import org.springframework.web.context.support.ReferencePostProcessor;

import com.ibaset.common.dao.JdbcDaoSupport;
import com.ibaset.common.security.context.ContextUtil;
import com.ibaset.common.security.context.CustomInMemoryDaoImpl;
import com.ibaset.common.security.context.SoluminaUser;
import com.ibaset.common.sql.DatabaseInformationHolder;
import com.ibaset.common.sql.ParameterHolder;
import com.ibaset.solumina.sfcore.application.ILogin;

/**
 * Base class for running DAO tests.
 */
public class SoluminaTestCase extends AbstractTransactionalSpringContextTests
{

	private static boolean setUp = false;

	protected static Logger logger = LoggerFactory.getLogger(SoluminaTestCase.class);

	private JdbcDaoSupport jdbcDaoSupport = null;

	private ILogin login;



	protected static String[] cxtList = null;


	public SoluminaTestCase() {
		super();
		setAutowireMode(AUTOWIRE_BY_NAME);
	}

	public static void assertEquals(Object expected, Object actual)
	{
		if (expected != null)
		{
			assertNotNull("Expected value was " + expected.toString() +
					" , but actual value was null", actual);
		}

		TestCase.assertEquals(expected, actual);

	}

	public static void assertEquals(String message,
									Object expected,
									Object actual)
	{
		if (expected != null)
		{
			assertNotNull("Expected value was " + expected.toString() +
					" , but actual value was null", actual);
		}

		TestCase.assertEquals(message, expected, actual);
	}

	public static void assertEquals(String message,
									String expected,
									String actual)
	{
		if (expected != null)
		{
			assertNotNull("Expected value was " + expected.toString() +
					" , but actual value was null", actual);
		}

		TestCase.assertEquals(message, expected, actual);
	}

	public static void assertEquals(String message, List expected, List actual)
	{
		assertEquals(expected, actual);
	}

	public static void assertEquals(List expected, List actual)
	{
		if (expected != null)
		{
			assertNotNull(actual);
		}

		if (expected.size() != actual.size())
		{
			fail(" Actual result was size " + actual.size() + ", expected " +
					expected.size());
		}

		Iterator expectedIt = expected.iterator();
		Iterator actualIt = actual.iterator();
		Object key = null;
		Map expectedMap = null;
		Map actualMap = null;
		while (expectedIt.hasNext())
		{
			expectedMap = (Map) expectedIt.next();
			actualMap = (Map) actualIt.next();
			Iterator mapIterator = expectedMap.keySet().iterator();
			while (mapIterator.hasNext())
			{
				key = mapIterator.next();
				assertEquals(	"Comparison failed for " +
										expectedMap.get(key) + " and " +
										actualMap.get(key),
								expectedMap.get(key),
								actualMap.get(key));

			}

		}
	}

	public static void assertEquals(Map expected, Map actual)
	{
		if (expected != null)
		{
			assertNotNull(actual);
		}

		if (expected.size() != actual.size())
		{
			fail(" Actual result was size " + actual.size() + ", expected " +
					expected.size());
		}

		Object key = null;
		Iterator mapIterator = expected.keySet().iterator();
		while (mapIterator.hasNext())
		{
			key = mapIterator.next();
			assertEquals(	"Comparison failed for " + expected.get(key) +
									" and " + actual.get(key),
							expected.get(key),
							actual.get(key));

		}

	}

	public static void assertEquals(String expected, String actual)
	{
		if (expected != null)
		{
			assertNotNull(actual);
		}
		TestCase.assertEquals(expected, actual);
	}

	private void setUpBeans() throws Exception
	{

		if (!setUp)
		{

	        String connId = null;
	        if (ContextUtil.getUser() != null &&
	                ContextUtil.getUser().getContext() != null)
	        {
	            connId = ContextUtil.getUser().getContext().getConnectionId();
	        }
	        SoluminaUser user = new SoluminaUser(   "SFMFG",
	                                                "SFMFG",
	                                                true,
	                                                true,
	                                                true,
	                                                true,
	                                                CustomInMemoryDaoImpl.getGrantedAuthorityList());
	        // Grant all roles to noddy.
	        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(user,
	                                                                                            user.getPassword(),
	                                                                                            user.getAuthorities());

	        // Create and store the Acegi SecureContext into the ContextHolder.
	        SecurityContextImpl secureContext = new SecurityContextImpl();
	        secureContext.setAuthentication(token);
	        SecurityContextHolder.setContext(secureContext);
	        ContextUtil.getUser().getContext().setConnectionId(connId);

            login.setUp(true);
            login.setSessionContext();
            
			setUp = true;
			

		}
		

	}

	protected String[] getConfigLocations()
	{

		if (SoluminaTestCase.cxtList == null)
		{
			SoluminaTestCase.cxtList = getSpringConfigLocations();
		}
		return SoluminaTestCase.cxtList;

	}

	public String[] getSpringConfigLocations()
	{

		if (logger.isDebugEnabled())
		{
			logger.debug("REALODING SPRING CONTEXT");
		}
		return getDefaultSpringConfigLocations();

	}
	public static String[] getDefaultSpringConfigLocations()
	{

		String dbName = getDatabaseName();

		String[] results =  new String[]
		{
				"classpath:/testContext.xml",
				"classpath:/sqlSecurityContext.xml",
				"classpath:/frameworkContext.xml",
				"classpath:/" + dbName + "FrameworkContext.xml",
				"classpath*:/businessContext.xml", 
				"classpath*:/daoContext.xml",
				"classpath*:/" + dbName + "DaoContext.xml",
				 "classpath*:/validationContext.xml"
		};
		
		try
		{
		    URL url=Thread.currentThread().getContextClassLoader().getResource("integrationContext.xml");
		    if(url!=null){
				List l = Arrays.asList(results);
				ArrayList l1 = new ArrayList(l);
				l1.add("classpath:/integrationContext.xml");
				results =(String[]) l1.toArray(new String[l1.size()]);
		    }
		}
		catch(Throwable t)
		{
		    logger.debug(" Integration Connector not in workspace", t);
		}
		
		List l = Arrays.asList(results);
		ArrayList l1 = new ArrayList(l);
		l1.add("classpath*:/soluminaComponentContext.xml");
		l1.add("classpath*:/soluminaExtensionContext.xml");
		results =(String[]) l1.toArray(new String[l1.size()]);
		return results;

	}

	@Override
	protected ConfigurableApplicationContext createApplicationContext(
			String[] locations) 
	{
		ConfigurableApplicationContext ctx= super.createApplicationContext(locations);
		DefaultListableBeanFactory bf=(DefaultListableBeanFactory)ctx.getBeanFactory();
		bf.addBeanPostProcessor(new ReferencePostProcessor(bf));
		bf.setInstantiationStrategy(new ExtensibleInstantiationStrategy());
		return ctx;
	}

	protected static String getDatabaseName()
	{
		Properties props = new Properties();

		InputStream fis = null;
		String dbName = "oracle";
		String url = System.getProperty("jdbc.url");
		String path = System.getProperty("user.dir") + "/build.properties";
		if(url==null) {
         		try
         		{
         			if (logger.isDebugEnabled())
         			{
         				logger.debug("loading " + path);
         			}
         			fis = new FileInputStream(path);
				props.load(fis);
				url = props.getProperty("jdbc.url");

         		}
         		catch (Exception fnf)
         		{
         		    logger.info(fnf.getMessage(), fnf);
         		}
			finally
			{
				if (fis != null)
				{
                    try {
                        fis.close();
                    } catch (IOException e) {
                        logger.debug(e.getMessage(), e);
                    }
				}
			}
		} 


		if(url!=null)
		{

			if (StringUtils.contains(url, "sqlserver"))
			{
				dbName = "microsoftSQLServer";
			}
			else
			{
				dbName = "oracle";
			}

			if (logger.isDebugEnabled())
			{
				logger.debug("Using database " + props.getProperty("jdbc.url"));
			}

		}
		return dbName;
	}

	protected void onSetUpInTransaction() throws Exception
	{
		setUpBeans();
		setUpTestCase();

	}

	/**
	 * override in subclass
	 */
	protected void setUpTestCase()
	{

	}

	public void setNativeJdbcExtractor(NativeJdbcExtractor extractor)
	{
		jdbcDaoSupport.getJdbcTemplate().setNativeJdbcExtractor(extractor);
	}

	/**
	 * @param query
	 */
	public void delete(String query)
	{
		jdbcDaoSupport.delete(query);
	}

	protected void addExpectedRow(	List results,
									Object[] columns,
									Object[] values)
	{
		HashMap m = new HashMap();

		for (int i = 0; i < columns.length; i++)

		{
			m.put(columns[i], values[i]);
		}
		results.add(m);

	}

	/**
	 * @param query
	 * @param params
	 */
	public void delete(String query, ParameterHolder params)
	{
		jdbcDaoSupport.delete(query, params);
	}

	/**
	 * @param query
	 * @param params
	 */
	public void insert(String query, ParameterHolder params)
	{
		jdbcDaoSupport.insert(query, params);
	}

	/**
	 * @param string
	 * @param params
	 * @return
	 */
	public int queryForInt(String string, ParameterHolder params)
	{
		return jdbcDaoSupport.queryForInt(string, params);
	}

	/**
	 * @param string
	 * @param params
	 * @return
	 */
	public int queryForInt(String string)
	{
		return jdbcDaoSupport.queryForInt(string);
	}

	/**
	 * @param string
	 * @return
	 */
	public List queryForList(String string)
	{
		return jdbcDaoSupport.queryForList(string);
	}

	public List queryForList(String string, boolean includeLobs)
	{
		return queryForList(string, null, includeLobs);
	}

	/**
	 * @param string
	 * @param params
	 * @return
	 */
	public List queryForList(String string, ParameterHolder params)
	{
		return jdbcDaoSupport.queryForList(string, params);
	}

	/**
	 * @param string
	 * @param class1
	 * @return
	 */
	public Object queryForObject(String string, Class class1)
	{
		return jdbcDaoSupport.queryForObject(string, class1);
	}

	/**
	 * @param string
	 * @param params
	 * @param class1
	 * @return
	 */
	public Object queryForObject(	String string,
									ParameterHolder params,
									Class class1)
	{
		return jdbcDaoSupport.queryForObject(string, params, class1);
	}

	/**
	 * @param string
	 * @return
	 */
	public String queryForString(String string)
	{
		return jdbcDaoSupport.queryForString(string);
	}

	/**
	 * @param string
	 * @param params
	 * @return
	 */
	public String queryForString(String string, ParameterHolder params)
	{
		return jdbcDaoSupport.queryForString(string, params);
	}

	/**
	 * @param string
	 * @param params
	 * @return
	 */
	public int update(String string, ParameterHolder params)
	{
		return jdbcDaoSupport.update(string, params);
	}

	public int update(String string)
	{
		return jdbcDaoSupport.update(string);
	}

	public JdbcTemplate getJdbcTemplate()
	{
		return this.jdbcDaoSupport.getJdbcTemplate();
	}

	public LobHandler getLobHandler()
	{
		return jdbcDaoSupport.getLobHandler();
	}

	public List queryForList(	String string,
								ParameterHolder params,
								boolean includeLobs)
	{
		return jdbcDaoSupport.queryForList(string, params, includeLobs);
	}

	public void setDataSource(DataSource dataSource)
	{

		this.jdbcDaoSupport = new JdbcDaoSupport();
		jdbcDaoSupport.setDatabaseInformation((DatabaseInformationHolder) applicationContext.getBean("databaseInformation"));
		jdbcDaoSupport.setJdbcTemplate(new JdbcTemplate(dataSource));

	}

	public void setLobHandler(LobHandler lobHandler)
	{
		jdbcDaoSupport.setLobHandler(lobHandler);
	}

	public String getDualTable()
	{
		return jdbcDaoSupport.getDualTable();
	}

	public String getTimestampFunction()
	{
		return jdbcDaoSupport.getTimestampFunction();
	}

	public void setLogin(ILogin flagSetup)
	{
		this.login = flagSetup;
	}

	public Map queryForMap(String string, ParameterHolder params)
	{
		return jdbcDaoSupport.queryForMap(string, params);
	}

	public Map queryForMap(String string)
	{
		return jdbcDaoSupport.queryForMap(string);
	}

	public String getSchemaPrefix()
	{
		return jdbcDaoSupport.getSchemaPrefix();
	}

}