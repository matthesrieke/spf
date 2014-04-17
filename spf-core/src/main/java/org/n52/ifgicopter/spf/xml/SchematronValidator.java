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
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.LocatorImpl;


import net.sf.saxon.Transform;

/**
 * This class validates an XML instance against
 * a schematron XML schema profile.
 * 
 * @author Matthes Rieke <m.rieke@uni-muenster.de>
 *
 */
public class SchematronValidator {

	private static final Log log = LogFactory.getLog(SchematronValidator.class);

	private String schematronFile;
	private String xmlInstanceFile;
	List<String> assertFails = new ArrayList<String>();
	private static final String DOCS_FOLDER = "config/spf/docs";

	/**
	 * Constuctor creating a validator for SPF schematron schema.
	 * @param xmlInstanceFile the xml instance file name
	 */
	public SchematronValidator(String xmlInstanceFile) {
		this(DOCS_FOLDER+"/spf_SensorML_profile.sch", xmlInstanceFile);
	}

	/**
	 * @param schematronFile the schematron schema file name
	 * @param xmlInstanceFile the xml instance file name
	 */
	public SchematronValidator(String schematronFile, String xmlInstanceFile) {
		this.schematronFile = schematronFile;
		this.xmlInstanceFile = xmlInstanceFile;
	}

	/**
	 * @param xmlObject an {@link XmlObject}
	 */
	public SchematronValidator(XmlObject xmlObject) {
		File tmp = new File("" + DOCS_FOLDER+ "/tmp/tmp_instance.xml");
		if (!tmp.exists()) {
			try {
				tmp.createNewFile();
			} catch (IOException e) {
				log.warn(e.getMessage(), e);
			}
		}

		try {
			XmlOptions opts = new XmlOptions();
			opts.setSaveUseOpenFrag();
			xmlObject.save(tmp, opts);
		} catch (IOException e) {
			log.warn(e.getMessage(), e);
		}

		this.schematronFile = DOCS_FOLDER+"/spf_SensorML_profile.sch";
		this.xmlInstanceFile = DOCS_FOLDER+ "/tmp/tmp_instance.xml";
	}


	/**
	 * constructor for spf plugin validation
	 * 
	 * @param sb a stringbuilder object
	 */
	public SchematronValidator(StringBuilder sb) {
		File tmp = new File(DOCS_FOLDER+ "/tmp/tmp_instance.xml");
		if (!tmp.exists()) {
			try {
				tmp.createNewFile();
			} catch (IOException e) {
				log.warn(e.getMessage(), e);
			}
		}

		try {
			FileWriter fw = new FileWriter(tmp);
			fw.write(sb.toString());
			fw.flush();
			fw.close();

		} catch (IOException e) {
			log.warn(e.getMessage(), e);
		}

		this.schematronFile = DOCS_FOLDER+"/spf_SensorML_profile.sch";
		this.xmlInstanceFile = DOCS_FOLDER+ "/tmp/tmp_instance.xml";
	}

	/**
	 * Here the real validation process is started.
	 * 
	 * @return true if no schematron rule was violated.
	 */
	public boolean validate() {
		Transform trans = new Transform();

		/*
		 * transform the schematron
		 */
		String[] arguments = new String[] {"-x", "org.apache.xerces.parsers.SAXParser", "-w1", "-o",
				DOCS_FOLDER+"/tmp/tmp.xsl", this.schematronFile, DOCS_FOLDER+"/iso_svrl_for_xslt2.xsl",  "generate-paths=yes"
		};
		trans.doTransform(arguments, "java net.sf.saxon.Transform");

		/*
		 * transform the instance
		 */
		String report = DOCS_FOLDER+"/tmp/"+ this.xmlInstanceFile.substring(this.xmlInstanceFile.lastIndexOf("/") + 1) +".report.xml";
		arguments = new String[] {"-x", "org.apache.xerces.parsers.SAXParser", "-w1", "-o",
				report, this.xmlInstanceFile, DOCS_FOLDER+"/tmp/tmp.xsl"};
		trans.doTransform(arguments, "java net.sf.saxon.Transform");


		LocatorImpl locator = new LocatorImpl();
		/*
		 * an extension of DefaultHandler
		 */
		DefaultHandler handler = new DefaultHandler() {

			private String failTmp;
			private Locator locator2;
			private boolean insideFail = false;

			@Override
			public void startElement(String uri, String localName,
					String qName, Attributes attributes)
			throws SAXException {
				if (qName.endsWith("failed-assert")) {
					this.failTmp = "Assertion error at \"" +attributes.getValue("test")+ "\" (line "+ 
					this.locator2.getLineNumber() +"): ";
					this.insideFail = true;
				}
			}

			@Override
			public void endElement(String uri, String localName,
					String qName) throws SAXException {
				if (qName.endsWith("failed-assert")) {
					SchematronValidator.this.assertFails.add(this.failTmp);
					this.failTmp = null;
					this.insideFail = false;
				}
			}

			@Override
			public void characters(char[] ch, int start, int length)
			throws SAXException {
				if (this.insideFail) {
					this.failTmp += new String(ch, start, length).trim();
				}
			}

			@Override
			public void setDocumentLocator(Locator l) {
				this.locator2 = l;
			}

		};
		handler.setDocumentLocator(locator);

		try {
			SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
			parser.parse(new File(report), handler);
		} catch (SAXException e) {
			log.warn(e.getMessage(), e);
		} catch (IOException e) {
			log.warn(e.getMessage(), e);
		} catch (ParserConfigurationException e) {
			log.warn(e.getMessage(), e);
		}

		return (this.assertFails.size() == 0) ? true : false;
	}


	/**
	 * @return a list of assertion errors
	 */
	public List<String> getAssertFails() {
		return this.assertFails;
	}

	/**
	 * @param args unused
	 */
	public static void main(String[] args) {
		SchematronValidator validator = new SchematronValidator("SensorML_dummy.xml");
		if (!validator.validate()) {
			for (String string : validator.getAssertFails()) {
				log.info(string);
			}
		}
	}

}
