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
package com.ibaset.common.sql;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;


public interface IResultObjectEncoder
{

	public void renderResultObject(ResultObject object, OutputStream response) throws IOException;

	public void renderResultObjects(List<ResultObject> results, OutputStream response) throws IOException;

	public void renderResultObject(ResultObject object, OutputStream response, boolean useBinary) throws IOException;
	
	public void renderResultObject(ResultObject object, OutputStream response, boolean useBinary, String encodingType) throws IOException;

	public void renderResultObjects(List<ResultObject> results, OutputStream response, boolean useBinary) throws IOException;
	
	public void renderResultObjects(List<ResultObject> results, OutputStream response, boolean useBinary, String encodingType) throws IOException;	
	
    public String getResponseType();
}
