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
package com.ibaset.web.servlet.solumina.upload;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.FileUploadException;

import com.ibaset.common.BinaryParameter;
import com.ibaset.web.servlet.solumina.ByteArrayParameter;

public class MultipartFormData {

	/**
	 * The maximum size permitted for an uploaded file. A value of -1 indicates no maximum.
	 */
	private long sizeMax = -1;


	/**
	 * The content encoding to use when reading part headers.
	 */
	private String headerEncoding;
	
	/**
	 * HTTP content type header name.
	 */
	public static final String CONTENT_TYPE = "Content-type";


	/**
	 * HTTP content disposition header name.
	 */
	public static final String CONTENT_DISPOSITION = "Content-disposition";


	/**
	 * Content-disposition value for form data.
	 */
	public static final String FORM_DATA = "form-data";


	/**
	 * Content-disposition value for file attachment.
	 */
	public static final String ATTACHMENT = "attachment";


	/**
	 * Part of HTTP content type header.
	 */
	public static final String MULTIPART = "multipart/";


	/**
	 * HTTP content type header for multipart forms.
	 */
	public static final String MULTIPART_FORM_DATA = "multipart/form-data";


	/**
	 * HTTP content type header for multiple uploads.
	 */
	public static final String MULTIPART_MIXED = "multipart/mixed";

	/**
	 * The maximum length of a single header line that will be parsed (1024 bytes).
	 */
	public static final int MAX_HEADER_SIZE = 1024;

	
	public  Map<String, BinaryParameter> parseRequest(HttpServletRequest ctx) throws FileUploadException
	{
		if (ctx == null)
		{
			throw new IllegalArgumentException("ctx parameter cannot be null");
		}

		Map<String, BinaryParameter> result = new HashMap<String, BinaryParameter>();
		String contentType = ctx.getContentType();

		if ((null == contentType) || (!contentType.toLowerCase().startsWith(MULTIPART)))
		{
			throw new FileUploadException("the request doesn't contain a " +
					MULTIPART_FORM_DATA + " or " + MULTIPART_MIXED +
					" stream, content type header is " + contentType);
		}
		int requestSize = ctx.getContentLength();

		if (requestSize == -1)
		{
			throw new FileUploadException("the request was rejected because its size is unknown");
		}

		if (sizeMax >= 0 && requestSize > sizeMax)
		{
			throw new FileUploadException("the request was rejected because "
					+ "its size exceeds allowed range");
		}

		try
		{
			byte[] boundary = getBoundary(contentType);
			if (boundary == null)
			{
				throw new FileUploadException("the request was rejected because "
						+ "no multipart boundary was found");
			}

			InputStream input = ctx.getInputStream();

			MultipartFormInputStream multi = new MultipartFormInputStream(input, boundary);
			multi.setHeaderEncoding(headerEncoding);

			boolean nextPart = multi.skipPreamble();

			// Don't allow a header larger than this size (to prevent DOS
			// attacks)
			final int maxHeaderBytes = 65536;
			while (nextPart)
			{
				Map headers = parseHeaders(multi.readHeaders(maxHeaderBytes));
				String fieldName = getFieldName(headers);
				if (fieldName != null)
				{
					String subContentType = getHeader(headers, CONTENT_TYPE);
					if (subContentType != null &&
							subContentType.toLowerCase().startsWith(MULTIPART_MIXED))
					{
						// Multiple files.
						byte[] subBoundary = getBoundary(subContentType);
						multi.setBoundary(subBoundary);
						boolean nextSubPart = multi.skipPreamble();
						while (nextSubPart)
						{
							headers = parseHeaders(multi.readHeaders(maxHeaderBytes));
							if (getFileName(headers) != null)
							{
		                        ByteArrayOutputStream os = new ByteArrayOutputStream();
								try
								{
									multi.readBodyData(os);
								}
								finally
								{
									os.close();
								}
								result.put(fieldName, new ByteArrayParameter(fieldName, os.toByteArray()));
							}
							else
							{
								// Ignore anything but files inside
								// multipart/mixed.
								multi.discardBodyData();
							}
							nextSubPart = multi.readBoundary();
						}
						multi.setBoundary(boundary);
					}
					else
					{
                        ByteArrayOutputStream os = new ByteArrayOutputStream();
						try
						{
							multi.readBodyData(os);
						}
						finally
						{
							os.close();
						}
						result.put(fieldName, new ByteArrayParameter(fieldName, os.toByteArray()));
					}
				}
				else
				{
					// Skip this part.
					multi.discardBodyData();
				}
				nextPart = multi.readBoundary();
			}
		}
		catch (IOException e)
		{
            throw new FileUploadException("Processing of " + MULTIPART_FORM_DATA +
					" request failed. " + e.getMessage(), e);
		}

		return result;
	}
	/**
	 * Retrieves the boundary from the <code>Content-type</code> header.
	 *
	 * @param contentType
	 *            The value of the content type header from which to extract the boundary value.
	 *
	 * @return The boundary, as a byte array.
	 */
	protected byte[] getBoundary(String contentType)
	{
		ParameterParser parser = new ParameterParser();
		parser.setLowerCaseNames(true);
		// Parameter parser can handle null input
		Map params = parser.parse(contentType, ';');
		String boundaryStr = (String)params.get("boundary");

		if (boundaryStr == null)
		{
			return null;
		}
		byte[] boundary;
		try
		{
			boundary = boundaryStr.getBytes("ISO-8859-1");
		}
		catch (UnsupportedEncodingException e)
		{
			boundary = boundaryStr.getBytes();
		}
		return boundary;
	}


	/**
	 * Retrieves the file name from the <code>Content-disposition</code> header.
	 *
	 * @param headers
	 *            A <code>Map</code> containing the HTTP request headers.
	 *
	 * @return The file name for the current <code>encapsulation</code>.
	 */
	protected String getFileName(Map /* String, String */headers)
	{
		String fileName = null;
		String cd = getHeader(headers, CONTENT_DISPOSITION);
		if (cd.startsWith(FORM_DATA) || cd.startsWith(ATTACHMENT))
		{
			ParameterParser parser = new ParameterParser();
			parser.setLowerCaseNames(true);
			// Parameter parser can handle null input
			Map params = parser.parse(cd, ';');
			if (params.containsKey("filename"))
			{
				fileName = (String)params.get("filename");
				if (fileName != null)
				{
					fileName = fileName.trim();
					int index = fileName.lastIndexOf('\\');
					if (index == -1)
					{
						index = fileName.lastIndexOf('/');
					}
					if (index != -1)
					{
						fileName = fileName.substring(index + 1);
					}
				}
				else
				{
					// Even if there is no value, the parameter is present, so
					// we return an empty file name rather than no file name.
					fileName = "";
				}
			}
		}
		return fileName;
	}


	/**
	 * Retrieves the field name from the <code>Content-disposition</code> header.
	 *
	 * @param headers
	 *            A <code>Map</code> containing the HTTP request headers.
	 *
	 * @return The field name for the current <code>encapsulation</code>.
	 */
	protected String getFieldName(Map /* String, String */headers)
	{
		String fieldName = null;
		String cd = getHeader(headers, CONTENT_DISPOSITION);
		if (cd != null && cd.startsWith(FORM_DATA))
		{

			ParameterParser parser = new ParameterParser();
			parser.setLowerCaseNames(true);
			// Parameter parser can handle null input
			Map params = parser.parse(cd, ';');
			fieldName = (String)params.get("name");
			if (fieldName != null)
			{
				fieldName = fieldName.trim();
			}
		}
		return fieldName;
	}

	/**
	 * <p>
	 * Parses the <code>header-part</code> and returns as key/value pairs.
	 *
	 * <p>
	 * If there are multiple headers of the same names, the name will map to a comma-separated list
	 * containing the values.
	 *
	 * @param headerPart
	 *            The <code>header-part</code> of the current <code>encapsulation</code>.
	 *
	 * @return A <code>Map</code> containing the parsed HTTP request headers.
	 */
	protected Map /* String, String */parseHeaders(String headerPart)
	{
		Map headers = new HashMap();
		char[] buffer = new char[MAX_HEADER_SIZE];
		boolean done = false;
		int j = 0;
		int i;
		String header, headerName, headerValue;
		try
		{
			while (!done)
			{
				i = 0;
				// Copy a single line of characters into the buffer,
				// omitting trailing CRLF.
				while (i < 2 || buffer[i - 2] != '\r' || buffer[i - 1] != '\n')
				{
					buffer[i++] = headerPart.charAt(j++);
				}
				header = new String(buffer, 0, i - 2);
				if (header.equals(""))
				{
					done = true;
				}
				else
				{
					if (header.indexOf(':') == -1)
					{
						// This header line is malformed, skip it.
						continue;
					}
					headerName = header.substring(0, header.indexOf(':')).trim().toLowerCase();
					headerValue = header.substring(header.indexOf(':') + 1).trim();
					if (getHeader(headers, headerName) != null)
					{
						// More that one header of that name exists,
						// append to the list.
						headers.put(headerName, getHeader(headers, headerName) + ',' + headerValue);
					}
					else
					{
						headers.put(headerName, headerValue);
					}
				}
			}
		}
		catch (IndexOutOfBoundsException e)
		{
			// Headers were malformed. continue with all that was
			// parsed.
		}
		return headers;
	}


	/**
	 * Returns the header with the specified name from the supplied map. The header lookup is
	 * case-insensitive.
	 *
	 * @param headers
	 *            A <code>Map</code> containing the HTTP request headers.
	 * @param name
	 *            The name of the header to return.
	 *
	 * @return The value of specified header, or a comma-separated list if there were multiple
	 *         headers of that name.
	 */
	protected final String getHeader(Map /* String, String */headers, String name)
	{
		return (String)headers.get(name.toLowerCase());
	}

}