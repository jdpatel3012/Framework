/**
 * Proprietary and Confidential
 * Copyright 1995-2012 iBASEt, Inc.
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

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibaset.common.client.SoluminaServiceLocator;
import com.ibaset.common.solumina.exception.SoluminaException;
import com.ibaset.common.sql.IPassThroughProc;
import com.ibaset.common.sql.IPassThroughQuery;
import com.ibaset.common.sql.ResultObject;
import com.ibaset.common.sql.security.SQLSecurityManager;
import com.ibaset.common.util.ClassUtils;
import com.ibaset.solumina.stat.Statistics;

/**
 * Class responsible for calling methods
 * 
 * */
public class Gateway 
{
    private SQLSecurityManager securityManager;
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private IPassThroughProc passThroughProc;
    private IPassThroughQuery passThroughQuery;
    private Statistics statistics;
	

	public Gateway(SQLSecurityManager securityManager,
			IPassThroughQuery passThroughQuery, IPassThroughProc passThroughProc,Statistics statistics) {
		super();
		this.securityManager = securityManager;
		this.passThroughQuery = passThroughQuery;
		this.passThroughProc = passThroughProc;
		this.statistics = statistics;
	}

	protected InvocationTarget createInvocationTarget(String objectName, 
													  String procedureName,
													  String[] procedureParameterNames) throws Exception
	{
        Object invokableService = null;
        Class<?> interfaceClass = null;
        if(!StringUtils.isEmpty(objectName))
        {
	        try
	        {
	        	interfaceClass = Class.forName(objectName);
	            invokableService = SoluminaServiceLocator.locateService(interfaceClass);
	        }
	        catch (Exception e)
	        {
                logger.error("unable to find bean for " + objectName, e);
	        }
        }
        if(invokableService != null)
        {
        	//Java method
        	return createInvocationTarget(invokableService, interfaceClass, procedureName, procedureParameterNames);
        }		
        if (StringUtils.isNotEmpty(procedureName))
        {
            // this must be a PL/SQL code (not converted yet)
            if (!StringUtils.isEmpty(objectName))
            {
                procedureName = objectName + "." + procedureName;
            }
            return new ProcedureCall(passThroughProc, procedureName, procedureParameterNames);
        }

        else
        {
            // This is for sure an error null proc can not be
            // handled
            throw new SoluminaException("A Procedure or Function should have a name !!");
        }
        
	}
	
	protected InvocationTarget createInvocationTarget(Object o, Class<?> interfaceClass, String methodName, String[] procedureParameterNames) throws Exception {
		final int paramNumber = procedureParameterNames.length;
    	Method method = ClassUtils.findMethod(interfaceClass, methodName, paramNumber);
    	MethodCall result = null;
        if (method!=null)
        {
        	result = new MethodCall(securityManager, o, method, procedureParameterNames);
        }
        else
        {
	    	//try to find method with an object parameter
	    	method = ClassUtils.findMethod(interfaceClass, methodName, 1);
	        if(method != null)
	        {
	        	//try to find matching constructor 
	        	Constructor<?>[] ctors=method.getParameterTypes()[0].getConstructors();
	        	Constructor<?> ctor=null;
	            for (int i = 0; i < ctors.length; i++)
	            {
	                    if (ctors[i].getParameterTypes().length == paramNumber)
	                    {
	                    	ctor=ctors[i];
	                    	break;
	                    }
	            }
	            if(ctor != null){
	            	result = new ConstructorCall(securityManager, o, method, ctor, procedureParameterNames);
	            }
	        }
        }
        if(result != null)result.setStatistics(statistics);
        //cause an exception
        else ClassUtils.getMethod(interfaceClass, methodName, paramNumber);
    	return result;
	}
	    	
    /**
     * @param sql
     * @param sessionId
     * @param startRow
     * @param endRow
     * @param levelOneSortKeys
     * @param levelTwoSortKeys
     * @param bindSet
     * @param bindVariables
     * @return ResultObject
     */
    protected ResultObject callSqlQuery(String sql,
                                      String sessionId,
                                      int startRow,
                                      int endRow,
                                      Map<String, String> levelOneSortKeys,
                                      Map<String, String> levelTwoSortKeys,
                                      String bindSet,
                                      Map<String, String> bindVariables)
    {
        ResultObject result = null;
        boolean flag = securityManager.isProtectQueries();
        try
        {
        	securityManager.setProtectQueries(true);        	
            result = passThroughQuery.executeQuery(sql,
                                                   startRow,
                                                   endRow,
                                                   levelOneSortKeys,
                                                   levelTwoSortKeys,
                                                   bindSet,
                                                   bindVariables);
        }
        catch (Exception e)
        {
        	logger.error("Error executing passthrough query", e);
        	result = new ResultObject();
        	result.addError(e);
        }
        finally
        {
        	securityManager.setProtectQueries(flag);        	
        }

        return result;
    }
}
