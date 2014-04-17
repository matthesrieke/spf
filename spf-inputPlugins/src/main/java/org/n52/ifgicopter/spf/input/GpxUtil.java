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

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * @author Daniel Nüst (d.nuest@52north.org)
 * 
 */
public abstract class GpxUtil {

    private static Log log = LogFactory.getLog(GpxUtil.class);

    private static final String KEY_VALUE_DELIM = "=";

    private static final String KVP_DELIM = ";";

    /**
     * 
     * @param data
     * @return
     */
    public static Map<String, Object> decodeDataString(String data) {
        Map<String, Object> newData = new HashMap<String, Object>();
        if (data == null)
            return newData;

        String[] dataArray = data.split(KVP_DELIM);
        for (String kvp : dataArray) {
            try {
                String[] split = kvp.split(KEY_VALUE_DELIM);

                if (split.length < 2) {
                    if (log.isDebugEnabled())
                        log.debug("Could not decode string " + kvp);
                    continue;
                }

                String key = split[0];
                String valueString = split[1];

                Object value = getValue(valueString);

                newData.put(key, value);
            }
            catch (Exception e) {
                log.warn("Could not decode string " + kvp);
            }
        }

        return newData;
    }

    /**
     * 
     * @param data
     * @return
     */
    public static String encodeDataString(Map<String, Object> data) {
        StringBuilder values = new StringBuilder();
        for (Entry<String, Object> entry : data.entrySet()) {
            values.append(entry.getKey());
            values.append(KEY_VALUE_DELIM);
            values.append(entry.getValue().toString());
            values.append(KVP_DELIM);
        }

        return values.toString();
    }

    /**
     * 
     * @param valueString
     * @return
     */
    private static Object getValue(String valueString) {
        try {
            double d = Double.parseDouble(valueString);
            return Double.valueOf(d);
        }
        catch (NumberFormatException e) {
            // try something else
        }

        try {
            int i = Integer.parseInt(valueString);
            return Integer.valueOf(i);
        }
        catch (NumberFormatException e) {
            // try something else
        }

        return valueString;
    }
    
    
    
    /**
     * @param rand  can be null, then a new {@link Random} is used.
     * @param min
     * @param max
     * @return
     */
    public static int randomNumber(Random rand, int min, int max) {
        Random r = null;
        if (rand == null) {
            r = new Random();
        }
        else
            r = rand;

        return min + r.nextInt(max - min);
    }
    
    /**
     * @param d
     * @param numberOfDecimalPlaces
     * @return
     */
    public static double roundToDecimals(double d, int numberOfDecimalPlaces) {
        int temp = (int) ( (d * Math.pow(10, numberOfDecimalPlaces)));
        return (temp / Math.pow(10, numberOfDecimalPlaces));
    }

}
