package com.ibaset.web.servlet.solumina;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.springframework.web.WebApplicationInitializer;

import com.ibaset.solumina.logging.appender.SoluminaRollingFileAppender;

public class SoluminaWebApplicationInitializer implements WebApplicationInitializer {

	@Override
	public void onStartup(ServletContext servletContext) throws ServletException {
		String contextPath = servletContext.getContextPath();
		SoluminaRollingFileAppender.setContextPath(contextPath);
	}

}
