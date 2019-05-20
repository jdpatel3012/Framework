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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.lang.StringUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.object.MappingSqlQuery;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.userdetails.jdbc.JdbcDaoImpl;

import com.ibaset.common.FrameworkConstants;
import com.ibaset.common.Reference;
import com.ibaset.solumina.sfcore.application.ILicense;

public class SoluminaJdbcDaoImpl extends JdbcDaoImpl
{
    private MappingSqlQuery authoritiesByUsernameMapping;

    private MappingSqlQuery usersByUsernameMapping;

    private SoluminaUserDetailsLoader soluminaUserDetailsLoader;
    private String ldapPrivQuery;
    @Reference
    private ILicense license = null;

    public void initMappingSqlQueries()
    {
        this.usersByUsernameMapping = new UsersByUsernameMapping(getDataSource());
        this.authoritiesByUsernameMapping = new AuthoritiesByUsernameMapping(getDataSource());
    }
    
    public GrantedAuthority[] selectSoluminaPrivsForRoles(GrantedAuthority[] externalRoles)
    {
    	return selectSoluminaPrivsForRoles(Arrays.asList(externalRoles));
    }

    public GrantedAuthority[] selectSoluminaPrivsForRoles(Collection<? extends GrantedAuthority> externalRoles)
    {
        List privs = new ArrayList();
        String csvRoleName = "";
        for(GrantedAuthority grantedAuthority:externalRoles)
        {
            csvRoleName+=grantedAuthority.getAuthority();
            csvRoleName+=",";
        }
        List stringPrivs = this.getJdbcTemplate()
            .queryForList(ldapPrivQuery,
                          new Object[]{csvRoleName});
        
        Iterator it = stringPrivs.iterator();
        while (it.hasNext())
        {
            Map row = (Map) it.next();
            String role = (String)row.get("PRIV");
            privs.add(new SimpleGrantedAuthority(role));
            
        }
        
        
        return (GrantedAuthority[])privs.toArray(new GrantedAuthority[privs.size()]);
        
    }
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException,
                                                          DataAccessException
    {
        List users = usersByUsernameMapping.execute(username);

        if (users.size() == 0)
        {
            throw new UsernameNotFoundException("User not found");
        }

        UserDetails user = (UserDetails) users.get(0); // contains no
        // GrantedAuthority[]

        Object[] params = new Object[6];
        params[0] = user.getUsername();
        params[2] = user.getUsername();
        params[4] = user.getUsername();
        String licenseValue = license.getFullUserLicenseFlagFromDb();
        boolean isLoadLatestLicense = StringUtils.equals(licenseValue, FrameworkConstants.YES);
        if(isLoadLatestLicense)
        {        	
        	params[1] = FrameworkConstants.YES;
        	params[3] = FrameworkConstants.YES;
        	params[5] = FrameworkConstants.YES;
        }
        else
        {
        	params[1] = FrameworkConstants.NO;
        	params[3] = FrameworkConstants.NO;
        	params[5] = FrameworkConstants.NO;
        }
                
        List dbAuths = authoritiesByUsernameMapping.execute(params);

        if (dbAuths.size() == 0)
        {
        	dbAuths.add(new SimpleGrantedAuthority("J2EE_CONNECT"));
        }

        GrantedAuthority[] arrayAuths =
        {};

        addCustomAuthorities(user.getUsername(), dbAuths);

        arrayAuths = (GrantedAuthority[]) dbAuths.toArray(arrayAuths);

        String returnUsername = user.getUsername();

        if (!isUsernameBasedPrimaryKey())
        {
            returnUsername = username;
        }

        UserDetails u = soluminaUserDetailsLoader.createSoluminaUser(returnUsername,
                                                                     user.getPassword(),
                                                                     user.isEnabled(),
                                                                     user.isAccountNonLocked(),
                                                                     arrayAuths);

        return u;
    }

    protected class AuthoritiesByUsernameMapping extends MappingSqlQuery
    {
        protected AuthoritiesByUsernameMapping(DataSource ds)
        {
            super(ds, getAuthoritiesByUsernameQuery());
            declareParameter(new SqlParameter(Types.VARCHAR));
            declareParameter(new SqlParameter(Types.VARCHAR));
            declareParameter(new SqlParameter(Types.VARCHAR));
            declareParameter(new SqlParameter(Types.VARCHAR));
            declareParameter(new SqlParameter(Types.VARCHAR));
            declareParameter(new SqlParameter(Types.VARCHAR));
            compile();
        }

        protected Object mapRow(ResultSet rs, int rownum) throws SQLException
        {
            String roleName = getRolePrefix() + rs.getString(2);
            SimpleGrantedAuthority authority = new SimpleGrantedAuthority(roleName);

            return authority;
        }
    }

    /**
     * Query object to look up a user.
     */
    protected class UsersByUsernameMapping extends MappingSqlQuery
    {
        protected UsersByUsernameMapping(DataSource ds)
        {
            super(ds, getUsersByUsernameQuery());
            declareParameter(new SqlParameter(Types.VARCHAR));
            compile();
        }

        protected Object mapRow(ResultSet rs, int rownum) throws SQLException
        {
            String username = rs.getString(1);

            //default to -1 to avoid a hard 500 error in tomcat
            String password = StringUtils.defaultIfEmpty(rs.getString(2), "-1");
            boolean enabled = rs.getBoolean(3);
            boolean unlockedUser = rs.getBoolean(4); 

            UserDetails user = soluminaUserDetailsLoader.buildSoluminaUser(username,
                                                                           password,
                                                                           enabled,
                                                                           unlockedUser);

            return user;
        }

    }

    public SoluminaUserDetailsLoader getSoluminaUserDetailsLoader()
    {
        return soluminaUserDetailsLoader;
    }

    public void setSoluminaUserDetailsLoader(SoluminaUserDetailsLoader soluminaUserDetailsLoader)
    {
        this.soluminaUserDetailsLoader = soluminaUserDetailsLoader;
    }

    public String getLdapPrivQuery()
    {
        return ldapPrivQuery;
    }

    public void setLdapPrivQuery(String ldapPrivQuery)
    {
        this.ldapPrivQuery = ldapPrivQuery;
    }

}
