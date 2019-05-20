package com.ibaset.web.servlet.solumina.debug;

import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;

public class LoggingServletOutputStream extends ServletOutputStream {

	private ServletOutputStream delegate;
	private OutputStream log;

	public LoggingServletOutputStream(ServletOutputStream delegate,	OutputStream log) {
		super();
		this.delegate = delegate;
		this.log = log;
	}

	public void close() throws IOException {
		delegate.close();
		log.flush();
	}

	public void flush() throws IOException {
		delegate.flush();
		log.flush();
	}

	public void write(byte[] b, int off, int len) throws IOException {
		delegate.write(b, off, len);
		log.write(b, off, len);
	}

	public void write(int b) throws IOException {
		delegate.write(b);
		log.write(b);
	}

	@Override
	public boolean isReady() {
		return delegate.isReady();
	}

	@Override
	public void setWriteListener(WriteListener paramWriteListener) {
		delegate.setWriteListener(paramWriteListener);
	}
	
	
}
