/**
 * Proprietary and Confidential
 * Copyright 1995-2018 iBASEt, Inc.
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.annotation.adapters.HexBinaryAdapter;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibaset.common.client.SoluminaServiceLocator;
import com.ibaset.common.context.SoluminaContextHolder;
import com.ibaset.common.context.TransactionalContext;
import com.ibaset.common.security.context.UserContext;
import com.ibaset.common.solumina.ISoluminaInfo;
import com.ibaset.common.sql.security.UnauthorizedAccessException;

public class SoluminaCipherUtilsImpl implements ISoluminaCipherUtils {

	Logger logger = LoggerFactory.getLogger(this.getClass());
	private final String ALGORITHM = "AES/CBC/PKCS5Padding";
	private final String SQL_ENCRYPTION = "SQL_ENCRYPTION";
	private final String UNICODE_FORMAT = "UTF-8";
	// staticKey and ivParamHex must be unique at WinClient, SWEP and MT side
	private final String staticKey = ";Vj#/9d!r?T3/*Qb";
	private final String ivParamHex = "6B1E2FFFE8A114009D8FE22F6DB5F876";
	private Boolean sqlEncryptionEnabled = false;
	private HexBinaryAdapter adapter = new HexBinaryAdapter();

	private Cipher initializeCipher(boolean useStaticKey) throws Exception {
		UserContext userContext = SoluminaContextHolder.getUserContext();
		String privateKey = useStaticKey ? staticKey : preparePrivateKey(userContext.getConnectionId(), staticKey, 16);
		Cipher cipher = Cipher.getInstance(ALGORITHM);
		SecretKeySpec secretKey = new SecretKeySpec(Arrays.copyOf(privateKey.getBytes(UNICODE_FORMAT), 16), "AES");
		byte[] iv = adapter.unmarshal(ivParamHex);
		IvParameterSpec paramSpec = new IvParameterSpec(iv);
		cipher.init(Cipher.DECRYPT_MODE, secretKey, paramSpec);
		return cipher;
	}

	public String decryptSqlText(String sqlText, boolean useStaticKey) {
		String decryptedText = StringUtils.EMPTY;
		if (sqlEncryptionEnabled) {
			try {
				Cipher cipher = null;
				if (useStaticKey) {
					cipher = initializeCipher(useStaticKey);
				} else {
					TransactionalContext transactionalContext = SoluminaContextHolder.getTransactionalContext();
					cipher = transactionalContext.getCipher();

					if (cipher == null) {
						cipher = initializeCipher(useStaticKey);
						transactionalContext.setCipher(cipher);
					}
				}

				byte[] encryptedText = adapter.unmarshal(sqlText);
				decryptedText = new String(cipher.doFinal(encryptedText));
			} catch (Exception e) {
				logger.error("Error while decryption", e);
				throw new UnauthorizedAccessException("Invalid/Tempered sql.");
			}
		} else {
			decryptedText = sqlText;
		}

		return decryptedText;
	}

	public void setupSqlEncryptionFlag() {
		ISoluminaInfo soluminaInfo = (ISoluminaInfo) SoluminaServiceLocator.locateService(ISoluminaInfo.class);
		String soluminaMTCapabilities = soluminaInfo.getCapabilities();
		try {
			Properties pr = new Properties();
			pr.load(new ByteArrayInputStream(soluminaMTCapabilities.getBytes()));
			String sqlEncryption = pr.getProperty(SQL_ENCRYPTION);
			if (StringUtils.isNotEmpty(sqlEncryption)
					&& Character.toString(sqlEncryption.toUpperCase().charAt(0)).matches("[TY1-9]")) {
				sqlEncryptionEnabled = true;
			}
		} catch (IOException e) {
			logger.error("Error while reading MT capabilities params", e);
		}
	}

	private String preparePrivateKey(String dynamicKey, String staticKey, int length) {
		String privateKey = StringUtils.EMPTY;
		dynamicKey = StringUtils.defaultIfEmpty(dynamicKey, StringUtils.EMPTY);
		staticKey = StringUtils.defaultIfEmpty(staticKey, StringUtils.EMPTY);

		if (dynamicKey.length() < (length - 4)) {
			dynamicKey = StringUtils.rightPad(dynamicKey, length - 4, staticKey.substring(dynamicKey.length()));
		}

		privateKey = dynamicKey.substring(0, length - 4) + staticKey.substring(length - 4);

		return privateKey;
	}
}
