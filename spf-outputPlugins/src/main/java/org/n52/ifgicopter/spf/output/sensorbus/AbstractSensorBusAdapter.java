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

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.n52.ifgicopter.spf.output.IOutputPlugin;
import org.n52.ifgicopter.spf.xml.Plugin;
import org.n52.sensorbus.comm.AbstractDataCommitter;
import org.n52.sensorbus.comm.xmpp.chat.XMPPMUCConfiguration;
import org.n52.sensorbus.comm.xmpp.chat.XMPPMUCDataCommitter;
import org.n52.sensorbus.sensor.ISensor;


/**
 * Abstract class of a SensorAdapter for the SPFramework.
 * 
 * @author Matthes Rieke <m.rieke@uni-muenster.de>
 *
 */
public abstract class AbstractSensorBusAdapter {
	
	protected final Log log = LogFactory.getLog(AbstractSensorBusAdapter.class);
	protected Map<Plugin, ISensor> pluginDescriptions = new HashMap<Plugin, ISensor>();
	private XMPPMUCConfiguration config;
	protected AbstractDataCommitter commiter;
	protected SensorBusPlugin parent;
	
	/**
	 * @param parent the parent as an instance of {@link IOutputPlugin}
	 */
	public AbstractSensorBusAdapter(SensorBusPlugin parent) {
		this.parent = parent;
	}
	
	/**
	 * @throws Exception if initilisation fails
	 */
	public void init() throws Exception {
		if (this.log.isInfoEnabled()) {
			this.log.info("Initialising "+ this.getClass().getCanonicalName());
		}

		this.config = new XMPPMUCConfiguration("config/sensorbus/xmpp-config.properties");
		this.commiter = new XMPPMUCDataCommitter(this.config);

		this.commiter.connect();
	}

	/**
	 * @throws Exception if shutdown fails
	 */
	public void shutdown() throws Exception {
		this.log.info("shutting down " +this.getClass().getSimpleName());

		for (ISensor sensor : this.pluginDescriptions.values()) {
			this.commiter.disconnect(sensor);
		}

		this.commiter.close();
	}

	/**
	 * abstract method for processing multiple data tuples.
	 * 
	 * @param data the data tuples.
	 * @param plugin the input plugin
	 */
	public abstract void processData(Map<Long, Map<String, Object>> data, Plugin plugin);

	/**
	 * abstract method for processing one data tuple.
	 * 
	 * @param data one data tuple
	 * @param timestamp the timestamp as unix time
	 * @param plugin the input plugin which generated the output
	 */
	public abstract void processSingleData(Map<String, Object> data, Long timestamp,
			Plugin plugin);

}
