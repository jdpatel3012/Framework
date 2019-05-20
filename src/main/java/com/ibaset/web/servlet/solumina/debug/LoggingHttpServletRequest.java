package com.ibaset.web.servlet.solumina.debug;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpUpgradeHandler;
import javax.servlet.http.Part;

public class LoggingHttpServletRequest implements HttpServletRequest {

	private HttpServletRequest delegate;
	private LoggingServletInputStream loggingInput;
	
	public LoggingHttpServletRequest(HttpServletRequest delegate, OutputStream log) throws IOException {
		super();
		this.delegate = delegate;
		this.loggingInput = new LoggingServletInputStream(delegate.getInputStream(), log);
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

	public String getHeader(String arg0) {
		return delegate.getHeader(arg0);
	}

	public Enumeration getHeaderNames() {
		return delegate.getHeaderNames();
	}

	public Enumeration getHeaders(String arg0) {
		return delegate.getHeaders(arg0);
	}

	public ServletInputStream getInputStream() throws IOException {
		return loggingInput;
	}

	public int getIntHeader(String arg0) {
		return delegate.getIntHeader(arg0);
	}

	public String getLocalAddr() {
		return delegate.getLocalAddr();
	}

	public String getLocalName() {
		return delegate.getLocalName();
	}

	public int getLocalPort() {
		return delegate.getLocalPort();
	}

	public Locale getLocale() {
		return delegate.getLocale();
	}

	public Enumeration getLocales() {
		return delegate.getLocales();
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

	public String getRequestURI() {
		return delegate.getRequestURI();
	}

	public StringBuffer getRequestURL() {
		return delegate.getRequestURL();
	}

	public String getRequestedSessionId() {
		return delegate.getRequestedSessionId();
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

	public boolean isRequestedSessionIdFromURL() {
		return delegate.isRequestedSessionIdFromURL();
	}

	public boolean isRequestedSessionIdFromUrl() {
		return delegate.isRequestedSessionIdFromUrl();
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

	@Override
	public AsyncContext getAsyncContext() {
		return delegate.getAsyncContext();
	}

	@Override
	public DispatcherType getDispatcherType() {
		return delegate.getDispatcherType();
	}

	@Override
	public ServletContext getServletContext() {
		return delegate.getServletContext();
	}

	@Override
	public boolean isAsyncStarted() {
		return delegate.isAsyncStarted();
	}

	@Override
	public boolean isAsyncSupported() {
		return delegate.isAsyncSupported();
	}

	@Override
	public AsyncContext startAsync() {
		return delegate.startAsync();
	}

	@Override
	public AsyncContext startAsync(ServletRequest arg0, ServletResponse arg1) {
		return delegate.startAsync(arg0, arg1);
	}

	@Override
	public boolean authenticate(HttpServletResponse arg0) throws IOException,
			ServletException {
		return delegate.authenticate(arg0);
	}

	@Override
	public Part getPart(String arg0) throws IOException, IllegalStateException,
			ServletException {
		return delegate.getPart(arg0);
	}

	@Override
	public Collection<Part> getParts() throws IOException,
			IllegalStateException, ServletException {
		return delegate.getParts();
	}

	@Override
	public void login(String arg0, String arg1) throws ServletException {
		delegate.login(arg0, arg1);
	}

	@Override
	public void logout() throws ServletException {
		delegate.logout();
	}

	@Override
	public long getContentLengthLong() {
		return delegate.getContentLengthLong();
	}

	@Override
	public String changeSessionId() {
		return delegate.changeSessionId();
	}

	@Override
	public <T extends HttpUpgradeHandler> T upgrade(Class<T> paramClass)
			throws IOException, ServletException {
		return delegate.upgrade(paramClass);
	}
}
