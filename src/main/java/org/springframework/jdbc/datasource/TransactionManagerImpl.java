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
package org.springframework.jdbc.datasource;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.support.JdbcDaoSupport;
import org.springframework.jdbc.object.StoredProcedure;
import org.springframework.orm.hibernate4.SessionHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.ibaset.common.context.SoluminaContextHolder;
import com.ibaset.common.context.TransactionalContext;
import com.ibaset.common.dao.DataEvent;
import com.ibaset.common.dao.DataListener;
import com.ibaset.common.enums.FrameworkEnums.ConnectionType;
import com.ibaset.common.event.SoluminaApplicationEvent;
import com.ibaset.common.event.SoluminaEventPublisher;
import com.ibaset.common.security.context.ContextUtil;
import com.ibaset.common.security.context.SoluminaUser;
import com.ibaset.common.security.context.UserContext;
import com.ibaset.common.sql.cache.TransactionQueryCache;
import com.ibaset.common.sql.cache.TransactionQueryCacheFactory;
import com.ibaset.common.util.ProgressTracker;
import com.ibaset.solumina.sfcore.application.ILogin;
import com.ibaset.solumina.sfcore.application.ISoluminaSavePoint;
import com.ibaset.solumina.sfcore.application.ITransactionManager;

/**
 * Transaction manager controls transactions during request processing.
 * */
public class TransactionManagerImpl extends JdbcDaoSupport	implements
															ITransactionManager
{
	
	private ILogin login = null;
	private DataSource unprotectedDataSource;
	//hibernate-compatibility:
	private PlatformTransactionManager transactionManager;
	private SessionFactory sessionFactory;
	private static final String THREAD_SET_NAME="SOLUMINA_THREAD_SET";
	
	//udv script caching:
	private TransactionQueryCacheFactory queryCacheFactory;
	
	private KillConnectionProcedure killProc = new KillConnectionProcedure();
	private KillSessionProcedure killSessionProc = new KillSessionProcedure();
	private DataListener dataListener;
	
	private SoluminaTransactionTracker transactionTracker;
	
	private SoluminaEventPublisher soluminaEventPublisher = null;
	
	private ISoluminaSavePoint soluminaSavePoint = null;
	
	private ConnectionHolder beginTransaction(){		
		ConnectionHolder conn = null;
		if(transactionManager!=null) transactionManager.getTransaction(new DefaultTransactionDefinition());
		
		ConnectionHolder currentConHolder = (ConnectionHolder) TransactionSynchronizationManager.getResource(getDataSource());

		if (currentConHolder != null)
		{
	        ConnectionProxy connectionWrapper = (ConnectionProxy)getCloseSuppressingConnectionProxy(currentConHolder.getConnection());
			try {
				// Replaced protected method call with java-reflection
		        Method method1 = currentConHolder.getClass().getDeclaredMethod("setConnection", Connection.class);
				method1.setAccessible(true);
				method1.invoke(currentConHolder, connectionWrapper);
				
		        Method method2 = currentConHolder.getClass().getDeclaredMethod("setTransactionActive", boolean.class);
				method2.setAccessible(true);
				method2.invoke(currentConHolder, true);
//				currentConHolder.setConnection(connectionWrapper);
			} catch (NoSuchMethodException | SecurityException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}

			conn = currentConHolder;
		}
		else
		{
			ConnectionProxy connectionWrapper = (ConnectionProxy)getCloseSuppressingConnectionProxy(getConnection());
			conn = new ConnectionHolder(connectionWrapper, true);

		}
//		conn.setTransactionActive(true);
		conn.requested();
		conn.setSynchronizedWithTransaction(true);

		if(sessionFactory!=null)
		{
			Object res=TransactionSynchronizationManager.getResource(sessionFactory);
			if(res!=null) {
				setSessionHolderForCurrentRequest((SessionHolder) res);
			}
		}
		queryCacheFactory.initiateTransactionQueryCache();
		return conn;
		
	}
	public void begin()	throws CannotGetJdbcConnectionException,
						IllegalStateException,
						SQLException
	{
		UserContext ctx = ContextUtil.getUser().getContext();
		
		TransactionalContext transactionalContext = SoluminaContextHolder.getTransactionalContext();
		ConnectionHolder conn = transactionalContext.getUdvScriptConnection();

		if (conn == null)
		{
			conn=transactionalContext.getConnectionHolder();
			transactionalContext.setUdvScriptConnection(conn);
			transactionalContext.setConnectionHolder(null);
		}

		if (logger.isDebugEnabled())
			logger.debug("BEGIN: Context conn " + conn);
		ProgressTracker.setProgress(0);
	}

	private Throwable createAbortException(String reason){
		SQLException sqe = new SQLException(">>Operation aborted by "+reason+"<<");
		return new Error(sqe);
	}

	public int kill()
	{
		return kill(ContextUtil.getUser(), SoluminaContextHolder.getUserContext(), "user");
	}
	
	public int kill(String connectionId) throws SQLException 
	{
		// Call DB's SFCORE_KILL_CONNECTION
		Map<String, String> m = new HashMap<String, String>();
		m.put("connectionId", connectionId);
		this.killProc.execute(m, unprotectedDataSource);
		return 1;
	}
	private void killSession(String connectionId) throws SQLException 
	{
		// Call DB's SFCORE_KILL_SESSION
		Map<String, String> m = new HashMap<String, String>();
		m.put("connectionId", connectionId);
		this.killSessionProc.execute(m, unprotectedDataSource);
	}

	@SuppressWarnings({ "deprecation"})
	private synchronized int kill(SoluminaUser user, UserContext ctx, String reason) {
		HashSet set = (HashSet)ctx.get(THREAD_SET_NAME);
		int stopped = 0;
		long thisId = Thread.currentThread().getId();
		if(set!=null) {
		    int active = Thread.activeCount();
		    Thread all[] = new Thread[active];
		    Thread.enumerate(all);
		    for (int i = 0; i < active; i++) {
				long threadId = all[i].getId();
		    	if(thisId != threadId && set.contains(threadId)){
		    		if(all[i].isAlive())
		    			all[i].stop(createAbortException(reason));
		    		stopped++; 
		    	}
		    }
		    //check whether threads were actually stopped
		    active = Thread.activeCount();
		    all = new Thread[active];
		    Thread.enumerate(all);
		    boolean stillAlive = false;
		    for (int i = 0; i < active; i++) {
				long threadId = all[i].getId();
		    	if(thisId != threadId && set.contains(threadId))
		    	{
		    		if(all[i].isAlive())
		    		{
		    			stillAlive = true;
		    			break;
		    		}
		    	}
		    }
		    if(stillAlive) 
		    {
	    		String connectionId = ctx.getConnectionId(); 
		    	//force session kill in database
		    	if(logger.isDebugEnabled())logger.debug("Kill session for "+connectionId);
		    	try 
		    	{
					if(!StringUtils.isEmpty(connectionId))
					{
						killSession(connectionId);
					}
				} 
		    	catch (Throwable e) 
		    	{
		    		logger.error("Kill session for "+connectionId+" failed:", e);
				}
		    }
		}
		return stopped;
	}

	@SuppressWarnings("unchecked")
	private synchronized void registerThread(){
		UserContext ctx = ContextUtil.getUser().getContext();
		HashSet set = (HashSet)ctx.get(THREAD_SET_NAME);
		if(set==null) {
			set=new HashSet();
			ctx.set(THREAD_SET_NAME, set);
		}
		Long threadId = Thread.currentThread().getId();
		set.add(threadId);
	}
	@SuppressWarnings("unchecked")
	private synchronized void unregisterThread(){
		UserContext ctx = ContextUtil.getUser().getContext();
		HashSet set = (HashSet)ctx.get(THREAD_SET_NAME);
		if(set!=null) {
			Long threadId = Thread.currentThread().getId();
			set.remove(threadId);
		}
	}
	
    private class KillConnectionProcedure extends StoredProcedure
    {
        public KillConnectionProcedure()
        {
        	SqlParameter connectionId = new SqlParameter("connectionId", Types.VARCHAR);
            String SQL = "SFCORE_KILL_CONNECTION";
            setSql(SQL);
            declareParameter(connectionId);
        }

        @SuppressWarnings("unchecked")
		public Map<String, Object> execute(Map<String, String> params, DataSource ds)
        {
        	synchronized (this) 
        	{
                if (!isCompiled())
                {
                	setJdbcTemplate(createJdbcTemplate(ds));
                    compile();
                }
			}
            return execute(params);
        }
    }
    private class KillSessionProcedure extends StoredProcedure
    {
        public KillSessionProcedure()
        {
        	SqlParameter connectionId = new SqlParameter("connectionId", Types.VARCHAR);
            String SQL = "SFCORE_KILL_SESSION";
            setSql(SQL);
            declareParameter(connectionId);
        }

        @SuppressWarnings("unchecked")
		public Map<String, Object> execute(Map<String, String> params, DataSource ds)
        {
        	synchronized (this) 
        	{
                if (!isCompiled())
                {
                	setJdbcTemplate(createJdbcTemplate(ds));
                    compile();
                }
			}
            return execute(params);
        }
    }
    private void startTransactionTracker(SoluminaUser user)
    {
    	UserContext ctx = user.getContext();
    	//make sure this is a Windows Client connection
    	if(transactionTracker != null 
    		&& ctx.getConnectionType().contains(ConnectionType.FAT.value()) 
    		&& ctx.isInitiatedByClient())
    	{
    		transactionTracker.trackSoluminaUser(user, transactionTracker.getClientKeepAliveTimeout());
    	}
    }
	public void initOnRequest(boolean setSessionContext)
	{
		registerThread();
		SoluminaUser user = ContextUtil.getUser();
		UserContext ctx = user.getContext();
		
		TransactionalContext transactionalContext = SoluminaContextHolder.getTransactionalContext();
		ConnectionHolder conHolder = transactionalContext.getUdvScriptConnection();

		if (conHolder != null && conHolder.getConnectionHandle() == null)
		{
		    transactionalContext.setUdvScriptConnection(null);
			conHolder = null;
		}

		if (logger.isDebugEnabled())
			logger.debug("INIT: Context conn " + conHolder);
		
		if (conHolder == null)
		{
			//start thread connection
			conHolder=beginTransaction();
			transactionalContext.setConnectionHolder(conHolder);
			startTransactionTracker(user);
			
		}
		else 
		{
			setSessionContext = false; //using UDV script connection
			TransactionUtils.resumeJmsTransaction(user);
			if(transactionTracker != null) transactionTracker.activityOnSoluminaUser(user);
		}
		
		//setup connection
		DataSource ds = getDataSource();

		conHolder.setSynchronizedWithTransaction(true);

		if (TransactionSynchronizationManager.hasResource(ds))
		{
			TransactionSynchronizationManager.unbindResource(ds);
		}
		TransactionSynchronizationManager.bindResource(ds, conHolder);

		if(sessionFactory != null)
		{
		    SessionHolder sessionHolder = getSessionHolderForCurrentRequest();
			if (TransactionSynchronizationManager.hasResource(sessionFactory))
			{
				TransactionSynchronizationManager.unbindResource(sessionFactory);
			}

			if(sessionHolder!=null) TransactionSynchronizationManager.bindResource(	sessionFactory, sessionHolder);
		}
		if (!TransactionSynchronizationManager.isSynchronizationActive())
		{
			TransactionSynchronizationManager.initSynchronization();
		}
				
	}
	public void cleanupOnResponse(boolean clearSessionContext)
	{
		cleanupOnResponse(clearSessionContext, true);
	}

	public void cleanupOnResponse(boolean clearSessionContext, boolean isCommit)
	{
		unregisterThread();
		SoluminaUser user = ContextUtil.getUser();
		UserContext ctx = user.getContext();
		try
		{
			soluminaSavePoint.releaseSavepoint();
			
			TransactionalContext transactionalContext = SoluminaContextHolder.getTransactionalContext();
			ConnectionHolder conn = transactionalContext.getConnectionHolder();
			
			if(conn!=null)
			{
			    transactionalContext.setConnectionHolder(null);
				try
				{
					handleCommitOrRollback(isCommit, user, ctx, conn);
					TransactionUtils.handleJmsCommitOrRollback(isCommit, user);
				} 
				catch (Throwable e) 
				{
					logger.error("error committing thread transaction", e);
					TransactionUtils.handleJmsCommitOrRollback(false, user);
				}
			} 
			else if(transactionalContext.getUdvScriptConnection()!=null)
			{
				TransactionUtils.suspendJmsTransaction(user);
			}
			else
			{
				TransactionUtils.handleJmsCommitOrRollback(isCommit, user);
			}
		}
		finally
		{
			if (logger.isDebugEnabled())
				logger.debug("Cleanup on response");
			if (TransactionSynchronizationManager.hasResource(getDataSource()))
			{
				TransactionSynchronizationManager.unbindResource(getDataSource());
			}
	
			if(sessionFactory != null)
			{
				if (TransactionSynchronizationManager.hasResource(sessionFactory))
				{
					TransactionSynchronizationManager.unbindResource(sessionFactory);
				}
			}
			if (TransactionSynchronizationManager.isSynchronizationActive())
			{
				TransactionSynchronizationManager.clearSynchronization();
			}
		}
	}

	protected final DataEvent fireBeforeEvent(int type){
		DataEvent event = null;
        if(dataListener!=null){
        	event=new DataEvent();
        	event.setType(type);
        	try{
        		dataListener.beforeEvent(event);
        	} catch (Exception e) {
        		logger.error("dataListener failed", e);
			}
        	event.setFailure(true);
        }
		return event;
	}
	protected final void fireAfterEvent(DataEvent event){
        if(dataListener!=null){
        	try{
        		dataListener.afterEvent(event);
        	} catch (Exception e) {
        		logger.error("dataListener failed", e);
			}
        }
	}
	
	private int getNumberOfThreads(SoluminaUser user, UserContext ctx)
	{
		HashSet set = (HashSet)ctx.get(THREAD_SET_NAME);
		int count = set.size();
		long thisId = Thread.currentThread().getId();
		if(set.contains(thisId)) --count;
		return count;
	}
	
    private void handleCommitOrRollback(boolean isCommit, SoluminaUser user, UserContext ctx)
    {
        transactionTracker.trackSoluminaUser(user, transactionTracker.getClientKeepAliveTimeout());
    	TransactionalContext transactionalContext = SoluminaContextHolder.getTransactionalContext();
		ConnectionHolder conn1 = transactionalContext.getUdvScriptConnection();
		
		if(conn1==null) conn1 = transactionalContext.getConnectionHolder();
		if (user != null && ctx!=null)
		{
			if (isCommit)
			{
				if (ctx.soluminaApplicationEventExists())
				{
					// Publish SoluminaApplicationEvents
					List<SoluminaApplicationEvent> eventList = ctx.popOffSoluminaApplicationEvents();
		        	int size = eventList.size();
		        	for (int i=0; i<size; i++)
		        	{
		        		soluminaEventPublisher.publishEvent(eventList.get(i));
		        		transactionTracker.trackSoluminaUser(user, transactionTracker.getClientKeepAliveTimeout());
		        	}
				}
			}
			else
			{
				ctx.clearEvents();
				ctx.clearSoluminaApplicationEvents();
			}
		}
		
		boolean success = false;
		try
		{
			if(conn1 != null) handleCommitOrRollback(isCommit, user, ctx, conn1);
			TransactionUtils.handleJmsCommitOrRollback(isCommit, user);
			success = true;
		}
		finally
		{
		    transactionalContext.setUdvScriptConnection(null);
		    transactionalContext.setConnectionHolder(null);
			if(transactionTracker != null) transactionTracker.removeSoluminaUser(user, ctx);
			if(!success)TransactionUtils.handleJmsCommitOrRollback(false, user);
		}
    }
    private void handleCommitOrRollback(boolean isCommit, SoluminaUser user, UserContext userContext, ConnectionHolder conn1)
    {
    	DataEvent event = fireBeforeEvent(isCommit ? DataEvent.COMMIT : DataEvent.ROLLBACK);
    	try
    	{
    		soluminaSavePoint.releaseSavepoint();
    		doCommitOrRollback(isCommit, user, userContext, conn1);
    		if(event!=null) event.setFailure(false);
    	} 
    	finally
    	{
    		fireAfterEvent(event);
        	if(transactionTracker != null && transactionTracker.isTracking(user, userContext))
        	{
        		if(getNumberOfThreads(user, userContext)==0) transactionTracker.removeSoluminaUser(user, userContext);
        	}
    	}
    }

    private void doCommitOrRollback(boolean isCommit, SoluminaUser user, UserContext userContext, ConnectionHolder conn1)
	{
    	Throwable error = null;
    	
    	try{
    		queryCacheFactory.cleanupTransactionQueryCache(isCommit);
    	}
    	catch (Throwable e) {
    		error = e;
    		if(isCommit){
    			logger.error("Query cache commit failed.",e);
    			isCommit = false;
    		}
		}
    	boolean connectionProcessed=false;
    	if(sessionFactory!=null)
    	{
    		SessionHolder sessionHolder = getSessionHolderForCurrentRequest();
			
			if (TransactionSynchronizationManager.hasResource(sessionFactory))
			{
				TransactionSynchronizationManager.unbindResource(sessionFactory);
			}
    		if(sessionHolder!=null)
    		{
    			resetSessionHolderForCurrentRequest();
    			Session session=sessionHolder.getSession();
    			Transaction hibernateTransaction=sessionHolder.getTransaction();
				try
				{
	    			if(isCommit)
	    			{
		    				session.flush();
		    				if(hibernateTransaction.isActive())hibernateTransaction.commit();
	    			}
	    			else
	    			{
	    				if(hibernateTransaction.isActive())hibernateTransaction.rollback();
	    			}
				} 
				catch (Throwable e) 
				{
					isCommit = false;
					error = e;
					logger.error(e);
				}
				finally
				{
					try
					{
		    			//if(session.disconnect()==null)connectionProcessed=true;
						session.disconnect();
						connectionProcessed=true;
		    			session.close();
					} 
					catch (Throwable e) 
					{
						logger.error(e);
					} 
					
				}
    		}
    	}
    	
		if (conn1 != null && conn1.getConnectionHandle() == null)
		{
			conn1 = null;
		}

		if (logger.isDebugEnabled())
			logger.debug("COMMIT: Context conn " + conn1);
		Connection conn = null;
		if (conn1 != null)
		{

			try
			{
				Object outerConn = conn1.getConnection();

				if (outerConn != null &&
						outerConn instanceof ConnectionProxy)
				{

					conn = ((ConnectionProxy) outerConn).getTargetConnection();

				}
				else
				{
					conn = conn1.getConnection();
				}
				if (isCommit)
				{
					try
					{
						if(!connectionProcessed)conn.commit();
					}
					catch (Throwable e)
					{
						error = e;
						isCommit = false;
						logger.error(e);
					}
				}
				if(!isCommit)
				{

					try
					{
						if(!connectionProcessed)conn.rollback();
					}
					catch (Throwable e)
					{
						logger.error(e);
					}
				}
			}
			finally
			{
				if (TransactionSynchronizationManager.hasResource(getDataSource()))
				{
					TransactionSynchronizationManager.unbindResource(getDataSource());
				}
				try
				{
					if(conn!=null) DataSourceUtils.releaseConnection(conn, getDataSource());
				}
				catch (Throwable t)
				{
					logger.error(t);
				}
				if (TransactionSynchronizationManager.isSynchronizationActive())
				{
					TransactionSynchronizationManager.clearSynchronization();
				}
			}
		}
		if (!isCommit && user != null && userContext!=null)
		{
			userContext.clearEvents();
			userContext.clearSoluminaApplicationEvents();
		}
    	if(error!=null) throw new RuntimeException(error);
	}
    
	public void rollback(SoluminaUser user, UserContext userContext)
	{
		try
		{
			kill(user, userContext, "timeout");
		} 
		catch (Throwable e) {	}
		handleCommitOrRollback(false, user, userContext);
	}

	public void rollback()
	{
		handleCommitOrRollback(false, ContextUtil.getUser(), ContextUtil.getUser().getContext());
	}

	public void commit() throws CannotGetJdbcConnectionException, SQLException
	{
		SoluminaUser user = ContextUtil.getUser();
    	TransactionalContext transactionalContext = SoluminaContextHolder.getTransactionalContext();
		ConnectionHolder conn = transactionalContext.getUdvScriptConnection();
		if(conn==null) throw new DataAccessResourceFailureException("Transaction does not exist: unbalanced commit call or transaction was rolled back by due to a timeout");
		handleCommitOrRollback(true, user, ContextUtil.getUser().getContext());
	}

	private static class CloseSuppressingInvocationHandler	implements
															InvocationHandler
	{

		private final Connection target;

		public CloseSuppressingInvocationHandler(Connection target)
		{
			this.target = target;
		}

		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
		{
			
			Logger logger = LoggerFactory.getLogger(this.getClass());
			// Invocation on ConnectionProxy interface coming in...

			if (method.getName().equals("getTargetConnection"))
			{
				// Handle getTargetConnection method: return underlying
				// Connection.
				return this.target;
			}
			else if (method.getName().equals("equals"))
			{
				// Only consider equal when proxies are identical.
				return (proxy == args[0] ? Boolean.TRUE : Boolean.FALSE);
			}
			else if (method.getName().equals("hashCode"))
			{
				// Use hashCode of Connection proxy.
				return new Integer(hashCode());
			}
			else if (method.getName().equals("close"))
			{
				// Handle close method: don't pass the call on.
				logger.debug("tried to close a TxManager Connection");
				return null;
			}
			else if (method.getName().equals("commit"))
			{
				// Handle close method: don't pass the call on.
				logger.debug("tried to commit a TxManager Connection");
				return null;
			}
			else if (method.getName().equals("rollback")) 
			{
				if( args.length==0)
				{
					// Handle rollback method: don't pass the call on.
					logger.debug("tried to rollback a TxManager Connection");
					return null;
				}
				//rollback(Savepoint)
				TransactionalContext transactionalContext = SoluminaContextHolder.getTransactionalContext();
				TransactionQueryCache cache = transactionalContext.getUdvScriptQueryCache();
				if(cache!=null) cache.rollbackToSavepoint();

			}
			// Invoke method on target Connection.
			try
			{
				return method.invoke(this.target, args);
			}
			catch (InvocationTargetException ex)
			{
				throw ex.getTargetException();
			}
		}
	}

	protected ConnectionProxy getCloseSuppressingConnectionProxy(Connection target)
	{
		return (ConnectionProxy) Proxy.newProxyInstance(	ConnectionProxy.class.getClassLoader(),
													new Class[]
													{
														ConnectionProxy.class
													},
													new CloseSuppressingInvocationHandler(target));
	}

	public double getProgressEndBuffer() {
		return ProgressTracker.getProgressEndBuffer();
	}

	public void setProgressEndBuffer(double progressEndBuffer) {
		ProgressTracker.setProgressEndBuffer(progressEndBuffer);
	}

	public int getProgress() {
    	return ProgressTracker.getProgress();
	}

	public int getProgressMaximum() {
    	return ProgressTracker.getProgressMaximum();
	}

	public float getProgressRatio() 
	{
		return ProgressTracker.getProgressRatio();
	}

	public void incrementProgressByGivenPercent(int percentValue)
	{
		ProgressTracker.incrementProgressByGivenPercent(percentValue);
	}
	
	public void setProgress(int nv) {
		ProgressTracker.setProgress(nv);
	}

	public void setProgressMaximum(int maximum) {
		ProgressTracker.setProgressMaximum(maximum);
	}
	
	@SuppressWarnings("static-access")
	public void testProgress(int seconds)
	{
		ProgressTracker.testProgress(seconds);
	}

	public int getProcessCount() {
		return ProgressTracker.getProcessCount();
	}

	public void setDisableSavepoints(boolean disableSavepoints)
    {
    }

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	public void setQueryCacheFactory(TransactionQueryCacheFactory queryCacheFactory) {
		this.queryCacheFactory = queryCacheFactory;
	}

	public ILogin getLogin() {
		return login;
	}

	public void setLogin(ILogin login) {
		this.login = login;
	}
	public void setDataListener(DataListener dataListener) 
	{
		this.dataListener = dataListener;
	}
	
	public void setTransactionTracker(SoluminaTransactionTracker tranTracker) 
	{
		this.transactionTracker = tranTracker;
	}
	public SoluminaEventPublisher getSoluminaEventPublisher() {
		return soluminaEventPublisher;
	}
	public void setSoluminaEventPublisher(
			SoluminaEventPublisher soluminaEventPublisher) {
		this.soluminaEventPublisher = soluminaEventPublisher;
	}
	@Override
	public void executeInTransaction(Runnable code) 
	{
		initOnRequest(true);
		boolean success = false;
		try{
			code.run();
			cleanupOnResponse(true, true);
			success= true;
		} finally{
			if(!success) cleanupOnResponse(true, false);
		}
	}

	@Override
	public boolean isUDVControlled() 
	{
		SoluminaUser user = ContextUtil.getUser();
		if(user!=null)
		{
			TransactionalContext transactionalContext = SoluminaContextHolder.getTransactionalContext();
			return transactionalContext.getUdvScriptConnection() != null;
		}
		return false;
	}
	
	private boolean isMainRequest(TransactionalContext transactionalContext) {
		return transactionalContext.getSessionHolder() == null;
	}

	private boolean isGetXRequest(TransactionalContext transactionalContext) {
		return transactionalContext.getThreadSessionHolder() != null;
	}
	
	/**
	 * If current thread execution is for the actual request then set SessionHolder against 
	 * sessionFactory a key otherwise set SessionHolder against the getX request threadId as a key.
	 * 
	 * @param sessoinHolder
	 */
	private void setSessionHolderForCurrentRequest(SessionHolder sessoinHolder) {
		if (sessoinHolder != null) {
			TransactionalContext transactionalContext = SoluminaContextHolder.getTransactionalContext();
			if (isMainRequest(transactionalContext)) {
				transactionalContext.setSessionHolder(sessoinHolder);
			} else {
				transactionalContext.setThreadSessionHolder(sessoinHolder);
			}
		}
	}
	
	
	/**
	 * If current thread execution is for the getX request then retrieve 
	 * corresponding SessionHolder object otherwise retrieve from the actual request.
	 * 
	 * @return
	 */
	private SessionHolder getSessionHolderForCurrentRequest() {
		SessionHolder res = null;
		TransactionalContext transactionalContext = SoluminaContextHolder.getTransactionalContext();
		if (isGetXRequest(transactionalContext)) {
			res = transactionalContext.getThreadSessionHolder();
		} else {
			res = transactionalContext.getSessionHolder();
		}
		return res;
	}

	/**
	 * If current thread execution is for the getX request then set corresponding
	 * SessionHolder to NULL otherwise set SessionHolder to NULL for the actual request.
	 * 
	 * @return
	 */
	private void resetSessionHolderForCurrentRequest() {
		TransactionalContext transactionalContext = SoluminaContextHolder.getTransactionalContext();
		if(isGetXRequest(transactionalContext)){
			transactionalContext.setThreadSessionHolder(null);
		}else {
			transactionalContext.setSessionHolder(null);
		}
	}
	
	public DataSource getUnprotectedDataSource() 
	{
		return unprotectedDataSource;
	}
	public void setUnprotectedDataSource(DataSource unprotectedDataSource) 
	{
		this.unprotectedDataSource = unprotectedDataSource;
	}
	public ISoluminaSavePoint getSoluminaSavePoint() {
		return soluminaSavePoint;
	}
	public void setSoluminaSavePoint(ISoluminaSavePoint soluminaSavePoint) {
		this.soluminaSavePoint = soluminaSavePoint;
	}
	

}
