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

import java.util.Map;

import com.ibaset.common.BinaryParameter;
import com.ibaset.common.sql.ResultObject;

public interface InvocationTarget {

	public boolean isJava();
	public ResultObject invoke(Object[] args) ;
	
	public Object[] convertParameters(String[] parameterNames,
									  Map<String, BinaryParameter> parameterValues) throws Exception;
}
