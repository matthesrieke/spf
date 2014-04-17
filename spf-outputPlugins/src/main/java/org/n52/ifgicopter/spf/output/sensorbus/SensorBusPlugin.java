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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.n52.ifgicopter.spf.common.IModule;
import org.n52.ifgicopter.spf.gui.ModuleGUI;
import org.n52.ifgicopter.spf.output.IOutputPlugin;
import org.n52.ifgicopter.spf.xml.Plugin;

/**
 * Wrapper class for the Sensor Bus sensoradapters.
 * 
 * @author Matthes Rieke <m.rieke@uni-muenster.de>
 * 
 */
public class SensorBusPlugin implements IOutputPlugin {

    private final Log log = LogFactory.getLog(SensorBusPlugin.class);
    private AbstractSensorBusAdapter adapter;
    private int status = 1;
    private String errorString;
    private boolean extended;

    /**
     * @param usesExtend
     *        boolean string if the extended protocol should be used
     */
    public SensorBusPlugin(String usesExtend) {
        this.extended = Boolean.parseBoolean(usesExtend);

        if (this.extended) {
            this.adapter = new ExtendedSensorBusAdapter(this);
        }
        else {
            this.adapter = new SimpleSensorBusAdapter(this);
        }
    }

    @Override
    public void init() throws Exception {
        if (this.adapter != null) {
            try {
                this.adapter.init();
            }
            catch (Exception e) {
                this.errorOccured(e.getMessage());
            }
        }
    }

    @Override
    public void shutdown() throws Exception {
        if (this.status != IModule.STATUS_RUNNING) {
            this.log.warn("Plugin not running. Canceling data processing.");
            return;
        }

        if (this.adapter != null) {
            this.adapter.shutdown();
        }
    }

    @Override
    public int processData(Map<Long, Map<String, Object>> data, Plugin plugin) {

        if (this.status != IModule.STATUS_RUNNING) {
            return this.status;
        }

        if (this.adapter != null) {
            this.adapter.processData(data, plugin);
        }

        return this.status;
    }

    @Override
    public int processSingleData(Map<String, Object> data, Long timestamp, Plugin plugin) {

        if (this.status != IModule.STATUS_RUNNING) {
            /*
             * TODO: never get out of STATUS_NOT_RUNNING, stupid...
             */
            return this.status;
        }

        if (this.adapter != null) {
            this.adapter.processSingleData(data, timestamp, plugin);
        }

        return this.status;
    }

    @Override
    public String getStatusString() {
        if (this.status == IModule.STATUS_RUNNING) {
            return "SensorBus OutputPlugin running normally.";
        }
        return this.errorString;
    }

    /**
     * @param message
     *        the error message
     */
    public void errorOccured(String message) {
        this.errorString = message;
        this.status = IModule.STATUS_NOT_RUNNING;
    }

    @Override
    public String getName() {
        return "SensorBusOutputPlugin";
    }

    @Override
    public int getStatus() {
        return this.status;
    }

    @Override
    public void restart() throws Exception {
        this.adapter.shutdown();

        /*
         * just create new objects
         */
        if (this.extended) {
            this.adapter = new ExtendedSensorBusAdapter(this);
        }
        else {
            this.adapter = new SimpleSensorBusAdapter(this);
        }

        this.adapter.init();
    }

    @Override
    public ModuleGUI getUserInterface() {
        return null;
    }

}
