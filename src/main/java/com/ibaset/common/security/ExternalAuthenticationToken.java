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

import java.util.Collection;
import java.util.LinkedList;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;


public class ExternalAuthenticationToken extends UsernamePasswordAuthenticationToken
{

    private static final long serialVersionUID = 1L;

    public ExternalAuthenticationToken(Object principal, Object credentials, Collection<? extends GrantedAuthority> authorities)
    {
        super(principal, credentials, authorities);
    }
    
    public ExternalAuthenticationToken(Object principal, Object credentials, String groups)
    {
        super(principal, credentials, createAuthorities(groups));
    }
    
    private static final Collection<GrantedAuthority> createAuthorities(String groups){
    	Collection<GrantedAuthority> authorities = new LinkedList<GrantedAuthority>();
        if(groups!=null && groups.length()>0)
        {
            String group[]=groups.split(",");
            if(group.length>0)
            {
                for(int i=0;i<group.length;++i)authorities.add(new SimpleGrantedAuthority(group[i]));
            }
        }
    	    	
        /*GrantedAuthority[] authorities=null;
        if(groups!=null && groups.length()>0)
        {
            String group[]=groups.split(",");
            if(group.length>0)
            {
                authorities=new GrantedAuthority[group.length];
                for(int i=0;i<group.length;++i)authorities[i]=new SimpleGrantedAuthority(group[i]);
            }
        }*/
        return authorities;
    }
}
