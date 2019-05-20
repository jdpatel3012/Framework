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
package com.ibaset.solumina.sfcore.application.impl;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jgroups.Address;
import org.springframework.beans.factory.InitializingBean;

import com.ibaset.common.Reference;
import com.ibaset.common.client.SoluminaServiceLocator;
import com.ibaset.common.solumina.ClusterEvent;
import com.ibaset.common.solumina.IClusterEventListener;
import com.ibaset.common.solumina.ISoluminaCluster;
import com.ibaset.solumina.sfcore.application.ILogin;
import com.ibaset.solumina.sfcore.application.IMTNode;
import com.ibaset.solumina.sfcore.application.ITransactionManager;
import com.ibaset.solumina.sfcore.dao.IMTNodeDao;
import com.ibaset.solumina.sffnd.dao.IGlobalConfigurationDao;

public class MTNodeImpl implements IMTNode, IClusterEventListener, InitializingBean {

    public static final String NODE_INACTIVITY_TIMEOUT_MILLIS = "NODE_INACTIVITY_TIMEOUT_MILLIS";
    
	private IMTNodeDao mtNodeDao;
	/**
	 * Unique MT node id
	 * */
	private String nodeId = null;
	private String ipAddress = null;
	private String machineName = null;
	private Logger logger = LoggerFactory.getLogger(this.getClass());
	private long inactiveNodeTimeout;
	@Reference
	private ISoluminaCluster soluminaCluster;
	@Reference
	private ILogin login;
	@Reference
	private ITransactionManager transactionManager;
	private String connectionCleanerUser = "CONNECTION_CLEANER";
	private boolean contextInitialized = false;
	
	@Reference
	private IGlobalConfigurationDao globalConfigurationDao;
	
	public MTNodeImpl() 
	{
		soluminaCluster = (ISoluminaCluster) SoluminaServiceLocator.locateService(ISoluminaCluster.class);

		if (soluminaCluster != null && soluminaCluster.getLocalNodeId() != null)
		{
			nodeId = soluminaCluster.getLocalNodeId();
			soluminaCluster.addClusterEventListener(this);
		}
		else
		{
			nodeId = UUID.randomUUID().toString();
		}
		
		try 
		{
			machineName = InetAddress.getLocalHost().getHostName();
			ipAddress = InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			//do nothing
		}
		if (logger.isDebugEnabled())
		{
			logger.debug("MT NodeID: "+nodeId+" MachineName: "+machineName+" IPAddress: "+ipAddress);
		}
	}
	
	public String getNodeId() 
	{
		return nodeId;
	}
	
	public synchronized void registerNode()
	{
		if (!mtNodeDao.isNodeIdRegistered(nodeId))
		{
			mtNodeDao.registerNode(nodeId, machineName, ipAddress);
			login.setNodeId(nodeId);
			soluminaCluster.setLocalNodeId(nodeId);
			if (logger.isDebugEnabled())
			{
				logger.debug("Registering Node: "+nodeId+" MachineName: "+machineName+" IPAddress: "+ipAddress);
			}
		}
	}
	
	public void unregisterNode()
	{
		List<String> thisNodeConnections = mtNodeDao.selectConnectionIds(nodeId);
		for (int i=0; i<thisNodeConnections.size(); ++i)
		{
			String connectionId = thisNodeConnections.get(i);
			try
			{
				transactionManager.kill(connectionId);
			} 
			catch(Exception ex)
			{
				logger.error("Error cleaning " + connectionId, ex);
			}
			if (logger.isDebugEnabled())
			{
				logger.debug("Unregistered connection killed: "+connectionId);
			}
		}
		mtNodeDao.unregisterNode(nodeId);
		if (logger.isDebugEnabled())
		{
			logger.debug("Unregistering Node: "+nodeId+" MachineName: "+machineName+" IPAddress: "+ipAddress);
		}
	}

	/* (non-Javadoc)
	 * @see com.ibaset.solumina.sfcore.application.impl.IMTNode#nodeHeartbeat()
	 */
	@Override
	public void nodeHeartbeat()
	{
		if (isContextInitialized()) {
			try {
				/*
				 * GE-1967: Now nodeHeartbeat will only update last activity timestamp for node.
				 * Idle node connections will be removed by nodeCleanupCronJob
				 */

				registerNode();
				mtNodeDao.updateNodeLastActivityTime(nodeId);
			} catch (Exception ex) {
				logger.error("Node heartbeat failure.", ex);
			}
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.ibaset.solumina.sfcore.application.IMTNode#cleanupIdleNodes()
	 */
	@Override
    public void cleanupIdleNodes()
    {
        try
        {
            long time = mtNodeDao.selectDBTimeStamp().getTime() - inactiveNodeTimeout;
            Timestamp inactiveSince = new Timestamp(time);
            List<String> connections = mtNodeDao.selectConnectionIdsFromInactiveNodes(inactiveSince);
            for(int i=0; i<connections.size(); ++i)
            {
                String connectionId = connections.get(i);
                try
                {
                    transactionManager.kill(connectionId);
                } 
                catch(Exception ex)
                {
                    logger.error("Error cleaning " + connectionId, ex);
                }
                if (logger.isDebugEnabled())
                {
                    logger.debug("Inactive connection killed: "+connectionId);
                }
            }
            mtNodeDao.removeInactiveNodes(inactiveSince);
        } 
        catch (Exception ex)
        {
            logger.error("Node cleanup failure.", ex);
        }
    }
	
	public void removeDanglingConnections()
	{
		if (logger.isDebugEnabled()) 
		{
			logger.debug("Removing dangling connections: BEGIN...");
		}
		
		List<String> connections = mtNodeDao.selectDanglingConnectionIds();
		for(int i=0; i<connections.size(); ++i)
		{
			String connectionId = connections.get(i);
			try
			{
				login.logout(connectionCleanerUser, connectionId);
			} 
			catch(Exception ex)
			{
				logger.error("Error cleaning " + connectionId, ex);
			}
			if (logger.isDebugEnabled()) 
			{
				logger.debug("Removing dangling connection: "+connectionId);
			}
		}
		
		if (logger.isDebugEnabled()) 
		{
			logger.debug("Removing dangling connections: DONE...");
		}
	}

	@Override
	public void handleClusterEvent(ClusterEvent event) 
	{
		if (soluminaCluster.isCoordinator())
		{
			if (event.getType() == ClusterEvent.MEMBER_LEFT)
			{
				String leftAddressString = ((Address) event.getData()).toString();
				String[] tok = leftAddressString.split(":", 2);
				String leftNodeId = tok[tok.length-1];
				if (mtNodeDao.isNodeIdRegistered(leftNodeId))
				{
					List<String> connections = mtNodeDao.selectConnectionIds(leftNodeId);
					for(int i=0; i<connections.size(); ++i)
					{
						String connectionId = connections.get(i);
						try
						{
							login.logout(connectionCleanerUser, connectionId);
						} 
						catch(Exception ex)
						{
							logger.error("Error cleaning " + connectionId, ex);
						}
					}
					mtNodeDao.unregisterNode(leftNodeId);
				}
				if (logger.isDebugEnabled()) 
				{
					logger.debug("Cluster member left and node unregistered. ClusterAddress: "+leftAddressString+" NodeId: "+leftNodeId);
				}
			}
		}
	}

	public IMTNodeDao getMtNodeDao() {
		return mtNodeDao;
	}

	public void setMtNodeDao(IMTNodeDao nodeDao) {
		this.mtNodeDao = nodeDao;
	}

	public long getInactiveNodeTimeout() 
	{
		return inactiveNodeTimeout;
	}

	@Override
	public void setInactiveNodeTimeout(long inactiveNodeTimeout) 
	{
		this.inactiveNodeTimeout = inactiveNodeTimeout;
	}

    @Override
    public String getIpAddress() {
        return ipAddress;
    }

	@Override
    public String getMachineName() {
        return machineName;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        
        try {
            
            String parameterValue = globalConfigurationDao.selectParameterValue("FOUNDATION", NODE_INACTIVITY_TIMEOUT_MILLIS);
            inactiveNodeTimeout = Long.parseLong(parameterValue);
            logger.info("MT node inactivity timeout is set to: " + inactiveNodeTimeout);
            
        }catch(Exception e) {
            logger.warn("Could not load node inactivity timeout from global parameters. Using value from xml.");
        }
    }

    public void setGlobalConfigurationDao(IGlobalConfigurationDao globalConfigurationDao) {
        this.globalConfigurationDao = globalConfigurationDao;
    }

	public boolean isContextInitialized() {
		return contextInitialized;
	}

	public void setContextInitialized(boolean contextInitialized) {
		this.contextInitialized = contextInitialized;
	}

}
