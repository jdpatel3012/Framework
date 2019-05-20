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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;

import junitparams.JUnitParamsRunner;

@RunWith(JUnitParamsRunner.class)
public class TestLicenseImpl
{
	@InjectMocks
	@Spy
	LicenseImpl licenseImpl = new LicenseImpl();
	
	@Test
	public void testIsCompatibleWithMajorRelease()
	{
		LicenseImpl.LicenseFileCompatibilityInfo LicenseFileCompatibilityInfo = getLicenseCompatibilityInfo("G8R2", "G8R2");
		boolean isCompatibleWithMajorRelease=  licenseImpl.isCompatibleWithMajorRelease(LicenseFileCompatibilityInfo);
		
		assertTrue(isCompatibleWithMajorRelease);
	}


	@Test
	public void testIsCompatibleWithMajorRelease_IncorrectVersions()
	{
		LicenseImpl.LicenseFileCompatibilityInfo LicenseFileCompatibilityInfo = getLicenseCompatibilityInfo("G8R1", "G8R2");
		boolean isCompatibleWithMajorRelease=  licenseImpl.isCompatibleWithMajorRelease(LicenseFileCompatibilityInfo);
		
		assertFalse(isCompatibleWithMajorRelease);
	}

	@Test
	public void testIsCompatibleWithMajorRelease_MinorDBVersions()
	{
		LicenseImpl.LicenseFileCompatibilityInfo LicenseFileCompatibilityInfo = getLicenseCompatibilityInfo("G8R2", "G8R2SP6");
		boolean isCompatibleWithMajorRelease=  licenseImpl.isCompatibleWithMajorRelease(LicenseFileCompatibilityInfo);
		
		assertTrue(isCompatibleWithMajorRelease);
	}
	

	@Test
	public void testIsCompatibleWithMajorRelease_MinorLicenseVersions()
	{
		LicenseImpl.LicenseFileCompatibilityInfo LicenseFileCompatibilityInfo = getLicenseCompatibilityInfo("G8R2SP6", "G8R2");
		boolean isCompatibleWithMajorRelease=  licenseImpl.isCompatibleWithMajorRelease(LicenseFileCompatibilityInfo);
		
		assertTrue(isCompatibleWithMajorRelease);
	}
	
	@Test
	public void testIsCompatibleWithMajorRelease_SoluminaLicenseIgnoreCase()
	{
		LicenseImpl.LicenseFileCompatibilityInfo LicenseFileCompatibilityInfo = getLicenseCompatibilityInfo("G8r2", "G8R2");
		boolean isCompatibleWithMajorRelease=  licenseImpl.isCompatibleWithMajorRelease(LicenseFileCompatibilityInfo);
		
		assertTrue(isCompatibleWithMajorRelease);
	}
	
	@Test
	public void testIsCompatibleWithMajorRelease_DBIgnoreCase()
	{
		LicenseImpl.LicenseFileCompatibilityInfo LicenseFileCompatibilityInfo = getLicenseCompatibilityInfo("G8R2", "g8r2");
		boolean isCompatibleWithMajorRelease=  licenseImpl.isCompatibleWithMajorRelease(LicenseFileCompatibilityInfo);
		
		assertTrue(isCompatibleWithMajorRelease);
	}	
	
	@Test
	public void testIsCompatibleWithMajorRelease_NotValid_LicenseVersion()
	{
		LicenseImpl.LicenseFileCompatibilityInfo LicenseFileCompatibilityInfo = getLicenseCompatibilityInfo("G8R2", "g");
		boolean isCompatibleWithMajorRelease=  licenseImpl.isCompatibleWithMajorRelease(LicenseFileCompatibilityInfo);
		
		assertFalse(isCompatibleWithMajorRelease);
	}	

	@Test
	public void testIsCompatibleWithMajorRelease_NotValid_DBVersion()
	{
		LicenseImpl.LicenseFileCompatibilityInfo LicenseFileCompatibilityInfo = getLicenseCompatibilityInfo("G", "g8r2");
		boolean isCompatibleWithMajorRelease=  licenseImpl.isCompatibleWithMajorRelease(LicenseFileCompatibilityInfo);
		
		assertFalse(isCompatibleWithMajorRelease);
	}	

	@Test
	public void testIsCompatibleWithMajorRelease_Empty_Versions()
	{
		LicenseImpl.LicenseFileCompatibilityInfo LicenseFileCompatibilityInfo = getLicenseCompatibilityInfo("", "");
		boolean isCompatibleWithMajorRelease=  licenseImpl.isCompatibleWithMajorRelease(LicenseFileCompatibilityInfo);
		
		assertFalse(isCompatibleWithMajorRelease);
	}	

	@Test
	public void testIsCompatibleWithMajorRelease_DBEmpty_Version()
	{
		LicenseImpl.LicenseFileCompatibilityInfo LicenseFileCompatibilityInfo = getLicenseCompatibilityInfo("", "G8R2");
		boolean isCompatibleWithMajorRelease=  licenseImpl.isCompatibleWithMajorRelease(LicenseFileCompatibilityInfo);
		
		assertFalse(isCompatibleWithMajorRelease);
	}
	
	@Test
	public void testIsCompatibleWithMajorRelease_LicenseEmpty_Version()
	{
		LicenseImpl.LicenseFileCompatibilityInfo LicenseFileCompatibilityInfo = getLicenseCompatibilityInfo("G8R2", "");
		boolean isCompatibleWithMajorRelease=  licenseImpl.isCompatibleWithMajorRelease(LicenseFileCompatibilityInfo);
		
		assertFalse(isCompatibleWithMajorRelease);
	}	
	private LicenseImpl.LicenseFileCompatibilityInfo getLicenseCompatibilityInfo(String licenseVersion, String dataBaseVersion) {
		LicenseImpl.LicenseFileCompatibilityInfo LicenseFileCompatibilityInfo = new  LicenseImpl().new LicenseFileCompatibilityInfo(licenseVersion, dataBaseVersion);
		return LicenseFileCompatibilityInfo;
	}

}
