/**
 * Proprietary and Confidential
 * Copyright 1995-2017 iBASEt, Inc.
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
package com.ibaset.solumina.sfcore.application.impl;

import com.ibaset.common.FrameworkConstants;
import com.ibaset.solumina.sfcore.application.ILicenseCompatibilityCheck;

public class MSSqlLicenseCompatibilityCheck implements ILicenseCompatibilityCheck {
	
	private String licenseCompatibleQuery = "select SFMFG.SFCORE_GET_BLOB_VALU(?, '"+FrameworkConstants.PRODUCT_VERSION_FROM_LICENSE_FILE+"') PRODUCT_VERSION, RELEASE from (select top(1) * from sfdb_info where release like 'G8R2%') a ;";

	
	@Override
	public String getLicenseCompatibilityCheckSQL() {
		return licenseCompatibleQuery;
	}

}
