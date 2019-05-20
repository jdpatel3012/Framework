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
package org.springframework.security.web.authentication.www;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.providers.ExpiringUsernameAuthenticationToken;
import org.springframework.security.web.AuthenticationEntryPoint;

import com.ibaset.common.security.ExternalAuthenticationToken;

public class BasicProcessingFilter extends BasicAuthenticationFilter {
		//org.acegisecurity.ui.basicauth.BasicProcessingFilter {
	
	private boolean denyAllClientAccess = false;

	public BasicProcessingFilter(AuthenticationManager authenticationManager,
			AuthenticationEntryPoint authenticationEntryPoint) {
		super(authenticationManager, authenticationEntryPoint);
	}

	/*
	 * Skip basic authentication if there was a successful external authentication
	 * */
	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException 
	{
		if (isDenyAllClientAccess())
		{
			if (((HttpServletRequest) request).getServletPath().contains("/gateway"))
			{
				throw new RuntimeException("ALL CLIENT ACCESS DENIED");
			}
		}
        Authentication existingAuth = SecurityContextHolder.getContext().getAuthentication();

        if(existingAuth != null && existingAuth.isAuthenticated() && (existingAuth instanceof ExternalAuthenticationToken || existingAuth instanceof ExpiringUsernameAuthenticationToken)) {
        	chain.doFilter(request, response);
        }
        else
        {
    		super.doFilterInternal(request, response, chain);
        }

	}

	public boolean isDenyAllClientAccess() {
		return denyAllClientAccess;
	}

	public void setDenyAllClientAccess(boolean denyAllClientAccess) {
		this.denyAllClientAccess = denyAllClientAccess;
	}

}
