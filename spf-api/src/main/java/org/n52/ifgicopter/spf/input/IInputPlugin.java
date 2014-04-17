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

package org.n52.ifgicopter.spf.input;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.n52.ifgicopter.spf.common.IModule;
import org.n52.ifgicopter.spf.gui.ModuleGUI;
import org.n52.ifgicopter.spf.output.IOutputPlugin;


/**
 * @author Matthes Rieke <m.rieke@uni-muenster.de>
 *
 */
public interface IInputPlugin extends IModule {
	
	
	/**
	 * @return the configuration file of the plugin.
	 */
	public InputStream getConfigFile();
	
	/**
	 * this is called in cycle by the framework core.
	 * if it returns true the {@link #getNewData()} method is called.
	 * 
	 * @return true if new data is available in the outputlist
	 */
	public boolean hasNewData();
	
	/**
	 * this method returns the new available data sets of the input plugin.
	 * 
	 * @return new data as a list of maps
	 */
	public List<Map<String, Object>> getNewData();
	
	/**
	 * @return the current status of this plugin. See {@link IModule#STATUS_RUNNING} and {@link IModule#STATUS_NOT_RUNNING}.
	 */
	public int getStatus();
	
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
	 * If this method returns a JPanel not equal null
	 * the Panel is then rendered in the framework gui.
	 * 
	 * @return the user interface for this input plugin.
	 */
	public ModuleGUI getUserInterface();
	
}
