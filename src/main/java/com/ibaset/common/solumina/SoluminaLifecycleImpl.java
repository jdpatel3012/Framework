package com.ibaset.common.solumina;

import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import com.ibaset.common.context.ThreadContext;

public class SoluminaLifecycleImpl implements ISoluminaLifecycle {

    protected final Logger logger = LoggerFactory.getLogger(getClass());
	private LinkedList<ISoluminaLifecycleListener> listeners;
	
	private final Lock lifecycleLock;
	private final Condition startupDone;
	private volatile boolean cleanupDone;
	private volatile boolean contextDone;
	
	
	public SoluminaLifecycleImpl() 
	{
		listeners = new LinkedList<ISoluminaLifecycleListener>();
		lifecycleLock = new ReentrantLock();
		startupDone = lifecycleLock.newCondition();
	}

	@Override
	public void addListener(ISoluminaLifecycleListener listener) {
		lifecycleLock.lock();
		try
		{
			if(!listeners.contains(listener))
			{
				listeners.add(listener);
				if(cleanupDone && contextDone)
				{
					try 
					{
						listener.afterStartup();
					} 
					catch (Exception e) 
					{
						logger.error("Solumina life cycle listener failed:", e);
					}	
				}
			}
			
		}
		finally
		{
			lifecycleLock.unlock();
		}
	}

	@Override
	public void removeListener(ISoluminaLifecycleListener listener) {
		lifecycleLock.lock();
		try
		{
			listeners.remove(listener);
		}
		finally
		{
			lifecycleLock.unlock();
		}
	}

	@Override
	public void waitUntilStarted() 
	{
		lifecycleLock.lock();
		try
		{
			if(cleanupDone && contextDone) return;
			try 
			{
				startupDone.await();
			} 
            catch (InterruptedException e) {
                logger.debug(e.getMessage(), e);
            }
		}
		finally
		{
			lifecycleLock.unlock();
		}
	}
	
	public void cleanupDone()
	{
		lifecycleLock.lock();
		try
		{
			if(cleanupDone) return;
			cleanupDone = true;
			if(cleanupDone && contextDone) startup();; 
		}
		finally
		{
			lifecycleLock.unlock();
		}
	}
	public void contextDone(ApplicationContext applicationContext)
	{
		lifecycleLock.lock();
		try
		{
			if(contextDone) return;
			contextDone = true;
			String[] names = applicationContext.getBeanNamesForType(ISoluminaLifecycleListener.class);
			for(String name:names)
			{
				ISoluminaLifecycleListener listener = (ISoluminaLifecycleListener)applicationContext.getBean(name);
				addListener(listener);
			}
			if(cleanupDone && contextDone) startup(); 
		}
		finally
		{
			lifecycleLock.unlock();
		}
	}
	
	private void startup()
	{
		logger.info("Solumina started");
		startupDone.signalAll(); 
		for(ISoluminaLifecycleListener l : listeners)
		{
			try
			{
				l.afterStartup();
			} 
			catch (Throwable e) 
			{
				logger.error("Solumina life cycle listener failed:", e);
			}
		}
	}
	
	public void shutdown()
	{
		logger.info("Solumina is shutting down");
		
		ThreadContext.getInstance().setSessionId();
		lifecycleLock.lock();

		try
		{
			for(ISoluminaLifecycleListener l : listeners)
			{
				try
				{
					l.beforeShutdown();
				} 
				catch (Throwable e) 
				{
					logger.error("Solumina life cycle listener failed:", e);
				}
			}
			listeners.clear();
			contextDone = false;
			cleanupDone = false;
		}
		finally
		{
			lifecycleLock.unlock();
			ThreadContext.getInstance().clear();
		}
		
		deregisterJdbcDrivers();
		
	}

	private void deregisterJdbcDrivers() {
		Enumeration<Driver> jdbcDrivers = DriverManager.getDrivers();
		while (jdbcDrivers.hasMoreElements()) {
			Driver driver = jdbcDrivers.nextElement();
			try {
				logger.debug(String.format("Deregistering jdbc driver: %s", driver));
				DriverManager.deregisterDriver(driver);
			} catch (SQLException e) {
				logger.warn(String.format("Error while deregistering driver %s", driver), e);
			}
		}
	}

	public void setCleanupDone(boolean cleanupDone) 
	{
		this.cleanupDone = cleanupDone;
	}

	public void setContextDone(boolean contextDone) 
	{
		this.contextDone = contextDone;
	}

}
