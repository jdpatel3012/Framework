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
 * DAO for boot library.
 * @since 4.5.0.0
 * */
public interface IBootLibDao 
{
    public String selectBootId(String bootID);
    
    int deleteBootId(String bootID);
    
    void insertBootId(
			String bootID, 
			String description, 
			String bootText);
    
    int updateBootId(
			String bootID, 
			String description, 
			String bootText);
    
    public boolean selectBootIdExists(String bootId);

}
