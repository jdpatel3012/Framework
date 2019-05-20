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
package com.ibaset.common.solumina;

import java.net.InetAddress;

import org.jgroups.stack.IpAddress;

public class ClusterMember implements IClusterMember 
{

	private IpAddress address;
	private String nodeId;
	
	public ClusterMember(IpAddress address) 
	{
		super();
		this.address = address;
	}
	
	public ClusterMember(IpAddress address, String nodeId) 
	{
		super();
		this.address = address;
		this.nodeId = nodeId;
	}

	@Override
	public InetAddress getAddress() 
	{
		return address.getIpAddress();
	}

	public String getNodeId() 
	{
		return nodeId;
	}

	public void setNodeId(String nodeId) 
	{
		this.nodeId = nodeId;
	}

	@Override
	public int getPort() 
	{
		return address.getPort();
	}

	@Override
	public int hashCode() 
	{
		return address.hashCode();
	}

	@Override
	public boolean equals(Object obj) 
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		ClusterMember other = (ClusterMember) obj;
		if (address == null) 
		{
			if (other.address != null)	return false;
		} 
		else if (!address.equals(other.address))return false;
		if (nodeId == null)
		{
			if (other.nodeId != null) return false;
		}
		else if (!nodeId.equals(other.nodeId))return false;
		return true;
	}

}
