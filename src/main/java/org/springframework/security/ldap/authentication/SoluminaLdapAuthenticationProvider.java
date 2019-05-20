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
package org.springframework.security.ldap.authentication;


import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.ldap.userdetails.LdapAuthoritiesPopulator;
import org.springframework.security.ldap.userdetails.LdapUserDetails;

import com.ibaset.common.security.context.SoluminaUser;
import com.ibaset.common.security.context.SoluminaUserDetailsLoader;

public class SoluminaLdapAuthenticationProvider extends
                                               LdapAuthenticationProvider
{
	// Seems like class is not used anywhere
    private SoluminaUserDetailsLoader soluminaUserDetailsLoader;

    public SoluminaLdapAuthenticationProvider(LdapAuthenticator authenticator,
                                              LdapAuthoritiesPopulator authoritiesPopulator)
    {
        super(authenticator, authoritiesPopulator);
    }
    
    /*protected UserDetails createUserDetails(LdapUserDetails ldapUser, String username, String password) 
    {
        UserDetails details = super.createUserDetails(ldapUser, username, password);
        SoluminaUser soluminaUser = soluminaUserDetailsLoader.createSoluminaUser(details);
        return soluminaUser;
    }


    public void setSoluminaUserDetailsLoader(SoluminaUserDetailsLoader soluminaUserDetailsLoader)
    {
        this.soluminaUserDetailsLoader = soluminaUserDetailsLoader;
    }*/

}
