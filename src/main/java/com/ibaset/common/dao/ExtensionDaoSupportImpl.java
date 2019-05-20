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
package com.ibaset.common.dao;

import java.util.Date;

import org.springframework.jdbc.support.incrementer.DataFieldMaxValueIncrementer;

import com.ibaset.common.dao.JdbcDaoSupport;

public class ExtensionDaoSupportImpl extends JdbcDaoSupport implements ExtensionDaoSupport
{
	private DataFieldMaxValueIncrementer soluminaSequence;
	
    /* (non-Javadoc)
     * @see com.ibaset.solumina.ic.common.ExtensionDaoSupport#selectDBSystemDate()
     */
    public Date selectDBSystemDate()
    {
        StringBuffer selectSql = new StringBuffer() .append("SELECT ")
                                                    .append(getTimestampFunction())
                                                    .append(getDualTable());

        Date systemDate = (Date) queryForObject(selectSql.toString(),
                                                Date.class);
        return systemDate;
    }

	public DataFieldMaxValueIncrementer getSoluminaSequence() 
	{
		return soluminaSequence;
	}

	public void setSoluminaSequence(DataFieldMaxValueIncrementer soluminaSequence) 
	{
		this.soluminaSequence = soluminaSequence;
	}
}
