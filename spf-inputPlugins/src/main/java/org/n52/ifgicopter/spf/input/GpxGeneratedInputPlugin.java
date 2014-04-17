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

import java.util.Arrays;
import java.util.Map;
import java.util.Random;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.topografix.gpx.x1.x1.WptType;



/**
 * 
 * Waypoints-based feeding of positional data with generated values.
 * 
 * @author Daniel Nüst (d.nuest@52north.org)
 * 
 */
public class GpxGeneratedInputPlugin extends GpxInputPlugin {

    protected static Log log = LogFactory.getLog(GpxGeneratedInputPlugin.class);

    private static final int[] RANDOM_INT_RANGE = new int[] { -10, 20};

    private Random rand = new Random(5585l);

    private int value2Pointer = 0;

    private double[] values2 = new double[] {100.0d,
                                             110.0d,
                                             120.0d,
                                             130.0d,
                                             140.0d,
                                             150.0d,
                                             160.0d,
                                             170.0d,
                                             180.0d,
                                             190.0d,
                                             200.0d,
                                             200.0d,
                                             200.0d,
                                             200.0d,
                                             300.0d,
                                             510.0d,
                                             550.0d,
                                             150.0d,
                                             160.0d,
                                             180.0d,
                                             500.0d,
                                             530.0d,
                                             100.0d,
                                             120.0d,
                                             140.0d,
                                             140.0d,
                                             10130.0d, // fake error value
                                             130.0d,
                                             120.0d,
                                             120.0d,
                                             110.0d,
                                             110.0d};

    /**
     * 
     * @param configFile
     */
    @ConstructorParameters({"configuration file"})
    public GpxGeneratedInputPlugin(String configFile) {
        super(configFile);
    }
    
    /**
     * 
     * @param configFile
     * @param inputFile
     */
    @ConstructorParameters({"configuration file", "stored GPX path"})
    public GpxGeneratedInputPlugin(String configFile, String inputFile) {
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

        int value1 = GpxUtil.randomNumber(this.rand, RANDOM_INT_RANGE[0], RANDOM_INT_RANGE[1]);
        // a little bit randomization
        double value2 = this.values2[this.value2Pointer++ % this.values2.length]
                * (this.rand.nextDouble() * 0.2d + 0.9d);
        value2 = GpxUtil.roundToDecimals(value2, 3);

        newDataSet.put("temperature", Integer.valueOf(value1));
        // newDataSet.put("humidity", Double.valueOf(value2));
        newDataSet.put("pollutant", Double.valueOf(value2));

        log.debug("SENDING DATA: " + Arrays.deepToString(newDataSet.entrySet().toArray()));
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

        this.pluginName = "GPX Generated Input";
    }

}
