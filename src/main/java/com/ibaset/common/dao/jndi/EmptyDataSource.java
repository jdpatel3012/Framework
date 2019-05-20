package com.ibaset.common.dao.jndi;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

import javax.sql.DataSource;

/**
 * Datasource that throws an SQLFeatureNotSupportedException on any operation.
 * 
 * */
public class EmptyDataSource implements DataSource {

	private String errorMessage = "Data access is not configured";
	
	public EmptyDataSource() {
		super();
	}
	
	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	private void reportError() throws SQLFeatureNotSupportedException{
		throw new SQLFeatureNotSupportedException(errorMessage);
	}
	
	public Connection getConnection() throws SQLException {
		reportError();
		return null;
	}

	public Connection getConnection(String username, String password) throws SQLException {
		reportError();
		return null;
	}

	public int getLoginTimeout() throws SQLException {
		reportError();
		return 0;
	}

	public PrintWriter getLogWriter() throws SQLException {
		reportError();
		return null;
	}

	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		reportError();
		return false;
	}

	public void setLoginTimeout(int seconds) throws SQLException {
		reportError();
	}

	public void setLogWriter(PrintWriter out) throws SQLException {
		reportError();
	}

	public <T> T unwrap(Class<T> iface) throws SQLException {
		reportError();
		return null;
	}

	public Logger getParentLogger() throws SQLFeatureNotSupportedException {
		reportError();
		return null;
	}

}
