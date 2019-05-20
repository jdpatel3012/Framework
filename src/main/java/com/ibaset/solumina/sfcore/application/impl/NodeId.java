package com.ibaset.solumina.sfcore.application.impl;

import java.io.Serializable;

import org.jgroups.stack.IpAddress;

public class NodeId implements Serializable {

	private static final long serialVersionUID = -1818084857506236641L;
	private IpAddress nodeAddress;
	private String nodeId;
	
	public NodeId() {
	}

	public IpAddress getNodeAddress() {
		return nodeAddress;
	}

	public void setNodeAddress(IpAddress nodeAddress) {
		this.nodeAddress = nodeAddress;
	}

	public String getNodeId() {
		return nodeId;
	}

	public void setNodeId(String nodeId) {
		this.nodeId = nodeId;
	}

}
