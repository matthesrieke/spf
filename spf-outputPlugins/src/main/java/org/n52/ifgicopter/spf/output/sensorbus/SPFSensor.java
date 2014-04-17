/**
 * ﻿Copyright (C) 2012
 * by 52 North Initiative for Geospatial Open Source Software GmbH
 *
 * Contact: Andreas Wytzisk
 * 52 North Initiative for Geospatial Open Source Software GmbH
 * Martin-Luther-King-Weg 24
 * 48155 Muenster, Germany
 * info@52north.org
 *
 * This program is free software; you can redistribute and/or modify it under
 * the terms of the GNU General Public License serviceVersion 2 as published by the
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

package org.n52.ifgicopter.spf.output.sensorbus;

import org.n52.ifgicopter.spf.xml.Item;
import org.n52.ifgicopter.spf.xml.Plugin;
import org.n52.sensorbus.sensor.ISensor;
import org.n52.sensorbus.sensor.SensorDescription;
import org.n52.sensorbus.util.MessageFormat;


/**
 * SPF specific implementation of {@link ISensor}.
 * 
 * @author Matthes Rieke <m.rieke@uni-muenster.de>
 *
 */
public class SPFSensor implements ISensor {


	private SensorDescription sensorDesc;
	private String[] locationProperties;

	/**
	 * @param plugin the underlying plugin
	 * @param usingTupleMessages if this {@link SPFSensor} uses the extended
	 * protocol (PublishTuple) to send data
	 */
	public SPFSensor(Plugin plugin, boolean usingTupleMessages) {
		String[] observedProps = new String[plugin.getInputProperties().size() -1];
		String[] uomProps = new String[plugin.getInputProperties().size() -1];

		int i = 0;
		Item item;
		for (String string : plugin.getInputProperties()) {
			if (string.equals(plugin.getTime().getProperty())) continue;
			observedProps[i] = string;

			item = plugin.getItem(string);
			if (item == null) {
				/*
				 * must be a compound
				 */
				item = plugin.getLeafItemOfCompound(string, null);
			}

			uomProps[i++] = item.getUom();
		}
		
		String[] propNames = null;
		String description;
		if (plugin.isMobile()) {
			/*
			 * use the custom fields to define
			 * the dynamic position properties
			 * (only for standard protocol)
			 * 
			 * also the locationProperties are
			 * used by the extended protocol
			 */
			description = plugin.getName() +" (mobile platform) connected via SPFramework";
			
			/*
			 * the axis ordering is treated like the coordinates
			 * of the swe:Position were defined
			 */
			if (plugin.getLocation().getAltitudeName() != null) {
				this.locationProperties = new String[3];
				propNames = new String[3];
				propNames[2] = MessageFormat.LOCATION_IDENTIFIERS[2];
				this.locationProperties[2] = plugin.getLocation().getAltitudeName();
			}
			else {
				this.locationProperties = new String[2];
				propNames = new String[2];
			}

			this.locationProperties[0] = plugin.getLocation().getFirstCoordinateName();
			this.locationProperties[1] = plugin.getLocation().getSecondCoordinateName();
			propNames[0] = MessageFormat.LOCATION_IDENTIFIERS[0];
			propNames[1] = MessageFormat.LOCATION_IDENTIFIERS[1];
		}
		else {
			description = plugin.getName() +" (stationary platform) connected via SPFramework";
		}


		if (!usingTupleMessages) {
			/*
			 * standard protocol
			 */
				
				this.sensorDesc = new SensorDescription(plugin.getName(),
						description,
						plugin.getName(),
						propNames,
						this.locationProperties,
						plugin.getLocation().getReferenceFrame(),
						plugin.getLocation().getY(),
						plugin.getLocation().getX(),
						plugin.getLocation().getZ(),
						observedProps,
						uomProps,
						"variable",
				"ms");	
			
		}
		else {
			/*
			 * extended protocol
			 */
			this.sensorDesc = new SensorDescription(plugin.getName(),
					description,
					plugin.getName(),
					null,
					null,
					plugin.getLocation().getReferenceFrame(),
					plugin.getLocation().getY(),
					plugin.getLocation().getX(),
					plugin.getLocation().getZ(),
					observedProps,
					uomProps,
					"variable",
			"ms");
		}
	}

	@Override
	public void start() {
	    //
	}

	@Override
	public void stop() {
	    //
	}

	@Override
	public SensorDescription getSensorDescription() {
		return this.sensorDesc;
	}

	/**
	 * @return the locationProperties array.
	 */
	public String[] getLocationProperties() {
		return this.locationProperties;
	}

}
