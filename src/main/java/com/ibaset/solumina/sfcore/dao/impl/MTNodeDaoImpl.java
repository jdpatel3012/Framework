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
package com.ibaset.solumina.sfcore.dao.impl;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.ibaset.common.dao.JdbcDaoSupport;
import com.ibaset.common.enums.FrameworkEnums.ConnectionType;
import com.ibaset.common.sql.ParameterHolder;
import com.ibaset.solumina.sfcore.dao.IMTNodeDao;

public class MTNodeDaoImpl extends JdbcDaoSupport implements IMTNodeDao {

	public MTNodeDaoImpl() {}

	@Override
	public void registerNode(String nodeId, String machineName, String ipAddress) {
		StringBuffer insertSql = new StringBuffer()	.append("INSERT INTO SFCORE_MT_NODE ( ")
					.append("    MT_NODE_ID, ")
					.append("    START_TIME_STAMP_DATE, ")
					.append("    LAST_ACTIVE_TIME_STAMP, ")
					.append("    MACHINE_NAME, ")
					.append("    IP_ADDRESS")
					.append(" ) VALUES ( ")
					.append("    ?, ")
					.append(getTimestampFunction()+", ")
					.append(getTimestampFunction()+", ")
					.append("    ?, ")
					.append("    ?)");
		
		ParameterHolder params = new ParameterHolder();
		params.addParameter(nodeId);
		params.addParameter(machineName);
		params.addParameter(ipAddress);
		insert(insertSql.toString(), params);
	}

	@Override
	public void updateNodeLastActivityTime(String nodeId) 
	{
		StringBuffer updateSql = new StringBuffer().append(" UPDATE ")
				.append("    SFCORE_MT_NODE ")
				.append("SET ")
				.append("    LAST_ACTIVE_TIME_STAMP = "+getTimestampFunction())
				.append(" WHERE ")
				.append("    MT_NODE_ID = ?");

		ParameterHolder params = new ParameterHolder();
		params.addParameter(nodeId);
		update(updateSql.toString(), params);
	}

	@Override
	public void unregisterNode(String nodeId) 
	{
		StringBuffer updateSql = new StringBuffer().append(" DELETE FROM ")
				.append("    SFCORE_MT_NODE ")
				.append(" WHERE ")
				.append("    MT_NODE_ID = ?");

		ParameterHolder params = new ParameterHolder();
		params.addParameter(nodeId);
		delete(updateSql.toString(), params);
	}

	@Override
	public void removeInactiveNodes(Timestamp inactiveSince) 
	{
		StringBuffer updateSql = new StringBuffer().append(" DELETE FROM ")
				.append("    SFCORE_MT_NODE ")
				.append(" WHERE ")
				.append("    LAST_ACTIVE_TIME_STAMP <= ?");

		ParameterHolder params = new ParameterHolder();
		params.addParameter(inactiveSince);
		delete(updateSql.toString(), params);
	}
	
	public boolean isNodeIdRegistered(String nodeId)
	{
		ParameterHolder selectParams = new ParameterHolder();
		selectParams.addParameter(nodeId);
        List list = queryForListFromCache(
        		"SELECT "
        		+ "		MT.MT_NODE_ID "
        		+ " FROM "
        		+ "		SFCORE_MT_NODE MT "
        		+ " WHERE "
        		+ "		MT.MT_NODE_ID = ? ", selectParams);
        return list.size() > 0;
	}

	@Override
	public List<String> selectConnectionIdsFromInactiveNodes(Timestamp inactiveSince) 
	{
        ParameterHolder params = new ParameterHolder();
        params.addParameter(inactiveSince);
        List list = queryForListFromCache(
        		"SELECT "
        		+ "		CON.CONNECTION_ID "
        		+ " FROM "
        		+ "		SFCORE_CONNECTION_DESC CON, "
        		+ "		SFCORE_MT_NODE MT "
        		+ " WHERE "
        		+ "		CON.MT_NODE_ID = MT.MT_NODE_ID "
        		+ "     AND CON.CONNECTION_TYPE != '"+ ConnectionType.API_WEB.value() +"' "
        		+ "		AND MT.LAST_ACTIVE_TIME_STAMP <= ? ", params);
        List<String> result = new ArrayList<String>(list.size());
        for(int i=0;i<list.size();++i){
        	Map m = (Map)list.get(i);
        	String cid = (String) m.get("CONNECTION_ID");
        	result.add(cid);
        }
		return result;
	}
	
	public List<String> selectConnectionIds(String mtNodeId) 
	{
        ParameterHolder params = new ParameterHolder();
        params.addParameter(mtNodeId);
        List list = queryForListFromCache(
        		"SELECT "
        		+ "		CON.CONNECTION_ID "
        		+ " FROM "
        		+ "		SFCORE_CONNECTION_DESC CON "
        		+ " WHERE "
        		+ "     CON.CONNECTION_TYPE != '"+ ConnectionType.API_WEB.value() +"' AND "
        		+ "		CON.MT_NODE_ID = ? ", params);
        List<String> result = new ArrayList<String>(list.size());
        for(int i=0;i<list.size();++i){
        	Map m = (Map)list.get(i);
        	String cid = (String) m.get("CONNECTION_ID");
        	result.add(cid);
        }
		return result;
	}
	
	public List<String> selectDanglingConnectionIds() 
	{
        ParameterHolder params = new ParameterHolder();
        List list = queryForList(
        		 "SELECT "
        		+ "		CON.CONNECTION_ID "
        		+ " FROM "
        		+ "		SFCORE_CONNECTION_DESC CON "
        		+ " WHERE "
        		+ "     CONNECTION_TYPE != '"+ ConnectionType.API_WEB.value() +"' AND "
        		+ "		CON.MT_NODE_ID NOT IN (SELECT MT_NODE_ID FROM SFCORE_MT_NODE) ");
        List<String> result = new ArrayList<String>(list.size());
        for(int i=0;i<list.size();++i){
        	Map m = (Map)list.get(i);
        	String cid = (String) m.get("CONNECTION_ID");
        	result.add(cid);
        }
		return result;
	}
	
	//FND-28175
	public Date selectDBTimeStamp()
	{
		StringBuffer selectSql = new StringBuffer().append("SELECT ")
													.append(getSchemaPrefix() + "SFDB_SYSDATE()" )
													.append(getDualTable());
		
		return (Date) queryForObject(selectSql.toString(), Date.class);
	}
}
