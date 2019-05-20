package org.springframework.jdbc.datasource;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibaset.common.Reference;
import com.ibaset.common.context.SoluminaContextHolder;
import com.ibaset.common.context.ThreadContext;
import com.ibaset.common.security.context.SoluminaUser;
import com.ibaset.common.security.context.UserContext;
import com.ibaset.solumina.sfcore.application.ITransactionManager;

/**
 * Tracks Solumina user activity and rolls back inactive transactions 
 * */
public class SoluminaTransactionTracker implements Runnable {

	protected final Logger logger = LoggerFactory.getLogger(getClass());
	private Map<String, Record> lastActiveTimes;
	//it's safe to assume that client is terminated if server didn't get getProgressRatio requests from it after this period of time
	private int timeout = 30;
	//it's safe to assume that client is terminated if server didn't get any requests from it after this period of time 
	private int clientKeepAliveTimeout = 125;
	private volatile boolean enabled;
	private final Object monitor=new Object();
	@Reference
	private ITransactionManager transactionManager;
	
	static class Record
	{
		volatile long lastActivity;
		final SoluminaUser user;
		volatile int timeout;
		volatile String sessionId;
		public Record(SoluminaUser user, long lastActivity, int timeout) {
			super();
			this.user = user;
			this.lastActivity = lastActivity;
			this.timeout = timeout;
			this.sessionId = ThreadContext.getInstance().getSessionId();
		}
	}
	
	public SoluminaTransactionTracker() 
	{
		lastActiveTimes = Collections.synchronizedMap(new HashMap<String, Record>());
		enabled = true;
		Thread t = new Thread(this, getClass().getName());
		t.setDaemon(true);
		t.start();
	}

	public int getClientKeepAliveTimeout() 
	{
		return clientKeepAliveTimeout;
	}

	public void setClientKeepAliveTimeout(int clientKeepAliveTimeout) 
	{
		this.clientKeepAliveTimeout = clientKeepAliveTimeout;
	}

	public void setTimeout(int timeout) 
	{
		this.timeout = timeout;
	}

	public void setEnabled(boolean enabled) 
	{
		this.enabled = enabled;
	}

	public boolean isTracking(SoluminaUser user, UserContext userContext)
	{
		return lastActiveTimes.containsKey(userContext.getConnectionId());
	}
	/**
	 * Registers activity on a Solumina user instance.
	 * */
	public void activityOnSoluminaUser(SoluminaUser user)
	{
		final String connectionId = user.getContext().getConnectionId();
		if(StringUtils.isEmpty(connectionId)) return;
		if(logger.isDebugEnabled())
		{
			logger.debug("Activity for connection "+connectionId);
		}
		Record r = lastActiveTimes.get(connectionId);
		if(r != null) r.lastActivity = System.currentTimeMillis();
	}
	
	public void trackSoluminaUser(SoluminaUser user)
	{
		trackSoluminaUser(user, timeout);
	}
	
	public void trackSoluminaUser(SoluminaUser user, int customTimeoutInSeconds)
	{
		final String connectionId = user.getContext().getConnectionId();
		if(StringUtils.isEmpty(connectionId)) return;
		if(logger.isDebugEnabled())
		{
			logger.debug("Tracking transaction for connection "+connectionId);
		}
		Record r = lastActiveTimes.get(connectionId);
		if(r == null) 
		{
			lastActiveTimes.put(connectionId, new Record(user, System.currentTimeMillis(), customTimeoutInSeconds));
		}
		else 
		{
			r.lastActivity = System.currentTimeMillis();
			r.timeout = customTimeoutInSeconds;
		}
	}
	
	public void removeSoluminaUser(SoluminaUser user, UserContext userContext)
	{
		final String connectionId = userContext.getConnectionId();
		if(logger.isDebugEnabled())
		{
			logger.debug("Stop tracking transaction for connection "+connectionId);
		}
		lastActiveTimes.remove(connectionId);
	}
	
	public void removeConnectionId(String connectionId)
	{
		if(logger.isDebugEnabled())
		{
			logger.debug("Removing tracking transaction for connection "+connectionId);
		}
		lastActiveTimes.remove(connectionId);
	}

	private boolean isTimedOut(Record r)
	{
		return System.currentTimeMillis() - r.lastActivity >= r.timeout * 1000;
	}
	
	public void run()
	{
		logger.info("Solumina transaction tracker started");
		while(enabled)
		{
			try
			{
				synchronized (monitor) 
				{
					try
					{
						monitor.wait(timeout * 1000L);
					} 
					catch (InterruptedException e) 
					{
					}
				}
				checkExpiredUsers();
			}
			catch (ThreadDeath e) 
			{
				logger.info("Solumina transaction tracker terminated", e);
				throw e;
			}
			catch (Throwable e) 
			{
				logger.warn("Solumina transaction tracker", e);
			}
		}
		logger.info("Solumina transaction tracker stopped");
	}
	
	private void checkExpiredUsers() 
	{
		String[] keys = null;
		synchronized (lastActiveTimes) 
		{
			keys = lastActiveTimes.keySet().toArray(new String[lastActiveTimes.size()]);
		}
		for(String connectionId : keys)
		{
			Record r = lastActiveTimes.get(connectionId);
			if(r != null)
			{
				if(logger.isDebugEnabled())
				{
					logger.debug("Millis since last activity for connection "+connectionId+": "+(System.currentTimeMillis() - r.lastActivity));
				}
				if(isTimedOut(r))
				{
				    ThreadContext.getInstance().setSessionId(r.sessionId);
					removeConnectionId(connectionId);
					UserContext userContext = SoluminaContextHolder.getUserContext();
					
					if (StringUtils.equals(connectionId, userContext.getConnectionId()))
					{
						expire(r.user, userContext);
					}
					removeDanglingSoluminaContexts(r.sessionId);
					ThreadContext.getInstance().clear();
				}
			}
		}
	}
	
	private void removeDanglingSoluminaContexts(String sessionId) {
		
		logger.debug("Removing dangling TransactionContext and UserContext for sessionId : " + sessionId);
		SoluminaContextHolder.cleanupSoluminaContexts(sessionId);
	}
	
	private void expire(SoluminaUser user, UserContext userContext)
	{
		final String connectionId = userContext.getConnectionId();
		if(logger.isInfoEnabled())
		{
			logger.info("Closing inactive connection "+connectionId+" for user " +user.getUsername());
		}
		try
		{
			transactionManager.rollback(user, userContext);
		} 
		catch (Throwable e) 
		{
			logger.warn("Stopping transaction for connection "+connectionId, e);
		}
	}

	public void cleanUp() throws Exception
	{
		if(enabled)
		{
			logger.info("Terminating " + getClass().getName());
			enabled = false;
			synchronized (monitor) 
			{
				monitor.notifyAll();
			}
		}
	}
}
