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
package org.springframework.security.authentication.dao;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import com.ibaset.common.security.ExternalAuthenticationToken;
import com.ibaset.common.security.context.SoluminaUserDetailsLoader;

public class SoluminaDaoAuthenticationProvider extends AbstractUserDetailsAuthenticationProvider
{

    private DaoAuthenticationProviderImpl delegateProvider;
    private SoluminaUserDetailsLoader soluminaUserDetailsLoader;
    
    protected void additionalAuthenticationChecks(UserDetails userDetails,
                                                  UsernamePasswordAuthenticationToken authentication) throws AuthenticationException
    {
        if(!(authentication instanceof ExternalAuthenticationToken))
            delegateProvider.additionalAuthentications(userDetails, authentication);
    }
    
    protected UserDetails retrieveUser(String username,
                                       UsernamePasswordAuthenticationToken authentication) throws AuthenticationException
    {
    	//The user MUST exist in the Solumina database
    	UserDetails details=delegateProvider.retrieveUserDetails(username, authentication);
    	
        if(authentication instanceof ExternalAuthenticationToken)
        {
            ExternalAuthenticationToken token=(ExternalAuthenticationToken)authentication;
            //external authorization
            if(token.getAuthorities()!=null && !token.getAuthorities().isEmpty())
            {
            	String password=(String)token.getCredentials();
            	if(password==null) password="";
                User user=new User(details.getUsername(), 
                		password, 
                		details.isEnabled(), 
                		details.isAccountNonExpired(), 
                		details.isCredentialsNonExpired(), 
                		details.isAccountNonLocked(), 
                		token.getAuthorities());
                return soluminaUserDetailsLoader.createSoluminaUser(user);
            }
        }
        return details;
    }
    
	protected Authentication createSuccessAuthentication(Object principal, Authentication authentication, UserDetails user) {
		if(authentication instanceof ExternalAuthenticationToken)
		{
			ExternalAuthenticationToken result = new ExternalAuthenticationToken(principal,
	                authentication.getCredentials(), user.getAuthorities());
	        result.setDetails(authentication.getDetails());
	        return result;
		}
		return super.createSuccessAuthentication(principal, authentication, user);
	}
    
    public void setDelegateProvider(DaoAuthenticationProviderImpl delegate)
    {
        this.delegateProvider = delegate;
    }
    public void setSoluminaUserDetailsLoader(SoluminaUserDetailsLoader soluminaUserDetailsLoader)
    {
        this.soluminaUserDetailsLoader = soluminaUserDetailsLoader;
    }

}
