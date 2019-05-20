/**
 * Proprietary and Confidential
 * Copyright 1995-2011 iBASEt, Inc.
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
package org.springframework.web.context.support;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanInstantiationException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.support.CglibSubclassingInstantiationStrategy;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.util.ClassUtils;

import com.ibaset.common.event.DynamicClassExtender;
import com.ibaset.common.event.ExtensionRegistry;

/**
 * Instantiation strategy creates extensions for original beans.
 */
public final class ExtensibleInstantiationStrategy extends CglibSubclassingInstantiationStrategy {
	private Map<Class<?>, Class<?>> extensionCache;
	private ExtensionRegistry registry;
	protected Logger logger = LoggerFactory.getLogger(this.getClass());
	
	public ExtensibleInstantiationStrategy() {
		super();
		this.extensionCache = Collections.synchronizedMap(new HashMap<Class<?>, Class<?>>(1000));
	}

	@Override
	public Object instantiate(RootBeanDefinition beanDefinition, String beanName, BeanFactory owner) {
		Class<?> beanClass = beanDefinition.getBeanClass();
		Class<?> extension = getExtension(beanClass);
		Object bean = null;
		try {
			if (extension != null) {
				bean = BeanUtils.instantiateClass(extension);
			} else {
				bean = super.instantiate(beanDefinition, beanName, owner);
			}
		} catch (RuntimeException e) {
			logger.error("Unable to create bean " + beanName, e);
			throw e;
		}
		return bean;
	}

	private Class<?> getExtension(Class<?> clazz) {
		if (clazz == null)
			return null;
		if (extensionCache.containsKey(clazz)) {
			return extensionCache.get(clazz);
		}
		synchronized (extensionCache) {
			if (registry == null) {
				registry = ExtensionRegistry.getInstance();
				String props = "extension.properties";
				try {
					registry.loadProperties(props);
				} catch (IOException e) {
					logger.error("Can't load extension registry from " + props, e);
				}
			}
		}
		Class<?>[] interfaces = ClassUtils.getAllInterfacesForClass(clazz);
		Class<?> extendedInterface = null;
		Class<?> extension = null;
		for (Class<?> i : interfaces) {
			extension = registry.findDynamicExtension(i.getName());

			if (extension != null) {
				extendedInterface = i;
				break;
			}
		}
		Class<?> newExtension = extension;
		try {
			while (newExtension != null) {
				extension = DynamicClassExtender.extend(clazz, extendedInterface, extension);
				newExtension = registry.findDynamicExtension(StringUtils.substringBefore(extension.getName(), "$$"));
				if (newExtension != null) {
					clazz = extension;
					extension = newExtension;
				}
			}
		} catch (IOException e) {
			throw new BeanInstantiationException(extension, "Can't create extension for " + clazz.getName(), e);
		}
		extensionCache.put(clazz,  extension);

		return extension;
	}
}
