/**
 * Proprietary and Confidential
 * Copyright 1995-2011 iBASEt, Inc.
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
package com.ibaset.solumina.sfcore.dao;

/**
 * DAO for hierarchical configurations.
 * @since 4.5.0.0
 * */
public interface ICfgLibDao {

	/**
	 * Selects module configuration text for with specified cfgId.
	 * */
    String selectCfgText(String cfgId, String cfgModuleName);
    
	/**
	 * Inserts module configuration for with specified cfgId.
	 * */
    void insertCfg(String cfgId, 
    			String cfgModuleName,
    			String description,
    			String cfgText);
    
	/**
	 * Updates module configuration for with specified cfgId.
	 * */
    int updateCfg(String cfgId, 
			String cfgModuleName,
			String description,
			String cfgText);

    /**
     * Deletes module configuration with specified cfgId.
     * @param cfgId configuration id
     * @param cfgModuleName module name
     * */
    int deleteCfg(String cfgId, String cfgModuleName);

    /**
     * Deletes configurations for all modules with specified cfgId
     * @param cfgId configuration id
     * */
    int deleteAll(String cfgId);

    /**
     * @param groupName
     * @return
     */
    public boolean selectGroupNameExists(String groupName);
}
