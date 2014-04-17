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
 * POJO for the metadata of an input plugin.
 * 
 * @author Matthes Rieke <m.rieke@uni-muenster.de>
 *
 */
public class PluginMetadata {
	
	private String name = "";
	private String uniqueID = "";
	private String contactEmail = "";
	private boolean mobile = false;
	private String position = "";
	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}
	/**
	 * @param uniqueID the uniqueID to set
	 */
	public void setUniqueID(String uniqueID) {
		this.uniqueID = uniqueID;
	}
	/**
	 * @param contactEmail the contactEmail to set
	 */
	public void setContactEmail(String contactEmail) {
		this.contactEmail = contactEmail;
	}
	/**
	 * @param mobile the mobile to set
	 */
	public void setMobile(boolean mobile) {
		this.mobile = mobile;
	}
	/**
	 * @param position the position to set
	 */
	public void setPosition(String position) {
		this.position = position;
	}
	/**
	 * @return the name
	 */
	public String getName() {
		return this.name;
	}
	/**
	 * @return the uniqueID
	 */
	public String getUniqueID() {
		return this.uniqueID;
	}
	/**
	 * @return the contactEmail
	 */
	public String getContactEmail() {
		return this.contactEmail;
	}
	/**
	 * @return the mobile
	 */
	public boolean isMobile() {
		return this.mobile;
	}
	/**
	 * @return the position
	 */
	public String getPosition() {
		return this.position;
	}
	
	

}
