/**
 * ﻿Copyright (C) 2009
 * by 52 North Initiative for Geospatial Open Source Software GmbH
 *
 * Contact: Andreas Wytzisk
 * 52 North Initiative for Geospatial Open Source Software GmbH
 * Martin-Luther-King-Weg 24
 * 48155 Muenster, Germany
 * info@52north.org
 *
 * This program is free software; you can redistribute and/or modify it under
 * the terms of the GNU General Public License version 2 as published by the
 * Free Software Foundation.
 *
 * This program is distributed WITHOUT ANY WARRANTY; even without the implied
 * WARRANTY OF MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program (see gnu-gpl v2.txt). If not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA or
 * visit the Free Software Foundation web page, http://www.fsf.org.
 */

package org.n52.ifgicopter.spf.xml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringWriter;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.sax.SAXSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlObject;
import org.n52.ifgicopter.spf.SPFRegistry;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;


/**
 * Helper class for XML functions
 * 
 * @author Matthes Rieke <m.rieke@uni-muenster.de>
 *
 */
public class XMLTools {
	
	static final Log log = LogFactory.getLog(XMLTools.class);
	
	/**
	 * @param f the xml file
	 * @return the parsed {@link Document}
	 */
	public static Document parseDocument(File f) {
		DocumentBuilderFactory fac = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = null;
		try {
			builder = fac.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			log.warn(null, e);
			return null;
		}

		//get the config as Document
		Document doc = null;
		try {
			doc = builder.parse(f);
		} catch (SAXException e) {
			log.warn(null, e);
			return null;
		} catch (IOException e) {
			log.warn(null, e);
			return null;
		}
		
		/*
		 * do we validate?
		 */
		if (!Boolean.parseBoolean(SPFRegistry.getInstance().getConfigProperty(SPFRegistry.VALIDATE_XML_PROP))) {
			return doc;
		}
		
		SchemaFactory factory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
		
		Schema schema = null;
		try {
			schema = factory.newSchema();
		} catch (SAXException e) {
			log.warn(null, e);
			return null;
		}
		
		Validator val = schema.newValidator();
		val.setErrorHandler(new ErrorHandler() {
			@Override
			public void warning(SAXParseException exception) throws SAXException {
				log.warn(null, exception);
			}
			@Override
			public void fatalError(SAXParseException exception) throws SAXException {
				log.warn(null, exception);
			}
			@Override
			public void error(SAXParseException exception) throws SAXException {
				warning(exception);
			}
		});
		
		/*
		 * do the validation
		 */
		try {
			val.validate(new SAXSource(new InputSource(new FileInputStream(f))));
		} catch (FileNotFoundException e) {
			log.warn(null, e);
			return null;
		} catch (SAXException e) {
			log.warn(null, e);
			return null;
		} catch (IOException e) {
			log.warn(null, e);
			return null;
		}
		
		return doc;
	}
	
    /**
     * 
     * Serializes the given XML tree to string form, including the standard 
     * XML header and indentation if desired. This method relies on the 
     * serialization API from Apache Xerces, since JAXP has on equivalent.
     *
     * @param xml
     *        The XML tree to serialize.
     * 
     * @param printHeader
     *        True if you want the XML header printed before the XML.
     * 
     * @param printIndents
     *        True if you want pretty-printing - child elements will be 
     *        indented with symmetry.
     * 
     * @return The string representation of the given Node.
     * 
     */
    public static String toString(Node xml, 
                                  boolean printHeader, 
                                  boolean printIndents)
    {
        short type = xml.getNodeType();
        
        if (type == Node.TEXT_NODE)
            return xml.getNodeValue();
        
        //
        // NOTE: This serialization code is not part of JAXP/DOM - it is 
        //       specific to Xerces and creates a Xerces dependency for 
        //       this class.
        //
        XMLSerializer serializer = new XMLSerializer();
        serializer.setNamespaces(true);
        
        OutputFormat formatter = new OutputFormat();        
        formatter.setOmitXMLDeclaration(!printHeader);        
        formatter.setIndenting(printIndents);        
        serializer.setOutputFormat(formatter);
        
        StringWriter writer = new StringWriter();
        serializer.setOutputCharStream(writer);
        
        try
        {
            if (type == Node.DOCUMENT_NODE)
                serializer.serialize((Document)xml);
            
            else
                serializer.serialize((Element)xml);
        }
        
        //
        // we are using a StringWriter, so this "should never happen". the 
        // StringWriter implementation writes to a StringBuffer, so there's 
        // no file I/O that could fail.
        //
        // if it DOES fail, we re-throw with a more serious error, because 
        // this a very common operation.
        //
        catch (IOException error)
        {
            throw new RuntimeException(error.getMessage(), error);
        }
        
        return writer.toString();
    }
    
	/**
	 * Use this method if you have an instance of an abstract
	 * xml element (substitution groups, etc..) with an xsi:type
	 * attribute defining the instance of the abstract element.
	 * It replaces the abstract element with the instance element.
	 * 
	 * @param xobj the abstract element
	 * @param newInstance the new {@link QName} of the instance
	 */
	public static void replaceXsiTypeWithInstance(XmlObject xobj, QName newInstance) {
		XmlCursor cursor = xobj.newCursor();
		cursor.setName(newInstance);
		cursor.removeAttribute(new QName("http://www.w3.org/2001/XMLSchema-instance",
				"type"));
	}

}
