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
package com.ibaset.common.util;

import java.util.Comparator;
import java.util.Map;

/**
 * Compares ListOrderedMaps of rows from the SFPL_PLAN_NODE table using the
 * PLAN_NO.
 * 
 * @author rebecca
 */
public class PlanNodeComparator implements Comparator
{
    /*
     * (non-Javadoc)
     * 
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    public int compare(Object lhsObject, Object rhsObject)
    {
        boolean lhsObjectIsMap = lhsObject instanceof java.util.Map;
        boolean rhsObjectIsMap = rhsObject instanceof java.util.Map;

        if (!lhsObjectIsMap || !rhsObjectIsMap)
        {
            throw new IllegalArgumentException("Both arguments for the compare method must be of type java.util.Map");
        }

        Map lhsMap = (Map) lhsObject;
        Map rhsMap = (Map) rhsObject;

        String lhsNodeNumber = (String) lhsMap.get("NODE_NO");
        String rhsNodeNumber = (String) rhsMap.get("NODE_NO");

        return lhsNodeNumber.compareTo(rhsNodeNumber);
    }

}