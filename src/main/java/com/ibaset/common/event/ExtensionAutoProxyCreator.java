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
package com.ibaset.common.event;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.framework.autoproxy.AbstractAutoProxyCreator;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.util.ClassUtils;

import com.ibaset.common.client.SoluminaServiceLocator;
import com.ibaset.common.solumina.exception.SoluminaException;

public class ExtensionAutoProxyCreator extends AbstractAutoProxyCreator 
	implements InitializingBean, BeanFactoryPostProcessor
{
    private static final long serialVersionUID = 3552572922280039176L;
    
    private Class<?>[] extendedInterfaces;

    private final transient Set targetBeans = Collections.synchronizedSet(new HashSet());
    
    protected Object[] getAdvicesAndAdvisorsForBean(Class beanClass,
                                                    String beanName,
                                                    TargetSource customTargetSource)
    {
    	for(int i=0;i<extendedInterfaces.length;++i)
    	{
            if (extendedInterfaces[i].isAssignableFrom(beanClass))
            {
                return PROXY_WITHOUT_ADDITIONAL_INTERCEPTORS;
            }
    	}
        return DO_NOT_PROXY;
    }
    
    public void setEventProperties(Properties eventProperties)
    {
    	ExtensionRegistry registry = ExtensionRegistry.getInstance();
    	if(registry.getExtendedInterfaces() == null)
    	{
	    	try 
	    	{
				ExtensionRegistry.getInstance().loadProperties(eventProperties);
			} 
	    	catch (IOException e) 
	    	{
				logger.error("Unable to load extension properties", e);
			}
    	}
    }

	public void afterPropertiesSet() throws Exception 
    {
        extendedInterfaces=ExtensionRegistry.getInstance().getExtendedInterfaces();
	}
	
	
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
		beanFactory.addBeanPostProcessor(this);
	}

	/*
	 * Creates an extension proxy for objects that implement an extended interface. 
	 * Creates a transactional proxy if the object is already proxied
     * @see org.springframework.aop.framework.autoproxy.AbstractAutoProxyCreator#postProcessAfterInitialization(java.lang.Object,
     *      java.lang.String)
     */
	@Override
    public Object postProcessAfterInitialization(Object bean, String beanName)
    {

    	Object targetBean=bean;
    	while(targetBean instanceof Advised){
    		try
    		{
    			targetBean=((Advised)targetBean).getTargetSource().getTarget();
    		} 
    		catch (Exception e) 
    		{
				logger.warn("Unable to get target for "+bean, e);
				break;
			}
    	}
        if (targetBean==null || this.targetBeans.contains(targetBean)) {
        	return bean;
        }
        Class beanClass = targetBean.getClass();
        if (isInfrastructureClass(beanClass) || shouldSkip(beanClass, beanName))
        {
            return bean;
        }

        Class[] interfaces = ClassUtils.getAllInterfacesForClass(beanClass);
        ExtensionRegistry registry = ExtensionRegistry.getInstance();
        for (int i = 0; i < interfaces.length; i++)
        {
        	Class<?> extensionClass = registry.findProxyExtension(interfaces[i].getName());
            if (extensionClass!=null)
            {
                try
                {
               		targetBeans.add(targetBean);
                    ProxyFactory factory = new ProxyFactory(ClassUtils.getAllInterfacesForClass(extensionClass));
                   	SoluminaServiceLocator.addTransactionInterceptor(factory, false);
                    
                    ExtensionMethodInterceptor inter = new ExtensionMethodInterceptor(extensionClass);
                    inter.setOriginalBean(bean);
                    factory.addAdvice(inter);
                    logger.debug("Creating extension proxy for "+bean+"="+extensionClass.getName());
                    
                    return factory.getProxy(); 
                }
                catch (Exception e)
                {
                    logger.error("error trying to add an extension proxy for bean "
                            + bean
                            + " customer class name "
                            + extensionClass.getName(), e);
                    this.targetBeans.remove(targetBean);
                }
            }
        }
        return bean;
    }

    private class ExtensionMethodInterceptor implements MethodInterceptor, ExtensionInterceptor
    {

        private Object extensionBean = null;
        private Class extensionClass;
        private Object originalBean;

        ThreadLocal alreadyCalledForThread = new ThreadLocal()
        {
        	@Override
            protected synchronized Object initialValue()
            {
                return new ArrayList();
            }
        };

        public ExtensionMethodInterceptor(Class extensionClass)
        {
            this.extensionClass = extensionClass;
        }

        public void setOriginalBean(Object bean)
        {
            this.originalBean = bean;
        }

		public Class getExtensionClass() 
		{
			return extensionClass;
		}

		public Object getOriginalBean() 
		{
			return originalBean;
		}

		private Object invokeInternal(Method m, Object bean, Object[] args)
		{
			try {
				return m.invoke(bean, args);
			} catch (InvocationTargetException ite) {
				throw new SoluminaException(ite.getCause().getMessage(), ite);
			} catch (Exception t) {
				Throwable throwable = t;
				while (t.getCause() != null) {
					throwable = t.getCause();
				}
				Exception e = (Exception) throwable;
				
				throw new SoluminaException(e.getMessage(), e);
			}
		}

        public synchronized Object getOrCreateExtension() throws Exception
        {
        	if(extensionBean==null) 
        	{
        		Object targetBean = SoluminaServiceLocator.locateService(extensionClass);
            	while(targetBean instanceof Advised)
            	{
            		extensionBean = targetBean;
           			targetBean=((Advised)targetBean).getTargetSource().getTarget();
            	}
            	if(targetBean!=null) {
            		extensionBean = targetBean;
            	}
        	}
        	return extensionBean;
        }
        public Object invoke(MethodInvocation invocation) throws Throwable
        {

            Object[] args = invocation.getArguments();
            Method m = invocation.getMethod();
            if (((ArrayList) alreadyCalledForThread.get()).contains(m))
            {
                return invokeInternal(m, originalBean, args);
            }
            else
            {

                Class interfaceToBeCalled = m.getDeclaringClass();
                Object extension = getOrCreateExtension();
                if(interfaceToBeCalled.isInstance(extension))
                {
                    ((ArrayList) alreadyCalledForThread.get()).add(m);
                    try
                    {
                    	return invokeInternal(m, extension, args);
                    }
                    finally
                    {
                    	((ArrayList) alreadyCalledForThread.get()).remove(m);
                    }
                } 
                else
                {
                	return invokeInternal(m, originalBean, args);
                }
            }

        }

    }
}