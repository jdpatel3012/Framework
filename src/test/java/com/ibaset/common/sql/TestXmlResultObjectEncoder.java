/**
 * Proprietary and Confidential
 * Copyright 1995-2016 iBASEt, Inc.
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
package com.ibaset.common.sql;

import static org.junit.Assert.assertEquals;
import java.io.IOException;
import java.io.OutputStream;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JUnitParamsRunner.class)
public class TestXmlResultObjectEncoder
{
    
    private XmlResultObjectEncoder xmlResultObjEncoder;
    
    private OutputStream outputStream;
    
    @Before
    public void setUp()
    {
        xmlResultObjEncoder = new XmlResultObjectEncoder();
    }
    
    // GE-5383
    public static Object[] getRegionSpecificStringContainingSpecialChar()
    {
        return new Object[][] { 
                { "€ÄÅéöÉÖ Swedish Characters", "&#8364;&#196;&#197;&#233;&#246;&#201;&#214; Swedish Characters" },
                { "ÆŸÿÜûÔœÏÊêçÇàÂ French Characters", "&#198;&#376;&#255;&#220;&#251;&#212;&#339;&#207;&#202;&#234;&#231;&#199;&#224;&#194; French Characters" },
                { "§ÜüÄäÖöß Germen Characters", "&#167;&#220;&#252;&#196;&#228;&#214;&#246;&#223; Germen Characters" },
                { "~#$^@! English Characters", "~#$^@! English Characters" } };
    }
    
    // GE-5383
    @Test
    @Parameters(method = "getRegionSpecificStringContainingSpecialChar")
    public void testWriteXML(String inputString, String expectedString) throws IOException
    {
        // ARRANGE
        outputStream = new OutputStream()
        
        {
            private StringBuilder string = new StringBuilder();
            
            @Override
            public void write(int x) throws IOException
            {
                this.string.append((char) x);
            }
            
            @Override
            public void write(byte[] c) throws IOException
            {
                this.string.append(new String(c));
            }
            
            public String toString()
            {
                return this.string.toString();
            }
        };
        
        // ACT
        xmlResultObjEncoder.writeXML(outputStream, inputString, null);
        
        // ASSERT
        
        String outputString = outputStream.toString();
        
        assertEquals(expectedString, outputString);
    }
}
