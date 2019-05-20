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

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jgroups.Address;
import org.jgroups.Channel;
import org.jgroups.Event;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.Receiver;
import org.jgroups.View;
import org.jgroups.stack.IpAddress;
import org.springframework.beans.factory.InitializingBean;

import com.ibaset.common.FrameworkConstants;
import com.ibaset.common.context.ThreadContext;
import com.ibaset.common.solumina.exception.SoluminaException;
import com.ibaset.solumina.sffnd.application.IGlobalConfiguration;

public class SoluminaCluster implements 
		ISoluminaCluster, 
		Receiver,
		InitializingBean
{
	
	private Channel channel;
	private static final Logger logger = LoggerFactory.getLogger(SoluminaCluster.class);
	private ArrayList<IClusterEventListener> listeners;
	private IGlobalConfiguration globalConfiguration;
	private String channelConfig;
	private String clusterName = "SOLUMINA_5K_CLUSTER";
	private String localNodeId = null;
    final List<Address> members=new ArrayList<>();

	public SoluminaCluster() 
	{
		listeners = new ArrayList<>();
	}

	public void addClusterEventListener(IClusterEventListener listener) 
	{
		listeners.add(listener);
	}

	public void removeClusterEventListener(IClusterEventListener listener) 
	{
		listeners.remove(listener);
	}

	public boolean isStandalone() 
	{
		return channel == null;
	}

	public boolean isCoordinator() 
	{
		if(channel!=null)
		{
			try
			{
				Address myAddress = channel.getAddress();
				Address coordinator = channel.getView().getMembers().get(0);
				return myAddress.equals(coordinator);
			} 
			catch (Exception e) 
			{
				logger.warn("Unable to access channel", e);
			}
		}
		return true;
	}
	
	private IpAddress toIpAddress(Address address)
	{
		if(channel!=null)
		{
			Address phys = (Address) channel.down(new Event(Event.GET_PHYSICAL_ADDRESS, address));
			if (phys instanceof IpAddress) 
			{
				return (IpAddress) phys;
			}
		}
		throw new SoluminaException("Unable to acquire physical address of a cluster node: "+address);
	}
	@Override
	public IClusterMember getLocalMember() 
	{
		if(channel != null)
		{
			return new ClusterMember(toIpAddress(channel.getAddress()), localNodeId);
		}
		throw new SoluminaException("Cluster is disabled");
	}

	public IClusterMember[] getMembers()
	{
		if(channel != null)
		{
			try
			{
				List<Address> viewMembers = channel.getView().getMembers();
				IClusterMember[] result = new IClusterMember[viewMembers.size()]; 
				for(int i=0;i<result.length;++i)
				{
					Address jAddr = viewMembers.get(i);
					result[i]= new ClusterMember(toIpAddress(jAddr));
				}
				return result;
			} 
			catch (Exception e) 
			{
				logger.warn("Unable to access cluster members", e);
			}
		}
		return new IClusterMember[0];
	}

	private void fireEvent(ClusterEvent e)
	{
		for(IClusterEventListener l : listeners)
		{
			try
			{
				l.handleClusterEvent(e);
			}
			catch (Exception ex) 
			{
				logger.error("Error in cluster event handler", ex);
			}
		}
	}

	public void memberJoined(Address address) 
	{
        if (logger.isInfoEnabled()) 
        {
            logger.info("A new member at address '" + address + "' has joined Solumina cluster");
        }
		fireEvent(new ClusterEvent(ClusterEvent.MEMBER_JOINED, address));
	}

	public void memberLeft(Address address) 
	{
        if (logger.isInfoEnabled()) 
        {
        	logger.info("Member at address '" + address + "' has left Solumina cluster");
        }
		fireEvent(new ClusterEvent(ClusterEvent.MEMBER_LEFT, address));
	}

	public void sendClusterEvent(ClusterEvent event) 
	{
		if(channel!=null)
		{
	        Message msg=null;
	        try 
	        {
	            if(event == null) {
	            	return;
	            }
	            msg=new Message(null, null, event);
	            channel.send(msg);
	        }
	        catch(Exception ex) 
	        {
	            if(logger.isErrorEnabled()) {
	            	logger.error("error sending notification", ex);
	            }
	        }
		}
	}

	private void startNotificationBus(String properties) throws Exception
	{
        if(logger.isDebugEnabled())
        {
        	logger.debug("Cluster name: "+clusterName);
        	logger.debug("Cluster configuration: "+properties);
        }
        channel = new JChannel(properties);
        localNodeId = UUID.randomUUID().toString();
        channel.setName(InetAddress.getLocalHost().getHostName()+":"+localNodeId);
        channel.setReceiver(this);
        channel.connect(clusterName);
		//Receive its own broadcast messages to the group (value is Boolean). Default is on.
        channel.setDiscardOwnMessages(true);
	}
	
	private void init() throws Exception
	{
		if(logger.isDebugEnabled())
		{
			org.jgroups.logging.LogFactory.getLog("org.jgroups").setLevel("debug");
		}
		if(channelConfig.indexOf('{')==-1)
		{
			startNotificationBus(channelConfig);
			return;
		}
		String address = globalConfiguration.getParameterValue(
				FrameworkConstants.FOUNDATION, "CLUSTER_ADDRESS");
		if(address!=null)
		{
			int i = address.lastIndexOf(':');
			if(i!=-1)
			{
				String mcastAddress=address.substring(0, i);
				String port=address.substring(i+1);
				if(Integer.parseInt(port) != 0)
				{
					String props=MessageFormat.format(channelConfig, mcastAddress, port);
					startNotificationBus(props);
			        logger.info("Solumina cluster node started successfully");
			        logger.info("Cluster Address: " + channel.getAddress() + ". Local Node ID: " + localNodeId);
				}
			}
			else
			{
				logger.error("Unable to recognize cluster address: '"+address+"'");
			}
		}
	}
	public void afterPropertiesSet() throws Exception 
	{
		try{
			init();
		} catch (Exception e) {
			logger.error("Unable to initialize cluster node", e);
			channel = null;
		}
	}
	
	public void cleanUp()
	{
       	logger.info("Stopping cluster node");
       	if(channel!=null) 
       	{
       		channel.close();
       		channel = null;
       	}
	}

	public byte[] getState() {
		return new byte[0];
	}

	public void setState(byte[] state) {
		// Do nothing
	}

	public void getState(OutputStream arg0) throws Exception {
		// Do nothing
	}

	public void setState(InputStream arg0) throws Exception {
		// Do nothing
	}

	public void unblock() {
		// Do nothing
	}

	@Override
	public void receive(Message msg) {
        Object obj;

        if(msg == null || msg.getLength() == 0) {
        	return;
        }
        try {
            obj=msg.getObject();
            if(!(obj instanceof ClusterEvent)) 
            {
                if(logger.isErrorEnabled()) {
                	logger.error("expected an instance of ClusterEvent (received " + obj.getClass().getName() + ')');
                }
                return;
            }
            ClusterEvent event = (ClusterEvent) obj;
            fireEvent(event);
        }
        catch(Exception ex) 
        {
            if(logger.isErrorEnabled()) {
            	logger.error("exception=" + ex);
            }
        }
	}

	@Override
	public void block() {
		// Do nothing
	}

	@Override
	public void suspect(Address suspected) {
		// Do nothing
	}

	@Override
    public synchronized void viewAccepted(View newView) 
	{
        List<Address> joinedMembers = null;
        List<Address> leftMembers = null;
        List<Address> tmp = null;
        if(newView == null) {
        	return;
        }
        tmp=newView.getMembers();

        logger.info("Cluster changed - " + tmp);
        logger.info("Cluster coordinator is: " + tmp.get(0));
        
        synchronized(members) {
            joinedMembers = getNewMembers(tmp);
            leftMembers = getLeftMembers(tmp);
            // adjust our own membership
            members.clear();
            members.addAll(tmp);
        }

        boolean sessionCreated = false;

        if(ThreadContext.getInstance().getSessionId() == null) {
            ThreadContext.getInstance().setSessionId();
            sessionCreated = true;
        }
        
        if(!joinedMembers.isEmpty()) {
            for(int i=0; i < joinedMembers.size(); i++) {
                memberJoined((Address) joinedMembers.get(i));
            }
        }
        
        if(!leftMembers.isEmpty()) {
            for(int i=0; i < leftMembers.size(); i++) {
                memberLeft((Address) leftMembers.get(i));
            }
        }
        
        if (sessionCreated) {
            ThreadContext.getInstance().clear();
        }
    }

	private List<Address> getLeftMembers(List<Address> tmp) {
		List<Address> leftMembers=new ArrayList<>();;
		for(int i=0; i < members.size(); i++) {
			Address tempMember=members.get(i);
		    if(!tmp.contains(tempMember))
		        leftMembers.add(tempMember);
		}
		return leftMembers;
	}

	private List<Address> getNewMembers(List<Address> tmp) {
		List<Address> joinedMembers = new ArrayList<>();
		for(int i=0; i < tmp.size(); i++) {
			Address tempMember=tmp.get(i);
		    if(!members.contains(tempMember))
		        joinedMembers.add(tempMember);
		}
		return joinedMembers;
	}

	public void setGlobalConfiguration(IGlobalConfiguration globalConfiguration) 
	{
		this.globalConfiguration = globalConfiguration;
	}

	public void setChannelConfig(String channelConfig) 
	{
		this.channelConfig = channelConfig;
	}

	public void setClusterName(String clusterName) 
	{
		this.clusterName = clusterName;
	}

	public String getLocalNodeId() {
		return localNodeId;
	}

	public void setLocalNodeId(String localNodeId) {
		this.localNodeId = localNodeId;
	}

}
