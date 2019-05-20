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
package com.ibaset.common.security;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationDetailsSource;
//import org.springframework.security.authentication.AuthenticationDetailsSourceImpl;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;

/**
 * Filter processes external authentication result. Successfully authenticated username should be passed in com.ibaset.saea.user attribute.
 * */
public final class ExternalAuthenticationProcessingFilter implements Filter
{

    public static final String SAEA_USER="com.ibaset.saea.user";
    public static final String SAEA_PASSWORD="com.ibaset.saea.password";
    public static final String SAEA_ROLES="com.ibaset.saea.roles";
    private static final Logger logger = LoggerFactory.getLogger(ExternalAuthenticationProcessingFilter.class);

//    private AuthenticationDetailsSource authenticationDetailsSource = new AuthenticationDetailsSourceImpl();
    private AuthenticationDetailsSource authenticationDetailsSource = new WebAuthenticationDetailsSource();
    private AuthenticationManager authenticationManager;
    
    public void destroy()
    {
    }
    public void init(FilterConfig arg0) throws ServletException
    {
    }


    public void doFilter(ServletRequest request,
                         ServletResponse response,
                         FilterChain chain) throws IOException, ServletException
    {

        String username=(String)request.getAttribute(SAEA_USER);
        if(username!=null)
        {
            if(authenticationIsRequired(username))
            {
                ExternalAuthenticationToken authRequest=new ExternalAuthenticationToken(
                                                                    username,
                                                                    request.getAttribute(SAEA_PASSWORD),
                                                                    (String)request.getAttribute(SAEA_ROLES));
                authRequest.setDetails(authenticationDetailsSource.buildDetails((HttpServletRequest) request));
                Authentication authResult;

                try 
                {
                    authResult = authenticationManager.authenticate(authRequest);
                    SecurityContextHolder.getContext().setAuthentication(authResult);
                } 
                catch (AuthenticationException failed) 
                {
                    // Authentication failed
                    if (logger.isDebugEnabled()) {
                        logger.debug("Authentication request for user: " + username + " failed: " + failed.toString());
                    }

                    SecurityContextHolder.getContext().setAuthentication(null);
                }
            }
        }
        else
        {
            Authentication existingAuth = SecurityContextHolder.getContext().getAuthentication();
            if(existingAuth instanceof ExternalAuthenticationToken) 
            {
	            if (!(request instanceof HttpServletRequest)) 
	            {
	                throw new ServletException("Can only process HttpServletRequest");
	            }
	            HttpServletRequest httpRequest = (HttpServletRequest) request;
	            String header = httpRequest.getHeader("Authorization");
	            if(header!=null)
	            {
                	//remove external authentication if  request contains another authentication request
                    SecurityContextHolder.getContext().setAuthentication(null);
	            }
            }
        }
        chain.doFilter(request, response);
        
    }
    private boolean authenticationIsRequired(String username) {
        Authentication existingAuth = SecurityContextHolder.getContext().getAuthentication();

        if(existingAuth == null || !existingAuth.isAuthenticated()) {
            return true;
        }

        if (existingAuth instanceof UsernamePasswordAuthenticationToken && !existingAuth.getName().equals(username)) {
            return true;
        }

        return false;
    }
    public void setAuthenticationDetailsSource(AuthenticationDetailsSource authenticationDetailsSource)
    {
        this.authenticationDetailsSource = authenticationDetailsSource;
    }
    public void setAuthenticationManager(AuthenticationManager authenticationManager)
    {
        this.authenticationManager = authenticationManager;
    }

}
