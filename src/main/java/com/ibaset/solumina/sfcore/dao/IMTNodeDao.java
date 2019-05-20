/**
 * Proprietary and Confidential
 * Copyright 1995-2014 iBASEt, Inc.
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
package com.ibaset.solumina.sfcore.dao;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

public interface IMTNodeDao {
	
	void registerNode(String nodeId, String machineName, String ipAddress);
	
	void updateNodeLastActivityTime(String nodeId);
	
	void unregisterNode(String nodeId);
	
	void removeInactiveNodes(Timestamp inactiveSince);
	
	boolean isNodeIdRegistered(String nodeId);
	
	List<String> selectConnectionIdsFromInactiveNodes(Timestamp inactiveSince);
	
	List<String> selectConnectionIds(String mtNodeId);
	
	List<String> selectDanglingConnectionIds();

	//FND-28175
	public Date selectDBTimeStamp();
	
}
