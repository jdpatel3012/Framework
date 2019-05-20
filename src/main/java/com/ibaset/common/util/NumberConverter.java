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
package com.ibaset.common.util;

import org.apache.commons.beanutils.Converter;
import org.apache.commons.lang.math.NumberUtils;

public class NumberConverter implements Converter
{

	public Object convert(Class type, Object value)
	{
		if(NumberUtils.isNumber((String) value))
		{
			return org.springframework.util.NumberUtils.parseNumber((String) value, type);
		}
		else
		{
			return null;
		}
	}

}
