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

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibaset.common.context.SoluminaContextHolder;
import com.ibaset.common.security.context.UserContext;
import com.ibaset.solumina.sfcore.application.IConnectionHeartbeat;
import com.ibaset.solumina.sfcore.dao.ILoginDao;

public class ConnectionHeartbeatImpl implements IConnectionHeartbeat {

	private static final Logger logger = LoggerFactory.getLogger(ConnectionHeartbeatImpl.class);

	private ILoginDao loginDao;

	public void updateHeartbeat() {

		UserContext ctx = SoluminaContextHolder.getUserContext();
		logger.debug("Updating Heartbeat for connctionId:  " + ctx.getConnectionId());

		loginDao.updateHeartBeat(ctx.getConnectionId());
		
		Date date = new Date();
		long currentTimeInMilliSec = date.getTime();		
		ctx.setLastUpdatedHeartBeat(currentTimeInMilliSec);

		logger.debug("Heartbeat is updated sucessfully for connctionId:  " + ctx.getConnectionId());
	}

	public void setLoginDao(ILoginDao loginDao) {
		this.loginDao = loginDao;
	}
}
