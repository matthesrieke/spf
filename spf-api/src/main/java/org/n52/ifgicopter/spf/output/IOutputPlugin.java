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

package org.n52.ifgicopter.spf.output;

import java.util.Map;

import org.n52.ifgicopter.spf.common.IModule;
import org.n52.ifgicopter.spf.gui.ModuleGUI;
import org.n52.ifgicopter.spf.input.IInputPlugin;
import org.n52.ifgicopter.spf.xml.Plugin;


/**
 * 
 * Interface for a IOutputPlugin. a Plugin waits for
 * new data obtained by the {@link SPFEngine} and works with
 * the new data.
 * 
 * @author Matthes Rieke <m.rieke@uni-muenster.de>
 * 
 */
public interface IOutputPlugin extends IModule {
	

	/**
	 * The map contains one mandatory item: the time property of the {@link IInputPlugin}.
	 * All other attributes are optional and hold
	 * the measurements of the Sensor
	 * 
	 * @param data a map containing the values of a measurement.
	 * @param plugin the plugin which has generated the output
	 * @return integer representing the status
	 */
	public abstract int processData(Map<Long, Map<String, Object>> data, Plugin plugin);

	/**
	 * The map contains one mandatory item: the time property of the {@link IInputPlugin}.
	 * All other attributes are optional and hold
	 * the measurements of the Sensor
	 * 
	 * @param data a map containing the values of a measurement.
	 * @param timestamp the time of measurement
	 * @param plugin the plugin which has generated the output
	 * @return integer representing the status
	 */
	public abstract int processSingleData(Map<String, Object> data, Long timestamp, Plugin plugin);
	
	/**
	 * This method should return a String representing
	 * the current status of the {@link IOutputPlugin}.
	 *
	 * @return the status string 
	 */
	public String getStatusString();
	
	/**
	 * @return the globally used name of this plugin
	 */
	public String getName();
	
	/**
	 * @return the status of the plugin
	 */
	public int getStatus();

	/**
	 * this method might be called sometime (e.g., if metadata
	 * of InputPlugins have changed).
	 * The implementation must reset its state to an init-state.
	 * @throws Exception if restart failed
	 */
	public abstract void restart() throws Exception;
	
	/**
	 * If this method returns a {@link ModuleGUI} not equal null
	 * the Panel is then rendered in the framework gui.
	 * 
	 * @return the user interface for this input plugin.
	 */
	public ModuleGUI getUserInterface();
	

}
