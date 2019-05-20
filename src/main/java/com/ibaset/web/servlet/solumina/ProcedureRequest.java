/**
 * Proprietary and Confidential
 * Copyright 1995-2013 iBASEt, Inc.
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
package com.ibaset.web.servlet.solumina;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DeadlockLoserDataAccessException;

import com.ibaset.common.BinaryParameter;
import com.ibaset.common.client.SoluminaServiceLocator;
import com.ibaset.common.context.SoluminaContextHolder;
import com.ibaset.common.security.context.ContextUtil;
import com.ibaset.common.security.context.RequestContext;
import com.ibaset.common.security.context.UserContext;
import com.ibaset.common.sql.ResultObject;
import com.ibaset.common.util.ProgressTracker;
import com.ibaset.solumina.sfcore.application.IMessage;
import com.ibaset.solumina.sfcore.application.ISoluminaSavePoint;

public final class ProcedureRequest extends SoluminaRequest {

    protected static final Logger logger = LoggerFactory.getLogger(ProcedureRequest.class);
    static ISoluminaSavePoint soluminaSavePoint = null;
    static Gateway gateway;
    static boolean disableSavepoints;
    static long maxDeadlockDelay = 1000;
    static int deadlockRetries = 3;
    
	public ProcedureRequest(Map<String, BinaryParameter> requestParameterMap,
			HttpServletRequest request, HttpServletResponse response)	throws IOException 
	{
		super(requestParameterMap, request, response);
	}
	public boolean execute() throws Exception
	{
    	ArrayList<ResultObject> results = new ArrayList<ResultObject>();
        BinaryParameter p = requestParameterMap.get(OBJECT_NAME);
        String charsetName = p.getCharset();
    	String objectName = p.getString();
    	
        if (logger.isDebugEnabled())
        {        	
            UserContext ctx = SoluminaContextHolder.getUserContext();
            logger.debug("Procedure request begin: object_name=" + objectName + 
            		", procedure_name=" + procedureName +
            		", solumina_connection_id=" + ctx.getConnectionId() +
            		", locale=" + ctx.getLocale() +", sessionId="+sessionId);
        }
    	
    	ResultObject resultObject = null;
    	try 
    	{
    		resultObject = prepareProcedure(objectName, results);
    	} 
    	finally
    	{
    		ContextUtil.setRequestContext(null);    		
    	}
    	boolean isCommit = true;
    	if(resultObject!=null)
    	{
    		reportDeadlockError(resultObject);
            writeResultObject(  resultObject,
		                        true,
		                        charsetName);
            boolean hasErrors = resultObject.hasErrors();
            if(hasErrors){
            UserContext ctx = SoluminaContextHolder.getUserContext();
            StringBuilder logEntry = new StringBuilder();
            logEntry.append("Procedure request returned errors:\n")
            .append("objectName=").append(objectName).append("\n")
            .append("procedureName=").append(procedureName).append("\n")
            .append("ctx.getConnectionId()=").append(ctx.getConnectionId()).append("\n")
            .append("ctx.getLocale()=").append(ctx.getLocale()).append("\n")
            .append("sessionId=").append(sessionId).append("\n")
            .append("Number of Errors=").append(resultObject.getErrors().size()).append("\n");
            // Loop through errors and add to log entry in easily readable format
            int errornum = 0;
            for(Throwable error : resultObject.getErrors()){
                errornum++;
                logEntry.append("=================================================\n")
                .append("Error ").append(errornum).append(" of ").append(resultObject.getErrors().size()).append("\n")
                .append("Error cause: ").append(error.getCause()).append("\n")
                .append("Error message: ").append(error.getMessage()).append("\n")
                .append("Stack Trace:\n");
                for(StackTraceElement el : error.getStackTrace()){
                       logEntry.append(el.toString()).append("\n");
                }
                logEntry.append("=================================================\n");
        }
        logger.error(logEntry.toString());
                  
        }
        isCommit = !hasErrors;
            
    	}
    	else
    	{
        	writeResultObjects(results, charsetName);
    	}
    	return isCommit;
    }

	private static void reportDeadlockError(ResultObject resultObject)
	{
        final boolean hasErrors = resultObject.hasErrors(); 
        if(hasErrors) 
        {
        	Throwable error = resultObject.getErrors().get(0);
        	if(isDeadlockError(error))
        	{
        		IMessage message = SoluminaServiceLocator.locateService(IMessage.class);
        		try 
        		{
        			message.raiseError("MFI_D03B2C0525A747E4BEA0C21D91E5A018");
        		} 
        		catch (Exception e) 
        		{
        		    logger.debug(e.getMessage(), e);
        			resultObject.getErrors().clear();
        			resultObject.addError(e);
				}
        	}
        }
	}
	private static boolean isDeadlockError(Throwable error)
	{
		if(error instanceof DeadlockLoserDataAccessException) return true;
    	String errorMessage = error.getMessage();
    	final boolean result = errorMessage!=null && 
    		   (errorMessage.toLowerCase().contains("rerun the transaction") || 
    			errorMessage.contains("ORA-00060"));
    	if(!result && error.getCause()!=null) return isDeadlockError(error.getCause());
		return result;
	}
	    
    private String[] getKeyNames(String parameterName) throws IOException{
        // getting the parameter's names
    	String keySet = getString(requestParameterMap, parameterName);
        if (keySet != null && keySet.trim().length() > 0)
        {
        	return keySet.split("\\,");
        }  
        return new String[0];              
    }
    
    private ResultObject prepareProcedure(
    						String objectName,
    						ArrayList<ResultObject> results) throws Exception
    {
    	String methodName = null;
        // Extracting the procedure name when it is a java method
        if (
        		(
        		"com".equals(objectName) || 
        		"java".equals(objectName) ||
        		"net".equals(objectName) ||
        		"org".equals(objectName) 
        		)
                && StringUtils.contains(procedureName, "."))
        {
            String[] nameArray = StringUtils.split(procedureName, ".");
            objectName += ".";
            for (int i = 0; i < nameArray.length - 1; i++)
            {
                objectName += nameArray[i];
                if (i < nameArray.length - 2)
                {
                    objectName += ".";
                }
            }
            methodName = nameArray[nameArray.length - 1];
        }
        else
        {
        	methodName = procedureName;
        }
             
        // getting the parameter's names
    	String[] keyNames = getKeyNames(KEY_SET);
    	String[] auxKeyNames = getKeyNames(AUX_KEY_SET);
    	
        String batchStr = getString(requestParameterMap, BATCH);
        int batch = 0;
        try {
            if (!StringUtils.isEmpty(batchStr))
                batch = Integer.parseInt(batchStr);
        } catch (Exception e) {
            logger.debug(e.getMessage(), e);
        }
    	RequestContext ctx = new RequestContext();
    	ContextUtil.setRequestContext(ctx);
    	ctx.setBatchSize(batch == 0? 1 : batch);
    	ctx.setMethodName(methodName);
    	ctx.setObjectName(objectName);
    	ctx.setParameterNames(keyNames);
    	ctx.setAuxParameterNames(auxKeyNames);
        
        final boolean setSavepoint = !disableSavepoints && !isSystemProc();
        final InvocationTarget target = gateway.createInvocationTarget(objectName, methodName, keyNames);
        
        if(batch == 0)
        {
	        Object[][] arguments = new Object[1][keyNames.length];
	        arguments[0] = target.convertParameters(keyNames, requestParameterMap);
	        ctx.setBatchParameters(arguments);
	        Object[][] auxArguments = new Object[1][auxKeyNames.length];
	        auxArguments[0] = MethodCall.convertParamsToStrings(auxKeyNames, requestParameterMap);
	        ctx.setAuxBatchParameters(auxArguments);
	        
	        int retries = 0;
	        ResultObject resultObject = null;
	        do
	        {
		        if(setSavepoint) soluminaSavePoint.setSavepoint();
		        resultObject = target.invoke(arguments[0]);
		        final boolean hasErrors = resultObject.hasErrors(); 
		        if(setSavepoint && hasErrors) 
		        {
		        	soluminaSavePoint.rollbackSavepoint();
	            	Throwable error = resultObject.getErrors().get(0);
	            	if(isDeadlockError(error))
	            	{
	                    //retry transaction with deadlock error 3 times 
	            		// with increasing random delays
	            		if(++retries > deadlockRetries) break;
	            		Thread.sleep(retries * (long)(Math.random() * maxDeadlockDelay));
	            		continue;
	            	}
		        }
		        break;
	        } while(true);
	        return resultObject;
    	}
        else
        {
        	ResultObject errorResult = null;
        	ProgressTracker.setProgressMaximum(batch);
        	ProgressTracker.setProgress(0);
	        Object[][] batchArguments = new Object[batch][keyNames.length];
	        Object[][] auxArguments = new Object[batch][auxKeyNames.length];
        	//prepare arguments for batch execution
        	for(int b=1;b<=batch;++b)
        	{
                HashMap<String, BinaryParameter> byteParameterValues=new HashMap<String, BinaryParameter>();
                for (int i = 0; i < keyNames.length; i++)
                {
                	final String paramName = keyNames[i]+"_"+b;
                	final BinaryParameter paramValue = requestParameterMap.get(paramName);
                    byteParameterValues.put(keyNames[i], paramValue);
                }
                batchArguments[b-1] = target.convertParameters(keyNames, byteParameterValues);
                byteParameterValues.clear();
                for (int i = 0; i < auxKeyNames.length; i++)
                {
                	final String paramName = auxKeyNames[i]+"_"+b;
                	final BinaryParameter paramValue = requestParameterMap.get(paramName);
                    byteParameterValues.put(auxKeyNames[i], paramValue);
                }
                auxArguments[b-1] = MethodCall.convertParamsToStrings(auxKeyNames, byteParameterValues);
        	}
        	ctx.setBatchParameters(batchArguments);
        	ctx.setAuxBatchParameters(auxArguments);
        	//batch execution
        	for(int b=0;b<batch;++b)
        	{
        		ctx.setCurrentBatchIndex(b);
    	        if(setSavepoint) soluminaSavePoint.setSavepoint();
        		ResultObject resultObject = target.invoke(batchArguments[b]);
        		final boolean hasErrors = resultObject.hasErrors();
    	        if(setSavepoint && hasErrors) soluminaSavePoint.rollbackSavepoint();
                //check global error
                if(hasErrors)
                {
                	Throwable error = resultObject.getErrors().get(0);
                	String errorMessage = error.getMessage();
                	if( isDeadlockError(error) ||
                		error instanceof Error ||
                	   (errorMessage!=null && errorMessage.contains("Operation aborted")))
                	{
                		errorResult = resultObject;
                		break;
                	}
                }
                assignContext(resultObject, sessionId, true);
        		results.add(resultObject);
            	ProgressTracker.setProgress(b);
        	}
        	if(errorResult!=null)
        	{
        		results.clear();
        		return errorResult;
        	}
        }
        return null;
    }
    
    void writeResultObjects(ArrayList<ResultObject> results, 
    						String charsetName) throws IOException
    {
        response.setContentType(resultObjectEncoder.getResponseType());
        OutputStream sos = response.getOutputStream();
        if (canGzip)
        {
            sos = new GZIPOutputStream(sos);
        }
        if (logger.isDebugEnabled())
        {
       		logger.debug("request end: Results="+results.size());
        }
        if(results.size()>0) results.get(0).setCharsetName(charsetName);
        resultObjectEncoder.renderResultObjects(results, sos, useBinaryXml, encodingType);
    }

	
}
