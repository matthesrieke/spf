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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.joda.time.DateTime;
import org.n52.ifgicopter.spf.output.IOutputPlugin;
import org.n52.ifgicopter.spf.xml.Plugin;
import org.n52.sensorbus.sensor.ISensor;
import org.n52.sensorbus.util.AbstractSensorBusMessage;
import org.n52.sensorbus.util.DataTupleMessage;

/**
 * Class handling the data output to a SensorBus instance using the extended protocol with PublishTuple
 * messages.
 * 
 * @author Matthes Rieke <m.rieke@uni-muenster.de>
 * 
 */
public class ExtendedSensorBusAdapter extends AbstractSensorBusAdapter {

    /**
     * @param par
     *        the parent as an instance of {@link IOutputPlugin}
     */
    public ExtendedSensorBusAdapter(SensorBusPlugin par) {
        super(par);
    }

    @Override
    public void processData(Map<Long, Map<String, Object>> map, Plugin plugin) {
        if ( !this.pluginDescriptions.containsKey(plugin)) {
            registerAtSensorBus(plugin);
        }

        ISensor sensor = this.pluginDescriptions.get(plugin);

        for (Entry<Long, Map<String, Object>> entry : map.entrySet()) {
            try {
                sendToSensorBus(entry.getKey(), entry.getValue(), sensor);
            }
            catch (IllegalStateException e) {
                this.parent.errorOccured(e.getMessage());
            }
        }

    }

    @Override
    public void processSingleData(Map<String, Object> data, Long timestamp, Plugin plugin) {
        if ( !this.pluginDescriptions.containsKey(plugin)) {
            registerAtSensorBus(plugin);
        }

        sendToSensorBus(timestamp, data, this.pluginDescriptions.get(plugin));
    }

    private void registerAtSensorBus(Plugin plugin) {
        ISensor sensor = new SPFSensor(plugin, true);

        try {
            this.commiter.introduce(sensor);
        }
        catch (Exception e) {
            this.log.warn(e.getMessage(), e);
            this.parent.errorOccured(e.getMessage());
        }

        this.pluginDescriptions.put(plugin, sensor);
    }

    private void sendToSensorBus(Long timestamp, Map<String, Object> map, ISensor sensor) {
        if (this.commiter.isClosed()) {
            this.parent.errorOccured("No connection to Sensor Bus.");
            return;
        }
        try {
            this.commiter.sendMessage(createSensorBusMessage(sensor, timestamp, map).createMessage());
        }
        catch (Exception e) {
            this.log.warn(e.getMessage(), e);
            this.parent.errorOccured(e.getMessage());
        }
    }

    private AbstractSensorBusMessage createSensorBusMessage(ISensor sensor, Long timestamp, Map<String, Object> map) {

        SPFSensor spfs = (SPFSensor) sensor;

        List<String> observedProperties = new ArrayList<String>();
        ArrayList<Object> observedValues = new ArrayList<Object>();

        String[] locProps = spfs.getLocationProperties();

        for (Entry<String, Object> entry : map.entrySet()) {
            String property = entry.getKey();
            if (locProps != null) {
                /*
                 * its mobile
                 */
                if (property.equals(locProps[0]) || property.equals(locProps[1])
                        || (locProps.length == 3 && property.equals(locProps[2]))) {
                    continue;
                }
            }

            Object value = entry.getValue();
            if (value == null)
                continue;

            observedProperties.add(property);
            observedValues.add(value);
        }

        String[] location = null;

        if (locProps != null && map.containsKey(locProps[0]) && map.containsKey(locProps[1])) {
            /*
             * we have a dynamic location defined
             */
            location = new String[locProps.length];
            location[0] = map.get(locProps[0]).toString();
            location[1] = map.get(locProps[1]).toString();

            /*
             * we have a height property
             */
            if (location.length == 3) {
                if (map.containsKey(locProps[2])) {
                    location[2] = map.get(locProps[2]).toString();
                }
            }

        }

        return new DataTupleMessage(sensor.getSensorDescription().getSensorID(),
                                    new DateTime(timestamp).toString(),
                                    observedProperties,
                                    observedValues,
                                    null,
                                    location);
    }

}
