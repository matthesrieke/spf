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
/**
 * 
 */

package org.n52.ifgicopter.spf.input;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.topografix.gpx.x1.x1.WptType;

/**
 * 
 * Waypoints-based feeding of positional data with data values read from a comma seperated list from waypoint
 * comments.
 * 
 * @author Daniel Nüst (d.nuest@52north.org)
 * 
 */
public class GpxDataInputPlugin extends GpxInputPlugin {

    protected static Log log = LogFactory.getLog(GpxDataInputPlugin.class);

    /**
     * 
     * @param configFile
     */
    @ConstructorParameters({"configuration file"})
    public GpxDataInputPlugin(String configFile) {
        super(configFile);
    }

    /**
     * 
     * @param configFile
     * @param inputFile
     */
    @ConstructorParameters({"configuration file", "stored GPX path"})
    public GpxDataInputPlugin(String configFile, String inputFile) {
        super(configFile);
        setGpxFilePath(inputFile);
    }

    /**
     * @param currentWaypoint
     * @return
     */
    @Override
    protected Map<String, Object> getDataSet(WptType currentWaypoint) {
        Map<String, Object> newDataSet = getTimeAndCoordinateDataSet(currentWaypoint);

        Map<String, Object> newDataSetDecoded = GpxUtil.decodeDataString(currentWaypoint.getDesc());
        newDataSet.putAll(newDataSetDecoded);

        return newDataSet;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.n52.ifgicopter.spf.common.IModule#init()
     */
    @Override
    public void init() throws Exception {
        super.init();

        this.pluginName = "GPX Data Input";
    }

}
