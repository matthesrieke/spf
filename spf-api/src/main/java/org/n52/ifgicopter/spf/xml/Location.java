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
 * Class representing the Location property
 * of a Input plugin. The framework uses
 * this class to determine the field names
 * of a geolocation.
 * 
 * @author Matthes Rieke <m.rieke@uni-muenster.de>
 *
 */
public class Location extends CompoundItem {

	private String firstCoordinateName;
	private String secondCoordinateName;
	private String altitudeName;
	private String referenceFrame;
	private double x;
	private double y;
	private double z;
	private int dimension;

	/**
	 * @param prop the property name for the compounding element
	 */
	public Location(String prop) {
		super(prop);
	}

	/**
	 * @param property the first coordinate name
	 */
	public void setFirstCoordinateName(String property) {
		this.firstCoordinateName = property;
	}

	/**
	 * @return the property name for the second coordinate value.
	 */
	public String getSecondCoordinateName() {
		return this.secondCoordinateName;
	}

	/**
	 * @param secondCoordinateName the second coordinate name
	 */
	public void setSecondCoordinateName(String secondCoordinateName) {
		this.secondCoordinateName = secondCoordinateName;
	}

	/**
	 * @return the property name for the first coordinate value
	 */
	public String getFirstCoordinateName() {
		return this.firstCoordinateName;
	}

	/**
	 * @return the altitude name (optional)
	 */
	public String getAltitudeName() {
		return this.altitudeName;
	}

	/**
	 * @param altitudeName the altitude name (optional)
	 */
	public void setAltitudeName(String altitudeName) {
		this.altitudeName = altitudeName;
	}

	/**
	 * @param referenceFrame the reference frame of this location
	 */
	public void setReferenceFrame(String referenceFrame) {
		this.referenceFrame = referenceFrame;
	}

	/**
	 * @return the reference frame of this location
	 */
	public String getReferenceFrame() {
		return this.referenceFrame;
	}

	/**
	 * @param value static X axis value
	 */
	public void setX(double value) {
		this.x = value;
	}

	/**
	 * @param value static Y axis value
	 */
	public void setY(double value) {
		this.y = value;
	}

	/**
	 * @param value static Z axis value
	 */
	public void setZ(double value) {
		this.z = value;
	}

	/**
	 * @return the static X axis value
	 */
	public double getX() {
		return this.x;
	}

	/**
	 * @return the static Y axis value
	 */
	public double getY() {
		return this.y;
	}

	/**
	 * @return the static Z axis value
	 */
	public double getZ() {
		return this.z;
	}

	/**
	 * @param i the dimension
	 */
	public void setDimension(int i) {
		this.dimension = i;
	}

	/**
	 * @return the dimension
	 */
	public int getDimension() {
		return this.dimension;
	}

	/**
	 * e.g., calling setAxis("x", 12.0) behaves exactly
	 * the same as setX(12.0).
	 * 
	 * @param string the axis (x, y or z)
	 * @param value the value
	 */
	public void setAxis(String string, double value) {
		if (string.equalsIgnoreCase("x")) {
			this.setX(value);
		}
		else if (string.equalsIgnoreCase("y")) {
			this.setY(value);
		}
		else if (string.equalsIgnoreCase("z")) {
			this.setZ(value);
		}
	}
	

}
