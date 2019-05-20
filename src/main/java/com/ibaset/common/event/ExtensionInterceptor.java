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

/**
 * Interface of a Solumina extension interceptor
 * @since 4400
 * */
public interface ExtensionInterceptor 
{
	/**
	 * Returns extension class
	 * */
	Class getExtensionClass();
	/**
	 * Returns original object
	 * */
	Object getOriginalBean();
	/**
	 * Returns extension object
	 * */
	Object getOrCreateExtension() throws Exception;
}
