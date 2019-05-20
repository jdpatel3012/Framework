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

import com.ibaset.common.BinaryParameter;
import com.ibaset.common.sql.security.SQLSecurityManager;

public class ConstructorCall extends MethodCall{
	private Constructor<?> ctor;
	
	public ConstructorCall(SQLSecurityManager securityManager, 
						   Object target, 
			               Method method, 
			               Constructor<?> ctor, 
			               String[] ctorParameterNames) throws Exception {
		super(securityManager, target, method,ctorParameterNames);
		this.ctor = ctor;
	}

	@Override
	public Object[] convertParameters(String[] paramNames,
									  Map<String, BinaryParameter> paramVals) throws Exception 
	{
        Object[] finalParams = convertParams(ctor.getParameterTypes(), paramNames, paramVals);
    	Object param=ctor.newInstance(finalParams);
		return new Object[]{param};
	}

}
