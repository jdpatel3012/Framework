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
package com.ibaset.web.servlet.solumina;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServlet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.ibaset.common.context.ThreadContext;
import com.ibaset.common.solumina.IClusterMember;
import com.ibaset.common.solumina.ISoluminaCluster;
import com.ibaset.common.solumina.SoluminaLifecycleImpl;
import com.ibaset.solumina.sfcore.application.ILogin;
import com.ibaset.solumina.sfcore.application.IMTNode;
import com.ibaset.solumina.sfcore.dao.ILoginDao;

public class StartupLicenseCleanupServlet extends HttpServlet
{
	
	private Logger logger = LoggerFactory.getLogger(this.getClass());

	private static final long serialVersionUID = 1L;
	
	public void init(ServletConfig config)
	{
	    try {
    		boolean skipCleanup = false;
    		
    		ThreadContext.getInstance().setSessionId();
    		
    		WebApplicationContext ctx=WebApplicationContextUtils.getWebApplicationContext(config.getServletContext());
    		
    		ILoginDao loginDao = (ILoginDao) ctx.getBean("loginDao");
    		ISoluminaCluster cluster = (ISoluminaCluster)ctx.getBean("soluminaCluster");
    		if(!cluster.isStandalone())
    		{
    			IClusterMember localMember = cluster.getLocalMember();
    			IClusterMember[] members = cluster.getMembers();
    			for(IClusterMember m : members)
    			{
    				if(m.equals(localMember)) continue;
    				if(m.getAddress().equals(localMember.getAddress())) 
    				{
    					logger.debug("Found cluster node with same IP address. Skipping connection cleanup.");
    					skipCleanup = true;
    					break;
    				}
    			}
    		}
    		if(!skipCleanup)
    		{
    			// Remove dangling connections - run on coordinator only
    			if (cluster.isStandalone() || cluster.isCoordinator())
    			{
    				try
    				{
    					IMTNode mtNode = (IMTNode)ctx.getBean("mtNode");
    					mtNode.removeDanglingConnections();
    				}
    				catch(Exception e)
    				{
    					logger.error("Error removing dangling connections...", e);
    				}
    			}
    			
    			String ipAddr = "appserver";
    			try
    			{
    				InetAddress addr = InetAddress.getLocalHost();
    				ipAddr = addr.getHostAddress();
    			}
    			catch (UnknownHostException e1)
    			{
    				logger.error("Error getting appserver ip address", e1);
    			}
    			
    			List licenseResults = loginDao.selectLicensesToBeCleanedUp(ipAddr);
    	
    	
    			ILogin flagSetup = (ILogin) ctx.getBean("login");
    			
    			
    			
    			Iterator it = licenseResults.iterator();
    			while(it.hasNext())
    			{
    				Map row = (Map)it.next();
    				if(logger.isDebugEnabled()) logger.debug("Cleaning up connection for userid "+(String)row.get("USERID") +" and connection id: "+(String)row.get("CONNECTION_ID"));
    				flagSetup.logout((String)row.get("USERID"), (String)row.get("CONNECTION_ID"));
    				
    			}
    		}
    		SoluminaLifecycleImpl lifecycle = (SoluminaLifecycleImpl)ctx.getBean("soluminaLifecycle");
    		lifecycle.cleanupDone();
    		
	    }
	    finally {
	        ThreadContext.getInstance().clear();
	    }
	}
	


}
