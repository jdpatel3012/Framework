package com.ibaset.common.dao;

import com.ibaset.common.dao.jndi.EmptyDataSource;


/**
 * Base DAO class for Manufacturing Intelligence database.
 * 
 * MI database is a separate database from Solumina database. Solumina middle tier instance may or may not be connected with MI database.
 * JNDI context of the new datasource would be configured just like soluminaPrivateDS. New datasource cannot default to exisiting datasource.
 * On startup, Solumina middle tier needs to determine if there's a valid MI database connection:
 * Check to see if there's customer configured container resource for this datasource
 * Check to see if a valid connection can be made using the configured datasource resouce
 * This connection pool needs to be accessible from the DAO layer.
 * Middle tier - MI database DAO layer may require separate transaction boundary.
 * Current requirement is to restrict the access from Solumina middle tier to MI database as READ ONLY
 * */
public class MIJdbcDaoSupport extends JdbcDaoSupport {

	/**
	 * Returns true if MI database access is configured and can be used, false - otherwise.
	 * */
	public boolean isConnected()
	{
		return !(getDataSource() instanceof EmptyDataSource);
	}
}
