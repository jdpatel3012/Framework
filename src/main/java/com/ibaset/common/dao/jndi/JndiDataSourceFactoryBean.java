package com.ibaset.common.dao.jndi;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jndi.JndiObjectFactoryBean;

/**
 * Extension of a Spring JndiObjectFactoryBean that checks that datasource from jndi actually can return a connection.
 * */
public class JndiDataSourceFactoryBean extends JndiObjectFactoryBean {

	static Logger logger = LoggerFactory.getLogger(JndiDataSourceFactoryBean.class);

	private Object defaultObject;
	
	@Override
	public Object getObject() {
		return test(super.getObject());
	}

	private Object test(Object object){
		DataSource ds = (DataSource) object;
		if(logger.isDebugEnabled()) logger.debug("DataSource test");
		if(!(ds instanceof EmptyDataSource))
		{
			//verify DS can create connections
			try
			{
				Connection conn = ds.getConnection();
				conn.close();
				if(logger.isDebugEnabled()) logger.debug("DataSource verified: "+ds);
			} 
			catch(SQLException ex)
			{
				if(logger.isDebugEnabled()) logger.debug("DataSource "+ds+" failed: ", ex);
				return defaultObject;
			}
		}
		return ds;
	}

	@Override
	public void setDefaultObject(Object defaultObject) {
		this.defaultObject = defaultObject;
		super.setDefaultObject(defaultObject);
	}

}
