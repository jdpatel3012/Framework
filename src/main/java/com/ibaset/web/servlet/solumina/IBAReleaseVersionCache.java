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
package com.ibaset.web.servlet.solumina;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class IBAReleaseVersionCache {
    // Shared resource that needs protection
    private Map<String, String> cache;

    // Single instance kept
    private static IBAReleaseVersionCache instance = new IBAReleaseVersionCache();

    // Access method
    public static IBAReleaseVersionCache getInstance() {
        return instance;
    }

    // Private constructor to prevent instantiation
    private IBAReleaseVersionCache() {
        cache = new ConcurrentHashMap<String, String>();
    }

    public void put(String key, String value) {
        cache.put(key, value);
    }

    public String get(String key) {
        return cache.get(key);
    }
}