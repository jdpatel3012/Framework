package com.ibaset.web.servlet.solumina.debug;

import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;

public class LoggingServletInputStream extends ServletInputStream {

	private OutputStream log;
	private ServletInputStream delegate;
	public LoggingServletInputStream(ServletInputStream is, OutputStream out) {
		this.delegate = is;
		this.log = out;
	}

	@Override
	public int read() throws IOException {
		int b = delegate.read();
		log.write(b);
		return b;
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		int actual = delegate.read(b, off, len);
		if(actual > 0) log.write(b, off, actual);
		return actual;
	}

	@Override
	public void close() throws IOException {
		super.close();
		log.flush();
	}

	@Override
	public int available() throws IOException {
		return delegate.available();
	}

	@Override
	public void mark(int readlimit) {
		delegate.mark(readlimit);
	}

	@Override
	public boolean markSupported() {
		return delegate.markSupported();
	}

	@Override
	public void reset() throws IOException {
		delegate.reset();
	}

	@Override
	public long skip(long n) throws IOException {
		return delegate.skip(n);
	}

	@Override
	public boolean isFinished() {
		return delegate.isFinished();
	}

	@Override
	public boolean isReady() {
		return delegate.isReady();
	}

	@Override
	public void setReadListener(ReadListener paramReadListener) {
		delegate.setReadListener(paramReadListener);
	}

}
