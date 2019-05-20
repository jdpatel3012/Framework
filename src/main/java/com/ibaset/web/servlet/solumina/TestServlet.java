/**
 * Proprietary and Confidential
 * Copyright 1995-2019 iBASEt, Inc.
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

import static com.ibaset.common.FrameworkConstants.SAMEORIGIN;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;

import com.ibaset.common.client.SoluminaServiceLocator;
import com.ibaset.common.event.ExtensionRegistry;
import com.ibaset.common.solumina.ISoluminaInfo;
import com.ibaset.common.sql.Column;
import com.ibaset.common.sql.ColumnHeader;
import com.ibaset.common.sql.IPassThroughQuery;
import com.ibaset.common.sql.ResultObject;
import com.ibaset.common.sql.Row;
import com.ibaset.common.sql.security.SQLSecurityManager;
import com.ibaset.solumina.manifest.IManifestReader;
import com.ibaset.solumina.sfcore.application.ILicenseInfo;
import com.ibaset.solumina.sfcore.application.ILogin;

public class TestServlet extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	private static final String SOLUMINA_VERSION = "Solumina-G8";

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse)
	 */
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException,
	IOException
	{
		Date currentTime = new Date(System.currentTimeMillis());
		String username = StringUtils.upperCase(request.getParameter("username"));
		String password = request.getParameter("password");
		String errorMessage = "There was an error with the Username/Password supplied.  Please try again.";
		PrintWriter writer = response.getWriter();
		boolean loggedIn = (SoluminaWebLoginUtil.login(request, response, username, password))
							&& (SoluminaWebLoginUtil.hasPrivilege("@SoluminaDBA"));
		boolean emptyName = false;
		if (StringUtils.isEmpty(username))
		{
			emptyName = true;
		}
		else
		{
			username = new HTMLFilter().filter(username);
		}

		response.setContentType("text/html");
		response.addHeader("x-frame-options", SAMEORIGIN);
		
		writeHtmlHeader(writer);

		writer.println("	<body>                                                                                                                         ");
		writer.println("		<div id=\"page-container\" class=\"text-page\">                                                                            ");
		writer.println("			<div id=\"page\">                                                                                                      ");
		writer.println("				<div id=\"page-top\">                                                                                              ");
		writer.println("					<div id=\"title\">                                                                                             ");
		writer.println("						<h1>                                                                                                       ");
		writer.println("							<span class=\"in\">Test page</span>                                                                    ");
		writer.println("						</h1>                                                                                                      ");
		writer.println("						<p>                                                                                                        ");
		writer.println("							<span class=\"in\">Verification of Solumina Application Server Setup</span>                            ");

		writer.println("						</p>                                                                                                       ");
		writer.println("					</div>                                                                                                         ");
		writer.println("					<div id=\"sitemenu-container\">                                                                                ");
		writer.println("					</div>                                                                                                         ");
		writer.println("				</div>                                                                                                             ");
		writer.println("				<div class=\"clear\">                                                                                              ");
		writer.println("				</div>                                                                                                             ");
		writer.println("				<div id=\"page-content\">                                                                                          ");
		writer.println("					<div id=\"main\">                                                                                              ");
		writer.println("						<div id=\"main-top\">                                                                                      ");
		writer.println("						</div>                                                                                                     ");
		writer.println("						<div id=\"main-content\">                                                                                  ");
		writer.println("							<div class=\"article\">                                                                                ");
		writer.println("								<div class=\"article-content\">                                                                    ");
		writer.println("									<div class=\"RichTextElement\">                                                                ");


		if(!loggedIn)
		{
			if(!emptyName)
			{
				writer.println("<font color=red>"+errorMessage+"</font><br><HR>");
			}

			writer.println("										<form class=\"loginElement\" method=\"POST\" autocomplete=\"off\" action=\""+request.getContextPath()+"/test\" id=\"form\">         ");
			writer.println("											<h2>                                                                                   ");
			writer.println("												<span class=\"in\">Please Enter a valid Solumina Username/Password:</span>         ");
			writer.println("											</h2>                                                                                  ");
			writer.println("											<BR>                                                                                   ");
			writer.println("											<span class=\"in\">Username </span><span class=\"in\">                                 ");
			writer.println("											<input id=\"username\" type=\"text\" autocomplete=\"off\" name=\"username\" title=\"Username\"/>            ");
			writer.println("											<BR>                                                                                   ");
			writer.println("											</span> <span class=\"in\">Password&nbsp;</span><span class=\"in\">                    ");
			writer.println("											<input id=\"password\" type=\"password\" autocomplete=\"off\" name=\"password\" title=\"Password\"/>        ");
			writer.println("											<BR>                                                                                   ");
			writer.println("											</span>                                                                                ");
			writer.println("											<input type=\"submit\" class=\"submit\"/>                                              ");
			writer.println("										</form>                                                                                    ");
		}
		else
		{
			writer.println("<FONT color=green>Hello, "+username+"</font><HR>");

			writer.println("<H2><FONT color=green>Testing Database Connectivity:</font></H2>");
			if (getInfo(request, writer))
			{
			}
			
			try
			{
				loginToSolumina(writer);
				writer.println("<FONT color=green>Success logging in a user</font><HR>");
			}
			catch(Exception e)
			{
				String rawErrorMsg = e.getMessage();
				String errorMsg = null;
				
				writer.println("<FONT color=red>ERROR logging in a user<BR>");
				if ( rawErrorMsg.contains("NO_LICENSE_FOUND") )
				{
					errorMsg = "Solumina license not found, please contact your System Administrator.";
					
				}
				else
				{
					errorMsg = rawErrorMsg;
				}
				writer.println(errorMsg + "<BR>");
				writer.println("</FONT><HR>");
			}
			
			this.getSfdbInfo(writer);

			try
			{
				logoutFromSolumina();
				writer.println("<FONT color=green>Success logging out a user</font>");
			}
			catch(Exception e)
			{
				String rawErrorMsg = e.getMessage();
				String errorMsg = null;
				
				writer.println("<FONT color=red>ERROR logging out a user.<br><B>Please check to see if a Solumina License is loaded</B><br>");
				if ( rawErrorMsg.contains("NO_LICENSE_FOUND") )
				{
					errorMsg = "Please check to see if a Solumina License is loaded.";
					
				}
				else
				{
					errorMsg = rawErrorMsg;
				}
				writer.println(errorMsg + "<BR>");
				writer.println("</FONT>");
			}

		}
		if (loggedIn)
		{
			writer.println("<HR>");
			writer.println("<A HREF=\""+request.getContextPath()+"/sysinfo\">System Properties</A>");
		}

		writer.println("									</div>                                                                                         ");
		writer.println("								</div>                                                                                             ");
		writer.println("								<div class=\"article-info\">                                                                       ");
		writer.println("								</div>                                                                                             ");
		writer.println("							</div>                                                                                                 ");
		writer.println("							<div class=\"clear\">                                                                                  ");
		writer.println("							</div>                                                                                                 ");
		writer.println("						</div>                                                                                                     ");
		writer.println("						<div id=\"main-bottom\">                                                                                   ");
		writer.println("						</div>                                                                                                     ");
		writer.println("					</div>                                                                                                         ");
		writer.println("				</div>                                                                                                             ");
		writer.println("				<div class=\"clear\">                                                                                              ");
		writer.println("				</div>                                                                                                             ");
		if(loggedIn)
		{
			writer.println("					<div id=\"sitemenu-container\">                                                                                ");

			writer.println("<BR><A HREF=\""+request.getContextPath()+"/test\">Return to Test Page Login</A>");
			writer.println("					</div>                                                                                ");
		}
		writer.println("				<div id=\"page-bottom\">                                                                                           ");
		writer.println("					<div>                                                                                                          ");
		writer.println("						<p>                                                                                                        ");
		writer.println("							Copyright "+(currentTime.getYear()+1900)+" iBASEt                                                                                  ");
		writer.println("						</p>                                                                                                       ");
		writer.println("					</div>                                                                                                         ");
		writer.println("				</div>                                                                                                             ");
		writer.println("			</div>                                                                                                                 ");
		writer.println("		</div>                                                                                                                     ");
		writer.println("	</body>                                                                                                                        ");
		writer.println("</html>                                                                                                                           ");
		writer.println("                                                                                                                                  ");

		writer.close();
	}

	private void queryPublicConnection() throws Exception
	{
		IPassThroughQuery ptq = (IPassThroughQuery) SoluminaServiceLocator.locateService(IPassThroughQuery.class);
		ResultObject ro = ptq.executeQuery("SELECT COUNT(*) FROM SFCORE_USER", 1, 100, new java.util.HashMap(), new java.util.HashMap());
		if(ro.getErrors() !=null && ro.getErrors().size()>0)
		{
			String errors = "";
			for(int i=0;i<ro.getErrors().size();i++)
			{
				errors+=ro.getErrors().get(i)+":";
			}

			throw new Exception(errors);
		}
	}
	
	private void getSfdbInfo(PrintWriter out)
	{
		//JdbcDaoSupport dao = (JdbcDaoSupport) SoluminaServiceLocator.locateService(JdbcDaoSupport.class);
		IPassThroughQuery ptq = (IPassThroughQuery) SoluminaServiceLocator.locateService(IPassThroughQuery.class);
		ResultObject countResult = ptq.executeQuery("SELECT COUNT(*) FROM SFMFG.SFDB_INFO", 1, 1, new java.util.HashMap(), new java.util.HashMap());
		int recordCount = Integer.parseInt(countResult.getRows().get(0).getColumns().get(0).getValue().toString());
		ResultObject ro = ptq.executeQuery("SELECT RELEASE, VERSION, UPDT_USERID, TIME_STAMP, RELEASE_TYPE, EXE_COUNT, LONG_FND_VER, OOB_RELEASE FROM SFMFG.SFDB_INFO ORDER BY TIME_STAMP", 1, recordCount, new java.util.HashMap(), new java.util.HashMap());
		
		if(ro.getErrors() == null || ro.getErrors().size()==0)
		{
			out.println("<FONT color=green>Database Installation Information:</font>");
			displayResultObjectInATable(ro, out);
			out.println("<HR>");
		}
	}

	/**
	 * @param out
	 * @return
	 */
	private void loginToSolumina(PrintWriter out) throws Exception
	{
		ILogin login = (ILogin) SoluminaServiceLocator.locateService(ILogin.class);
		login.setUp(false);
		login.setSessionContext();
	}

	/**
	 * @param out
	 * @return
	 */
	private void logoutFromSolumina() throws Exception
	{
		ILogin login = (ILogin) SoluminaServiceLocator.locateService(ILogin.class);
		login.logout();
	}

	/**
	 * @param request
	 * @param out
	 * @return
	 * @throws IOException 
	 */
	private boolean getInfo(HttpServletRequest request, PrintWriter out) throws IOException
	{
		StringBuffer errorMessage = new StringBuffer("<B><FONT color=red>ERROR: Database Connection could not be established using MT_CONPOOL_USER</FONT></B>");
		Connection conn = SoluminaServiceLocator.getDatabaseConnection();
		ILicenseInfo licenseInfo = (ILicenseInfo) SoluminaServiceLocator.locateService(ILicenseInfo.class);
		ISoluminaInfo soluminaInfo = (ISoluminaInfo) SoluminaServiceLocator.locateService(ISoluminaInfo.class);
		SQLSecurityManager securityManager = (SQLSecurityManager) SoluminaServiceLocator.locateService(SQLSecurityManager.class);
		boolean error = true;
		if (conn != null)
		{
			DatabaseMetaData metaData;
			IManifestReader manifestReader = (IManifestReader) SoluminaServiceLocator.locateService(IManifestReader.class);
			try
			{
				metaData = conn.getMetaData();

				out.println("\t<B><FONT color=green>Success establishing database connection using "+metaData.getUserName()+":</font></B><br>");
				out.println("\t<B>Driver Name:                </B>" + metaData.getDriverName()                  + "<br>");
				out.println("\t<B>Driver Version:             </B>" + metaData.getDriverVersion()               + "<br>");
				out.println("\t<B>Database Version:           </B>" + metaData.getDatabaseProductVersion()      + "<br>");
				out.println("\t<B>IBA Version:                </B>" + soluminaInfo.getSoluminaMTVersion()                    + "<br>");		
				out.println("\t<B>Build Number:                </B>" + manifestReader.getMiddleTierVersion()                    + "<br>");		
				out.println("\t<B>SCM Revision:               </B>" + manifestReader.getRevision()                          + "<br>");		
				out.println("\t<B>Customer Extension Version(s): </B>" + getCustomerVersion()                      + "<br>");
				out.println("\t<B>JDBC Version:               </B>" + metaData.getJDBCMajorVersion()            + "<br>");
				out.println("\t<B>JDBC URL:                   </B>");
				out.println("<div style=\"word-wrap:break-word; display:inline;\" >" + metaData.getURL() + "</div><br>");
				out.println("\t<B>JDBC User Name:             </B>" + metaData.getUserName()                    + "<br>");
				out.println("\t<B>Security Enabled:           </B>" + securityManager.getInstance().isEnabled() + "<br>");
				out.println("\t<B>XML Enabled:                </B>" + licenseInfo.isXmlEnabled()                    + "<br>");
				out.println("\t<B>LTA Enabled:                </B>" + licenseInfo.isLtaEnabled()                    + "<br>");
				out.println("\t<BR>");
				error = false;
			}
			catch (SQLException e)
			{
				e.printStackTrace();
				error = true;
				out.println(errorMessage.append(e.getMessage()).toString()
						+ "<br>");
			}
			finally
			{
				SoluminaServiceLocator.releaseConnection(conn);
			}
		}
		return error;
	}


	private String getCustomerVersion() 
	{
		return ExtensionRegistry.getInstance().getVersion();
	}

	public void doGet(HttpServletRequest request, HttpServletResponse response)	throws ServletException,
	IOException
	{
		doPost(request, response);
	}
	
	private void displayResultObjectInATable(ResultObject ro, PrintWriter out)
	{
		List columnHeadersList = ro.getColumnHeaders();
		List rowsList = ro.getRows();
		
		Iterator columnHeadersIterator = columnHeadersList.iterator();
		Iterator rowsIterator = rowsList.iterator();
		
		out.println("<table border=1>");
		out.println("<tr>");
		while (columnHeadersIterator.hasNext())
		{
			ColumnHeader columnHeader = (ColumnHeader) columnHeadersIterator.next();
			out.println("<td>" + columnHeader.getColumnName() + "</td>");
		}
		out.println("</tr>");
		while (rowsIterator.hasNext())
		{
			Row row = (Row) rowsIterator.next();
			List rowColumnsList = row.getColumns();
			Iterator rowColumnsIterator = rowColumnsList.iterator();
			out.println("<tr>");
			while (rowColumnsIterator.hasNext())
			{
				Column column = (Column) rowColumnsIterator.next();
				if (column.getValue() == null)
				{
					out.println("<td>&nbsp;</td>");
				}
				else
				{
					out.println("<td>" + column.getValue().toString() + "</td>");
				}
			}
			out.println("</tr>");
		}
		out.println("</table>");
	}

	private void writeHtmlHeader(PrintWriter writer)
	{
		writer.println("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">                 ");
		writer.println("<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\" lang=\"en\">                                                         ");
		writer.println("	<head>                                                                                                                         ");
		writer.println("		<meta http-equiv=\"content-type\" content=\"text/html; charset=UTF-8\" />                                                  ");
		writer.println("		<title>                                                                                                                    ");
		writer.println("			Test page | iBASEt | "+SOLUMINA_VERSION);
		writer.println("		</title>                                                                                                                   ");
		writer.println("		<meta name=\"author\" content=\"iBASEt\" />                                                                                ");
		writer.println("		<style type=\"text/css\">                                                                                                  ");
		writer.println(".clear {                                                                                                                          ");
		writer.println("	clear: both;                                                                                                                   ");
		writer.println("	height: 0;                                                                                                                     ");
		writer.println("}                                                                                                                                 ");
		writer.println("                                                                                                                                  ");
		writer.println("#index .clear {                                                                                                                   ");
		writer.println("	clear:both;                                                                                                                    ");
		writer.println("}                                                                                                                                 ");
		writer.println("                                                                                                                                  ");
		writer.println(".hidden {                                                                                                                         ");
		writer.println("	display:none;                                                                                                                  ");
		writer.println("}                                                                                                                                 ");
		writer.println("                                                                                                                                  ");
		writer.println("p {                                                                                                                               ");
		writer.println("	padding-bottom: 10px;                                                                                                          ");
		writer.println("	margin: 0;                                                                                                                     ");
		writer.println("}                                                                                                                                 ");
		writer.println("                                                                                                                                  ");
		writer.println("a {                                                                                                                               ");
		writer.println("	color:#000000;                                                                                                                 ");
		writer.println(" 	font-weight:bold;                                                                                                              ");
		writer.println("	text-decoration: none;                                                                                                         ");
		writer.println("}                                                                                                                                 ");
		writer.println("                                                                                                                                  ");
		writer.println("a:hover {                                                                                                                         ");
		writer.println("	color:#bf8100;                                                                                                                 ");
		writer.println("}                                                                                                                                 ");
		writer.println("                                                                                                                                  ");
		writer.println("html>body ul {                                                                                                                    ");
		writer.println("	padding-left:1.7em;                                                                                                            ");
		writer.println("	line-height:150%;                                                                                                              ");
		writer.println("}                                                                                                                                 ");
		writer.println("                                                                                                                                  ");
		writer.println("img {                                                                                                                             ");
		writer.println("	border: none;                                                                                                                  ");
		writer.println("}                                                                                                                                 ");
		writer.println("                                                                                                                                  ");
		writer.println("img.narrow {                                                                                                                      ");
		writer.println("	display:block;                                                                                                                 ");
		writer.println("	float:right;                                                                                                                   ");
		writer.println("	clear:right;                                                                                                                   ");
		writer.println("	margin-left:10px;                                                                                                              ");
		writer.println("}                                                                                                                                 ");
		writer.println("                                                                                                                                  ");
		writer.println("img.wide {                                                                                                                        ");
		writer.println("	display:block;                                                                                                                 ");
		writer.println("	clear:both;                                                                                                                    ");
		writer.println("}                                                                                                                                 ");
		writer.println("                                                                                                                                  ");
		writer.println("                                                                                                                                  ");
		writer.println("body {                                                                                                                            ");
		writer.println("	background: #002441;                                                                                                           ");
		writer.println("	text-align:left;                                                                                                               ");
		writer.println("	font: 0.76em \"Lucida Grande\", Helvetica, Verdana, sans-serif;                                                                ");
		writer.println("}                                                                                                                                 ");
		writer.println("	                                                                                                                               ");
		writer.println("                                                                                                                                  ");
		writer.println("#page {                                                                                                                           ");
		writer.println("	margin: 10px auto;                                                                                                             ");
		writer.println("	width: 760px;                                                                                                                  ");
		writer.println("	background:#fdfde0;                                                                                                            ");
		writer.println("}                                                                                                                                 ");
		writer.println("                                                                                                                                  ");
		writer.println("#page-top {                                                                                                                       ");
		writer.println("	background: #f3f3c3;                                                                                                           ");
		writer.println("}                                                                                                                                 ");
		writer.println("                                                                                                                                  ");
		writer.println("#title {                                                                                                                          ");
		writer.println("	text-align:center;                                                                                                             ");
		writer.println("	padding: 2em 1em 0px 1em;                                                                                                      ");
		writer.println("}                                                                                                                                 ");
		writer.println("                                                                                                                                  ");
		writer.println("#logo-container {                                                                                                                 ");
		writer.println("	position:relative;                                                                                                             ");
		writer.println("	top: -6px;                                                                                                                     ");
		writer.println("}                                                                                                                                 ");
		writer.println("                                                                                                                                  ");
		writer.println("h1 {                                                                                                                              ");
		writer.println("	color:#333;                                                                                                                    ");
		writer.println("	margin-bottom:0.4em;                                                                                                           ");
		writer.println("	text-transform: uppercase;                                                                                                     ");
		writer.println("	letter-spacing: 2px;                                                                                                           ");
		writer.println("	font: 1.2em Helvetica, Verdana, sans-serif;                                                                                    ");
		writer.println("}                                                                                                                                 ");
		writer.println("                                                                                                                                  ");
		writer.println("h1 a {                                                                                                                            ");
		writer.println("	color:#333;                                                                                                                    ");
		writer.println("	font-weight:normal;                                                                                                            ");
		writer.println("	text-decoration: none;                                                                                                         ");
		writer.println("}                                                                                                                                 ");
		writer.println("                                                                                                                                  ");
		writer.println("#title p {                                                                                                                        ");
		writer.println("	color:#999;                                                                                                                    ");
		writer.println("	padding:0px;                                                                                                                   ");
		writer.println("	margin-bottom:2px;                                                                                                             ");
		writer.println("}                                                                                                                                 ");
		writer.println("                                                                                                                                  ");
		writer.println("#sitemenu-container {                                                                                                             ");
		writer.println("	text-align:center;                                                                                                             ");
		writer.println("	padding-top:200px;	                                                                                                           ");
		writer.println("	position: relative;                                                                                                            ");
		writer.println("}                                                                                                                                 ");
		writer.println("                                                                                                                                  ");
		writer.println("                                                                                                                                  ");
		writer.println("#sitemenu-content ul {                                                                                                            ");
		writer.println("	background: #f3f3c3;                                                                                                           ");
		writer.println("	margin: 0;                                                                                                                     ");
		writer.println("	padding: 6px 0.3em;                                                                                                            ");
		writer.println("}                                                                                                                                 ");
		writer.println("                                                                                                                                  ");
		writer.println(" html #sitemenu-container {                                                                                                      ");
		writer.println("	padding-top: 0px;                                                                                                              ");
		writer.println("	height:1%;                                                                                                                     ");
		writer.println("}                                                                                                                                 ");
		writer.println("                                                                                                                                  ");
		writer.println("* html #sitemenu-content ul {                                                                                                     ");
		writer.println("	margin-top: 200px;                                                                                                             ");
		writer.println("	height:1%;                                                                                                                     ");
		writer.println("}                                                                                                                                 ");
		writer.println("/* End hide from IE5-mac */                                                                                                       ");
		writer.println("                                                                                                                                  ");
		writer.println("                                                                                                                                  ");
		writer.println("#sitemenu-content li {                                                                                                            ");
		writer.println("	list-style-type: none;                                                                                                         ");
		writer.println("	display: inline;                                                                                                               ");
		writer.println("	padding: 0px 10px;                                                                                                             ");
		writer.println("	font-weight: normal;                                                                                                           ");
		writer.println("}                                                                                                                                 ");
		writer.println("                                                                                                                                  ");
		writer.println("#sitemenu-content a, #sitemenu-content a:focus, #sitemenu-content a:active {                                                      ");
		writer.println("	font-weight:normal;                                                                                                            ");
		writer.println("}                                                                                                                                 ");
		writer.println("                                                                                                                                  ");
		writer.println(".currentPage {                                                                                                                    ");
		writer.println("	color:#bf8100;                                                                                                                 ");
		writer.println(" 	font-weight: bold;                                                                                                             ");
		writer.println("}                                                                                                                                 ");
		writer.println("                                                                                                                                  ");
		writer.println("#page-content {                                                                                                                   ");
		writer.println("	background: #fdfde0;                                                                                                           ");
		writer.println("	margin:10px 12px;                                                                                                              ");
		writer.println("	min-height:100px;                                                                                                              ");
		writer.println("}                                                                                                                                 ");
		writer.println("                                                                                                                                  ");
		writer.println("#sidebar-container {                                                                                                              ");
		writer.println("	width:210px;                                                                                                                   ");
		writer.println("	 float: left;                                                                                                                  ");
		writer.println("}                                                                                                                                 ");
		writer.println("                                                                                                                                  ");
		writer.println(".allow-sidebar #main {                                                                                                            ");
		writer.println("	width:510px;                                                                                                                   ");
		writer.println("	float:right;                                                                                                                   ");
		writer.println("}                                                                                                                                 ");
		writer.println("                                                                                                                                  ");
		writer.println("h2 {                                                                                                                              ");
		writer.println("	font-size: 1.2em;                                                                                                              ");
		writer.println("	font-weight: bold;                                                                                                             ");
		writer.println("	color: #333333;                                                                                                                ");
		writer.println("	margin: 0;                                                                                                                     ");
		writer.println("	padding-bottom: 5px;                                                                                                           ");
		writer.println("}                                                                                                                                 ");
		writer.println("                                                                                                                                  ");
		writer.println(".article h3 {                                                                                                                     ");
		writer.println("	font-size: 12px;                                                                                                               ");
		writer.println("	font-weight: bold;                                                                                                             ");
		writer.println("	color: #666;                                                                                                                   ");
		writer.println("	margin: 0;                                                                                                                     ");
		writer.println("	padding-bottom: 10px;                                                                                                          ");
		writer.println("}                                                                                                                                 ");
		writer.println("                                                                                                                                  ");
		writer.println(".article-thumbnail {                                                                                                              ");
		writer.println("	float: left;                                                                                                                   ");
		writer.println("	margin-right: 10px;                                                                                                            ");
		writer.println("	margin-bottom: 10px;                                                                                                           ");
		writer.println("}                                                                                                                                 ");
		writer.println("                                                                                                                                  ");
		writer.println(".callout {                                                                                                                        ");
		writer.println("	float: right;                                                                                                                  ");
		writer.println("	width:200px;                                                                                                                   ");
		writer.println("	margin: 0;                                                                                                                     ");
		writer.println("	padding-left: 15px;                                                                                                            ");
		writer.println("}                                                                                                                                 ");
		writer.println("                                                                                                                                  ");
		writer.println(".article p {                                                                                                                      ");
		writer.println("	margin-bottom: 3px;                                                                                                            ");
		writer.println("}                                                                                                                                 ");
		writer.println("                                                                                                                                  ");
		writer.println(".timestamp {                                                                                                                      ");
		writer.println("	color: #999999;                                                                                                                ");
		writer.println("	font-size: 10px;                                                                                                               ");
		writer.println("}                                                                                                                                 ");
		writer.println("                                                                                                                                  ");
		writer.println("img.narrow {                                                                                                                      ");
		writer.println("	clear: right;                                                                                                                  ");
		writer.println("	display: block;                                                                                                                ");
		writer.println("	float: right;                                                                                                                  ");
		writer.println("}                                                                                                                                 ");
		writer.println("                                                                                                                                  ");
		writer.println("img.wide {                                                                                                                        ");
		writer.println("	clear: both;                                                                                                                   ");
		writer.println("	display: block;                                                                                                                ");
		writer.println("}                                                                                                                                 ");
		writer.println("                                                                                                                                  ");
		writer.println("#page-bottom {                                                                                                                    ");
		writer.println("	padding: 2px 2px 0px 0px;                                                                                                      ");
		writer.println("	background:#002441;                                                                                                            ");
		writer.println("	color: #fdfde0;                                                                                                                ");
		writer.println("	text-align: right;                                                                                                             ");
		writer.println("	clear:both;                                                                                                                    ");
		writer.println("}                                                                                                                                 ");
		writer.println("                                                                                                                                  ");
		writer.println("#page-bottom p {                                                                                                                  ");
		writer.println("	padding:0px;                                                                                                                   ");
		writer.println("}                                                                                                                                 ");
		writer.println("                                                                                                                                  ");
		writer.println("#page-bottom a {                                                                                                                  ");
		writer.println("	color: #999;                                                                                                                   ");
		writer.println("}                                                                                                                                 ");
		writer.println("                                                                                                                                  ");
		writer.println("#page-bottom a:hover {                                                                                                            ");
		writer.println("	color:#bf8100;                                                                                                                 ");
		writer.println("}                                                                                                                                 ");
		writer.println("                                                                                                                                  ");
		writer.println("                                                                                                                                  ");
		writer.println("/* Other specific classes nested with an .article */                                                                              ");
		writer.println("	                                                                                                                               ");
		writer.println(".photo {                                                                                                                          ");
		writer.println("	text-align: center;                                                                                                            ");
		writer.println("	background: transparent;                                                                                                       ");
		writer.println("	}                                                                                                                              ");
		writer.println("                                                                                                                                  ");
		writer.println(".photo h3 {                                                                                                                       ");
		writer.println("	}                                                                                                                              ");
		writer.println("                                                                                                                                  ");
		writer.println(".photo img {                                                                                                                      ");
		writer.println("	text-align: center;                                                                                                            ");
		writer.println("	}                                                                                                                              ");
		writer.println("                                                                                                                                  ");
		writer.println(".caption {                                                                                                                        ");
		writer.println("	text-align: center;                                                                                                            ");
		writer.println("	margin-bottom: 5px;                                                                                                            ");
		writer.println("	}                                                                                                                              ");
		writer.println("                                                                                                                                  ");
		writer.println("                                                                                                                                  ");
		writer.println("*:first-child+html .gridItem img {                                                                                                ");
		writer.println("	position:relative;                                                                                                             ");
		writer.println("	top:0px;                                                                                                                       ");
		writer.println("}                                                                                                                                 ");
		writer.println("                                                                                                                                  ");
		writer.println("*:first-child+html .gridItem h3 {                                                                                                 ");
		writer.println("	position:relative;                                                                                                             ");
		writer.println("	top:0px;                                                                                                                       ");
		writer.println("}                                                                                                                                 ");
		writer.println("                                                                                                                                  ");
		writer.println("                                                                                                                                  ");
		writer.println("form.loginElement input{                                                                                                          ");
		writer.println("	width:275px;                                                                                                                   ");
		writer.println("	margin:4px 0;                                                                                                                  ");
		writer.println("	                                                                                                                               ");
		writer.println("}                                                                                                                                 ");
		writer.println("form.loginElement input.submit {                                                                                                  ");
		writer.println("	width:auto;                                                                                                                    ");
		writer.println("	display:block;                                                                                                                 ");
		writer.println("	margin-left:285px;                                                                                                             ");
		writer.println("	margin-right:2%;                                                                                                               ");
		writer.println("                                                                                                                                  ");
		writer.println("}                                                                                                                                 ");
		writer.println("form.loginElement th {                                                                                                            ");
		writer.println("	text-align:right;                                                                                                              ");
		writer.println("	font-weight:normal;                                                                                                            ");
		writer.println("	width:25%;                                                                                                                     ");
		writer.println("}                                                                                                                                 ");
		writer.println("                                                                                                                                  ");
		writer.println("/* Contact Element For IE 7 */                                                                                                    ");
		writer.println("*:first-child+html form.loginElement input.submit {                                                                               ");
		writer.println("	padding:0px 10%;                                                                                                               ");
		writer.println("	min-width: auto;                                                                                                               ");
		writer.println("}                                                                                                                                 ");
		writer.println("                                                                                                                                  ");
		writer.println("</style>                                                                                                                          ");
		writer.println("	</head>                                                                                                                        ");

	}
}
