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

import java.util.ArrayList;
import java.util.Collection;

import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.test.AbstractDependencyInjectionSpringContextTests;

import com.ibaset.common.security.context.CustomInMemoryDaoImpl;
import com.ibaset.common.security.context.SoluminaUser;

/**
 * @author joes
 *
 */
public abstract class BaseTestCase extends AbstractDependencyInjectionSpringContextTests
{

	

     
    protected String[] getConfigLocations()
    {
    	
    	if(SoluminaTestCase.cxtList == null)
    	{
    		SoluminaTestCase test = new SoluminaTestCase();
    		
    		SoluminaTestCase.cxtList= test.getSpringConfigLocations();
    	}
    	return SoluminaTestCase.cxtList;
    	
    	 
        
      
    }
    protected void onSetUp()
    {

        setUpTestCase();
		SoluminaUser user = new SoluminaUser(	"SFMFG",
												"SFMFG",
												true,
												true,
												true,
												true,
												CustomInMemoryDaoImpl.getGrantedAuthorityList());
		
		Collection<GrantedAuthority> authorities = user.getAuthorities();
		
		// Grant all roles to SFMFG
		TestingAuthenticationToken token = new TestingAuthenticationToken(	user,
																			user.getPassword(),
																			authorities == null ? null: new ArrayList<GrantedAuthority>(authorities) );

		// Create and store the Acegi SecureContext into the ContextHolder.
		SecurityContextImpl secureContext = new SecurityContextImpl();

		secureContext.setAuthentication(token);

		SecurityContextHolder.setContext(secureContext);
        

        
    }
        
    
    protected void onTearDown()
    {
    	
    }
	
    protected void setUpTestCase()
    {
        
    }
}