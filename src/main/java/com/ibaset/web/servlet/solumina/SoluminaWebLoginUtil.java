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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpUpgradeHandler;
import javax.servlet.http.Part;

import org.apache.commons.codec.binary.Base64;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.ibaset.common.client.SoluminaServiceLocator;
import com.ibaset.common.context.SoluminaContextHolder;
import com.ibaset.common.security.context.ContextUtil;
import com.ibaset.common.security.context.SoluminaUser;
import com.ibaset.solumina.sfcore.application.ILogin;

public class SoluminaWebLoginUtil 
{

	public static boolean login(HttpServletRequest request, HttpServletResponse response, String user, String password)
	{
		boolean loggedIn = false;
		try
		{
			if(user!=null && password != null)
			{
				SoluminaUser oldUser = ContextUtil.getUser();
				String oldConnectionId = null;
				String oldUserId = null;
				if(oldUser!=null)
				{
					oldConnectionId = SoluminaContextHolder.getUserContext().getConnectionId();
					oldUserId = oldUser.getUsername();
				}
				WebApplicationContext ctx = WebApplicationContextUtils.getWebApplicationContext(request.getSession().getServletContext());
				Filter filter = (Filter)ctx.getBean("filterChainProxy");
				
				String token = user+":"+password;
				String basicAuth = "Basic "+new String(Base64.encodeBase64(token.getBytes()));
				request.removeAttribute("basicProcessingFilter.FILTERED");
				LoginServletRequest req = new LoginServletRequest(request, basicAuth);
				LoginServletResponse res = new LoginServletResponse(response);
				
				filter.doFilter(req, res, new EmptyFilterChain());
				
				if(!res.isError())
				{
			        Authentication existingAuth = SecurityContextHolder.getContext().getAuthentication();
			        if(existingAuth != null && existingAuth.isAuthenticated()) 
			        {
						ILogin login = (ILogin) SoluminaServiceLocator.locateService(ILogin.class);
						if(oldConnectionId !=null)
						{
							login.logout(oldUserId, oldConnectionId);
							
							SoluminaContextHolder.cleanupCurrentSoluminaContexts();
						}
						login.setUp(false);
						loggedIn = true;
			        }
				}
			}
		}
		catch (Exception e)
		{
			loggedIn = false;
		}
		
		return loggedIn;
	} 
	
	public static boolean hasPrivilege(String privilege)
	{
		return ContextUtil.getUser().hasPrivilege(privilege);
	}
	private static class EmptyFilterChain implements FilterChain{
		public void doFilter(ServletRequest arg0, ServletResponse arg1)
				throws IOException, ServletException {
		}
	}
	private static class LoginServletRequest implements HttpServletRequest{
		private HttpServletRequest delegate;
		private String basicAuth;

		public LoginServletRequest(HttpServletRequest delegate, String basicAuth) {
			super();
			this.delegate = delegate;
			this.basicAuth = basicAuth;
		}

		public Object getAttribute(String arg0) {
			return delegate.getAttribute(arg0);
		}

		public Enumeration getAttributeNames() {
			return delegate.getAttributeNames();
		}

		public String getAuthType() {
			return delegate.getAuthType();
		}

		public String getCharacterEncoding() {
			return delegate.getCharacterEncoding();
		}

		public int getContentLength() {
			return delegate.getContentLength();
		}

		public String getContentType() {
			return delegate.getContentType();
		}

		public String getContextPath() {
			return delegate.getContextPath();
		}

		public Cookie[] getCookies() {
			return delegate.getCookies();
		}

		public long getDateHeader(String arg0) {
			return delegate.getDateHeader(arg0);
		}

		public String getHeader(String name) {
			if("Authorization".equals(name)) return this.basicAuth;
			return delegate.getHeader(name);
		}

		public Enumeration getHeaderNames() {
			return delegate.getHeaderNames();
		}

		public Enumeration getHeaders(String arg0) {
			return delegate.getHeaders(arg0);
		}

		public ServletInputStream getInputStream() throws IOException {
			return delegate.getInputStream();
		}

		public int getIntHeader(String arg0) {
			return delegate.getIntHeader(arg0);
		}

		public String getLocalAddr() {
			return delegate.getLocalAddr();
		}

		public Locale getLocale() {
			return delegate.getLocale();
		}

		public Enumeration getLocales() {
			return delegate.getLocales();
		}

		public String getLocalName() {
			return delegate.getLocalName();
		}

		public int getLocalPort() {
			return delegate.getLocalPort();
		}

		public String getMethod() {
			return delegate.getMethod();
		}

		public String getParameter(String arg0) {
			return delegate.getParameter(arg0);
		}

		public Map getParameterMap() {
			return delegate.getParameterMap();
		}

		public Enumeration getParameterNames() {
			return delegate.getParameterNames();
		}

		public String[] getParameterValues(String arg0) {
			return delegate.getParameterValues(arg0);
		}

		public String getPathInfo() {
			return delegate.getPathInfo();
		}

		public String getPathTranslated() {
			return delegate.getPathTranslated();
		}

		public String getProtocol() {
			return delegate.getProtocol();
		}

		public String getQueryString() {
			return delegate.getQueryString();
		}

		public BufferedReader getReader() throws IOException {
			return delegate.getReader();
		}

		public String getRealPath(String arg0) {
			return delegate.getRealPath(arg0);
		}

		public String getRemoteAddr() {
			return delegate.getRemoteAddr();
		}

		public String getRemoteHost() {
			return delegate.getRemoteHost();
		}

		public int getRemotePort() {
			return delegate.getRemotePort();
		}

		public String getRemoteUser() {
			return delegate.getRemoteUser();
		}

		public RequestDispatcher getRequestDispatcher(String arg0) {
			return delegate.getRequestDispatcher(arg0);
		}

		public String getRequestedSessionId() {
			return delegate.getRequestedSessionId();
		}

		public String getRequestURI() {
			return delegate.getRequestURI();
		}

		public StringBuffer getRequestURL() {
			return delegate.getRequestURL();
		}

		public String getScheme() {
			return delegate.getScheme();
		}

		public String getServerName() {
			return delegate.getServerName();
		}

		public int getServerPort() {
			return delegate.getServerPort();
		}

		public String getServletPath() {
			return delegate.getServletPath();
		}

		public HttpSession getSession() {
			return delegate.getSession();
		}

		public HttpSession getSession(boolean arg0) {
			return delegate.getSession(arg0);
		}

		public Principal getUserPrincipal() {
			return delegate.getUserPrincipal();
		}

		public boolean isRequestedSessionIdFromCookie() {
			return delegate.isRequestedSessionIdFromCookie();
		}

		public boolean isRequestedSessionIdFromUrl() {
			return delegate.isRequestedSessionIdFromUrl();
		}

		public boolean isRequestedSessionIdFromURL() {
			return delegate.isRequestedSessionIdFromURL();
		}

		public boolean isRequestedSessionIdValid() {
			return delegate.isRequestedSessionIdValid();
		}

		public boolean isSecure() {
			return delegate.isSecure();
		}

		public boolean isUserInRole(String arg0) {
			return delegate.isUserInRole(arg0);
		}

		public void removeAttribute(String arg0) {
			delegate.removeAttribute(arg0);
		}

		public void setAttribute(String arg0, Object arg1) {
			delegate.setAttribute(arg0, arg1);
		}

		public void setCharacterEncoding(String arg0)
				throws UnsupportedEncodingException {
			delegate.setCharacterEncoding(arg0);
		}

		public long getContentLengthLong() {
			return delegate.getContentLengthLong();
		}

		public String changeSessionId() {
			return delegate.changeSessionId();
		}

		public boolean authenticate(HttpServletResponse paramHttpServletResponse)
				throws IOException, ServletException {
			return delegate.authenticate(paramHttpServletResponse);
		}

		public ServletContext getServletContext() {
			return delegate.getServletContext();
		}

		public AsyncContext startAsync() throws IllegalStateException {
			return delegate.startAsync();
		}

		public void login(String paramString1, String paramString2)
				throws ServletException {
			delegate.login(paramString1, paramString2);
		}

		public AsyncContext startAsync(ServletRequest paramServletRequest,
				ServletResponse paramServletResponse)
				throws IllegalStateException {
			return delegate.startAsync(paramServletRequest,
					paramServletResponse);
		}

		public void logout() throws ServletException {
			delegate.logout();
		}

		public Collection<Part> getParts() throws IOException, ServletException {
			return delegate.getParts();
		}

		public boolean isAsyncStarted() {
			return delegate.isAsyncStarted();
		}

		public boolean isAsyncSupported() {
			return delegate.isAsyncSupported();
		}

		public Part getPart(String paramString) throws IOException,
				ServletException {
			return delegate.getPart(paramString);
		}

		public AsyncContext getAsyncContext() {
			return delegate.getAsyncContext();
		}

		public DispatcherType getDispatcherType() {
			return delegate.getDispatcherType();
		}

		public <T extends HttpUpgradeHandler> T upgrade(Class<T> paramClass)
				throws IOException, ServletException {
			return delegate.upgrade(paramClass);
		}

	}
	private static class LoginServletResponse implements HttpServletResponse{
		private HttpServletResponse delegate;
		private boolean error;

		public LoginServletResponse(HttpServletResponse delegate) {
			super();
			this.delegate = delegate;
		}

		public boolean isError() {
			return error;
		}

		public void addCookie(Cookie arg0) {
			delegate.addCookie(arg0);
		}

		public void addDateHeader(String arg0, long arg1) {
			delegate.addDateHeader(arg0, arg1);
		}

		public void addHeader(String arg0, String arg1) {
			delegate.addHeader(arg0, arg1);
		}

		public void addIntHeader(String arg0, int arg1) {
			delegate.addIntHeader(arg0, arg1);
		}

		public boolean containsHeader(String arg0) {
			return delegate.containsHeader(arg0);
		}

		public String encodeRedirectUrl(String arg0) {
			return delegate.encodeRedirectUrl(arg0);
		}

		public String encodeRedirectURL(String arg0) {
			return delegate.encodeRedirectURL(arg0);
		}

		public String encodeUrl(String arg0) {
			return delegate.encodeUrl(arg0);
		}

		public String encodeURL(String arg0) {
			return delegate.encodeURL(arg0);
		}

		public void flushBuffer() throws IOException {
			delegate.flushBuffer();
		}

		public int getBufferSize() {
			return delegate.getBufferSize();
		}

		public String getCharacterEncoding() {
			return delegate.getCharacterEncoding();
		}

		public String getContentType() {
			return delegate.getContentType();
		}

		public Locale getLocale() {
			return delegate.getLocale();
		}

		public ServletOutputStream getOutputStream() throws IOException {
			return delegate.getOutputStream();
		}

		public PrintWriter getWriter() throws IOException {
			return delegate.getWriter();
		}

		public boolean isCommitted() {
			return delegate.isCommitted();
		}

		public void reset() {
			delegate.reset();
		}

		public void resetBuffer() {
			delegate.resetBuffer();
		}

		public void sendError(int error, String arg1) throws IOException {
			this.error = true;
		}

		public void sendError(int error) throws IOException {
			this.error = true;
		}

		public void sendRedirect(String arg0) throws IOException {
			delegate.sendRedirect(arg0);
		}

		public void setBufferSize(int arg0) {
			delegate.setBufferSize(arg0);
		}

		public void setCharacterEncoding(String arg0) {
			delegate.setCharacterEncoding(arg0);
		}

		public void setContentLength(int arg0) {
			delegate.setContentLength(arg0);
		}

		public void setContentType(String arg0) {
			delegate.setContentType(arg0);
		}

		public void setDateHeader(String arg0, long arg1) {
			delegate.setDateHeader(arg0, arg1);
		}

		public void setHeader(String arg0, String arg1) {
			delegate.setHeader(arg0, arg1);
		}

		public void setIntHeader(String arg0, int arg1) {
			delegate.setIntHeader(arg0, arg1);
		}

		public void setLocale(Locale arg0) {
			delegate.setLocale(arg0);
		}

		public void setStatus(int arg0, String arg1) {
			delegate.setStatus(arg0, arg1);
		}

		public void setStatus(int arg0) {
			delegate.setStatus(arg0);
		}

		public void setContentLengthLong(long paramLong) {
			delegate.setContentLengthLong(paramLong);
		}

		public int getStatus() {
			return delegate.getStatus();
		}

		public String getHeader(String paramString) {
			return delegate.getHeader(paramString);
		}

		public Collection<String> getHeaders(String paramString) {
			return delegate.getHeaders(paramString);
		}

		public Collection<String> getHeaderNames() {
			return delegate.getHeaderNames();
		}
	}
}
