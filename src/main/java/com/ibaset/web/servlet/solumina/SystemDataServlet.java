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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.RuntimeMXBean;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;

import com.ibaset.common.FrameworkConstants;
import com.ibaset.common.client.SoluminaServiceLocator;
import com.ibaset.common.sql.Column;
import com.ibaset.common.sql.ColumnHeader;
import com.ibaset.common.sql.IPassThroughQuery;
import com.ibaset.common.sql.ResultObject;
import com.ibaset.common.sql.Row;
import static com.ibaset.common.FrameworkConstants.SAMEORIGIN;

public class SystemDataServlet extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	private static final String SOLUMINA_VERSION = "Solumina-G8";
	private static final String NOT_APPLICABLE = "N/A";
	private static final String UNKNOWN = "Unknown";
	private String remoteAddress;
	private String remoteHostname;
	private String clientAddress;
	private String clientHostname;

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
		
		remoteAddress = request.getLocalAddr();
		remoteHostname = request.getLocalName();
		clientAddress = request.getRemoteAddr();
		clientHostname = request.getRemoteHost();
		
		if (clientHostname.equalsIgnoreCase(clientAddress))
		{
			clientHostname = UNKNOWN;
		}
		
		response.setContentType("text/html");
		response.addHeader("x-frame-options", SAMEORIGIN);
		
		if("text".equals(request.getParameter("save")))
		{
			String requestedURL = request.getRequestURL().toString();
			URL sysinfoPage = new URL(requestedURL);
			String tmpDir = gettmpDir();
			if(tmpDir!= null){
				String fileName = tmpDir.replaceAll("\\\\", "/") + "/SystemProperties.txt";
				String sysinfoText ="";
				sysinfoText += "###############################################################\r\n";
				sysinfoText += "##### SYSTEM INFORMATION AT: " + currentTime + " #####\r\n";
				sysinfoText += "##### Requested URL: " + requestedURL + " ####\r\n";
				sysinfoText += "###############################################################\r\n";
				sysinfoText += this.getWebContentAsText(sysinfoPage);
				sysinfoText += "\r\n\r\n";
				
				String prevSysinfoContent = this.getFileContent(fileName);
				
				this.writeToFile(fileName, prevSysinfoContent + sysinfoText);
				writer.println("File saved to the location in the server where Solumina Middle Tier is installed: <br>");
				writer.println(fileName + "<br>");
			}
			writer.println("<BR><A HREF=\""+request.getContextPath()+"/sysinfo\">Back to System Properties Page</A>");
			writer.println("<BR><A HREF=\""+request.getContextPath()+"/test\">Back to Test Page</A>");
			return;
		}

		writeHtmlHeader(writer);
		
		writer.println("	<body>                                                                                                                         ");
		writer.println("		<div id=\"page-container\" class=\"text-page\">                                                                            ");
		writer.println("			<div id=\"page\">                                                                                                      ");
		writer.println("				<div id=\"page-top\">                                                                                              ");
		writer.println("					<div id=\"title\">                                                                                             ");
		writer.println("						<h1>                                                                                                       ");
		writer.println("							<span class=\"in\">Solumina System Properties</span>                                                                    ");
		writer.println("						</h1>                                                                                                      ");
		writer.println("						<p>                                                                                                        ");
		writer.println("							<span class=\"in\">Solumina Application Server Properties</span>                            ");

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

			writer.println("										<form class=\"loginElement\" method=\"POST\" autocomplete=\"off\" action=\""+request.getContextPath()+"/sysinfo\" id=\"form\">         ");
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
			this.getSystemInfo(writer);
			this.getDBInfo(writer);
			//this.getDBInitParams(writer);
			this.getSffndGlobalParams(writer);
			this.getSfdbInfo(writer);
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
		writer.println("					<div id=\"sitemenu-container\">                                                                                ");
		if(loggedIn)
		{
			writer.println("<BR><A HREF=\""+request.getContextPath()+"/sysinfo?save=text\">Save Content</A>");
			writer.println("<BR><A HREF=\""+request.getContextPath()+"/test\">Return to Test Page Login</A>");
		}
		writer.println("					</div>                                                                                ");
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

	public void doGet(HttpServletRequest request, HttpServletResponse response)	throws ServletException,
																				IOException
	{
		doPost(request, response);
	}
	
	private void getSystemInfo(PrintWriter out)
	{
	    RuntimeMXBean mx = ManagementFactory.getRuntimeMXBean();
		MemoryMXBean mmx = ManagementFactory.getMemoryMXBean();
		MemoryUsage heapM = mmx.getHeapMemoryUsage();
		MemoryUsage permgenM = mmx.getNonHeapMemoryUsage();
		Properties sysProps = System.getProperties();
		Set<String> set = new TreeSet<String>();
		final int bytesInMB = 1048576;
		String tomcatVersion = NOT_APPLICABLE;
		String catalinaHome = sysProps.getProperty("catalina.home");
		if(!StringUtils.isEmpty(catalinaHome)){
			StringTokenizer tomcatVersionToken = new StringTokenizer(catalinaHome, "-");
            while (tomcatVersionToken.hasMoreTokens())
            {
                  tomcatVersion = tomcatVersionToken.nextToken();
            }
		}
	    for (Enumeration e = sysProps.propertyNames(); e.hasMoreElements();)
	    {
	    	set.add((String) e.nextElement());
	    }
		
		out.println("\t<H2><FONT color=green>System Information:</font></H2>");
		out.println("\t<B>Tomcat Version: </B>" + tomcatVersion + "<br>");
		out.println("\t<B>IP Address: </B>" + remoteAddress + "<br>");
		out.println("\t<B>Host Name: </B>" + remoteHostname + "<br>");
		out.println("\t<B>Client IP Address: </B>" + clientAddress + "<br>");
		out.println("\t<B>Client Host Name: </B>" + clientHostname + "<br>");
	    out.println("\t<B>Solumina Start Time: </B>" + new Date(mx.getStartTime()) + "<br>");
	    out.println("\t<B>Solumina Up Time: </B>" + mx.getUptime() + " ms<br>");
	    out.println("\t<B>Maximum Allowable Memory: </B>" + heapM.getMax()/bytesInMB + "MB<br>");
	    out.println("\t<B>Committed Memory: </B>" + heapM.getCommitted()/bytesInMB + "MB<br>");
	    out.println("\t<B>Used Memory: </B>" + heapM.getUsed()/bytesInMB + "MB<br>");
	    out.println("\t<B>Initial Memory Requested: </B>" + heapM.getInit()/bytesInMB + "MB<br>");
	    out.println("\t<B>Maximum Allowable PermGen Memory: </B>" + permgenM.getMax()/bytesInMB + "MB<br>");
	    out.println("\t<B>Total PermGen Memory: </B>" + permgenM.getCommitted()/bytesInMB + "MB<br>");
	    out.println("\t<B>Used PermGen Memory: </B>" + permgenM.getUsed()/bytesInMB + "MB<br>");
	    out.println("\t<B>Initial PermGen Memory Requested: </B>" + permgenM.getInit()/bytesInMB + "MB<br>");
	    
	    out.println("\t<BR>");
	    out.println("\t<B>JVM Input Argument: </B>" + mx.getInputArguments() + "<br>");
	    out.println("<HR>");

	    for (Iterator it = set.iterator(); it.hasNext();) 
	    {
		    String key = (String) it.next(); //determine the key name
		    /* Display the key name and its value. */
		    out.print("\t<B>"+key+": </B>");
		    out.println("<div style=\"word-wrap:break-word; display:inline;\" >" + sysProps.getProperty(key) + "</div>");
	    	out.println("<br>");
	    }
	    out.println("<HR>");	  
	}
	
	private void getDBInitParams(PrintWriter out)
	{
		IPassThroughQuery ptq = (IPassThroughQuery) SoluminaServiceLocator.locateService(IPassThroughQuery.class);
		String queryString = "SELECT * FROM SFMFG.SFDB_INIPARAMETER_V";
		ResultObject ro = ptq.executeQuery(queryString, 1, 200, new java.util.HashMap(), new java.util.HashMap());
		if(ro.getErrors() == null || ro.getErrors().size()==0)
		{
			out.println("<FONT color=green>Database Init Parameters:</font>");
			displayResultObjectInATable(ro, out);
			out.println("<HR>");
		}
	}
	
	private void getSffndGlobalParams(PrintWriter out)
	{
		IPassThroughQuery ptq = (IPassThroughQuery) SoluminaServiceLocator.locateService(IPassThroughQuery.class);
		//String queryString = "SELECT * FROM SFMFG.SFFND_GLOBAL_CONFIGURATION";
		//String queryString = "SELECT CONFIG_MODULE_NAME, PARAMETER_NAME, PARAMETER_VALUE, UPDT_USERID, TIME_STAMP, LAST_ACTION FROM SFMFG.SFFND_GLOBAL_CONFIGURATION";
		String queryString = "SELECT CONFIG_MODULE_NAME, PARAMETER_NAME, PARAMETER_VALUE FROM SFMFG.SFFND_GLOBAL_CONFIGURATION";
		ResultObject ro = ptq.executeQuery(queryString, 1, 200, new java.util.HashMap(), new java.util.HashMap());
		if(ro.getErrors() == null || ro.getErrors().size()==0)
		{
			out.println("<FONT color=green>Global Parameters:</font>");
			displayResultObjectInATable(ro, out);
			out.println("<HR>");
		}
	}
	
	private void getSfdbInfo(PrintWriter out)
	{
		IPassThroughQuery ptq = (IPassThroughQuery) SoluminaServiceLocator.locateService(IPassThroughQuery.class);
		ResultObject ro = ptq.executeQuery("SELECT RELEASE, VERSION, UPDT_USERID, TIME_STAMP, RELEASE_TYPE, EXE_COUNT, LONG_FND_VER, OOB_RELEASE FROM SFMFG.SFDB_INFO ORDER BY TIME_STAMP DESC", 1, 200, new java.util.HashMap(), new java.util.HashMap());
		
		if(ro.getErrors() == null || ro.getErrors().size()==0)
		{
			out.println("<FONT color=green>SFDB_INFO Information:</font>");
			displayResultObjectInATable(ro, out);
			out.println("<HR>");
		}
	}

	private void getDBInfo(PrintWriter out)
	{
		Connection conn = SoluminaServiceLocator.getDatabaseConnection();
		if (conn != null)
		{
			DatabaseMetaData metaData;
			try
			{
				metaData = conn.getMetaData();
				out.println("<FONT color=green>Database Information:</font><br>");
				out.println("\t<B>Driver Name:        </B>" + metaData.getDriverName()             + "<br>");
				out.println("\t<B>Driver Version:     </B>" + metaData.getDriverVersion()          + "<br>");
				out.println("\t<B>Database Version:   </B>" + metaData.getDatabaseProductVersion() + "<br>");
				out.println("\t<B>JDBC Version:       </B>" + metaData.getJDBCMajorVersion()       + "<br>");
				out.println("\t<B>JDBC URL:                   </B>");
                out.println("<div style=\"word-wrap:break-word; display:inline;\" >" + metaData.getURL() + "</div><br>");
				out.println("\t<B>JDBC User Name:     </B>" + metaData.getUserName()               + "<br>");
				out.println("<HR>");

			}
			catch (Exception e)
			{
			
			}
			finally
			{
				SoluminaServiceLocator.releaseConnection(conn);
			}
		}
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
	
	private String getWebContentAsText(URL url)
	{
		String contentText = "";
		BufferedReader br = null;
		try
		{
			br = new BufferedReader(new InputStreamReader(url.openStream()));
			String newLine;
			boolean bodyStarted = false;
			while ( (newLine = br.readLine()) != null)
			{
				if (newLine.contains("<body>"))
				{
					bodyStarted = true;
				}
				if (bodyStarted)
				{
					newLine = newLine.replaceAll("\\<.*?\\>", "");
					newLine = newLine.replaceAll("\t", "");
					if (newLine.trim().length() != 0)
					{
						contentText += newLine + "\r\n";
					}
				}
			}
		}
		catch (MalformedURLException mfe) 
		{
			mfe.printStackTrace();
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
        finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
		
		return contentText;
	}
	
	private void writeToFile(String fileName, String content)
	{
	    FileWriter out = null;
		try
		{
			out = new FileWriter(fileName);
			out.write(content + "\r\n");
			out.close();
		}
		catch (IOException e)
		{

        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
	}
	
	private String getFileContent(String fileName)
	{
		File file = new File(fileName);
		String content = "";
		String readLine;
		if (file.exists())
		{
		    BufferedReader in = null;
			try
			{
				in = new BufferedReader(new FileReader(fileName));
				while ( (readLine = in.readLine()) != null)
				{
					content += readLine + "\r\n";
				}
				in.close();
			}
			catch (IOException e)
			{

			} finally {
			    if (in != null) {
			        try {
                        in.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
			    }
			}
		}
		return content;
	}
	/**
	 * @return the temp directory location.
	 */
	private String gettmpDir()
	{
		
		String tmpDir = System.getProperty("catalina.base");
		if(tmpDir == null){
			tmpDir = System.getProperty("java.io.tmpdir");
		}
		if(tmpDir == null){
			try{
	    		//Create temp file.
	    		File temp = File.createTempFile("temp-file", ".tmp"); 
			   //Get temp file path
	    		String absolutePath = temp.getAbsolutePath();
	    		tmpDir = absolutePath.substring(0,absolutePath.lastIndexOf(File.separator));
	    		temp.delete();
			}catch(IOException e){
	    		e.printStackTrace();
	    	}
		}
		return tmpDir;
	}	
}

