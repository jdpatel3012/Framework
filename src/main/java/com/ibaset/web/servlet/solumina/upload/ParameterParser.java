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


import java.util.HashMap;
import java.util.Map;

/**
 * A simple parser intended to parse sequences of name/value pairs. Parameter values are expected to
 * be enclosed in quotes if they contain unsafe characters, such as '=' characters or separators.
 * Parameter values are optional and can be omitted.
 * 
 * <p>
 * <code>param1 = value; param2 = "anything goes; really"; param3</code>
 * </p>
 * 
 * @author <a href="mailto:oleg@ural.ru">Oleg Kalnichevski</a>
 */

public final class ParameterParser
{
	/**
	 * String to be parsed.
	 */
	private char[] chars = null;

	/**
	 * Current position in the string.
	 */
	private int pos = 0;

	/**
	 * Maximum position in the string.
	 */
	private int len = 0;

	/**
	 * Start of a token.
	 */
	private int i1 = 0;

	/**
	 * End of a token.
	 */
	private int i2 = 0;

	/**
	 * Whether names stored in the map should be converted to lower case.
	 */
	private boolean lowerCaseNames = false;

	/**
	 * Default ParameterParser constructor.
	 */
	public ParameterParser()
	{
		super();
	}

	/**
	 * A helper method to process the parsed token. This method removes leading and trailing blanks
	 * as well as enclosing quotation marks, when necessary.
	 * 
	 * @param quoted
	 *            <tt>true</tt> if quotation marks are expected, <tt>false</tt> otherwise.
	 * @return the token
	 */
	private String getToken(boolean quoted)
	{
		// Trim leading white spaces
		while ((i1 < i2) && (Character.isWhitespace(chars[i1])))
		{
			i1++;
		}
		// Trim trailing white spaces
		while ((i2 > i1) && (Character.isWhitespace(chars[i2 - 1])))
		{
			i2--;
		}
		// Strip away quotation marks if necessary
		if (quoted)
		{
			if (((i2 - i1) >= 2) && (chars[i1] == '"') && (chars[i2 - 1] == '"'))
			{
				i1++;
				i2--;
			}
		}
		String result = null;
		if (i2 > i1)
		{
			result = new String(chars, i1, i2 - i1);
		}
		return result;
	}

	/**
	 * Tests if the given character is present in the array of characters.
	 * 
	 * @param ch
	 *            the character to test for presence in the array of characters
	 * @param charray
	 *            the array of characters to test against
	 * 
	 * @return <tt>true</tt> if the character is present in the array of characters,
	 *         <tt>false</tt> otherwise.
	 */
	private static boolean isOneOf(char ch, final char[] charray)
	{
		boolean result = false;
		for (int i = 0; i < charray.length; i++)
		{
			if (ch == charray[i])
			{
				result = true;
				break;
			}
		}
		return result;
	}

	/**
	 * Parses out a token until any of the given terminators is encountered.
	 * 
	 * @param terminators
	 *            the array of terminating characters. Any of these characters when encountered
	 *            signify the end of the token
	 * 
	 * @return the token
	 */
	private String parseToken(final char[] terminators)
	{
		char ch;
		i1 = pos;
		i2 = pos;
		while (pos < len)
		{
			ch = chars[pos];
			if (isOneOf(ch, terminators))
			{
				break;
			}
			i2++;
			pos++;
		}
		return getToken(false);
	}

	/**
	 * Parses out a token until any of the given terminators is encountered outside the quotation
	 * marks.
	 * 
	 * @param terminators
	 *            the array of terminating characters. Any of these characters when encountered
	 *            outside the quotation marks signify the end of the token
	 * 
	 * @return the token
	 */
	private String parseQuotedToken(final char[] terminators)
	{
		char ch;
		i1 = pos;
		i2 = pos;
		boolean quoted = false;
		boolean charEscaped = false;
		while (pos < len)
		{
			ch = chars[pos];
			if (!quoted && isOneOf(ch, terminators))
			{
				break;
			}
			if (!charEscaped && ch == '"')
			{
				quoted = !quoted;
			}
			charEscaped = (!charEscaped && ch == '\\');
			i2++;
			pos++;

		}
		return getToken(true);
	}

	/**
	 * Returns <tt>true</tt> if parameter names are to be converted to lower case when name/value
	 * pairs are parsed.
	 * 
	 * @return <tt>true</tt> if parameter names are to be converted to lower case when name/value
	 *         pairs are parsed. Otherwise returns <tt>false</tt>
	 */
	public boolean isLowerCaseNames()
	{
		return lowerCaseNames;
	}

	/**
	 * Sets the flag if parameter names are to be converted to lower case when name/value pairs are
	 * parsed.
	 * 
	 * @param b
	 *            <tt>true</tt> if parameter names are to be converted to lower case when
	 *            name/value pairs are parsed. <tt>false</tt> otherwise.
	 */
	public void setLowerCaseNames(boolean b)
	{
		lowerCaseNames = b;
	}

	/**
	 * Extracts a map of name/value pairs from the given string. Names are expected to be unique.
	 * 
	 * @param str
	 *            the string that contains a sequence of name/value pairs
	 * @param separator
	 *            the name/value pairs separator
	 * 
	 * @return a map of name/value pairs
	 */
	public Map parse(final String str, char separator)
	{
		if (str == null)
		{
			return new HashMap();
		}
		return parse(str.toCharArray(), separator);
	}

	/**
	 * Extracts a map of name/value pairs from the given array of characters. Names are expected to
	 * be unique.
	 * 
	 * @param chars
	 *            the array of characters that contains a sequence of name/value pairs
	 * @param separator
	 *            the name/value pairs separator
	 * 
	 * @return a map of name/value pairs
	 */
	public Map parse(final char[] chars, char separator)
	{
		if (chars == null)
		{
			return new HashMap();
		}
		return parse(chars, 0, chars.length, separator);
	}

	/**
	 * Extracts a map of name/value pairs from the given array of characters. Names are expected to
	 * be unique.
	 * 
	 * @param chars
	 *            the array of characters that contains a sequence of name/value pairs
	 * @param offset -
	 *            the initial offset.
	 * @param length -
	 *            the length.
	 * @param separator
	 *            the name/value pairs separator
	 * 
	 * @return a map of name/value pairs
	 */
	public Map parse(final char[] chars, int offset, int length, char separator)
	{

		if (chars == null)
		{
			return new HashMap();
		}
		HashMap params = new HashMap();
		this.chars = chars;
		pos = offset;
		len = length;

		String paramName = null;
		String paramValue = null;
		while (pos < len)
		{
			paramName = parseToken(new char[] { '=', separator });
			paramValue = null;
			if (pos < len && (chars[pos] == '='))
			{
				pos++; // skip '='
				paramValue = parseQuotedToken(new char[] { separator });
			}
			if (pos < len && (chars[pos] == separator))
			{
				pos++; // skip separator
			}
			if ((paramName != null) && (paramName.length() > 0))
			{
				if (lowerCaseNames)
				{
					paramName = paramName.toLowerCase();
				}
				params.put(paramName, paramValue);
			}
		}
		return params;
	}
}
