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
 * This represents the Time property of an Input plugin.
 * A definition can be set in the corresponding swe:Time
 * xml element.
 * 
 * @author Matthes Rieke <m.rieke@uni-muenster.de>
 *
 */
public class Time extends Item {

	private String referenceFrame;
	
	/**
	 * @param prop the property name
	 */
	public Time(String prop) {
		super(prop);
	}

	/**
	 * @return the referenceFrame
	 */
	public String getReferenceFrame() {
		return this.referenceFrame;
	}

	/**
	 * @param referenceFrame the referenceFrame to set
	 */
	public void setReferenceFrame(String referenceFrame) {
		this.referenceFrame = referenceFrame;
	}
	
	

	
}
