/**
 * Proprietary and Confidential
 * Copyright 1995-2018 iBASEt, Inc.
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
package com.ibaset.solumina.sffnd.application.impl;

import static com.ibaset.common.FrameworkConstants.ASSET_POLICY_OVERRIDE;
import static com.ibaset.common.FrameworkConstants.CREW_PRIOR_EXPERIENCE_RANGE;
import static com.ibaset.common.FrameworkConstants.EXTEND_CALIBRATION;
import static com.ibaset.common.FrameworkConstants.FOUNDATION;
import static com.ibaset.common.FrameworkConstants.MANUFACTURING_INTELLIGENCE;
import static com.ibaset.common.FrameworkConstants.NO;
import static com.ibaset.common.FrameworkConstants.NOT_APPLICABLE;
import static com.ibaset.common.FrameworkConstants.OPER_NUM_VALIDATION;
import static com.ibaset.common.FrameworkConstants.PRODUCTION_PROCESS_VERIFICATION_QTY;
import static com.ibaset.common.FrameworkConstants.READ_ACKNOWLEDGE_HORIZON_DAYS;
import static com.ibaset.common.FrameworkConstants.SAME_USER_MULTIPLE_SESSIONS_LIMIT;
import static com.ibaset.common.FrameworkConstants.SET_REJECTED_COMP_LOT_STATUS_TO_STOP;
import static com.ibaset.common.FrameworkConstants.SECURITY_GROUP_FLAG;
import static com.ibaset.common.FrameworkConstants.SUPPLIER_DPPM_HOLD_THRESHOLD;
import static com.ibaset.common.FrameworkConstants.TOOL_SERIAL_CREATE;
import static com.ibaset.common.FrameworkConstants.UID_ENTRY_NAME;
import static com.ibaset.common.FrameworkConstants.USER_ACTIVITY_THRESHOLD;
import static com.ibaset.common.FrameworkConstants.USER_LOGIN_FAILED_LIMIT;
import static com.ibaset.common.FrameworkConstants.USER_UPDATE_PRIV;
import static com.ibaset.common.FrameworkConstants.YES;
import static com.ibaset.common.FrameworkConstants.WS_MAX_CONNECTIONS_PER_HOST_PARAM_NAME;
import static com.ibaset.common.FrameworkConstants.WS_REQUEST_TIMEOUT_PARAM_NAME;
import static com.ibaset.common.FrameworkConstants.WS_SO_TIMEOUT_PARAM_NAME;
import static com.ibaset.common.util.SoluminaUtils.stringEquals;
import static org.apache.commons.lang.StringUtils.EMPTY;
import static org.apache.commons.lang.StringUtils.equalsIgnoreCase;
import static org.apache.commons.lang.StringUtils.upperCase;
import static org.apache.commons.lang.math.NumberUtils.isNumber;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.regex.Pattern;

import org.apache.commons.collections.map.ListOrderedMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;

import com.ibaset.common.Reference;
import com.ibaset.common.security.context.ContextUtil;
import com.ibaset.common.solumina.IValidator;
import com.ibaset.solumina.admin.SoluminaBeanService;
import com.ibaset.solumina.sfcore.application.IMTNode;
import com.ibaset.solumina.sfcore.application.IMessage;
import com.ibaset.solumina.sfcore.application.impl.MTNodeImpl;
import com.ibaset.solumina.sffnd.application.IEvent;
import com.ibaset.solumina.sffnd.application.IGlobalConfiguration;
import com.ibaset.solumina.sffnd.application.IUidEntry;
import com.ibaset.solumina.sffnd.dao.IGlobalConfigurationDao;

public class GlobalConfigurationImpl implements IGlobalConfiguration
{
    private static final Logger logger = LoggerFactory.getLogger(GlobalConfigurationImpl.class);
    
    private IMessage message = null;

    private IValidator validator = null;

    private IGlobalConfigurationDao globalConfigurationsDao = null;

    private IUidEntry uidEntry;
    
    @Reference
    private IMTNode mtNode;
    
    @Reference
    private SoluminaBeanService soluminaBeanService;

    @Reference
    private IEvent event; // PBL-168

    public Map loadDefaultGlobalConfigurationParameters()
    {
        Map defaultGlobalConfigurationParameters = null;
        // Get the default values for the mandatory global params
        try
        {
            defaultGlobalConfigurationParameters = globalConfigurationsDao.selectGlobalConfigurationParameters();

            // User_Activity_Threshold moved to Labor Type 
            
            /* String userActivityThreshold = (String) defaultGlobalConfigurationParameters.get(USER_ACTIVITY_THRESHOLD);
               defaultGlobalConfigurationParameters.put(USER_ACTIVITY_THRESHOLD,
                                                     new Integer(userActivityThreshold));
                                                     */
        }
        catch (IncorrectResultSizeDataAccessException exception)
        {
            if (exception.getActualSize() == 0)
            {
            	//GE-924
            	message.raiseError("MFI_B94233F81DB14EFA9A0E29072B558D82");
            }
        }

        return defaultGlobalConfigurationParameters;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibaset.solumina.sffnd.application.IGlobalConfigurations#updateGlobalConfiguration(java.lang.String,
     *      java.lang.String, java.lang.String)
     */
    public void updateGlobalConfiguration(String configurationModuleName,
                                          String parameterName,
                                          String parameterValue)
    {
        // Creates the Parameter Name list Collection.
        ArrayList parameterNameList = new ArrayList();
        parameterNameList.add(upperCase(ASSET_POLICY_OVERRIDE));
        parameterNameList.add(upperCase(EXTEND_CALIBRATION));
        parameterNameList.add(upperCase(TOOL_SERIAL_CREATE));

        // Check for privilege if changing Tooling Calibration Parameters.
        if (parameterNameList.contains(parameterName))
        {
            // Checks the user has privilege or not
            validator.checkUserPrivilege("CONFIG_UPDATE");
        }
        else
        {
            // Checks the user has privilege or not
            validator.checkUserPrivilege(USER_UPDATE_PRIV);

        }
        
        //FND-23182
        //Parameter Value must be 'Y' or 'N' for Global Parameter "MANUFACTURING_INTELLIGENCE"
        if (stringEquals(parameterName, MANUFACTURING_INTELLIGENCE) 
        		&& !(stringEquals(parameterValue, YES) 
        						|| stringEquals(parameterValue, NO)))
        {
        	message.raiseError("MFI_B02EABE2A99D49178C5943B930513218");
        }
        

        // Checks whether the value changes or not
        boolean configurationExists = globalConfigurationsDao.selectConfigurationExists(configurationModuleName,
                                                                                        parameterName,
                                                                                        parameterValue);

        // 01/07/2008 Gantra Utpal. FND-7275 Changed error message to warning
        // message.
        // If the record already exists then raise error
        if (configurationExists)
        {
        	message.showMessage("MFI_7A829754497B4DF2B7801AADB6C03F49");  //GE-924
        }
        
        // GE-1837. CREW_PRIOR_EXPERIENCE_RANGE must be zero or greater than zero
        if (stringEquals(upperCase(FOUNDATION), configurationModuleName)
        		&& stringEquals(CREW_PRIOR_EXPERIENCE_RANGE, parameterName)
        		&& Integer.parseInt(parameterValue) < 0)
        {
        	message.raiseError("MFI_D1CE2760EA7546929F83ACD89F974C1E");
        }
        // GE-1837. Setting the range > 90 can affect performance for parameter CREW_PRIOR_EXPERIENCE_RANGE
        if (stringEquals(upperCase(FOUNDATION), configurationModuleName)
        		&& stringEquals(CREW_PRIOR_EXPERIENCE_RANGE, parameterName)
        		&& Integer.parseInt(parameterValue) > 90)
        {
        	message.showMessage("MFI_B259CEB7B6234A218E2ED0684115C173");
        }

        // Checks if the value is negative for the Configuration Module
        // "FOUNDATION"
        // and Parameter Name "USER_ACTIVITY_THRESHOLD"
        if (stringEquals(upperCase(FOUNDATION),
                               configurationModuleName)
                && stringEquals(USER_ACTIVITY_THRESHOLD,
                                      parameterName)
                && (isNumber(parameterValue) && Integer.parseInt(parameterValue) < 0))
        {
            message.raiseError("MFI_87656");

        }
        
        // FND-24562
        if (stringEquals(upperCase(FOUNDATION), configurationModuleName)
        		&&(stringEquals(READ_ACKNOWLEDGE_HORIZON_DAYS, parameterName)
        				|| stringEquals(PRODUCTION_PROCESS_VERIFICATION_QTY, parameterName)	// FND-22918
        				|| stringEquals(USER_LOGIN_FAILED_LIMIT, parameterName) 	// FND-25544
        				|| stringEquals(SUPPLIER_DPPM_HOLD_THRESHOLD, parameterName)
        				|| stringEquals(SAME_USER_MULTIPLE_SESSIONS_LIMIT, parameterName) 	// GE-4900
        				|| stringEquals(WS_MAX_CONNECTIONS_PER_HOST_PARAM_NAME, parameterName))
        				&& (isNumber(parameterValue) && Integer.parseInt(parameterValue) <= 0))
        {
        	// GE-5511
        	// Parameter Value must be greater than 0
        	message.raiseError("MFI_213DCB785F5C4CC7BE93A024734B0E58");

        }
        
        if (stringEquals(configurationModuleName,upperCase(FOUNDATION)) && 
                stringEquals(parameterName,UID_ENTRY_NAME) && 
                stringEquals(parameterValue, NOT_APPLICABLE ))
        {
        	Map inputParams = new ListOrderedMap();
        	inputParams.put("UID_ENTRY_NAME",parameterValue);
        	uidEntry.validateUIDEntry(inputParams, "GlobalConfig");
        }
        
        // FND-19205
        if(equalsIgnoreCase(parameterValue, NOT_APPLICABLE) &&
        		stringEquals(parameterName,OPER_NUM_VALIDATION))
        {
        	parameterValue = NOT_APPLICABLE;
        }
        
        if (stringEquals(configurationModuleName,upperCase(FOUNDATION)) && 
        		stringEquals(parameterName,OPER_NUM_VALIDATION) && 
                !stringEquals(parameterValue, NOT_APPLICABLE ))
        {
        	try
        	{
        	    Pattern.compile(parameterValue);
        	}
        	catch(Exception e)
        	{
        		// GE-5511
        		// Invalid Regular Expression
        		message.raiseError("MFI_FCF98170F0CA49A6930BD18CC349BE8F");
        	}
        }

        
        // 01/07/2008 Ganatra Utpal. FND-7275,
        // Don't Update Parameter Value if there is no change.
        if (!configurationExists)
        {
        	// Updates the Configuration record
    		globalConfigurationsDao.updateGlobalConfiguration(configurationModuleName,
    		                                                  parameterName,
    		                                                  parameterValue,
    		                                                  ContextUtil.getUsername());
    		
        	if(stringEquals(configurationModuleName, upperCase(FOUNDATION))
    				&& SET_REJECTED_COMP_LOT_STATUS_TO_STOP.equals(parameterName)
    				&& NO.equals(parameterValue))
        	{
        		globalConfigurationsDao.updateAsWorkedItemForExistingRecords(parameterValue);
        	}
            executeAfterGlobalParameterUpdate(configurationModuleName, parameterName, parameterValue);
        }
    }

	protected void executeAfterGlobalParameterUpdate(String configurationModuleName, String parameterName,
			String parameterValue) 
	{
		if (stringEquals(configurationModuleName,
				upperCase(FOUNDATION))
				&& stringEquals(parameterName,
						SECURITY_GROUP_FLAG))
		{
			// Change of this Global Parameter requires a restart of the app
			// server. Please contact your system administrator
			message.showMessage("MFI_C3524A2AEEAC45DCB752CBC349DDBF83");
		}
		else if (stringEquals(configurationModuleName,upperCase(FOUNDATION)) && 
				stringEquals(parameterName,WS_REQUEST_TIMEOUT_PARAM_NAME))
		{
			soluminaBeanService.updateWsRequestTimeout(Integer.parseInt(parameterValue) * 1000);
		}
		else if (stringEquals(configurationModuleName,upperCase(FOUNDATION)) && 
				stringEquals(parameterName,WS_SO_TIMEOUT_PARAM_NAME))
		{
			soluminaBeanService.updateWsSoTimeout(Integer.parseInt(parameterValue) * 1000);
		}
		else if (stringEquals(configurationModuleName,upperCase(FOUNDATION)) && 
				stringEquals(parameterName,WS_MAX_CONNECTIONS_PER_HOST_PARAM_NAME))
		{
			soluminaBeanService.updateWsMaxConnectionsPerHost(Integer.parseInt(parameterValue));
		}
		else
		{
			// To re-initialize package global parameter variables, users
			// must
			// logout then login.
			message.showMessage("MFI_87513");
		}

		if (stringEquals(configurationModuleName, upperCase(FOUNDATION))
				&& stringEquals(parameterName, MTNodeImpl.NODE_INACTIVITY_TIMEOUT_MILLIS)) {
			updateMTNodeBeanProperty(parameterValue);
		}
	}
	
    private void updateMTNodeBeanProperty(String parameterValue) {
        
        try{
            long nodeInactivityTimeout = Long.parseLong(parameterValue);
            this.mtNode.setInactiveNodeTimeout(nodeInactivityTimeout);
        }catch(Exception e) {
            logger.warn("Unable to update node inactivity timeout in bean. This may require server restart to make it effactive.");
        }
    }

    public void updateMailGlobalConfiguration(String configModuleName,
                                              String mailServer,
                                              String mailUser,
                                              String mailPassword,
                                              String mailSender,
                                              String mailProtocol)
    {
    	if(mailServer == null)
    	{
    		mailServer = NOT_APPLICABLE;
    	}
    	if(mailUser == null)
    	{
    		mailUser = NOT_APPLICABLE;
    	}
    	if(mailPassword == null)
    	{
    		mailPassword = NOT_APPLICABLE;
    	}
    	if(mailSender == null)
    	{
    		mailSender = NOT_APPLICABLE;
    	}
    	if(mailProtocol == null)
    	{
    		mailProtocol = NOT_APPLICABLE;
    	}
        // Updates the Configuration record
        globalConfigurationsDao.updateGlobalConfiguration(upperCase(FOUNDATION),
                                                          "MAIL_SERVER",
                                                          mailServer,
                                                          ContextUtil.getUsername());

        // Updates the Configuration record
        globalConfigurationsDao.updateGlobalConfiguration(upperCase(FOUNDATION),
                                                          "MAIL_USER",
                                                          mailUser,
                                                          ContextUtil.getUsername());

        // Updates the Configuration record
        globalConfigurationsDao.updateGlobalConfiguration(upperCase(FOUNDATION),
                                                          "MAIL_PASSWORD",
                                                          mailPassword,
                                                          ContextUtil.getUsername());

        // Updates the Configuration record
        globalConfigurationsDao.updateGlobalConfiguration(upperCase(FOUNDATION),
                                                          "MAIL_SENDER",
                                                          mailSender,
                                                          ContextUtil.getUsername());

        // Updates the Configuration record
        globalConfigurationsDao.updateGlobalConfiguration(upperCase(FOUNDATION),
                                                          "MAIL_PROTOCOL",
                                                          mailProtocol,
                                                          ContextUtil.getUsername());

        // To re-initialize package global parameter variables, users must
        // logout then login.
        message.showMessage("MFI_87513");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibaset.solumina.sffnd.application.IGlobalConfiguration#getParameterValue(java.lang.String,
     *      java.lang.String)
     */
    public String getParameterValue(String configurationModuleName,
                                    String parameterName)
    {
        // Initializes the Parameter Value with null.
        String parameterValue = null;

        try
        {
            // Gets the Parameter Value from Global Configuration.
            parameterValue = globalConfigurationsDao.selectParameterValue(configurationModuleName,
                                                                          parameterName);
        }
        catch (EmptyResultDataAccessException exception)
        {
            // Does Nothing when Parameter Value does not exist.
        }

        // Returns the Parameter Value.
        return parameterValue;
    }
    
    /* (non-Javadoc)
     * @see com.ibaset.solumina.sffnd.application.IGlobalConfiguration#updateDefaultWODecrepancyInformation(java.lang.String,
     * java.lang.String, java.lang.String, java.lang.String, java.lang.String)
     */
    public void updateDefaultWODecrepancyInformation(String configurationModuleName,
                                          			 String defaultWODiscrepancyTypeParam,
                                          			 String defaultWODiscrepancyTypeValue,
                                          			 String defaultWODiscrepancyWorkFlowParam,
                                          			 String defaultWODiscrepancyWorkFlowValue)
    {
        // Checks the user has privilege or not
        validator.checkUserPrivilege(USER_UPDATE_PRIV);
        
        validator.checkMandatoryField("Discrepancy Type", defaultWODiscrepancyTypeValue);
        validator.checkMandatoryField("Work Flow", defaultWODiscrepancyWorkFlowValue);
        
        // Updates the Configuration record
        globalConfigurationsDao.updateGlobalConfiguration(configurationModuleName,
        												  defaultWODiscrepancyTypeParam,
        												  defaultWODiscrepancyTypeValue,
                                                          ContextUtil.getUsername());
        
        // Updates the Configuration record
        globalConfigurationsDao.updateGlobalConfiguration(configurationModuleName,
        												  defaultWODiscrepancyWorkFlowParam,
        												  defaultWODiscrepancyWorkFlowValue,
                                                          ContextUtil.getUsername());
        
        // To re-initialize package global parameter variables, users
        // must
        // logout then login.
        message.showMessage("MFI_87513");
    	
    }

    /**
     * Sets the IMessage
     * 
     * @param message
     *            the IMessage
     */
    public void setMessage(IMessage message)
    {
        this.message = message;
    }

    /**
     * Sets the IValidator
     * 
     * @param validator
     *            the IValidator
     */
    public void setValidator(IValidator validator)
    {
        this.validator = validator;
    }

    /**
     * Sets the IGlobalConfigurationDao
     * 
     * @param globalConfigurationsDao
     *            the IGlobalConfigurationDao
     */
    public void setGlobalConfigurationsDao(IGlobalConfigurationDao globalConfigurationsDao)
    {
        this.globalConfigurationsDao = globalConfigurationsDao;
    }

    public boolean isMixedCasePasswordAllowed()
    {
        boolean mixedCaseAllowed = false;

        try
        {
            String value = globalConfigurationsDao.selectGlobalConfigurationParameterValue("CORE",
                                                                        "MIXED_CASE_PASSWORDS");
            if ("Y".equals(value))
            {
                mixedCaseAllowed = true;
            }
        }
        catch (IncorrectResultSizeDataAccessException irse)
        {
            mixedCaseAllowed = false;
        }

        return mixedCaseAllowed;
    }
    
    public void setUidEntry(IUidEntry uidEntry)
    {
      this.uidEntry = uidEntry;
    }
}
