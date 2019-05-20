package org.springframework.security.authentication.dao;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;

public class DaoAuthenticationProviderImpl extends DaoAuthenticationProvider {

	/*
	 * Wrapper class created to avoid error of IllegalAccessError at DaoAuthenticationProvider.retrieveUser
	 */
	public void additionalAuthentications(UserDetails userDetails,
                                                  UsernamePasswordAuthenticationToken authentication) throws AuthenticationException
    {
		super.additionalAuthenticationChecks(userDetails, authentication);
    }
    
	public UserDetails retrieveUserDetails(String username,
                                       UsernamePasswordAuthenticationToken authentication) throws AuthenticationException
    {
    	UserDetails details=super.retrieveUser(username, authentication);
    	return details;
    } 
}
