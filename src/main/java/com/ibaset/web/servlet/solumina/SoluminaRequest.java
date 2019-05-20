/**
 * Proprietary and Confidential
 * Copyright 1995-2013 iBASEt, Inc.
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibaset.common.BinaryParameter;
import com.ibaset.common.FrameworkConstants;
import com.ibaset.common.context.SoluminaContextHolder;
import com.ibaset.common.context.TransactionalContext;
import com.ibaset.common.security.context.ContextUtil;
import com.ibaset.common.security.context.UserContext;
import com.ibaset.common.sql.Event;
import com.ibaset.common.sql.IResultObjectEncoder;
import com.ibaset.common.sql.ResultObject;
import com.ibaset.common.util.SoluminaUtils;
import com.ibaset.web.servlet.solumina.upload.MultipartFormData;
import com.ibaset.web.servlet.solumina.upload.ParameterParser;

public abstract class SoluminaRequest {

	//configurable parameters
    static String hashingAlgorithm = "SHA";
    static boolean DEBUG = false;
    static IResultObjectEncoder resultObjectEncoder = null;
	
	static final String X_SOLUMINA_CONNECTION_ID = "X-Solumina-Connection-Id";
	static final String SOLUMINA_CONNECTION_ID = "solumina_connection_id";
    static final String SQL = "sql";
    static final String START_ROW = "start_row";
    static final String END_ROW = "end_row";
    final static String OBJECT_NAME = "object_name";
    final static String PROCEDURE_NAME = "procedure_name";
    final static String BIND_SET = "bind_set";
    final static String KEY_SET = "key_set";
    final static String BATCH = "batch";
    final static String AUX_KEY_SET = "aux_key_set";
	
    protected static final Logger logger = LoggerFactory.getLogger(SoluminaRequest.class);
    
    protected HttpServletRequest request;
    protected HttpServletResponse response;
    protected Map<String, BinaryParameter> requestParameterMap;
    
    String sessionId;
    String ifNoneMatch;
    String encodingType;
    boolean canGzip = false;
    boolean useBinaryXml = false;
    String procedureName;
    
    
    public SoluminaRequest(Map<String, BinaryParameter>requestParameterMap,
			HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		super();
		this.request = request;
		this.response = response;
		this.requestParameterMap = requestParameterMap;
		
		sessionId = request.getSession(true).getId();
		ifNoneMatch = request.getHeader("If-None-Match");
		encodingType = request.getHeader("Accept-Encoding");
        if (encodingType != null)
        {
            if (encodingType.indexOf("gzip") >= 0)
            {
                canGzip = false;
            }
            if (encodingType.toUpperCase().indexOf(StringUtils.upperCase("binxml")) >= 0)
            {
            	useBinaryXml = true;
            }
        }
        final Locale locale = request.getLocale();
        if (ContextUtil.getUser() != null
                && ContextUtil.getUser().getContext() != null)
        {
            ContextUtil.getUser().getContext().setLocale(locale);
        }
        
        final String headerConnectionId = getString(requestParameterMap, "solumina_connection_id");
        if(!StringUtils.isBlank(headerConnectionId))
        {
            ContextUtil.getUser().getContext().setConnectionId(headerConnectionId);
        }
        procedureName = getString(requestParameterMap, PROCEDURE_NAME);
	}
    
    public abstract boolean execute() throws Exception;
    
    public void close()
    {
    	cleanupParameters(requestParameterMap);    	
    }
    
    public static SoluminaRequest createRequest(HttpServletRequest request,	HttpServletResponse response) throws Exception
    {
    	Map<String, BinaryParameter> requestParameterMap = SoluminaRequest.loadParameters(request);
    	String sql = getString(requestParameterMap, SQL);
    	boolean isSql = StringUtils.isNotBlank(sql);
    	if(isSql) return new SqlRequest(requestParameterMap, request, response);
    	return new ProcedureRequest(requestParameterMap, request, response);
    }
    
    public boolean isSystemProc()
    {
    	return  StringUtils.equalsIgnoreCase(procedureName, "sfcore_license_loader") ||
                StringUtils.contains(procedureName, "ILogin.getRelease") ||
                StringUtils.contains(procedureName, "ILogin.getUserAppRolePrivs") ||
                StringUtils.contains(procedureName, "ILogin.getX") ||
                StringUtils.contains(procedureName, "ILogin.log") ||
                StringUtils.contains(procedureName, "ISoluminaInfo") ||
                StringUtils.contains(procedureName, "ITransactionManager");
    }
    public boolean needLogin()
    {
    	UserContext ctx = ContextUtil.getUser().getContext();
        if (ContextUtil.getUser() != null
                && ctx != null
                && StringUtils.isEmpty(ctx.getConnectionId())
                && !isSystemProc())
        {
        	return true;
        }
        return false;
    	
    }
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
	public static Map<String, BinaryParameter> loadParameters(HttpServletRequest request) throws FileUploadException
    {
    	Map<String, BinaryParameter>requestParameterMap = null;
    	String charsetName = null;
        boolean isMultipartServletRequest =  ServletFileUpload.isMultipartContent(request);
        if (!isMultipartServletRequest)
        {
        	requestParameterMap = new HashMap<String, BinaryParameter>();
        	//GET parameters are ignored due to security concerns
        	//However, sometimes GET-requests are useful during debugging
        	if(DEBUG)
        	{
                java.util.Enumeration<String> names = request.getParameterNames();
                while(names.hasMoreElements())
                {
                    String name = names.nextElement();
                    requestParameterMap.put(name, new ByteArrayParameter(request.getParameter(name).getBytes()));
                }
        	}
        }
        else
        {
        	int requestSize = request.getContentLength();
        	if(requestSize > SoluminaUtils.getMaxMemoryRequestSize())
        	{
        		requestParameterMap = loadParametersInFile(request);
        	}
        	else
        	{
        		requestParameterMap = loadParametersInMemory(request);
        	}
        	//get charset in content type
        	String contentType = request.getContentType();
    		ParameterParser parser = new ParameterParser();
    		parser.setLowerCaseNames(true);
    		// Parameter parser can handle null input
    		Map params = parser.parse(contentType, ';');
        	charsetName = (String)params.get("charset");
        }
        if(charsetName != null)
        {
        	for(String name:requestParameterMap.keySet())
        	{
        		requestParameterMap.get(name).setCharset(charsetName);
        	}
        }
    	return requestParameterMap;
    }
    public static void cleanupParameters(Map<String, BinaryParameter> requestParameterMap)
    {
        if (requestParameterMap != null) {
            for (String name : requestParameterMap.keySet()) {
                try {
                    requestParameterMap.get(name).delete();
                } catch (Exception e) {
                    logger.debug(e.getMessage(), e);
                }
            }
        }
    }

    static Map<String, BinaryParameter> loadParametersInMemory(HttpServletRequest request1) throws FileUploadException
    {
    	MultipartFormData data = new MultipartFormData();
    	return data.parseRequest(request1);
    }
    
    @SuppressWarnings("unchecked")
	static Map<String, BinaryParameter> loadParametersInFile(HttpServletRequest request1) throws FileUploadException
    {
        // Create a factory for disk-based file items
        FileItemFactory factory = new DiskFileItemFactory();

        // Create a new file upload handler
        ServletFileUpload upload = new ServletFileUpload(factory);

        // Parse the request
        List<FileItem> items = upload.parseRequest(request1);
        
        Map<String, BinaryParameter> requestParameterMap = new HashMap<String, BinaryParameter>();
        Iterator<FileItem> iter = items.iterator();
        while (iter.hasNext()) 
        {
            FileItem item = (FileItem) iter.next();
            String fieldName = item.getFieldName();
            requestParameterMap.put(fieldName, new FileItemParameter(item));
        }
        return requestParameterMap;
    }

	protected static String getString(Map<String, BinaryParameter> requestParameterMap, String name) throws IOException
    {
    	BinaryParameter p = requestParameterMap.get(name);
    	if(p != null) return p.getString();
    	return null;
    }
    
    @SuppressWarnings("unchecked")
	static void assignContext(ResultObject resultObject, String sessionId, boolean isBatch)
    {
        if (ContextUtil.getUser() != null)
        {
        	UserContext ctx = ContextUtil.getUser().getContext();
            List<Event> l = ctx.popOffEvents();
            if(l!=null && l.size() > 0)resultObject.setEvents(l);
            
            TransactionalContext transactionalContext = SoluminaContextHolder.getTransactionalContext();
            if(transactionalContext.getResults()!=null)
            {
            	if(!resultObject.hasErrors()){
            		resultObject.add(transactionalContext.getResults());
            	}
            	
            	transactionalContext.setResults(null);
            }
        }
    	//Workaround for Windows Client that doesn't allow mixed results in a batch 
    	//Convert exception into a user message
    	if(isBatch){
    		if(resultObject.hasErrors())
	    	{
	        	Throwable error = resultObject.getErrors().get(0);
	        	resultObject.getErrors().clear();
	        	resultObject.getColumnHeaders().clear();
	        	resultObject.getRows().clear();
	        	Map<String, String> result = new HashMap<String, String>();
	            ByteArrayOutputStream os = new ByteArrayOutputStream();
	            PrintStream s = new PrintStream(os);
	            error.printStackTrace(s);
	            String userMessage = new String(os.toByteArray());
	           	result.put("@USERMESSAGE", userMessage);
	           	result.put("@ISERROR", FrameworkConstants.YES);
	        	resultObject.add(result);
	    	}
    		else
	    	{
	    		if(resultObject.getRows().isEmpty())
	    		{
		        	Map<String, String> result = new HashMap<String, String>();
		           	result.put("@USERMESSAGE", "");
		           	result.put("@ISERROR", FrameworkConstants.NO);
		        	resultObject.add(result);	    			
	    		}
	    	}
    	}
        
        resultObject.setSessionId(sessionId);
        resultObject.setConnectionId(ContextUtil.getUser().getContext().getConnectionId());
    }
	
    /**
     * @param response
     * @param resultObject
     * @param sessionId
     * @param canGzip
     * @throws IOException
     */
    protected void writeResultObject(
                                   ResultObject resultObject,
                                   boolean useHashing,
                                   String charsetName) throws Exception
    {
    	assignContext(resultObject, sessionId, false);
        if(useHashing) 
        {
        	String hash = resultObject.getHash(hashingAlgorithm);
        	if(hash.equals(ifNoneMatch))
        	{
        		response.addHeader(X_SOLUMINA_CONNECTION_ID, resultObject.getConnectionId());
        		response.sendError(HttpServletResponse.SC_NOT_MODIFIED);
        		return;
        	}
        	response.addHeader("ETag", hash);
        }
        response.setContentType(resultObjectEncoder.getResponseType());
        OutputStream sos = response.getOutputStream();

        if (canGzip)
        {
            sos = new GZIPOutputStream(sos);
        }
        if (logger.isDebugEnabled())
        {
        	if(resultObject.hasErrors())
        		logger.debug("request end: Error columns="+resultObject.getColumnHeaders().size()+" rows="+resultObject.getRows().size());
        	else
        		logger.debug("request end: Result columns="+resultObject.getColumnHeaders().size()+" rows="+resultObject.getRows().size());
        }
        resultObject.setCharsetName(charsetName);
        resultObjectEncoder.renderResultObject(resultObject, sos, useBinaryXml, encodingType);
    }

}
