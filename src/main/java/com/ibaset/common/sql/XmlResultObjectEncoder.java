/**
 * Proprietary and Confidential
 * Copyright 1995-2018 iBASEt, Inc.
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
package com.ibaset.common.sql;


import static com.ibaset.common.util.SoluminaUtils.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibaset.common.BinaryParameter;
import com.ibaset.common.FrameworkConstants;
import com.ibaset.common.security.context.ContextUtil;

public final class XmlResultObjectEncoder implements IResultObjectEncoder
{
    protected static final Logger logger = LoggerFactory.getLogger(XmlResultObjectEncoder.class);
    
    private static final byte[] EMPTY=new byte[0];

    private static final byte[] HEADER_1="<?xml version=\"1.0\" encoding=\"".getBytes();
    
    private static final byte[] HEADER_2="\"?>\n".getBytes();

    private static final byte[] RESULTS = "</results>".getBytes();

    private static final byte[]  ERROR = "</error>\n".getBytes();

    private static final byte[]  X2 = "\">".getBytes();

    private static final byte[]  ERROR_TYPE = "\t<error type=\"".getBytes();

    private static final byte[] END_EVENTS = "\t</events>\n".getBytes();

    private static final byte[]  DATA_EVENT = "</data>\n\t\t</event>\n".getBytes();

    private static final byte[]  ID_DATA = "</id>\n\t\t\t<data>".getBytes();

    private static final byte[]  TIME_ID = "</time>\n\t\t\t<id>".getBytes();

    private static final byte[]  TIME = "\">\n\t\t\t<time>".getBytes();

    private static final byte[]  EVENT_INDEX = "\t\t<event index=\"".getBytes();

    private static final byte[] BYTES1 = "\t<events>\n".getBytes();

    private static final byte[] EVENTS = "\t<events/>\n".getBytes();

    private static final byte[] END_DATA = "\t</data>\n".getBytes();

    private static final byte[] END_ROW = "\t\t</row>\n".getBytes();

    private static final byte[] END_COLUMN = "</column>\n".getBytes();

    private static final byte[] END_BRACKET = ">".getBytes();

    private static final byte[] IS_NULL_TRUE = " isNull=\"true\"".getBytes();

    private static final byte[]  X1 = ">\n".getBytes();

    private static final byte[]  X = "\"".getBytes();

    private static final byte[]  ROW_INDEX = "\t\t<row index=\"".getBytes();

    private static final byte[]  IS_SELECTED_TRUE = " isSelected=\"true\"".getBytes();

    private static final byte[] ROWS = "\t\t<rows/>\n".getBytes();

    private static final byte[] END_COL_TYPES = "\t\t</column_types>\n".getBytes();

    private static final byte[]  TYPE_COLUMN = "</type>\n\t\t\t</column>\n".getBytes();

    private static final byte[]  SIZE_TYPE = "</size>\n\t\t\t\t<type>".getBytes();

    private static final byte[]  SIZE = "\t\t\t\t<size>".getBytes();

    private static final byte[]  STRING = "</name>\n".getBytes();

    private static final byte[]  NAME = "\">\n\t\t\t\t<name>".getBytes();

    private static final byte[]  COLUMN_INDEX = "\t\t\t<column index=\"".getBytes();
    
    private static final byte[]  VALUE_LENGTH = " length=\"".getBytes();

    private static final byte[] TYPES = "\t\t<column_types>\n".getBytes();

    private static final byte[] COLUMN_TYPES = "\t\t<column_types/>\n".getBytes();

    private static final byte[]  RESULTS_SESSION_ID = "<results>\n\t<session_id>".getBytes();

    private static final byte[]   END_SESSION_ID=  "</session_id>\n".getBytes();
    
    private static final byte[]  CONNECTION_ID = "\t<solumina_connection_id>".getBytes();
    private static final byte[]  END_CONNECTION_ID=  "</solumina_connection_id>\n".getBytes();
    private static final byte[]  DATA = "\t<data>\n".getBytes();
    
    private static final byte[]  BATCH = "<batch>\n".getBytes();
    private static final byte[]  END_BATCH = "</batch>\n".getBytes();

    private static final byte[]  TRANSACTION_INDEX = "\t<transaction index=\"".getBytes();
    private static final byte[]  END_TRANSACTION = "\t</transaction>\n".getBytes();
    
    public XmlResultObjectEncoder() 
    {
	}
    
	public String getResponseType()
	{
        return "text/xml";
    }
    private static void writeIntBytes(OutputStream outputStream, long i) throws IOException
    {
        if(i<0) 
        {
            outputStream.write('-');
            i=-i;
        }
        if(i>9) writeIntBytes(outputStream, i/10);
        outputStream.write('0'+(int)(i%10));
    }
    
    @Override
	public void renderResultObject(ResultObject object, OutputStream response) throws IOException 
	{
		this.renderResultObject(object, response, false);
	}
    
    @Override
    public void renderResultObject(ResultObject object, OutputStream response, boolean useBinary) throws IOException
    {
    	this.renderResultObject(object, response, useBinary, null);
    }

	@Override
	public void renderResultObjects(List<ResultObject> results,	OutputStream response) throws IOException 
	{
		this.renderResultObjects(results, response, false);
	}
	
	@Override
	public void renderResultObjects(List<ResultObject> results,	OutputStream response, boolean useBinary) throws IOException 
    {
		this.renderResultObjects(results, response, useBinary, null);
    }

	@Override
	public void renderResultObject(ResultObject object, OutputStream outputStream, boolean useBinary, String encodingType) throws IOException
    {
    	String encoding = object.getCharsetName() == null ? "ISO-8859-1" : object.getCharsetName();
    	outputStream.write(HEADER_1);
    	outputStream.write(encoding.getBytes());
    	outputStream.write(HEADER_2);
        outputStream.write(RESULTS_SESSION_ID);
        if(object.getSessionId()!=null) outputStream.write(object.getSessionId().getBytes());
        outputStream.write(END_SESSION_ID);
        outputStream.write(CONNECTION_ID);
        if(object.getConnectionId()!=null) outputStream.write(object.getConnectionId().getBytes());
        outputStream.write(END_CONNECTION_ID);
        writeResultObject(object, outputStream, useBinary, encodingType);
        outputStream.write(RESULTS);
        outputStream.flush();
    }
    
	@Override
    public void renderResultObjects(List<ResultObject> results,	OutputStream outputStream, boolean useBinary, String encodingType) throws IOException 
    {
    	if(results ==null || results.size()==0) throw new IllegalArgumentException("No results provided");
    	ResultObject object = results.get(0);
    	String encoding = object.getCharsetName() == null ? "ISO-8859-1" : object.getCharsetName();
    	outputStream.write(HEADER_1);
    	outputStream.write(encoding.getBytes());
    	outputStream.write(HEADER_2);
        outputStream.write(RESULTS_SESSION_ID);
        if(object.getSessionId()!=null) outputStream.write(object.getSessionId().getBytes());
        outputStream.write(END_SESSION_ID);
        outputStream.write(CONNECTION_ID);
        if(object.getConnectionId()!=null) outputStream.write(object.getConnectionId().getBytes());
        outputStream.write(END_CONNECTION_ID);
        outputStream.write(BATCH);
        List<ColumnHeader> maxHeaders = object.getColumnHeaders();
        //calculate maximum header column lengths
    	for(int i=1;i<results.size();++i)
    	{
    		ResultObject res=results.get(i);
    		List<ColumnHeader> headers = res.getColumnHeaders();
    		if(headers.size()!=maxHeaders.size()) throw new IllegalArgumentException("Batch results must have same number of columns");
    		for(int k=0;k<headers.size();++k)
    		{
    			if(headers.get(k).getSize() > maxHeaders.get(k).getSize())
    			{
    				maxHeaders.set(k, headers.get(k));
    			}
    		}
    		res.setColumnHeaders(maxHeaders);
    	}
        
    	for(int i=0;i<results.size();++i)
    	{
            outputStream.write(TRANSACTION_INDEX);
            writeIntBytes(outputStream, i+1L);
            outputStream.write(X);
            outputStream.write(X1);
            writeResultObject(results.get(i), outputStream, useBinary, encodingType);
            outputStream.write(END_TRANSACTION);
    	}
        outputStream.write(END_BATCH);
        outputStream.write(RESULTS);
        outputStream.flush();
	}
    
	private void writeResultObject(ResultObject object,
                                   OutputStream outputStream,
                                   boolean useBinary, 
                                   String encodingType) throws IOException
    {
       
        //object.fixHeaderLengths();
        outputStream.write(DATA);

        final List<ColumnHeader> columnHeaders = object.getColumnHeaders();
        final int columnHeaderCount = columnHeaders.size();
        ColumnHeader header = null;

        if (object.getColumnHeaders().isEmpty())
        {
            outputStream.write(COLUMN_TYPES);
        }
        else
        {
            outputStream.write(TYPES);

            for(int i=0;i<columnHeaderCount;++i)
            {
                header = columnHeaders.get(i);
                if (!StringUtils.equals(header.getColumnName(),
                                        FrameworkConstants.ROWNUM))
                {

                    outputStream.write(COLUMN_INDEX);
                    writeIntBytes(outputStream, header.getIndex());
                    outputStream.write(NAME);
                    writeObject(outputStream, header.getColumnName(), object.getCharsetName());
                    outputStream.write(STRING);
                    outputStream.write(SIZE);
                    writeIntBytes(outputStream, header.getSize());
                    outputStream.write(SIZE_TYPE);
                    outputStream.write(header.getType().getBytes());
                    outputStream.write(TYPE_COLUMN);
                }
            }
            outputStream.write(END_COL_TYPES);
        }

        final List<Row> rows = object.getRows();
        final int rowCount = rows.size();

        Row row = null;
        Column column = null;

        if (rowCount == 0)
        {
            outputStream.write(ROWS);
        }
        for(int i=0;i<rowCount;++i)
        {
            row = rows.get(i);
            byte[] isSelected = null;
            if (row.isSelected())
            {
                isSelected = IS_SELECTED_TRUE;
            }
            else
            {
                isSelected = EMPTY;
            }
            outputStream.write(ROW_INDEX);
            writeIntBytes(outputStream, row.getIndex());
            outputStream.write(X);
            outputStream.write(isSelected);
            outputStream.write(X1);

            final List<Column> columns = row.getColumns();
            final int columnCount = columns.size();
            String clientType = ContextUtil.getUser().getContext().getUserLocaleParam("@ClientType");
            for(int k=0;k<columnCount;++k)
            {
                column = columns.get(k);
                header = columnHeaders.get(column.getIndex() - 1);
                if (!StringUtils.equals(header.getColumnName(),
                                        FrameworkConstants.ROWNUM))
                {
                	Object columnValue = column.getValue();
                	if (isColumnValueNotNull(columnValue) || isLastColumn(columns, k) || 
                 			isEncodingTypeBinXml(useBinary, encodingType) || isClientNotStandard(clientType)) 
                	{
	                    outputStream.write(COLUMN_INDEX);
	                    writeIntBytes(outputStream, column.getIndex());
	                    outputStream.write(X);
	
	                    byte[] data = null;
	                    BinaryParameter bp = null;
	                    if (columnValue == null)
	                    {
	                        outputStream.write(IS_NULL_TRUE);
	                    }
	                    else if(columnValue instanceof byte[])
	                    {
	                    	data = (byte[])columnValue;
	                    	if(useBinary)
	                    	{
		                        outputStream.write(VALUE_LENGTH);
		                        writeIntBytes(outputStream, data.length);
		                        outputStream.write(X);
	                    	}
	                    }
	                    else if(columnValue instanceof BinaryParameter)
	                    {
	                    	bp = (BinaryParameter)columnValue;
	                    	if(useBinary)
	                    	{
		                        outputStream.write(VALUE_LENGTH);
		                        writeIntBytes(outputStream, bp.getSize());
		                        outputStream.write(X);
	                    	}
	                    }
	                    outputStream.write(END_BRACKET);
	                    if (columnValue instanceof Date)
	                    {
	                        writeObject(outputStream, getSimpleDateFormat().format((Date) columnValue), object.getCharsetName());
	                    }
	                    else if(data != null)
	                    {
	                    	if(useBinary) outputStream.write(data);
	                    	else outputStream.write( Base64.encodeBase64(data) );
	                    }
	                    else if(bp!=null)
	                    {
	                    	write(outputStream, bp, useBinary, !object.canCache());
	                    }
	                    else
	                    {
	                    	writeObject(outputStream, columnValue, object.getCharsetName());
	                    }
	                    outputStream.write(END_COLUMN);
                	}
                }
            }
            outputStream.write(END_ROW);

        }
        outputStream.write(END_DATA);

        Iterator<Event> eventIterator = object.getEvents().iterator();
        Event event = null;
        int index = 1;

        if (object.getEvents().isEmpty())
        {
            outputStream.write(EVENTS);

        }
        else
        {
            outputStream.write(BYTES1);
        }
        while (eventIterator.hasNext())
        {

            event = eventIterator.next();
            outputStream.write(EVENT_INDEX);
            writeIntBytes(outputStream, index);
            outputStream.write(TIME);
            outputStream.write(getSimpleDateFormat().format(event.getDate()).getBytes());
            outputStream.write(TIME_ID);
            outputStream.write(event.getId().getBytes());
            outputStream.write(ID_DATA);
            writeObject(outputStream, event.getCommand(), object.getCharsetName());
            outputStream.write(DATA_EVENT);

            index++;

        }

        if (!object.getEvents().isEmpty())
        {
            outputStream.write(END_EVENTS);

        }

        Iterator<Throwable> errorIterator = object.getErrors().iterator();
        while (errorIterator.hasNext())
        {
            Throwable t = errorIterator.next();
            outputStream.write(ERROR_TYPE);
            outputStream.write(t.getClass().getName().getBytes());
            outputStream.write(X2);
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            PrintStream s = new PrintStream(os);
            t.printStackTrace(s);
            writeObject(outputStream, new String(os.toByteArray()), object.getCharsetName());
            outputStream.write(ERROR);
        }

    }

	private boolean isClientNotStandard(String clientType) {
		return !StringUtils.equalsIgnoreCase(clientType, "Standard");
	}
	
	private boolean isColumnValueNotNull(Object columnValue) 
	{
		return columnValue != null;
	}
	
	private boolean isEncodingTypeBinXml(boolean useBinary, String encodingType) 
	{
		return useBinary && StringUtils.equalsIgnoreCase(encodingType, "binxml");
	}
	
	private boolean isLastColumn(final List<Column> columns, int k) 
	{
		return k == columns.size()-1;
	}
	
    private SimpleDateFormat getSimpleDateFormat()
    {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.m");
    }
    /*
     * Writes BinaryParameter to given output stream and then deletes it. 
     * */
    private void write(final OutputStream outputStream,final BinaryParameter bp, boolean useBinary, boolean canDelete) throws IOException{
    	try
    	{
	    	if(bp.isInMemory())
	    	{
	    		if(useBinary)
	    		{
	    			outputStream.write(bp.getByteArray());
	    		}
	    		else
	    		{
	    			outputStream.write(Base64.encodeBase64(bp.getByteArray()));
	    		}
	    	}
	    	else
	    	{
				InputStream is = null;
		        try 
		        {
					is = bp.getInputStream();
		            byte buffer[] = new byte[8192];
		            for (int n; (n = is.read(buffer)) >= 0;) 
		            {
		            	if(useBinary)
		            	{
		            		outputStream.write(buffer, 0, n);
		            	}
		            	else
		            	{
		            		if(n == buffer.length)
		            		{
		            			outputStream.write(Base64.encodeBase64(buffer));
		            		}
		            		else
		            		{
		            			byte[] b = new byte[n];
		            			System.arraycopy(buffer, 0, b, 0, n);
		            			outputStream.write(Base64.encodeBase64(b));
		            		}
		            	}
		            }
		        } 
		        finally 
		        {
                    try {
                        if (is != null)
                            is.close();
                    } catch (IOException e) {
                        logger.debug(e.getMessage(), e);
                    }
		        }
	    	}
    	} 
    	finally
    	{
    		if(canDelete) bp.delete();
    	}
    }
    private void writeObject(final OutputStream outputStream,final Object obj,final String charsetName) throws IOException 
    {
        if (obj != null)
        {
            if (obj instanceof String)
            {
                writeXML(outputStream, (String) obj, charsetName);
            }
            else if (obj instanceof byte[])
            {
            	outputStream.write(Base64.encodeBase64((byte[]) obj));
            }
            else
            {
                writeXML(outputStream, obj.toString(), charsetName);
            }
        }
    	
    }
    protected void writeXML(final OutputStream outputStream, String str,final String charsetName)throws IOException 
    {
    	if(     str.indexOf('&')!=-1 || 
    			str.indexOf('<')!=-1 ||
    			str.indexOf('>')!=-1)
    	{
    		str = escapeXml(str);
    	}
    	if(charsetName!=null)
    	{
    		outputStream.write(str.getBytes(charsetName));
    	}
    	else
    	{
			int len = str.length();
			for (int i = 0; i < len; i++) 
			{
			    char c = str.charAt(i);
			    
			    // GE-7617
			    if ((int)c < 128)
                {
                    outputStream.write(c & 0xff); 
                }
                else
                {
                    String strInner = "&#"+ (int)c + ";";
                    for(char input  : strInner.toCharArray())
                    {
                        outputStream.write(input & 0xff);
                    }
                }
			}
    	}
    	
    }

}
