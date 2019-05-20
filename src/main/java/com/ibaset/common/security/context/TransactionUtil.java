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
package com.ibaset.common.security.context;

import com.ibaset.common.context.SoluminaContextHolder;
import com.ibaset.common.context.TransactionalContext;
import com.ibaset.common.sql.cache.QueryKey;
import com.ibaset.common.sql.cache.TransactionQueryCache;

public class TransactionUtil {

	public static boolean isActive(){
		SoluminaUser u=ContextUtil.getUser();
		if(u==null) return false;
		
		TransactionalContext transactionalContext = SoluminaContextHolder.getTransactionalContext();
		return transactionalContext.getUdvScriptConnection()!=null;
	}
	public static boolean hasCache(){
		SoluminaUser u=ContextUtil.getUser();
		if(u==null) return false;
		
		TransactionalContext transactionalContext = SoluminaContextHolder.getTransactionalContext();
		return transactionalContext.getUdvScriptQueryCache()!=null;
	}
	public static TransactionQueryCache getCache(){
		SoluminaUser u=ContextUtil.getUser();
		if(u==null) return null;
		
		TransactionalContext transactionalContext = SoluminaContextHolder.getTransactionalContext();
		return transactionalContext.getUdvScriptQueryCache();
	}
	
	public static boolean isCached(String query){
		return getFromCache(query)!=null;
	}
	public static Object getFromCache(String query){
		TransactionQueryCache cache = getCache();
		if(cache==null) return null;
		QueryKey key=new QueryKey(query, null, query);
		return cache.get(key);
	}
	public static void putToCache(String query, Object value){
		TransactionQueryCache cache = getCache();
		if(cache==null) return ;
		QueryKey key=new QueryKey(query, null, query);
		cache.put(key, value);
	}
}
