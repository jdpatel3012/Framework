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

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.ibaset.common.context.SoluminaContextHolder;
import com.ibaset.common.context.ThreadContext;
import com.ibaset.common.security.context.SoluminaUser;
import com.ibaset.common.security.context.UserContext;
import com.ibaset.solumina.sfcore.application.ILogin;
import com.ibaset.solumina.sfcore.application.ITransactionManager;

public class LogoutUserListener implements HttpSessionListener
 {
	Logger logger = LoggerFactory.getLogger(LogoutUserListener.class);

	public void sessionCreated(HttpSessionEvent sessionEvent) {

		logger.debug("Created User Session ID : " + sessionEvent.getSession().getId());
	}

	public void sessionDestroyed(HttpSessionEvent sessionEvent) {

		String sessionId = sessionEvent.getSession().getId();
		ThreadContext.getInstance().setSessionId(sessionId);

		WebApplicationContext ctx = WebApplicationContextUtils
				.getWebApplicationContext(sessionEvent.getSession().getServletContext());

		logger.debug("Expiring User Session ID : " + sessionId);

		SoluminaUser user = null;

		// j2ee 1.4 and up
		HttpSession session = sessionEvent.getSession();
		if (session != null) {
			SecurityContextImpl sci = (SecurityContextImpl) session
					.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);

			if (sci != null) {
				Authentication auth = sci.getAuthentication();

				if (auth != null) {
					user = (SoluminaUser) auth.getPrincipal();
				}
			}
		}

		if (user != null) {
			UserContext userContext = SoluminaContextHolder.getUserContext();
			try {
				// sessionEvent.
				ITransactionManager transactionManager = (ITransactionManager) ctx .getBean("soluminaTransactionManager");
				transactionManager.rollback(user, userContext);

			} catch (Throwable t) {
				logger.warn("Error rollingBack Connection in users session", t);
			}

			try {
				String connectionId = userContext.getConnectionId();
				if (connectionId != null) {
					userContext.setConnectionId(null);
					logger.debug("Logging out connection id " + connectionId);
					ILogin flagSetup = (ILogin) ctx.getBean("login");
					flagSetup.logout(user.getUsername(), connectionId);
				}
			} catch (Throwable t) {
				logger.warn("Error Logging User out", t);
			} 
		}
		
		logger.debug("Removing SoluminaContexts for sessionId : " + sessionId);
 		SoluminaContextHolder.cleanupSoluminaContexts(sessionId);
 		ThreadContext.getInstance().clear();
	}
}
