/**
 * Proprietary and Confidential
 * Copyright 1995-2012 iBASEt, Inc.
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
package com.ibaset.web.servlet.solumina;

import java.util.Iterator;
import java.util.Map;

import org.apache.commons.collections.map.ListOrderedMap;

import com.ibaset.common.BinaryParameter;
import com.ibaset.common.sql.Column;
import com.ibaset.common.sql.ColumnHeader;
import com.ibaset.common.sql.IPassThroughProc;
import com.ibaset.common.sql.ResultObject;
import com.ibaset.common.sql.Row;

public class ProcedureCall implements InvocationTarget {

	private IPassThroughProc passThroughProc;
	private String procName;
	private String[] procedureParameterNames;
	
	public ProcedureCall(IPassThroughProc passThroughProc, String procName,
			String[] procedureParameterNames) {
		super();
		this.passThroughProc = passThroughProc;
		this.procName = procName;
		this.procedureParameterNames = procedureParameterNames;
	}

	@Override
	public boolean isJava() {
		return false;
	}

	@Override
	public Object[] convertParameters(
			String[] paramNames,  
			Map<String, BinaryParameter> paramVals) throws Exception 
	{
        ListOrderedMap parameterMap = new ListOrderedMap();
        for (int i = 0; i < procedureParameterNames.length; i++)
        {
        	final String name = paramNames[i];
        	final BinaryParameter bp = paramVals.get(name);
        	parameterMap.put(procedureParameterNames[i], bp);
        }
		return new Object[]{parameterMap};
	}

	@SuppressWarnings("unchecked")
	@Override
	public ResultObject invoke(Object[] args) 
	{
        Map<String, BinaryParameter> binaryParams = (Map<String, BinaryParameter>)args[0];
        Map<String, Object> result = null;
        ResultObject resultObject = new ResultObject();
        try
        {
	        result = passThroughProc.execute(procName, binaryParams);
	        Iterator<String> it = result.keySet().iterator();
	        int i = 1;
	        Row row = null;
	        if (it.hasNext())
	        {
	            row = new Row(i);
	        }
	        while (it.hasNext())
	        {
	            String name = it.next().toString();
	            if (!"RESULT_NOT_FOUND".equals(name))
	            {
	                Object value = result.get(name);
	                if (value == null)
	                {
	                    value = "";
	                }
	                String type = value.getClass().getName();
	                resultObject.addColumnHeader(new ColumnHeader(i,
	                                                              name,
	                                                              type,
	                                                              value.toString()
	                                                                   .length()));
	                Column col = new Column(i, value);
	                row.addColumn(col);
	                i++;
	            }
	        }
	        if (row != null)
	        {
	            resultObject.addRow(row);
	        }
        }
        catch (Throwable e) 
        {
        	resultObject = new ResultObject();
        	resultObject.addError(e);
		}
        return resultObject;
	}

}
