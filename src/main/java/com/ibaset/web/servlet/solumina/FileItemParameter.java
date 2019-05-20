/**
 * Proprietary and Confidential
 * Copyright 1995-2010 iBASEt, Inc.
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
import java.io.InputStream;

import org.apache.commons.fileupload.FileItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of BinaryParameter backed by FileItem.
 * */
public final class FileItemParameter extends AbstractBinaryParameter {

    protected static final Logger logger = LoggerFactory.getLogger(FileItemParameter.class);
	private final FileItem item;
	
	public FileItemParameter(FileItem item) {
		super(item.getFieldName());
		this.item = item;
	}

	@Override
	public void delete() {
		item.delete();
	}

	@Override
	public byte[] getByteArray() {
		return item.get();
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return item.getInputStream();
	}

	@Override
	public long getSize() {
		return item.getSize();
	}

	@Override
	public boolean isInMemory() {
		return item.isInMemory();
	}
	@Override
	public String toString() 
	{
		String content = null;
		try {
			content = isInMemory()? getString() : "size ="+getSize();
        } catch (IOException e) {
            logger.debug(e.getMessage(), e);
        }
		return "FileItemParameter[" + content +"]";
	}

}
