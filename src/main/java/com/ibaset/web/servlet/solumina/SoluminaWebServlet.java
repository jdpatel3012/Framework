package com.ibaset.web.servlet.solumina;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
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
import com.ibaset.solumina.sfcore.application.ILogin;
import com.ibaset.solumina.sfcore.application.ISoluminaSavePoint;
import com.ibaset.solumina.sfcore.application.ITransactionManager;
import com.ibaset.solumina.stat.Statistics;

@Deprecated
@WebServlet(urlPatterns="/dummyServlet/notinused/*", asyncSupported = true)
public final class SoluminaWebServlet extends HttpServlet 
{
	private static final long serialVersionUID = -6018556364943647416L;
	

	private static final boolean DEBUG = false;
	

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    
    private final static String COMMUNICATION_ERROR = "Communication Error";

    private ILogin flagSetup = null;

    private ITransactionManager transactionManager = null;
    
    private ISoluminaSavePoint soluminaSavePoint = null;
    
    private Statistics statistics;
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
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest,
     *      javax.servlet.http.HttpServletResponse)
     */
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException,
                                                                                IOException
    {
		serveSoluminaRequest(request, response);
    }
	
	private void serveSoluminaRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException,
    IOException
    {
//		Debug output of request/response content to a log stream
//		request1 = new LoggingHttpServletRequest(request1, log);
//		response = new LoggingHttpServletResponse(response, log);
		
    	if(statistics != null && statistics.isEnabled()) statistics.startCategory(Statistics.REQUEST_CATEGORY);
        boolean isCommit = true;
        SoluminaRequest soluminaRequest = null;
        try
        {
    		UserContext ctx = ContextUtil.getUser().getContext();
    		ctx.setInitiatedByClient(true);
    		ctx.setUserHttpSession(request.getSession());
        	soluminaRequest = SoluminaRequest.createRequest(request, response);
        	@SuppressWarnings("static-access")
			String procedureName = request.getParameter(soluminaRequest.PROCEDURE_NAME);
        	//String procedureName = request.getString(request1.getParameterMap(), request.PROCEDURE_NAME);
        	logger.debug("Delphi Request: "+procedureName);
        	
        	TransactionalContext transactionalContext = SoluminaContextHolder.getTransactionalContext();
            if (StringUtils.equals(procedureName, "SFCORE_LOGOUT_USER")) {
                transactionalContext.setInvalidateUserSession(Boolean.TRUE);
            }

        	transactionManager.initOnRequest(false);
        	if(soluminaRequest.needLogin())
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
            
        	isCommit = soluminaRequest.execute();
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
            	logger.error("Error rolling back transaction", e1);
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
				soluminaRequest.writeResultObject(o, false, null);
			} 
            catch (Exception e1) 
			{
                logger.error("Unable to send error report to client", e1);
			}
        }
        finally
        {
            transactionManager.cleanupOnResponse(false, isCommit);
        	if(soluminaRequest!=null)soluminaRequest.close();
        	
        	TransactionalContext transactionalContext = SoluminaContextHolder.getTransactionalContext();
            if (transactionalContext.getInvalidateUserSession() != null && transactionalContext.getInvalidateUserSession().booleanValue()) {
                request.getSession().invalidate();
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
