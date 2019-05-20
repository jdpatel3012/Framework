package org.springframework.jdbc.datasource;

import java.sql.Connection;

public class ConnectionHolderUtils extends ConnectionHolder {

	/*
	 * To avoid: IllegalAccessError
	 * Extended the class ConnectionHolder, so the current class will be initialized with parent classLoader.
	 * And so, we can call protected method of ConnectionHolder from this class
	 */
	public ConnectionHolderUtils(Connection connection) {
		super(connection);
	}
	
	public void setConnection(ConnectionHolder connectionHolder, Connection connection) {
		connectionHolder.setConnection(connection);
	}
	
	public void setTransactionActive(ConnectionHolder connectionHolder, boolean transactionActive) {
		connectionHolder.setTransactionActive(transactionActive);
	}
}
