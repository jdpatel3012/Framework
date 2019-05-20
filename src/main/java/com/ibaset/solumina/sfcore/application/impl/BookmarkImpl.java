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
package com.ibaset.solumina.sfcore.application.impl;

import java.io.*;
import javax.xml.parsers.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.*;
import javax.xml.transform.stream.*;
import org.w3c.dom.*;

import com.ibaset.solumina.sfcore.application.IBookmark;

public class BookmarkImpl implements IBookmark
{

    public String convert3DViaViewToBookmark(String viewName)
    {
        String localString = null;
        try
        {
            DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
            Document doc = docBuilder.newDocument();
            Element root = doc.createElement("BOOKMARK");
            doc.appendChild(root);
            Element child = doc.createElement("BookmarkVersion");
            child.appendChild(doc.createTextNode("VIA1.0"));
            root.appendChild(child);
            child = doc.createElement("VIEW");
            child.appendChild(doc.createTextNode(viewName));
            root.appendChild(child);

            TransformerFactory transfac = TransformerFactory.newInstance();
            Transformer trans = transfac.newTransformer();
            trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            trans.setOutputProperty(OutputKeys.INDENT, "yes");

            StringWriter sw = new StringWriter();
            StreamResult result = new StreamResult(sw);
            DOMSource source = new DOMSource(doc);
            trans.transform(source, result);
            String xmlString = sw.toString();
            localString = xmlString;
        }
        catch (ParserConfigurationException pe)
        {
            localString = "";
        }
        catch (TransformerConfigurationException pe)
        {
            localString = "";
        }
        catch (TransformerException pe)
        {
            localString = "";
        }
        return localString;
    }

}
