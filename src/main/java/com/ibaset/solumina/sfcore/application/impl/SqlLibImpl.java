package com.ibaset.solumina.sfcore.application.impl;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.ibaset.common.dao.JdbcDaoSupport;
import com.ibaset.common.solumina.exception.SoluminaException;
import com.ibaset.common.sql.ParameterHolder;
import com.ibaset.common.sql.SQLTokenizer;
import com.ibaset.solumina.sfcore.application.ISqlLib;
import com.ibaset.solumina.sfcore.dao.IConfigurationDao;

public class SqlLibImpl implements ISqlLib 
{

	private IConfigurationDao configurationDao;
	private JdbcDaoSupport daoSupport;
	
	public List<Map<String, Object>> select(String sqlId, Map<String, Object> parameters) 
	{
		String sqlText = selectSqlIdText(sqlId);
		ArrayList<String> params=new ArrayList<String>();
		try
		{
			SQLTokenizer st=new SQLTokenizer(new StringReader(sqlText));
			int token =0;
			while((token=st.nextToken())!=SQLTokenizer.TT_EOF)
			{
				if(token==SQLTokenizer.TT_WORD && st.sval.charAt(0)==':')
				{
					String paramName = st.sval.substring(1);  
					params.add(paramName);
					sqlText = sqlText.replace(st.sval, "?");
				}
			}
		} 
		catch (Exception e) 
		{
			throw new RuntimeException(e);
		}
		ParameterHolder holder=new ParameterHolder();
		for(int i=0;i<params.size();++i)
		{
			String paramName = params.get(i);
			if(!parameters.containsKey(paramName)) throw new SoluminaException("Parameter "+paramName+" must be provided for "+sqlId);
			Object param = parameters.get(params.get(i));
			holder.addParameter(param);
		}
		return daoSupport.queryForList(sqlText, holder);
	}
	
	private String selectSqlIdText(String sqlId)
	{
		List res = configurationDao.selectSqlID(sqlId);
		if(res.size() == 0) throw new SoluminaException("SQL Id not found: "+sqlId);
		Map<String, Object> row = (Map<String, Object>)res.get(0);
		return (String)row.get("SQL_TEXT");
	}

	public void setDaoSupport(JdbcDaoSupport daoSupport) 
	{
		this.daoSupport = daoSupport;
	}

	public void setConfigurationDao(IConfigurationDao configurationDao) 
	{
		this.configurationDao = configurationDao;
	}

}
