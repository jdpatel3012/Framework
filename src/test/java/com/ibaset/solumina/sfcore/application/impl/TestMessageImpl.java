package com.ibaset.solumina.sfcore.application.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.ibaset.common.context.ThreadContext;
import com.ibaset.common.security.context.ContextUtil;
import com.ibaset.common.security.context.SoluminaUser;
import com.ibaset.solumina.sfcore.dao.IMessageDao;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;

@RunWith(JUnitParamsRunner.class)
public class TestMessageImpl {

    @Spy
    @InjectMocks
    MessageImpl message;
    @Mock
    IMessageDao messageDao;
    
    SoluminaUser user;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        ThreadContext.getInstance().setSessionId();
    }
    
    @After
    public void cleanup() {
        ThreadContext.getInstance().clear();        
    }
    
    @AfterClass
	public static void tearDown()
	{
		ContextUtil.setUserForTestPurposes(null);
	}
    
    @Test
    public void testReplaceTokensForMandatoryField(){
        String messageStr = "Mandatory field %V1 is blank";
        String[] replacementTokens = {"%V1"};
        String[] replacementStrings = {"Part No"};
        String result = message.replaceTokensForMandatoryField(messageStr, replacementTokens, replacementStrings);
        String expected = "'Mandatory field '+@ConvertLanguage(Part No)+' is blank'";
        assertEquals(expected, result);
    }
    
    // GE-11793
    @Test
    public void testReplaceTokensForMandatoryField_SingleQuoteHandling(){
    	String messageStr = "Champ obligatoire: %V1 est'vide";
    	String[] replacementTokens = {"%V1"};
    	String[] replacementStrings = {"Part No"};
    	String result = message.replaceTokensForMandatoryField(messageStr, replacementTokens, replacementStrings);
    	String expected = "'Champ obligatoire: '+@ConvertLanguage(Part No)+' est''vide'";
    	assertEquals(expected, result);
    }

    @Test
    public void testReplaceTokensForMandatoryField_NoReplaceTokens(){
        String messageStr = "Mandatory field %V1 is blank";
        String[] replacementTokens = {""};
        String[] replacementStrings = {"Part No"};
        String result = message.replaceTokensForMandatoryField(messageStr, replacementTokens, replacementStrings);
        String expected = "'Mandatory field %V1 is blank'";
        assertEquals(expected, result);
    }

    @Test
    public void testReplaceTokensForMandatoryField_TwoReplaceStrings(){
        String messageStr = "Mandatory field %V1 is blank";
        String[] replacementTokens = {"%V1"};
        String[] replacementStrings = {"Part No", "Part Rev"};
        String result = message.replaceTokensForMandatoryField(messageStr, replacementTokens, replacementStrings);
        String expected = "'Mandatory field '+@ConvertLanguage(Part No)+' is blank'";
        assertEquals(expected, result);
    }

    // GE-6619
    @Test
    public void testGetFormattedMessageBisCall(){
        // Arrange
        String[] replacementTokens = {"%V1"};
        String[] replacementStrings = {"Part No"};
        String messageId = "MANDATORY_FIELD";
        String msgText = "Mandatory field %V1 is blank";
        Mockito.when(message.getMessage(messageId)).thenReturn(msgText);
        boolean isBISCall = true;
        
        setUpUserContext(isBISCall);
        String expected = "Mandatory field Part No is blank";
        
        // Act
        String actual = message.getFormattedMessage(messageId, replacementTokens, replacementStrings, true);
        
        // Assert
        assertTrue(actual.contains(expected));
        
        clearInfoFromContext();
    }
    
    // GE-6619
    @Test
    public void testGetFormattedMessageNonBisCall(){
        // Arrange
        String[] replacementTokens = {"%V1"};
        String[] replacementStrings = {"Part No"};
        String messageId = "MANDATORY_FIELD";
        String msgText = "Mandatory field %V1 is blank";
        Mockito.when(message.getMessage(messageId)).thenReturn(msgText);
        String expected = "'Mandatory field '+@ConvertLanguage(Part No)+' is blank'";
        
        // Act
        String actual = message.getFormattedMessage(messageId, replacementTokens, replacementStrings, true);
        
        // Assert
        assertTrue(actual.contains(expected));
    }
    
    // GE-6619
    public void setUpUserContext(boolean isBISCall){
        user = new SoluminaUser("TESTUSER", "TESTUSER", true, true, true, true,
                new GrantedAuthority[] { new SimpleGrantedAuthority("HOLDER") });
        
        if(isBISCall)
        	user.getContext().setBisCall(true);
        
        ContextUtil.setUserForTestPurposes(user);
    }
    
    public Object[][] getParamsForReservedCharTest()
    {
        return new Object[][] {  
            { "Cannot modify %V1 %V2 ,references exist", "%V1,%V2", "SFWID_HOLDS ORDER_NO=12125_ORD1,OPER_NO=010", true, false,
            	">>'Cannot modify SFWID_HOLDS ORDER_NO=12125_ORD1 OPER_NO=010 ,references exist'"},
            
            { "Duplicate Item No/Item Rev", "", "", true, false,
        		">>Duplicate Item No/Item Rev"},
				
            // For build message call, responseToClient=false. No wrapping of message text
            { "Cannot modify %V1 %V2 ,references exist", "%V1,%V2", "SFWID_HOLDS ORDER_NO=12125_ORD1,OPER_NO=010", false, false,
        		">>Cannot modify SFWID_HOLDS ORDER_NO=12125_ORD1 OPER_NO=010 ,references exist"},
            
            // For BIS call, isBISCall=true. No wrapping of message text
            { "Cannot modify %V1 %V2 ,references exist", "%V1,%V2", "SFWID_HOLDS ORDER_NO=12125_ORD1,OPER_NO=010", false, true,
    			">>Cannot modify SFWID_HOLDS ORDER_NO=12125_ORD1 OPER_NO=010 ,references exist"},
            
            // For build message through BIS call, responseToClient=false, isBISCall=true. No wrapping of message text
            { "Cannot modify %V1 %V2 ,references exist", "%V1,%V2", "SFWID_HOLDS ORDER_NO=12125_ORD1,OPER_NO=010", true, true,
            	">>Cannot modify SFWID_HOLDS ORDER_NO=12125_ORD1 OPER_NO=010 ,references exist"},
    };
}
    
    // GE-12125
    @Test
    @Parameters(method = "getParamsForReservedCharTest")
    public void testGetFormattedMessage_VerifyReservedChars(String msgText,
												            String replacementTokensValues,
												            String replacementStringsValues, 
												            boolean responseToClient,
												            boolean isBISCall,
												            String expected)
    {
    	// Arrange
    	String messageId = "MSG_ID1";
        String[] replacementTokens = replacementTokensValues.split(",");
        String[] replacementStrings = replacementStringsValues.split(",");
        Mockito.when(message.getMessage(messageId)).thenReturn(msgText);
        
        setUpUserContext(isBISCall);
        
        // Act
        String actual = message.getFormattedMessage(messageId, replacementTokens, replacementStrings, responseToClient);
        
        // Assert
        assertTrue(actual.contains(expected));
        
        clearInfoFromContext();
    }

	private void clearInfoFromContext() {
		user.getContext().setBisCall(false);
	}

}
