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


/**
 * XML representation of the item element.
 * 
 * @author Matthes Rieke <m.rieke@uni-muenster.de>
 *
 */
public class Item {

	String property;
	String uom;
	String definition;
	Class<?> dataType;

	/**
	 * This constructor should only be used for default values
	 * (e.g., "time").
	 * 
	 * @param prop the name of the property
	 */
	public Item(String prop) {
		this.property = prop;
		this.dataType = Object.class;
	}

	/**
	 * @return the item property as a String
	 */
	public String getProperty() {
		return this.property;
	}

	/**
	 * @return the used input uom
	 */
	public String getUom() {
		return this.uom;
	}

	/**
	 * @return the used (java) datatype
	 */
	public Class<?> getDataType() {
		return this.dataType;
	}
	

	/**
	 * @return the definition
	 */
	public String getDefinition() {
		return this.definition;
	}

	/**
	 * @param uom the new unit of measurement
	 */
	public void setUom(String uom) {
		this.uom = uom;
	}

	/**
	 * @param dataType the new (java) datatype
	 */
	public void setDataType(Class<?> dataType) {
		this.dataType = dataType;
	}
	
	/**
	 * @param def the definition
	 */
	public void setDefinition(String def) {
		this.definition = def;
	}

	
	
}
