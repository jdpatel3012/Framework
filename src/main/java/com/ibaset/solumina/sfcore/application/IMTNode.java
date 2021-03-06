/**
 * Proprietary and Confidential
 * Copyright 1995-2015 iBASEt, Inc.
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
package com.ibaset.solumina.sfcore.application;

public interface IMTNode {

	public abstract void nodeHeartbeat();
	public abstract void cleanupIdleNodes();
	public String getNodeId();
	public void registerNode();
	public void unregisterNode();
	public void removeDanglingConnections();
	
	public String getIpAddress();
	
	public String getMachineName();
	
	public void setInactiveNodeTimeout(long inactiveNodeTimeout) ;
	public void setContextInitialized(boolean contextInitialized);
}
