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

import java.io.IOException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.text.ParseException;
import java.util.Date;
import java.util.Map;

import org.aopalliance.intercept.MethodInterceptor;
import org.apache.commons.beanutils.ConversionException;
import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.collections.map.ListOrderedMap;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.Advisor;
import org.springframework.aop.framework.Advised;

import com.ibaset.common.BinaryParameter;
import com.ibaset.common.Intercept;
import com.ibaset.common.OutputParameter;
import com.ibaset.common.client.SoluminaServiceLocator;
import com.ibaset.common.event.ExtensionInterceptor;
import com.ibaset.common.sql.ResultObject;
import com.ibaset.common.sql.security.SQLSecurityManager;
import com.ibaset.common.util.ClassUtils;
import com.ibaset.common.util.SoluminaUtils;
import com.ibaset.solumina.stat.Statistics;

public class MethodCall implements InvocationTarget {
	
	private static final Logger logger = LoggerFactory.getLogger(MethodCall.class);

	protected final Method method;
	protected final Object target;
	protected final MethodInterceptor interceptor;
	protected final SQLSecurityManager securityManager;
	protected final String[] methodParameterNames;
	protected Statistics statistics;
	private final String category;
	
	public MethodCall(SQLSecurityManager securityManager, Object target, Method method,String[] methodParameterNames) throws Exception{
		super();
		this.method = method;
		this.target = target;
    	Method annotatedMethod = method;
    	if (!annotatedMethod.isAnnotationPresent(Intercept.class))
    	{
        	//check annotation on implementation method too
    		Class<?> targetClass = resolveProxyClass(target);
    		annotatedMethod = 
    				targetClass!=null ? 
    				ClassUtils.findMethod(targetClass, method.getName(), method.getParameterTypes().length) : 
    				null;
    	}
    	if (annotatedMethod!=null && annotatedMethod.isAnnotationPresent(Intercept.class)) 
    	{
    		Intercept a=annotatedMethod.getAnnotation(Intercept.class);
    		Class<? extends MethodInterceptor> interceptorClass = a.value();
    		this.interceptor =
    			interceptorClass.isInterface() ?
    					(MethodInterceptor)SoluminaServiceLocator.locateService(interceptorClass):
    					interceptorClass.newInstance();
    	}else
    	{
    		this.interceptor = null;
    	}
    	this.securityManager = securityManager;
    	this.methodParameterNames = methodParameterNames;
    	StringBuilder mn = new StringBuilder();
    	mn.append(method.getDeclaringClass().getName()).append('.').append(method.getName()).append("()");
    	category = mn.toString();
	}
	
    @Override
	public boolean isJava() {
		return true;
	}

	public Statistics getStatistics() {
		return statistics;
	}

	public void setStatistics(Statistics statistics) {
		if(statistics.isEnabled()) this.statistics = statistics;
	}

	private static Class<?> resolveProxyClass(Object obj) throws Exception
    {
    	Object target=obj;
    	while(target instanceof Advised)
    	{
    		Advised proxy = (Advised)target;
   			target=proxy.getTargetSource().getTarget();
   			if(target==null)
   			{
   				for(Advisor advisor : proxy.getAdvisors())
   				{
   					if(advisor.getAdvice() instanceof ExtensionInterceptor)
   					{
   						ExtensionInterceptor ext = (ExtensionInterceptor)advisor.getAdvice();
   						return ext.getExtensionClass();
   					}
   				}
   			}
    	}
    	return target==null ? null : target.getClass();
    }
    public static Object[] convertParamsToStrings(String[] parameterNames, Map<String, BinaryParameter> paramVals) throws IOException
    {
        final Object[] finalParams = new Object[parameterNames.length];
        for (int i = 0; i < finalParams.length; i++)
        {
            Object o1 = null;
            String paramName = parameterNames[i];
            BinaryParameter bp = paramVals.get(paramName);
            o1= StringUtils.defaultIfEmpty(bp.getString(), null);
            if(o1 != null && "null".equals(o1)) o1 = null;
            finalParams[i] = o1;
        }    	
        return finalParams;
    }
    
    protected static Object[] convertParams(
    		Class<?>[] methodParameters, 
    		String[] parameterNames,
    		Map<String, BinaryParameter> paramVals) throws ParseException, IOException
    {
        final Object[] finalParams = new Object[methodParameters.length];
        for (int i = 0; i < finalParams.length; i++)
        {
            Object o1 = null;
            String paramName = parameterNames[i];
            BinaryParameter bp = paramVals.get(paramName);
            if(methodParameters[i].equals(String.class))
            {
                o1= StringUtils.defaultIfEmpty(bp.getString(), null);
                if(o1 != null && "null".equals(o1)) o1 = null;
            }
            else if(methodParameters[i].equals(Number.class))
            {
            	final String stringValue = StringUtils.defaultIfEmpty(bp.getString(), null);
            	if(stringValue != null && !"null".equals(stringValue))
            	{
            		o1 = new BigDecimal(SoluminaUtils.parseNumberLocaleSpecificToGeneral(stringValue)); // GE-3723
            	}
            }
            else if(methodParameters[i].equals(Date.class))
            {
            	final String stringValue = StringUtils.defaultIfEmpty(bp.getString(), null);
            	if(stringValue != null && !"null".equals(stringValue))
            	{
                	o1 = SoluminaUtils.parseDate(stringValue);
            	}
            }
            else if(methodParameters[i].equals(BinaryParameter.class))
            {
                o1 = paramVals.get(bp);
            }
            else if(methodParameters[i].equals(byte[].class))
            {
                o1 = bp.getByteArray();
            }
            else
            {
            	String stringValue = StringUtils.defaultIfEmpty(bp.getString(), null);
            	if(stringValue !=null && "null".equals(stringValue)) stringValue = null;
            	try {
               		o1 = ConvertUtils.convert(stringValue, methodParameters[i]);
               	} catch(ConversionException e) {
               		if(stringValue == null) {
               			logger.debug("Not able to convert null value into type: " + methodParameters[i] + ". Using null as converted value now.");
               			o1 = null;
               		} else {
               			logger.error("Conversion error occured while trying to convert value: " + stringValue + " into type: " + methodParameters[i]);
               			throw e;
               		}
               	}

            }
            finalParams[i] = o1;
        }
    	return finalParams;
    }

	@Override
	public Object[] convertParameters(String[] parameterNames,
									  Map<String, BinaryParameter> paramVals) throws Exception 
	{
		return convertParams(method.getParameterTypes(), parameterNames, paramVals);
	}
	
	@SuppressWarnings("unchecked")
	private ResultObject doInvoke(final Object[] args){
        Map<String, Object> outputParams = null;
        for (int i = 0; i < args.length; i++)
        {
            if (args[i] != null
                    && args[i].getClass()
                                     .equals(OutputParameter.class))
            {
            	if(outputParams == null) outputParams = new ListOrderedMap();
                outputParams.put(methodParameterNames[i],
                				 args[i]);
            }
        }
		ResultObject resultObject = null;
		try{
        	//turn SQL security off. It'll be turned on only if there is 
        	//a query to a protected object in the method's control flow.
        	securityManager.setProtectQueries(false);
        	securityManager.setThreadProtected(false);
			
    		Object obj = null;
			if(interceptor !=null)
			{
				SimpleMethodInvocation inv=new SimpleMethodInvocation(method, target, args);
				obj = interceptor.invoke(inv);
			}
			else
			{
				obj = method.invoke(target, args);
			}
	        boolean hasResult = (obj != null);
	        boolean hasOutputParams = outputParams!=null && !outputParams.isEmpty();

	        if(hasResult)
	        {
	        	if(!java.util.Map.class.isAssignableFrom(method.getReturnType()) &&
	        	   !java.util.Collection.class.isAssignableFrom(method.getReturnType()))
	        	{
	            	securityManager.setThreadProtected(false); //ignore non-collections result
	        	}
	        }
	        if (!hasResult && hasOutputParams)
	        {
	        	resultObject = new ResultObject(outputParams);
	        	securityManager.setThreadProtected(false); //ignore output params
	        }
	        else if (hasResult && hasOutputParams)
	        {
	        	resultObject = new ResultObject(obj, outputParams);
	        }
	        else if (hasResult && !hasOutputParams)
	        {
	            if (obj instanceof ResultObject)
	            {
	            	resultObject = (ResultObject) obj;
	            }
	            else 
	            {
	            	resultObject = new ResultObject(obj);
	            }
	        }
		}
		catch (Throwable e) 
		{
			resultObject = new ResultObject();
			resultObject.addError(e);
		}
		finally
		{
	    	if(resultObject!=null && securityManager.isThreadProtected())
	    	{
	    		securityManager.checkResultObject(resultObject);
	    	}
		}
        if(resultObject == null) resultObject = new ResultObject();
        return resultObject;
	}

	@Override
	public ResultObject invoke(final Object[] args) {
		ResultObject result = null; 
        if(statistics!=null){
        	String signature = null;
        	if(methodParameterNames.length>0){
        		StringBuilder sb = new StringBuilder();
                sb.append(method.getDeclaringClass().getName()).append('.');
                sb.append(method.getName()).append('(');
                sb.append(args[0]);
                for(int i=1;i<args.length;++i){
                	sb.append(',').append(args[i]);
                }
                sb.append(')');
                signature = sb.toString();
        	} else{
        		signature = category;
        	}
        	statistics.startCategory(signature).setCategoryGroup(category);
        }
		try{
			result = doInvoke(args);
			return result;
		}
		finally
		{
			if(statistics!=null) statistics.endCategory(result == null || result.hasErrors(), false);
		}
	}

}
