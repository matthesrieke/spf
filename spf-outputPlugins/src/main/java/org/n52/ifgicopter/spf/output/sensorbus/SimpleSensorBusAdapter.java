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

import java.util.Map;
import java.util.Map.Entry;

import org.n52.ifgicopter.spf.output.IOutputPlugin;
import org.n52.ifgicopter.spf.xml.Plugin;
import org.n52.sensorbus.sensor.ISensor;

/**
 * Class handling the data output to a SensorBus instance.
 * 
 * @author Matthes Rieke <m.rieke@uni-muenster.de>
 * 
 */
public class SimpleSensorBusAdapter extends AbstractSensorBusAdapter {

    /**
     * @param par
     *        the parent as an instance of {@link IOutputPlugin}
     */
    public SimpleSensorBusAdapter(SensorBusPlugin par) {
        super(par);
    }

    @Override
    public void processData(Map<Long, Map<String, Object>> map, Plugin plugin) {
        synchronized (this) {
            if ( !this.pluginDescriptions.containsKey(plugin)) {
                registerAtSensorBus(plugin);
            }
        }

        ISensor sensor = this.pluginDescriptions.get(plugin);

        for (Entry<Long, Map<String, Object>> entry : map.entrySet()) {
            sendToSensorBus(entry.getKey(), entry.getValue(), sensor);
        }

    }

    @Override
    public void processSingleData(Map<String, Object> data, Long timestamp, Plugin plugin) {
        synchronized (this) {
            if ( !this.pluginDescriptions.containsKey(plugin)) {
                registerAtSensorBus(plugin);
            }
        }

        sendToSensorBus(timestamp, data, this.pluginDescriptions.get(plugin));
    }

    private synchronized void registerAtSensorBus(Plugin plugin) {
        ISensor sensor = new SPFSensor(plugin, false);

        try {
            this.commiter.introduce(sensor);
        }
        catch (Exception e) {
            this.log.warn(e.getMessage(), e);
            this.parent.errorOccured(e.getMessage());
        }

        this.pluginDescriptions.put(plugin, sensor);
    }

    private synchronized void sendToSensorBus(Long timestamp, Map<String, Object> map, ISensor sensor) {
        if (this.commiter.isClosed()) {
            this.parent.errorOccured("No connection to Sensor Bus.");
        }

        try {
            for (String prop : map.keySet()) {
                this.commiter.sendData(sensor, map.get(prop), prop, timestamp.longValue());
            }
        }
        catch (Exception e) {
            this.log.warn(e.getMessage(), e);
            this.parent.errorOccured(e.getMessage());
        }
    }

}
