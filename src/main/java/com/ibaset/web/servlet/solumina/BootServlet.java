/**
 * Proprietary and Confidential
 * Copyright 1995-2011 iBASEt, Inc.
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

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.ibaset.solumina.sfcore.dao.IBootLibDao;

public class BootServlet extends HttpServlet {

	private static final String DEFAULT = "DEFAULT";
	private static final long serialVersionUID = 1L;
	private IBootLibDao dao;
    Logger logger = LoggerFactory.getLogger(this.getClass());

	@Override
	public void init(ServletConfig config) throws ServletException 
	{
		super.init(config);
        WebApplicationContext ctx = WebApplicationContextUtils.getWebApplicationContext(config.getServletContext());
        dao = (IBootLibDao) ctx.getBean("bootLibDao");
	}

	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException 
	{
		String path = req.getRequestURI();
		if(path.endsWith(".tcwp"))
		{
			handleTCWP(req, resp);
		}
		else
		{
			String bootId=req.getParameter("id");
			if(bootId == null || bootId.length()==0) bootId=DEFAULT;
			
			String bootText = "";
			try
			{
				bootText = dao.selectBootId(bootId);
			}
			catch (Exception e) 
			{
				String errorText ="Boot id not found: "+bootId; 
				logger.error(errorText, e);
				resp.sendError(HttpServletResponse.SC_NOT_FOUND, errorText);
			}
			resp.setContentType("text/plain");
			PrintWriter writer = resp.getWriter();
			writer.write(bootText);
			writer.close();
		}
	}

	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException 
	{
		super.doGet(req, resp);
	}
	
	private static final Pattern DB_ALIAS_PATTERN=Pattern.compile("DbAlias\\=([^\\s\\?]+)");
	
	protected void handleTCWP(HttpServletRequest request, HttpServletResponse response) throws IOException
	{
		String path = request.getRequestURI();
		String args = StringUtils.defaultIfEmpty(request.getQueryString(), StringUtils.EMPTY);
		if(args.length()>0)
		{
			args = URLDecoder.decode(args, "UTF-8");
		}
		String bootId = DEFAULT;
		String url = request.getRequestURL().toString();
		int i= path.lastIndexOf('/');
		if(i!=-1)
		{
			//cut off the file part
			url = url.substring(0, url.length() - (path.length()-i));
			//extract bootId from path
			int k = path.lastIndexOf('.');
			if(k != -1) bootId = path.substring(i+1, k);
		}
		try
		{
			String bootText = dao.selectBootId(bootId);
			//extract DbAlias property
			Matcher m=DB_ALIAS_PATTERN.matcher(bootText);
			if(m.find())
			{
				url = m.group(1);
			}
//			else
//			{
//				response.sendError(HttpServletResponse.SC_NOT_FOUND, "DbAlias not found in "+bootId);
//			}
		}
		catch (Exception e) 
		{
			String errorText ="Boot id not found: "+bootId; 
			response.sendError(HttpServletResponse.SC_NOT_FOUND, errorText);
		}
		StringBuilder result = new StringBuilder();
		result.append("url=").append(url).append("?id=").append(bootId).append("\r\n");
		result.append("args=").append(args).append("\r\n");
		
        response.setContentType("application/solumina-tc");
        OutputStream os = response.getOutputStream();
		os.write(result.toString().getBytes("UTF-8"));
		os.close();
	}

}
