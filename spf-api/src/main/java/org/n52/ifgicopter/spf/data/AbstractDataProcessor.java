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

package org.n52.ifgicopter.spf.data;

import java.util.Map;

import org.n52.ifgicopter.spf.xml.Plugin;


/**
 * Abstract class for all data processors.
 * One instances take a KVP of data plus
 * the plugin descriptions and
 * processes it.
 * This for example could be a UOM converter
 * or a SensorML process parser/implementation.
 * 
 * @author Matthes Rieke <m.rieke@uni-muenster.de>
 *
 */
public abstract class AbstractDataProcessor {
	
	protected Plugin plugin;

	/**
	 * Default constructor providing a {@link Plugin} instance.
	 * 
	 * @param plugin the plugin
	 */
	public AbstractDataProcessor(Plugin plugin) {
		this.plugin = plugin;
	}

	/**
	 * The processing method.
	 * 
	 * @param data the incoming data
	 * @return the processed data
	 */
	public abstract Map<String, Object> processData(Map<String, Object> data);

}
