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
package com.ibaset.common.security;



import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.encoding.BaseDigestPasswordEncoder;
import org.springframework.security.authentication.encoding.PasswordEncoder;

import com.ibaset.solumina.sffnd.application.IGlobalConfiguration;

public class Md5ShaPasswordEncoder extends BaseDigestPasswordEncoder implements
																	PasswordEncoder
{

	Logger logger = LoggerFactory.getLogger(this.getClass());
	private boolean uppercaseResults;
	
	private IGlobalConfiguration globalConfiguration;

	public boolean isPasswordValid(String encodedPassword, String cleartextPassword, Object salt)
	{
		
		if(!globalConfiguration.isMixedCasePasswordAllowed())
		{
			cleartextPassword = StringUtils.upperCase(cleartextPassword);
		}
		
		String pass1 = "" + encodedPassword;
		String pass2 = encodeInternal(mergePasswordAndSalt(cleartextPassword, salt, false));

		return pass1.equals(pass2);
	}

	public String encodePassword(String cleartextPassword, Object salt)
	{
		if(!globalConfiguration.isMixedCasePasswordAllowed())
		{
			cleartextPassword = StringUtils.upperCase(cleartextPassword);
		}
		
		return encodeInternal(mergePasswordAndSalt(cleartextPassword, salt, false));
	}

	private String encodeInternal(String cleartextPassword)
	{
		return encodeInternalSha1(encodeInternalMd5(cleartextPassword));
	}

	private String encodeInternalSha1(String cleartextPassword)
	{

		String firstPassEncoding = null;
		if (!getEncodeHashAsBase64())
		{
			firstPassEncoding = DigestUtils.shaHex(cleartextPassword);
		}

		else
		{
			byte[] encoded = Base64.encodeBase64(DigestUtils.sha(cleartextPassword));

			firstPassEncoding = new String(encoded);
		}

		if(uppercaseResults)
		{
			firstPassEncoding=StringUtils.upperCase(firstPassEncoding);
		}
		
		if(logger.isDebugEnabled()) logger.debug("Sha1 + md5 "+ firstPassEncoding);
		return firstPassEncoding;
	}

	private String encodeInternalMd5(String input)
	{
		String firstPassEncoding = null;
		if (!getEncodeHashAsBase64())
		{
			firstPassEncoding = DigestUtils.md5Hex(input);
		}

		else
		{
			byte[] encoded = Base64.encodeBase64(DigestUtils.md5(input));

			firstPassEncoding = new String(encoded);
		}
		
		if(uppercaseResults)
		{
			firstPassEncoding=StringUtils.upperCase(firstPassEncoding);
		}
		if(logger.isDebugEnabled()) logger.debug("md5 "+ firstPassEncoding);
		return firstPassEncoding;
	}

	public void setUppercaseResults(boolean uppercaseResults)
	{
		this.uppercaseResults=uppercaseResults;
	}


	public IGlobalConfiguration getGlobalConfiguration() {
		return globalConfiguration;
	}

	public void setGlobalConfiguration(IGlobalConfiguration globalConfiguration) {
		this.globalConfiguration = globalConfiguration;
	}
}
