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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;

import junit.framework.TestCase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.web.context.support.XmlWebApplicationContext;

import com.ibaset.common.security.context.CustomInMemoryDaoImpl;
import com.ibaset.common.security.context.SoluminaUser;
import com.ibaset.solumina.sfcore.application.ILogin;
import com.ibaset.solumina.sffnd.application.IParameters;

public abstract class AbstractTransactionalTestCase extends TestCase 
{
    protected static final Logger logger = LoggerFactory.getLogger(AbstractTransactionalTestCase.class);
	private static XmlWebApplicationContext ctx;
	private PlatformTransactionManager tm;
	private TransactionStatus ts;
    private ILogin login = null;
    private IParameters parameters;
	
	public PlatformTransactionManager getTransactionManager() {
		return tm;
	}
	
	public TransactionStatus getTransactionStatus() {
		return ts;
	}
	
	public ApplicationContext getApplicationContext(){
		return ctx;
	}
    public String[] getConfigLocations()
    {
    	return SoluminaTestCase.getDefaultSpringConfigLocations();
    }
    
    protected void setupUser()
    {
		SoluminaUser user = new SoluminaUser(	"ANONYMOUS",
						"ANONYMOUS",
						true,
						true,
						true,
						true,
						CustomInMemoryDaoImpl.getGrantedAuthorityList());
		
		Collection<GrantedAuthority> authorities = user.getAuthorities();
		
		// Grant all roles to SFMFG
		TestingAuthenticationToken token = new TestingAuthenticationToken(	user,
													user.getPassword(),
													authorities == null ? null : new ArrayList<GrantedAuthority>(authorities));
		
		// Create and store the Acegi SecureContext into the ContextHolder.
		SecurityContextImpl secureContext = new SecurityContextImpl();
		
		secureContext.setAuthentication(token);
		SecurityContextHolder.setContext(secureContext);
		login.setUp(true);
		parameters.setParameter("CLIENT_OS_USER", System.getProperty("user.name", "JUNIT"));
		try 
		{
			parameters.setParameter("CLIENT_COMPUTER_NAME", InetAddress.getLocalHost().toString());
		} 
		catch (UnknownHostException e) 
		{
			parameters.setParameter("CLIENT_COMPUTER_NAME", "localhost");
		}
    }
    protected synchronized void setUp() throws Exception
    {
        super.setUp();
        if(ctx==null)
        {
        	long startTime=System.currentTimeMillis();
            System.out.println("Loading context...");
        	ctx = new XmlWebApplicationContext();
            ctx.setConfigLocations(getConfigLocations());
            ctx.refresh();
            System.out.println("Context loaded in "+ (System.currentTimeMillis()-startTime)+" ms");
        }
		tm=(PlatformTransactionManager) ctx.getBean("transactionManager");
		ts=tm.getTransaction(new DefaultTransactionDefinition());
		ts.setRollbackOnly();
		//System.out.println("Transaction begin - "+ts);
		login=(ILogin) ctx.getBean("login");
		parameters = (IParameters)ctx.getBean("parameters");
        setupUser();
    }


    protected void shutdownUser(){
		login.logout();
    }
	protected void tearDown() throws Exception {
		super.tearDown();
		if(ts!=null) {
            try {
                tm.rollback(ts);
            } catch (Exception e) {
                logger.debug(e.getMessage(), e);
            }
		}
		shutdownUser();
		//System.out.println("Current transaction rolled back");
		ts=null;
		tm=null;
	}
}
