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
package com.ibaset.common.security.context;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.ibaset.common.solumina.exception.SoluminaException;
import com.ibaset.solumina.sffnd.application.IGlobalConfiguration;

public class SoluminaUserDetailsLoader
{
    private IGlobalConfiguration globalConfiguration;
    private SoluminaJdbcDaoImpl privQuery = null;

    protected UserDetails buildSoluminaUser(String username,
                                            String password,
                                            boolean enabled,
                                            boolean unlockedUser)
    {
        SoluminaUser user = new SoluminaUser(username,
                                             password,
                                             enabled,
                                             true,
                                             true,
                                             unlockedUser,
                                             new GrantedAuthority[]
                                                     {
                                                             new SimpleGrantedAuthority("HOLDER")
                                                     });
        addDbFlags(user);
        return user;
    }

    public SoluminaUser createSoluminaUser(UserDetails details)
    {

        GrantedAuthority[] privs = privQuery.selectSoluminaPrivsForRoles(details.getAuthorities());
        SoluminaUser user = new SoluminaUser(details.getUsername(),
                                             details.getPassword(),
                                             details.isEnabled(),
                                             true,
                                             true,
                                             details.isAccountNonLocked(),
                                             privs
                                             );
        addDbFlags(user);
        return user;
    }
    
    
    public SoluminaUser createSoluminaUser(String returnUsername,
                                           String password,
                                           boolean enabled,
                                           Collection<GrantedAuthority> authorities)
    {
    	return createSoluminaUser(returnUsername, 
    					   password, 
    					   enabled, 
    					   authorities == null? null : (GrantedAuthority[]) authorities.toArray(new GrantedAuthority[authorities.size()]));
    }

    public SoluminaUser createSoluminaUser(String returnUsername,
                                           String password,
                                           boolean enabled,
                                           GrantedAuthority[] arrayAuths)
    {
        return createSoluminaUser(returnUsername,
                                  password,
                                  enabled,
                                  true,
                                  arrayAuths);
    }
    
    public SoluminaUser createSoluminaUser(String returnUsername,
                                           String password,
                                           boolean enabled,
                                           boolean unlockedUser, 
                                           GrantedAuthority[] arrayAuths)
    {
        
        SoluminaUser u = new SoluminaUser(returnUsername,
                                          password,
                                          enabled,
                                          true,
                                          true,
                                          unlockedUser,
                                          arrayAuths);
        addDbFlags(u);
        return u;
    }

    private void addDbFlags(SoluminaUser u)
    {
        try
        {
            Map globalConfigMap = globalConfiguration.loadDefaultGlobalConfigurationParameters();
            String signoffAllOverideFlag = (String) globalConfigMap.get("SIGNOFF_ALL_OVERIDE_FLAG");
            String manualAttendanceFlag = (String) globalConfigMap.get("MANUAL_ATTENDANCE_FLAG");
            String sendLtaSystemFlag = (String) globalConfigMap.get("SEND_LTA_SYSTEM_FLAG");
            String transportAvailableFlag = (String) globalConfigMap.get("TRANSPORT_AVAILABLE_FLAG");

            u.getContext()
             .setUserActivityThreshold((Number) globalConfigMap.get("USER_ACTIVITY_THRESHOLD"));
            u.getContext()
             .setSignoffAllOverideFlag("Y".equals(signoffAllOverideFlag));
            u.getContext()
             .setManualAttendanceFlag("Y".equals(manualAttendanceFlag));
            u.getContext().setSendLtaSystemFlag("Y".equals(sendLtaSystemFlag));
            u.getContext()
             .setJobTimeout((Number) globalConfigMap.get("JOB_TIMEOUT"));
            u.getContext()
             .setTransportAvailableFlag("Y".equals(transportAvailableFlag));
            u.getContext()
             .setJobNextInterval((Number) globalConfigMap.get("JOB_NEXT_INTERVAL"));

        }
        catch (SoluminaException e)
        {
            throw new DataIntegrityViolationException(e.getMessage());
        }
    }

    public IGlobalConfiguration getGlobalConfiguration()
    {
        return globalConfiguration;
    }

    public void setGlobalConfiguration(IGlobalConfiguration globalConfiguration)
    {
        this.globalConfiguration = globalConfiguration;
    }

    public SoluminaJdbcDaoImpl getPrivQuery()
    {
        return privQuery;
    }

    public void setPrivQuery(SoluminaJdbcDaoImpl privQuery)
    {
        this.privQuery = privQuery;
    }
}
