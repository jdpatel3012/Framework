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

import java.text.ParseException;
import java.util.Date;

import org.apache.commons.beanutils.Converter;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;

import com.ibaset.common.FrameworkConstants;

public class DateConverter implements Converter
{

	private static final String[] parsePatterns = {FrameworkConstants.DATE_MASK, FrameworkConstants.DATE_MASK_NOTIME, FrameworkConstants.DATE_MASK_AM_PM};

	public Object convert(Class type, Object value)
	{
		Date convertedDate = (Date) null;
		
		String dateValue =  (String) value;
		
		if (value != null && StringUtils.isNotEmpty(dateValue) && !"null".equalsIgnoreCase(dateValue))
		{
			try
			{
				convertedDate = DateUtils.parseDate((String) value, parsePatterns);
			}
			catch (ParseException pe)
			{
				throw new RuntimeException("Unable to parse Object", pe);
			}
		}
		
		return convertedDate;
	}
}
