/**
 * Proprietary and Confidential
 * Copyright 1995-2018 iBASEt, Inc.
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
package com.ibaset.common.concurrent;

import java.lang.reflect.InvocationTargetException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.quartz.MethodInvokingJobDetailFactoryBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;

import com.ibaset.common.client.SoluminaServiceLocator;
import com.ibaset.common.context.SoluminaContextHolder;
import com.ibaset.common.context.ThreadContext;
import com.ibaset.common.security.context.ContextUtil;
import com.ibaset.common.security.context.SoluminaJdbcDaoImpl;
import com.ibaset.common.security.context.SoluminaUser;
import com.ibaset.common.security.context.SoluminaUserDetailsLoader;
import com.ibaset.common.security.context.UserContext;
import com.ibaset.common.solumina.ISoluminaCluster;
import com.ibaset.solumina.sfcore.application.ILogin;

public class SuperuserMethodInvokingFactoryBean extends
		MethodInvokingJobDetailFactoryBean 
{
	private static final Logger logger = LoggerFactory.getLogger(SuperuserMethodInvokingFactoryBean.class);
	private boolean distributed;

	public SuperuserMethodInvokingFactoryBean() {}

	private static void setupContext()
	{
		SoluminaJdbcDaoImpl privQuery = (SoluminaJdbcDaoImpl)SoluminaServiceLocator.locateService(SoluminaJdbcDaoImpl.class);
		SoluminaUserDetailsLoader soluminaUserDetailsLoader=(SoluminaUserDetailsLoader)SoluminaServiceLocator.locateService(SoluminaUserDetailsLoader.class);
        // Get privs as SF$SUPERUSER role so can perform all functionality in Solumina
        GrantedAuthority[] privs = privQuery.selectSoluminaPrivsForRoles(new GrantedAuthority[] {new SimpleGrantedAuthority("SF$SUPERUSER")});
        SoluminaUser user=soluminaUserDetailsLoader.createSoluminaUser("SOLUMINA-IC", "SOLUMINA-IC", true, privs);
		
		UserContext ctx = SoluminaContextHolder.getUserContext();
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(user,
                                                            user.getPassword(),
                                                            user.getAuthorities());

        // Create and store the Acegi SecureContext into the ContextHolder.
        SecurityContextImpl secureContext = new SecurityContextImpl();
        secureContext.setAuthentication(token);
        SecurityContextHolder.setContext(secureContext);

        ILogin login = (ILogin)SoluminaServiceLocator.locateService(ILogin.class);
        login.setUp(true, ctx.getConnectionType(), ctx.getUserType());
        login.setSessionContext();
	}
	
	@Override
	public Object invoke() throws InvocationTargetException, IllegalAccessException 
	{
		
		ISoluminaCluster cluster = (ISoluminaCluster)SoluminaServiceLocator.locateService(ISoluminaCluster.class);
		//scheduled job should run only on coordinator node of the cluster
		if(distributed || cluster == null || cluster.isCoordinator())
		{
			initiateSession();
			setupContext();
			
			logger.info(String.format("Starting job %1$s method %2$s ",this.getTargetObject(),this.getTargetMethod())) ;
			
			try
			{
				Object result = super.invoke();
				return result;
			} 
			finally
			{
				shutdownContext();
				closeSession();
			}
		}
		else
		{
			logger.info(String.format("Job %2$s will only run on coordinator.  isCoordinator: %1$b",cluster.isCoordinator(),this.getTargetMethod()));
		}
		return null;
	}
	private static void shutdownContext()
	{
		SoluminaUser user=ContextUtil.getUser();
        String connId = SoluminaContextHolder.getUserContext().getConnectionId();
        if(connId!=null)
        {
            ILogin login = (ILogin)SoluminaServiceLocator.locateService(ILogin.class);
            try
            {
            	login.logout(user.getUsername(), connId);
            }
            catch (Exception e) 
            {
            	//ignore
            }
            SoluminaContextHolder.getUserContext().setConnectionId(null);
        }		
	}

    private static void initiateSession() {
        
        ThreadContext.getInstance().setSessionId();
    }

    private static void closeSession() {
        SoluminaContextHolder.cleanupCurrentSoluminaContexts();
        ThreadContext.getInstance().clear();
    }

	public void setDistributed(boolean distributed) 
	{
		this.distributed = distributed;
	}

}
