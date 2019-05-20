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
package com.ibaset.web.servlet.solumina;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;

import org.aopalliance.intercept.MethodInvocation;

public final class SimpleMethodInvocation implements MethodInvocation {

	private Method method;
	private Object obj;
	private Object[] arguments;
	
	public SimpleMethodInvocation(Method method, Object obj, Object[] arguments) {
		super();
		this.method = method;
		this.obj = obj;
		this.arguments = arguments;
	}

	public Method getMethod() {
		return method;
	}

	public Object[] getArguments() {
		return arguments;
	}

	public AccessibleObject getStaticPart() {
		return method;
	}

	public Object getThis() {
		return obj;
	}

	public Object proceed() throws Throwable {
		return method.invoke(obj, arguments);
	}

}
