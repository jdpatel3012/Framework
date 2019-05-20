/**
 * Proprietary and Confidential
 * Copyright 1995-2017 iBASEt, Inc.
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
package com.ibaset.solumina.sfcore.application.impl;

import static com.ibaset.common.FrameworkConstants.PRIMARY_PROPER_CASE;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;

import static org.apache.commons.lang.StringUtils.*;
import static org.apache.commons.lang.math.NumberUtils.*;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import com.ibaset.common.security.context.ContextUtil;
import com.ibaset.common.security.context.SoluminaUser;
import com.ibaset.common.security.context.UserContext;
import com.ibaset.solumina.concurrent.user.ConnectionDescService;
import com.ibaset.solumina.sfcore.application.IMessage;
import com.ibaset.solumina.sfcore.dao.ILoginDao;
import com.ibaset.solumina.sfcore.application.impl.LoginImpl;
import com.ibaset.solumina.sfcore.dao.impl.LoginDaoImpl;
import com.ibaset.solumina.sffnd.application.IUserClass;
import com.ibaset.solumina.sffnd.dao.IGlobalConfigurationDao;

@RunWith(JUnitParamsRunner.class)
public class TestLoginImpl
{
	Field field1 = null;
	
	String connectionId = "MFI_86408A87A0AE4217BD6D6EC41F9FCE5E";
	String userId = "ROHIT1";
	String loginStatus = "SUCCESS";
	String ipAddress = "10.10.9.206";
	String applicationIniId = "10.10.9.206"; 
	String applicationVersion = "9.0";
	String connectionType = "";
	
	@InjectMocks
	@Spy
	LoginImpl login = new LoginImpl();
	
	@Mock
	LoginDaoImpl loginDao = null;
	
	@Mock
	IUserClass sffndUserClass = null;
	
	@Mock
	IMessage message = null;
	
	@Mock
	IGlobalConfigurationDao globalConfigurationDao = null;
	
	@Mock
	SoluminaUser testUser = null;
	
	@Mock
	UserContext mockUserContext = null;
	
	@Mock
    ConnectionDescService connectionDesc = null;
	
	@Before
	public void setUp() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException
	{
        
        field1 = LoginImpl.class.getDeclaredField("connectionDesc");
        field1.setAccessible(true);
        field1.set(login, connectionDesc);

		MockitoAnnotations.initMocks(this);
		
		// Set user for test purpose.
        when(testUser.getContext()).thenReturn(mockUserContext);
        when(mockUserContext.getConnectionType()).thenReturn(connectionType);
        when(mockUserContext.getConnectionId()).thenReturn(connectionId);
        
        ContextUtil.setUserForTestPurposes(testUser);
	}
	
	
	public static Object[] getParameters_GE4129()
    {
        return new Object[][]{  
                {2 , 1}, 
                {0 , 0}, 
        };
    }
	
	
	@Test
	@Parameters(method = "getParameters_GE4129")
	public void testUpdateLoginAttemptIfPreviousLoginFailed(int previousLoginAtempt, int numberOfTimesUpdateCall)
	{
		when(loginDao.selectUserLoginAttempt(userId)).thenReturn(previousLoginAtempt);
		
		
		Mockito.doNothing().when(loginDao).insertLoginAttempt(connectionId, 
                                                  			  userId,
                                                  			  loginStatus,
                                                  			  EMPTY,
                                                  			  connectionType,
                                                  			  ipAddress,
                                                  			  applicationIniId,
                                                  			  applicationVersion);
		
		login.saveLogin(connectionId, userId, loginStatus, ipAddress, applicationIniId, applicationVersion);
		Mockito.verify(loginDao,times(numberOfTimesUpdateCall)).updateUserAttempt(userId, INTEGER_ZERO, "Success");
	}
	
	//GE-4986
	@Test
	public void testSaveLoginAttempt_whenConnectionIdIsNull()
	{
	    testSaveLoginAttempt(null);
	    
	    verify(sffndUserClass, times(1)).updateUserLoginAttemptDetail(eq(connectionId),
                                                                      anyString(),
                                                                      anyString(),
                                                                      anyString(),
                                                                      anyString(),
                                                                      anyString(),
                                                                      anyString(),
                                                                      anyString(),
                                                                      any(Number.class),
                                                                      anyString());
	}
	
	//GE-4986
    @Test
    public void testSaveLoginAttempt_whenConnectionIdIsNotNull()
    {
        String connectionId = "CONNECT_1";
        
        testSaveLoginAttempt(connectionId);
        
        verify(sffndUserClass, times(1)).updateUserLoginAttemptDetail(eq(connectionId),
                                                                      anyString(),
                                                                      anyString(),
                                                                      anyString(),
                                                                      anyString(),
                                                                      anyString(),
                                                                      anyString(),
                                                                      anyString(),
                                                                      any(Number.class),
                                                                      anyString());
    }
	
    public void testSaveLoginAttempt(String connectionId)
    {
        String failureReason = "FAILURE_REASON";
       
        SoluminaUser testUser = mock(SoluminaUser.class);
        UserContext mockUserContext = mock(UserContext.class);
        when(testUser.getContext()).thenReturn(mockUserContext);
        
        when(globalConfigurationDao.selectParameterValue(anyString(),anyString())).thenReturn("3");
        when(loginDao.selectUserLoginAttempt(userId)).thenReturn(2);
        
        login.saveLoginAttempt(connectionId,
                               userId,
                               loginStatus,
                               failureReason,
                               ipAddress,
                               applicationIniId,
                               applicationVersion);
        
    }
    
    @Test
    public void testLoginUser_concurrentUserSessionsValidations()
    {
    	String supplierId = "SUPP1";
    	String appVersion = "1";
    	String pcFlag = "Y";
    	String userType = "USER TYPE";
    	
    	Mockito.doNothing().when(login).userLoginLockCheck(userId);
    	when(login.isNotAnonymousUser(userId)).thenReturn(false);
    	
    	login.loginUser(userId, supplierId, connectionType, ipAddress, applicationIniId, appVersion, 
    			pcFlag, userType, PRIMARY_PROPER_CASE);
    	
    	verify(connectionDesc, times(1)).userSessionsValidation(userId, applicationIniId, 
    			connectionType, ipAddress, PRIMARY_PROPER_CASE);
    }
    
	@AfterClass
    public static void clearUserStaticParameter()

    {
        ContextUtil.setUserForTestPurposes(null);
    }

}
