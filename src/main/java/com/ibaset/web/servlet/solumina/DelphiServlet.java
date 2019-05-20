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
package com.ibaset.web.servlet.solumina;

import static com.ibaset.common.FrameworkConstants.ANONYMOUS;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.Executor;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.beanutils.converters.StringArrayConverter;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.ibaset.common.OutputParameter;
import com.ibaset.common.concurrent.SoluminaCompletableFuture;
import com.ibaset.common.context.SoluminaContextHolder;
import com.ibaset.common.context.TransactionalContext;
import com.ibaset.common.security.ISoluminaCipherUtils;
import com.ibaset.common.security.context.ContextUtil;
import com.ibaset.common.security.context.UserContext;
import com.ibaset.common.sql.IPassThroughProc;
import com.ibaset.common.sql.IPassThroughQuery;
import com.ibaset.common.sql.IResultObjectEncoder;
import com.ibaset.common.sql.ResultObject;
import com.ibaset.common.sql.security.SQLSecurityManager;
import com.ibaset.common.util.DateConverter;
import com.ibaset.common.util.NumberConverter;
import com.ibaset.common.util.OutputParameterConverter;
import com.ibaset.common.util.SoluminaUtils;
import com.ibaset.solumina.sfcore.application.IConnectionHeartbeat;
import com.ibaset.solumina.sfcore.application.ILogin;
import com.ibaset.solumina.sfcore.application.ISoluminaSavePoint;
import com.ibaset.solumina.sfcore.application.ITransactionManager;
import com.ibaset.solumina.stat.Statistics;

public final class DelphiServlet extends HttpServlet
{
	private static final boolean DEBUG = false;
	
    private static final long serialVersionUID = 3569740897718579093L;

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    
    private final static String COMMUNICATION_ERROR = "Communication Error";

    private Executor asyncTaskExecutor;

    private IConnectionHeartbeat connectionHeartbeat;

	private ILogin flagSetup = null;

    private ITransactionManager transactionManager = null;
    
    private ISoluminaSavePoint soluminaSavePoint = null;
    
    private Statistics statistics;
    
    private volatile static long CONNECTION_HEARTBEAT_PERIOD_IN_MILLIS = 0;
    
    //private FileOutputStream log; 
    
    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.GenericServlet#init(javax.servlet.ServletConfig)
     */
    public void init(ServletConfig config)
    {

        ConvertUtils.register(new NumberConverter(), java.lang.Number.class);
        ConvertUtils.register(new NumberConverter(), java.lang.Integer.class);

        ConvertUtils.register(new NumberConverter(), java.lang.Double.class);

        ConvertUtils.register(new NumberConverter(), java.lang.Float.class);

        ConvertUtils.register(new DateConverter(), java.util.Date.class);
        ConvertUtils.register(new OutputParameterConverter(),
                              OutputParameter.class);
        ConvertUtils.register(new StringArrayConverter(new String[1]),
                              java.lang.String[].class);

        WebApplicationContext ctx = WebApplicationContextUtils.getWebApplicationContext(config.getServletContext());

        this.setConnectionHeartbeat((IConnectionHeartbeat) ctx.getBean("connectionHeartbeat"));
        this.setAsyncTaskExecutor((Executor) ctx.getBean("asyncTaskExecutor"));
        this.setFlagSetup((ILogin) ctx.getBean("login"));
        this.setTransactionManager((ITransactionManager) ctx.getBean("soluminaTransactionManager"));
        this.setSoluminaSavePoint((ISoluminaSavePoint) ctx.getBean("soluminaSavePoint"));
        String[] names=ctx.getBeanNamesForType(Statistics.class, false, false);
        if(names.length>0) 
            this.setStatistics((Statistics) ctx.getBean(names[0]));
                
        SoluminaRequest.resultObjectEncoder = (IResultObjectEncoder) ctx.getBean("resultObjectEncoder");
        SoluminaRequest.DEBUG = DEBUG;
        String hashingAlgorithm = config.getInitParameter("hashingAlgorithm");
        if(hashingAlgorithm == null) hashingAlgorithm = "SHA";
        SoluminaRequest.hashingAlgorithm = hashingAlgorithm; 
        
        IPassThroughProc passThroughProc = (IPassThroughProc) ctx.getBean("passThroughProc");
        IPassThroughQuery passThroughQuery = (IPassThroughQuery) ctx.getBean("passThroughQuery");
        SQLSecurityManager securityManager = (SQLSecurityManager) ctx.getBean("SQLSecurityManager");
        ISoluminaCipherUtils soluminaCipherUtils = (ISoluminaCipherUtils) ctx.getBean("soluminaCipherUtils");
        soluminaCipherUtils.setupSqlEncryptionFlag();
        
        SqlRequest.setSecurityManager(securityManager);
        SqlRequest.setPassThroughQuery(passThroughQuery);
        SqlRequest.setSoluminaCipherUtils(soluminaCipherUtils);
        
        ProcedureRequest.soluminaSavePoint = this.soluminaSavePoint;
        ProcedureRequest.gateway = new Gateway(securityManager, passThroughQuery, passThroughProc, statistics);
        ProcedureRequest.disableSavepoints = StringUtils.equalsIgnoreCase("true", config.getInitParameter("disableSavepoints"));
        
        String maxMemReqSize=config.getInitParameter("maxMemoryRequestSize");
        if(maxMemReqSize!=null)
        {
        	try
        	{
        		SoluminaUtils.setMaxMemoryRequestSize(Integer.parseInt(maxMemReqSize));
        	}
        	catch (NumberFormatException e) {
        	    logger.debug(e.getMessage(), e);
        	}
        }
        
        setConnectionHeartbeatTimestamp(config);
        
    }

	private void updateConnectionHeartbeatTimestamp() {
		UserContext ctx = SoluminaContextHolder.getUserContext();

		if (StringUtils.isNotEmpty(ctx.getConnectionId())
				&& !StringUtils.equals(ContextUtil.getUsername(), ANONYMOUS)) {

			try {
				Date date = new Date();
				long currentTimeInMilliSec = date.getTime();
				long lastUpdatedHeartBeatPeriod = ctx.getLastUpdatedHeartBeatTime();

				if ((currentTimeInMilliSec - lastUpdatedHeartBeatPeriod) > DelphiServlet
						.getConnectionHeartBeatPeriod()) {
					SoluminaCompletableFuture.runAsync(() -> connectionHeartbeat.updateHeartbeat(), asyncTaskExecutor)
							.whenComplete((voidValue, ex) -> {
								if (ex != null) {
									logger.warn("Unable to update heartbeat time for connectionId: "
											+ ctx.getConnectionId(), ex);
								}
							});
				}
			} catch (Throwable e) {
				logger.warn("Error on updating heartbeat time for connectionId: " + ctx.getConnectionId(), e);
			}
		}
	}
    
    private void setConnectionHeartbeatTimestamp(ServletConfig config) 
	{
		CONNECTION_HEARTBEAT_PERIOD_IN_MILLIS = 60000;
		String connectionHeartBeatPeriod = config.getServletContext().getInitParameter("connectionHeartBeatPeriodInSeconds"); 
        if(StringUtils.isNotEmpty(connectionHeartBeatPeriod))
        {
        	try
        	{
	        	long connectionHeartBeatPeriodInMillis = Long.parseLong(connectionHeartBeatPeriod)*1000;
	        	if(connectionHeartBeatPeriodInMillis > 0)
	        		CONNECTION_HEARTBEAT_PERIOD_IN_MILLIS = connectionHeartBeatPeriodInMillis;
	        	else
	        		logger.info("Connection HeartBeat Period can not be allow less than or equal to 0. Set to default 60 seconds.");
        	}
        	catch(NumberFormatException nfe)
        	{
        		logger.error("Connection HeartBeat Period is not numeric. Set to default 60 secods.");
        	}
        }
	}
	
	public static long getConnectionHeartBeatPeriod()
    {
    	return CONNECTION_HEARTBEAT_PERIOD_IN_MILLIS;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest,
     *      javax.servlet.http.HttpServletResponse)
     */
	@SuppressWarnings({ "unchecked", "static-access" })
	public void doPost(HttpServletRequest request1, HttpServletResponse response) throws ServletException,
                                                                                IOException
    {
//		Debug output of request/response content to a log stream
//		request1 = new LoggingHttpServletRequest(request1, log);
//		response = new LoggingHttpServletResponse(response, log);
		
    	if(statistics != null && statistics.isEnabled()) statistics.startCategory(Statistics.REQUEST_CATEGORY);
        boolean isCommit = true;
        SoluminaRequest request = null;
        try
        {
    		UserContext ctx = ContextUtil.getUser().getContext();
    		ctx.setInitiatedByClient(true);
    		ctx.setUserHttpSession(request1.getSession());
        	request = SoluminaRequest.createRequest(request1, response);
        	String procedureName = request1.getParameter(request.PROCEDURE_NAME);
        	//String procedureName = request.getString(request1.getParameterMap(), request.PROCEDURE_NAME);
        	logger.debug("Delphi Request: "+procedureName);
        	
        	TransactionalContext transactionalContext = SoluminaContextHolder.getTransactionalContext();
            if (StringUtils.equals(procedureName, "SFCORE_LOGOUT_USER")) {
                transactionalContext.setInvalidateUserSession(Boolean.TRUE);
            }
        	
           	transactionManager.initOnRequest(false);
        	if(request.needLogin())
        	{
        		try
        		{
        			flagSetup.setUp(false, ctx.getConnectionType(), ctx.getUserType());
        		}
                catch (Throwable t)
                {
                    logger.error("Error logging in user", t);
                    transactionManager.rollback();
                }
        	}
        	
        	updateConnectionHeartbeatTimestamp();
        	
        	isCommit = request.execute();
        	if(statistics != null) statistics.endCategory(!isCommit, false);        	
        }
        catch (Throwable e)
        {
        	isCommit = false;
        	if(statistics != null) statistics.endCategory(true, false);
            logger.error("Error on processing Solumina request", e);
            try
            {
                transactionManager.rollback();
            }
            catch (Exception e1)
            {
            	logger.error("Error rolloing back transaction", e1);
            }
            
            String errorMsg = e.getMessage();
            if ( errorMsg!=null && (errorMsg.contains("SFCORE_CONNECTION_PARAMS") 
            		|| errorMsg.contains("SFCORE_CONN_DETLS_CONN_FK")
            		|| errorMsg.contains("SFCORE_CONN_EVNT_CONN_FK")
            		|| errorMsg.contains("SFCORE_CONN_PARMS_CONN_FK")))
            {
            	e = new Throwable(COMMUNICATION_ERROR);
            }
            
            ResultObject o = new ResultObject();
            o.addError(e);
            //charset set to null to avoid UnsupportedEncodingException, 
            try 
            {
				request.writeResultObject(o, false, null);
			} 
            catch (Exception e1) 
			{
                logger.error("Unable to send error report to client", e1);
			}
        }
        finally
        {
            transactionManager.cleanupOnResponse(false, isCommit);
        	if(request!=null)request.close();
        	
            TransactionalContext transactionalContext = SoluminaContextHolder.getTransactionalContext();
            if (transactionalContext.getInvalidateUserSession() != null && transactionalContext.getInvalidateUserSession().booleanValue()) {
                request1.getSession().invalidate();
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest,
     *      javax.servlet.http.HttpServletResponse)
     */
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException,
                                                                               IOException
    {
    	//GET requests are ignored due to security concerns
    	//However, sometimes GET-requests are useful during debugging
    	if(DEBUG) doPost(request, response);
    }

	public void setAsyncTaskExecutor(Executor asyncTaskExecutor) {
		this.asyncTaskExecutor = asyncTaskExecutor;
	}
	
	public void setConnectionHeartbeat(IConnectionHeartbeat connectionHeartbeat) {
		this.connectionHeartbeat = connectionHeartbeat;
	}
    
    public void setFlagSetup(ILogin flagSetup)
    {
        this.flagSetup = flagSetup;
    }

    public ITransactionManager getTransactionManager()
    {
        return transactionManager;
    }

    public void setTransactionManager(ITransactionManager transactionManager)
    {
        this.transactionManager = transactionManager;
    }

	public void setStatistics(Statistics statistics) 
	{
		this.statistics = statistics;
	}

	public ISoluminaSavePoint getSoluminaSavePoint() {
		return soluminaSavePoint;
	}

	public void setSoluminaSavePoint(ISoluminaSavePoint soluminaSavePoint) {
		this.soluminaSavePoint = soluminaSavePoint;
	}

}
