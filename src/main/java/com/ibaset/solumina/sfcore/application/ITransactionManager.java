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
package com.ibaset.solumina.sfcore.application;

import java.sql.SQLException;

import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;

import com.ibaset.common.security.context.SoluminaUser;
import com.ibaset.common.security.context.UserContext;
import com.ibaset.common.util.ProgressTracker;

/**
 * Interface of Solumina Transaction Manager.
 * */
public interface ITransactionManager
{

	/**
	 * Begins UDV-controlled transaction. Usually such a transaction spans several
	 * request-response cycles.  Transaction handler is stored in UserContext.
	 * */
	public void begin() throws CannotGetJdbcConnectionException, IllegalStateException, SQLException;
	/**
	 * Starts a new request transaction or resumes previously started UDV-controlled transaction if present.
	 * */
	public void initOnRequest(boolean setSessionContext);
	/**
	 * Commits request transaction or clears transaction context of current transaction is
	 * UDV-controlled.
	 * */
	public void cleanupOnResponse(boolean clearSessionContext);
	/**
	 * Commits/rolls back request transaction or clears transaction context of current transaction is
	 * UDV-controlled.
	 * */
	public void cleanupOnResponse(boolean clearSessionContext, boolean isCommit);
	/**
	 * Commits UDV-controlled transaction started by begin method.
	 * @throws DataAccessResourceFailureException if UDV controlled transaction is not present in UserContext.
	 * */
	public void commit() throws CannotGetJdbcConnectionException, SQLException;
	/**
	 * Rolls back current transaction.
	 * */
	public void rollback() throws CannotGetJdbcConnectionException, SQLException;
	
	/**
	 * Rolls back transaction from given user context.
	 * @param userContext TODO
	 * */
	public void rollback(SoluminaUser user, UserContext userContext);
	
	/**
	 * Returns true if current transaction has started and is UDV-controlled.
	 * @since 5.0.0.0
	 * */
	public boolean isUDVControlled();
	
	/**
	 * Kill active threads in current user context and rollback transaction.
	 * @return number of stopped threads. May be 0 or more 
	 * */
	public int kill();
	
	/**
	 * Calls SFCORE_KILL_CONNECTION with the given connection ID.
	 * @param connection id to kill 
	 * @return number of stopped threads. May be 0 or more
	 * @throws SQLException 
	 * */
	public int kill(String connectionId) throws SQLException;
	
	/**
	 * @deprecated since G8R2SP2 use {@link ProgressTracker#getProcessCount()}
	 * Returns number of active threads in current session excluding one 
	 * that processes call to this method.  
	 * @return number of active threads. May be 0 or more
	 * */
	@Deprecated
	public int getProcessCount();
	
	/**
	 * @deprecated since G8R2SP2 use {@link ProgressTracker#getProgressRatio()}
	 * Returns progress ratio: getProgress()/getProgressMaximum()
	 * */
	@Deprecated
	public float getProgressRatio();
	/**
	 * @deprecated since G8R2SP2 use {@link ProgressTracker#setProgressMaximum(int)}
	 * Set progress maximum. Value must be a positive integer
	 * */
	@Deprecated
	public void setProgressMaximum(int maximum);
	/**
	 * @deprecated since G8R2SP2 use {@link ProgressTracker#setProgress(int)}
	 * Set progress value. 
	 * Value must be between 0 and maximum progress inclusive. 
	 * */
	@Deprecated
	public void setProgress(int nv);
	
	/**
	 * @deprecated since G8R2SP2 use {@link ProgressTracker#incrementProgressByGivenPercent(int)}
	 * Increase the progress by percentValue% of the maximum
	 * @param percentValue
	 */
	@Deprecated
	public void incrementProgressByGivenPercent(int percentValue);
	
	/**
	 * @deprecated since G8R2SP2 use {@link ProgressTracker#getProgress()}
	 * Returns current progress value.
	 * */
	@Deprecated
	public int getProgress();
	/**
	 * @deprecated since G8R2SP2 use {@link ProgressTracker#getProgressMaximum()}
	 * Returns progress maximum.
	 * Default value is 100
	 * */
	@Deprecated
	public int getProgressMaximum();
	
	/**
	 * @deprecated since G8R2SP2 use {@link ProgressTracker#getProgressEndBuffer()}
	 * @return a factor for the  buffer for the end of progress
	 */
	@Deprecated
	public double getProgressEndBuffer();
	
	/**
	 * @deprecated since G8R2SP2 use {@link ProgressTracker#testProgress(int)}
	 * Sets progress max to the given seconds, and increments the progress value every second.
	 * @param seconds
	 */
	@Deprecated
	public void testProgress(int seconds);

	/**
	 * Execute code represented by an instance of Runnable in a transaction.
	 * Note that method should not be called during another transaction.
	 * @since 4.6.0.0 
	 * */
	public void executeInTransaction(Runnable code);
}
