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
package com.ibaset.solumina.sfcore.application.impl;

import static com.ibaset.common.util.SoluminaUtils.stringEquals;

import java.math.BigInteger;
import java.sql.Types;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.support.SqlLobValue;
import org.springframework.jdbc.object.StoredProcedure;
import org.springframework.jdbc.support.lob.LobHandler;

import static com.ibaset.common.FrameworkConstants.*;
import com.ibaset.common.Reference;
import com.ibaset.common.client.SoluminaServiceLocator;
import com.ibaset.common.dao.JdbcDaoSupport;
import com.ibaset.common.security.context.ContextUtil;
import com.ibaset.common.solumina.exception.SoluminaException;
import com.ibaset.common.sql.ParameterHolder;
import com.ibaset.solumina.concurrent.user.NamedUserService;
import com.ibaset.solumina.sfcore.application.ILicense;
import com.ibaset.solumina.sfcore.application.ILicenseCompatibilityCheck;
import com.ibaset.solumina.sfcore.application.ILicenseInfo;
import com.ibaset.solumina.sfcore.application.IMessage;

public class LicenseImpl extends JdbcDaoSupport implements ILicense {

	private LicenseCheckInOutProc licenseCheckInOutProc = new LicenseCheckInOutProc();
	private LicenseLoaderProc licenseLoaderProc = new LicenseLoaderProc();
	private LicenseObjectsRecreateProc licenseObjectsRecreateProc = new LicenseObjectsRecreateProc();
	private AssignUserLicenseProc AssignUserLicenseProc = new AssignUserLicenseProc();
	private NamedUserService namedUserService;
	@Reference
	private IMessage message = null;
	@Reference
	private ILicenseInfo licenseInfo;
	
	private ILicenseCompatibilityCheck licenseCompatibilityCheck;

	public ILicenseCompatibilityCheck getLicenseCompatibilityCheck() {
		return licenseCompatibilityCheck;
	}

	public void setLicenseCompatibilityCheck(ILicenseCompatibilityCheck licenseCompatibilityCheck) {
		this.licenseCompatibilityCheck = licenseCompatibilityCheck;
	}

	public Boolean isLtaEnabled() 
	{
		return licenseInfo.isLtaEnabled();
	}

	public Boolean isXmlEnabled() 
	{
		return licenseInfo.isXmlEnabled();
	}
	
	public Boolean isArchivingEnabled() 
	{
		return licenseInfo.isArchivingEnabled();
	}
	
	/** (non-Javadoc)
	 * @see com.ibaset.solumina.sfcore.application.ILicense#setLtaEnabled(java.lang.Boolean)
	 * @deprecated
	 * @since 4430
	 */
	public void setLtaEnabled(Boolean ltaEnabled)
	{
	    
	}

	/** (non-Javadoc)
	 * @see com.ibaset.solumina.sfcore.application.ILicense#setXmlEnabled(java.lang.Boolean)
	 * @deprecated
	 * @since 4430
	 */
	public void setXmlEnabled(Boolean xmlEnabled)
	{
	    
	}
	
    public String getLicensePrefix()
    {
    	return licenseInfo.getLicensePrefix();
    }
    
    /**
     * 
     * @return Licensing mode (either 'NAMED' or 'CONCURRENT')
     */
    public String getLicensingMode()
    {
    	return licenseInfo.getLicensingMode();
    }
    
    private class LicenseCheckInOutProc extends StoredProcedure
    {
        public LicenseCheckInOutProc()
        {

            SqlParameter action       = new SqlParameter("action", Types.VARCHAR);
            SqlParameter licenseType  = new SqlParameter("licenseType", Types.VARCHAR);

            
            String SQL = "SFCORE_LICENSE_CHECKINOUT";
            setSql(SQL);
            
            declareParameter(licenseType);
            declareParameter(action);
        }

        @SuppressWarnings("rawtypes")
		public Map execute(Map params, DataSource ds)
        {
        	synchronized (this) 
        	{
                if (!isCompiled())
                {
                	setJdbcTemplate(createJdbcTemplate(ds));
                    compile();
                }
			}
            return execute(params);
        }
    }
    
    private class LicenseLoaderProc extends StoredProcedure
    {
        public LicenseLoaderProc()
        {
            SqlParameter licenseId    = new SqlParameter("licenseId", Types.VARCHAR);
            SqlParameter licenseData  = new SqlParameter("licenseData", Types.BLOB);

            String SQL = "SFCORE_LICENSE_LOADER";
            setSql(SQL);
            declareParameter(licenseId);
            declareParameter(licenseData);
        } 

        @SuppressWarnings("rawtypes")
		public Map execute(Map params, DataSource ds)
        {
        	synchronized (this) 
        	{
                if (!isCompiled())
                {
                	setJdbcTemplate(createJdbcTemplate(ds));
                    compile();
                }
			}
            return execute(params);
        }
    }
    
    private class LicenseObjectsRecreateProc extends StoredProcedure
    {
        public LicenseObjectsRecreateProc()
        {
            String sql = "SFCORE_CREATE_LICENSE_OBJECTS";
            setSql(sql);
        } 

        @SuppressWarnings("rawtypes")
        public Map execute(Map params, DataSource ds)
        {
        	synchronized (this) 
        	{
                if (!isCompiled())
                {
                	setJdbcTemplate(createJdbcTemplate(ds));
                    compile();
                }
			}
            return execute(params);
        }
    }
    
    private boolean existsLicenseType(String licenseType)
    {
    	StringBuffer selectSql = new StringBuffer().append("SELECT COUNT(*) ")
    											   .append("FROM "+getSchemaPrefix()+"SFCORE_LICENSE_INFO_V ")
    											   .append("WHERE ")
    											   .append("LICENSE = ?");
    	ParameterHolder params = new ParameterHolder();
    	params.addParameter(licenseType);
    											   
    	return queryForInt(selectSql.toString(), params) >= 1;
    }
    /**
     * 
     * @param userId user executing this stored procedure
     * @param action I for check in and O for check out
     * @param licenseType USER or SUPPLIER
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
	public void checkInOutNamedLicense(String userId, String action, String licenseType)
    {
        Map<String, String> licenseMap = new HashMap<String, String>();
        licenseMap.put("userId", userId);           // user executing this proc
        licenseMap.put("connectionId", "DUMMY");    // Always "DUMMY"
        licenseMap.put("action", action);           // "I" or "O" (in or out)
        licenseMap.put("licenseType", licenseType); // USER or SUPPLIER
        licenseMap.put("supplierId", null);

        try
        {
        	licenseCheckInOutProc.execute(licenseMap, this.getDataSource());
        }
        catch (DataAccessException dae)
        {
        	//throw new SoluminaException(">>License Error: " + dae.getMessage() +"<<");
        	throw new SoluminaException(dae.getMessage());
        }
    }

    public void checkInOutLicenseCount(String licenseType, String action)
    {
    	Map<String, String> licenseMap = new HashMap<String, String>();
    	licenseMap.put("licenseType", licenseType); 
    	licenseMap.put("action", action);
    	//licenseMap.put("licenseTypeOut", licenseTypeOut);
        try
        {
        	
        	Map resultMap = new HashMap(); 
        	resultMap = licenseCheckInOutProc.execute(licenseMap, this.getDataSource());
        }
        catch (DataAccessException dae)
        {
        	if(stringEquals(action, ILicense.CHECKOUT)
        			&& dae.getMessage() != null && (dae.getMessage().contains("SFFW_62D4CD37096926AFE0440003BA560E35")
        					|| dae.getMessage().contains("NO_MORE_LICENSES_AVAILABLE")))
        	{
        		 namedUserService.sendLicenseUnavailabilityEmailAsync(licenseType);
        	}
        	//throw new SoluminaException(">>License Error: " + dae.getMessage() +"<<");
        	throw new SoluminaException(dae.getMessage());
        }
    }
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
	public void licenseLoader(String licenseId, String licenseData)
    {
    	// License Data is coming in as HEX String so let's convert it back to binary
        byte[] licenseDataByteArray = new BigInteger(licenseData,16).toByteArray();
    	
        // Get the lob handler to pass it to the store proc call
    	LobHandler blobHandler = (LobHandler) SoluminaServiceLocator.getBean("lobHandler");
    	
    	SqlLobValue blobLicenseData = new SqlLobValue(licenseDataByteArray, blobHandler);
    	Map<String, Object> licenseMap = new HashMap<String, Object>();
    	licenseMap.put("licenseId", licenseId); 
    	licenseMap.put("licenseData", blobLicenseData);
      
    	LicenseFileCompatibilityInfo licenseFileCompatible = getLicenseFileCompatibilityData(blobLicenseData);
    	
		String versionFromLicenseFile = licenseFileCompatible.getSoluminaLicenseVersion();
		if (StringUtils.isEmpty(versionFromLicenseFile)) {
			versionFromLicenseFile = "";
		}
		if (!isCompatibleWithMajorRelease(licenseFileCompatible)) {
			message.raiseError("MFI_E123AC9BBD514E088CD3CF20419DFF45",
					versionFromLicenseFile, licenseFileCompatible.getRelease());
		}
		
        try
        {
        	licenseLoaderProc.execute(licenseMap, this.getDataSource());
        	licenseObjectsRecreateProc.execute(new HashMap(), this.getDataSource());
        }
        catch (Exception dae)
        {
        	//throw new SoluminaException(">>License Error: " + dae.getMessage() +"<<");
        	throw new SoluminaException(dae.getMessage());
        }
    }
    
	protected boolean isCompatibleWithMajorRelease(LicenseFileCompatibilityInfo licenseFileCompatible) {
		if (licenseFileCompatible != null) {
			if (licenseFileCompatible.isCompatibleWithMajorRelease()) {
				return true;
			}
		}
		return false;
	}

	public LicenseFileCompatibilityInfo getLicenseFileCompatibilityData(SqlLobValue blobLicenseData) {

		String selectSql = licenseCompatibilityCheck.getLicenseCompatibilityCheckSQL();

		ParameterHolder parameters = new ParameterHolder();

		parameters.addParameter(blobLicenseData);

		List<Map<String, String>> licenseFileCheckData = queryForList(selectSql, parameters);

		if (CollectionUtils.isNotEmpty(licenseFileCheckData)) {
			Iterator<Map<String, String>> iter = licenseFileCheckData.listIterator();
			if (iter.hasNext()) // there should be only one row from db
			{
				Map<String, String> soluminaLicenseVersionMap = iter.next();
				String soluminaLicenseVersion = soluminaLicenseVersionMap.get(PRODUCT_VERSION);
				String release = soluminaLicenseVersionMap.get(DATABASE_RELEASE_VERSION);
				return new LicenseFileCompatibilityInfo(soluminaLicenseVersion, release);
			}
		}
		return null;
	}

	public void clearInMemoryLicenseData()
    {
		licenseInfo.clearInMemoryLicenseData();
    }
    
    public boolean isApplicationLtaEnabled()
    {
        return licenseInfo.isApplicationLtaEnabled();
    }
        
    @SuppressWarnings("rawtypes")
	public String getFullUserLicenseFlagFromDb()
	{
		StringBuffer selectSql = new StringBuffer().append("SELECT ? FROM SFCORE_LICENSE_INFO_V A ")
												   .append(" WHERE A.LICENSE = ? ") 
												   .append(" AND A.TOTAL > 0 ");
		
		ParameterHolder parameters = new ParameterHolder();
		parameters.addParameter(X);
		parameters.addParameter(ILicense.FULL_USER);
		List list = queryForList(selectSql.toString(), parameters);
		if(list.size() > 0)
		{			
			return YES;
		}
		else
		{
			return NO;
		}
	}

	public void setNamedUserService(NamedUserService namedUserService) {
		this.namedUserService = namedUserService;
	}
	
	class LicenseFileCompatibilityInfo {

		private String soluminaLicenseVersion;
		private String release;

		public LicenseFileCompatibilityInfo(String soluminaLicenseVersion,String release ){
			this.soluminaLicenseVersion = soluminaLicenseVersion;
			this.release = release;
		}
		
		/**
		 * This method checks whether input license file is compatible with database/mt release or not.
		 * For e.g. G8R2 is only compatible with G8R2 or G8R2SP1, G8R2SP2, etc..
		 * @return
		 */
		public boolean isCompatibleWithMajorRelease() {
			if (StringUtils.isNotEmpty(soluminaLicenseVersion) && StringUtils.isNotEmpty(release)
					&& soluminaLicenseVersion.length() >= 4 && release.length() >= 4
					&& soluminaLicenseVersion.substring(0, 4).equalsIgnoreCase(release.substring(0, 4))) {
				return true;
			}
			return false;
		}

		public String getSoluminaLicenseVersion() {
			return soluminaLicenseVersion;
		}
		public void setSoluminaLicenseVersion(String soluminaLicenseVersion) {
			this.soluminaLicenseVersion = soluminaLicenseVersion;
		}
		public String getRelease() {
			return release;
		}
		public void setRelease(String release) {
			this.release = release;
		}
	}
	
	
    private class AssignUserLicenseProc extends StoredProcedure {
        public AssignUserLicenseProc() {

            SqlParameter licenseType = new SqlParameter("licenseType", Types.VARCHAR);
            SqlParameter userId = new SqlParameter("userId", Types.VARCHAR);
            SqlParameter ucfLicenseUserVch1 = new SqlParameter("ucfLicenseUserVch1", Types.VARCHAR);
            SqlParameter ucfLicenseUserVch2 = new SqlParameter("ucfLicenseUserVch2", Types.VARCHAR);
            SqlParameter ucfLicenseUserVch3 = new SqlParameter("ucfLicenseUserVch3", Types.VARCHAR);
            SqlParameter ucfLicenseUserVch4 = new SqlParameter("ucfLicenseUserVch4", Types.VARCHAR);
            SqlParameter ucfLicenseUserVch5 = new SqlParameter("ucfLicenseUserVch5", Types.VARCHAR);
            SqlParameter ucfLicenseUserVch6 = new SqlParameter("ucfLicenseUserVch6", Types.VARCHAR);
            SqlParameter ucfLicenseUserVch7 = new SqlParameter("ucfLicenseUserVch7", Types.VARCHAR);
            SqlParameter ucfLicenseUserVch8 = new SqlParameter("ucfLicenseUserVch8", Types.VARCHAR);
            SqlParameter ucfLicenseUserVch9 = new SqlParameter("ucfLicenseUserVch9", Types.VARCHAR);
            SqlParameter ucfLicenseUserVch10 = new SqlParameter("ucfLicenseUserVch10", Types.VARCHAR);
            SqlParameter ucfLicenseUserVch11 = new SqlParameter("ucfLicenseUserVch11", Types.VARCHAR);
            SqlParameter ucfLicenseUserVch12 = new SqlParameter("ucfLicenseUserVch12", Types.VARCHAR);
            SqlParameter ucfLicenseUserVch13 = new SqlParameter("ucfLicenseUserVch13", Types.VARCHAR);
            SqlParameter ucfLicenseUserVch14 = new SqlParameter("ucfLicenseUserVch14", Types.VARCHAR);
            SqlParameter ucfLicenseUserVch15 = new SqlParameter("ucfLicenseUserVch15", Types.VARCHAR);

            SqlParameter ucfLicenseUserNum1 = new SqlParameter("ucfLicenseUserNum1", Types.NUMERIC);
            SqlParameter ucfLicenseUserNum2 = new SqlParameter("ucfLicenseUserNum2", Types.NUMERIC);
            SqlParameter ucfLicenseUserNum3 = new SqlParameter("ucfLicenseUserNum3", Types.NUMERIC);
            SqlParameter ucfLicenseUserNum4 = new SqlParameter("ucfLicenseUserNum4", Types.NUMERIC);
            SqlParameter ucfLicenseUserNum5 = new SqlParameter("ucfLicenseUserNum5", Types.NUMERIC);

            SqlParameter ucfLicenseUserDate1 = new SqlParameter("ucfLicenseUserDate1", Types.DATE);
            SqlParameter ucfLicenseUserDate2 = new SqlParameter("ucfLicenseUserDate2", Types.DATE);
            SqlParameter ucfLicenseUserDate3 = new SqlParameter("ucfLicenseUserDate3", Types.DATE);
            SqlParameter ucfLicenseUserDate4 = new SqlParameter("ucfLicenseUserDate4", Types.DATE);
            SqlParameter ucfLicenseUserDate5 = new SqlParameter("ucfLicenseUserDate5", Types.DATE);

            SqlParameter ucfLicenseUserFlag1 = new SqlParameter("ucfLicenseUserFlag1", Types.CHAR);
            SqlParameter ucfLicenseUserFlag2 = new SqlParameter("ucfLicenseUserFlag2", Types.CHAR);
            SqlParameter ucfLicenseUserFlag3 = new SqlParameter("ucfLicenseUserFlag3", Types.CHAR);
            SqlParameter ucfLicenseUserFlag4 = new SqlParameter("ucfLicenseUserFlag4", Types.CHAR);
            SqlParameter ucfLicenseUserFlag5 = new SqlParameter("ucfLicenseUserFlag5", Types.CHAR);

            SqlParameter username = new SqlParameter("updtUserId", Types.VARCHAR);
            SqlParameter licenseKey = new SqlParameter("licenseKey", Types.VARCHAR);

            String sql = "SFCORE_ASSIGN_USER_LICENSE";
            setSql(sql);

            declareParameter(licenseType);
            declareParameter(userId);
            declareParameter(ucfLicenseUserVch1);
            declareParameter(ucfLicenseUserVch2);
            declareParameter(ucfLicenseUserVch3);
            declareParameter(ucfLicenseUserVch4);
            declareParameter(ucfLicenseUserVch5);
            declareParameter(ucfLicenseUserVch6);
            declareParameter(ucfLicenseUserVch7);
            declareParameter(ucfLicenseUserVch8);
            declareParameter(ucfLicenseUserVch9);
            declareParameter(ucfLicenseUserVch10);
            declareParameter(ucfLicenseUserVch11);
            declareParameter(ucfLicenseUserVch12);
            declareParameter(ucfLicenseUserVch13);
            declareParameter(ucfLicenseUserVch14);
            declareParameter(ucfLicenseUserVch15);

            declareParameter(ucfLicenseUserNum1);
            declareParameter(ucfLicenseUserNum2);
            declareParameter(ucfLicenseUserNum3);
            declareParameter(ucfLicenseUserNum4);
            declareParameter(ucfLicenseUserNum5);

            declareParameter(ucfLicenseUserDate1);
            declareParameter(ucfLicenseUserDate2);
            declareParameter(ucfLicenseUserDate3);
            declareParameter(ucfLicenseUserDate4);
            declareParameter(ucfLicenseUserDate5);

            declareParameter(ucfLicenseUserFlag1);
            declareParameter(ucfLicenseUserFlag2);
            declareParameter(ucfLicenseUserFlag3);
            declareParameter(ucfLicenseUserFlag4);
            declareParameter(ucfLicenseUserFlag5);
            declareParameter(username);
            declareParameter(licenseKey);
        }

        @SuppressWarnings("rawtypes")
        public Map execute(Map params, DataSource ds) {
            synchronized (this) {
                if (!isCompiled()) {
                    setJdbcTemplate(createJdbcTemplate(ds));
                    compile();
                }
            }
            return execute(params);
        }
    }
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
    public void insertUserLicense(String licenseType, String userId, String ucfLicenseUserVch1,
            String ucfLicenseUserVch2, String ucfLicenseUserVch3, String ucfLicenseUserVch4, String ucfLicenseUserVch5,
            String ucfLicenseUserVch6, String ucfLicenseUserVch7, String ucfLicenseUserVch8, String ucfLicenseUserVch9,
            String ucfLicenseUserVch10, String ucfLicenseUserVch11, String ucfLicenseUserVch12,
            String ucfLicenseUserVch13, String ucfLicenseUserVch14, String ucfLicenseUserVch15,
            Number ucfLicenseUserNum1, Number ucfLicenseUserNum2, Number ucfLicenseUserNum3, Number ucfLicenseUserNum4,
            Number ucfLicenseUserNum5, Date ucfLicenseUserDate1, Date ucfLicenseUserDate2, Date ucfLicenseUserDate3,
            Date ucfLicenseUserDate4, Date ucfLicenseUserDate5, String ucfLicenseUserFlag1, String ucfLicenseUserFlag2,
            String ucfLicenseUserFlag3, String ucfLicenseUserFlag4, String ucfLicenseUserFlag5) {
        Map licenseMap = new HashMap();
        licenseMap.put("licenseType", licenseType);
        licenseMap.put("userId", userId);
        licenseMap.put("ucfLicenseUserVch1", ucfLicenseUserVch1);
        licenseMap.put("ucfLicenseUserVch2", ucfLicenseUserVch2);
        licenseMap.put("ucfLicenseUserVch3", ucfLicenseUserVch3);
        licenseMap.put("ucfLicenseUserVch4", ucfLicenseUserVch4);
        licenseMap.put("ucfLicenseUserVch5", ucfLicenseUserVch5);
        licenseMap.put("ucfLicenseUserVch6", ucfLicenseUserVch6);
        licenseMap.put("ucfLicenseUserVch7", ucfLicenseUserVch7);
        licenseMap.put("ucfLicenseUserVch8", ucfLicenseUserVch8);
        licenseMap.put("ucfLicenseUserVch9", ucfLicenseUserVch9);
        licenseMap.put("ucfLicenseUserVch10", ucfLicenseUserVch10);
        licenseMap.put("ucfLicenseUserVch11", ucfLicenseUserVch11);
        licenseMap.put("ucfLicenseUserVch12", ucfLicenseUserVch12);
        licenseMap.put("ucfLicenseUserVch13", ucfLicenseUserVch13);
        licenseMap.put("ucfLicenseUserVch14", ucfLicenseUserVch14);
        licenseMap.put("ucfLicenseUserVch15", ucfLicenseUserVch15);

        licenseMap.put("ucfLicenseUserNum1", ucfLicenseUserNum1);
        licenseMap.put("ucfLicenseUserNum2", ucfLicenseUserNum2);
        licenseMap.put("ucfLicenseUserNum3", ucfLicenseUserNum3);
        licenseMap.put("ucfLicenseUserNum4", ucfLicenseUserNum4);
        licenseMap.put("ucfLicenseUserNum5", ucfLicenseUserNum5);

        licenseMap.put("ucfLicenseUserDate1", ucfLicenseUserDate1);
        licenseMap.put("ucfLicenseUserDate2", ucfLicenseUserDate2);
        licenseMap.put("ucfLicenseUserDate3", ucfLicenseUserDate3);
        licenseMap.put("ucfLicenseUserDate4", ucfLicenseUserDate4);
        licenseMap.put("ucfLicenseUserDate5", ucfLicenseUserDate5);

        licenseMap.put("ucfLicenseUserFlag1", ucfLicenseUserFlag1);
        licenseMap.put("ucfLicenseUserFlag2", ucfLicenseUserFlag2);
        licenseMap.put("ucfLicenseUserFlag3", ucfLicenseUserFlag3);
        licenseMap.put("ucfLicenseUserFlag4", ucfLicenseUserFlag4);
        licenseMap.put("ucfLicenseUserFlag5", ucfLicenseUserFlag5);

        licenseMap.put("updtUserId", ContextUtil.getUsername());
        licenseMap.put("licenseKey", "SOLUMINALKEY");

        try {
            AssignUserLicenseProc.execute(licenseMap, this.getDataSource());
        } catch (DataAccessException dae) {
            // throw new SoluminaException(">>License Error: " + dae.getMessage() +"<<");
            throw new SoluminaException(dae.getMessage());
        }
    }
}
