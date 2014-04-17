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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;

import org.n52.ifgicopter.spf.data.AbstractInterpolator;

/**
 * Simple implementation of a {@link AbstractInterpolator}. The underlying method used is a linear one: The
 * value is interpolated within the last two available measurements (a "line" is drawn between the two
 * points).
 * 
 * @author Matthes Rieke <m.rieke@uni-muenster.de>
 * 
 */
public class LinearInterpolator extends AbstractInterpolator {

    @Override
    public Map<String, Object> interpolateForTimestamp(Map<String, SortedMap<Long, Object>> data, long timestamp) {

        HashMap<String, Object> result = new HashMap<String, Object>();

        /*
         * get interpolated data for every item
         */
        for (Entry<String, SortedMap<Long, Object>> entry : data.entrySet()) {
            result.put(entry.getKey(), getValueForTimestamp(entry.getValue(), timestamp));
        }

        return result;
    }

    @Override
    public Map<String, Object> interpolateForTimestamp(Map<String, SortedMap<Long, Object>> data,
                                                       Long timestamp,
                                                       Set<String> forItems) {

        HashMap<String, Object> result = new HashMap<String, Object>();

        /*
         * get interpolated data for every item
         */
        Double tmp;
        for (Entry<String, SortedMap<Long, Object>> entry : data.entrySet()) {
            if ( !forItems.contains(entry.getKey()))
                continue;
            tmp = getValueForTimestamp(entry.getValue(), timestamp.longValue());
            if (tmp == null) {
                /*
                 * tuple broken, one value is null
                 */
                return null;
            }
            result.put(entry.getKey(), tmp);
        }

        return result;
    }

    private Double getValueForTimestamp(SortedMap<Long, Object> sortedMap, long timestamp) {
        Long first = null, last = null;

        /*
         * get the closest interval to the timestamp
         */
        for (Long time : sortedMap.keySet()) {
            if (time.longValue() <= timestamp) {
                first = time;
            }

            if (time.longValue() >= timestamp) {
                last = time;
                break;
            }
        }

        if (first != null && last != null) {

            if (first.equals(last)) {
                return asDouble(sortedMap.get(first));
            }

            /*
             * compute the relative delta of time
             */
            long delta = last.longValue() - first.longValue();
            long deltaToStamp = timestamp - first.longValue();

            double quotient = (double) delta / (double) deltaToStamp;

            /*
             * compute the value
             */
            double firstVal = asDouble(sortedMap.get(first)).doubleValue();
            double lastVal = asDouble(sortedMap.get(last)).doubleValue();

            return Double.valueOf(firstVal + (lastVal - firstVal) / quotient);
        }

        return null;
    }

    private Double asDouble(Object object) {
        if (object instanceof Integer) {
            Integer i = (Integer) object;
            return new Double(i.doubleValue());
        }
        if (object instanceof Double) {
            Double d = (Double) object;
            return d;
        }
        if (object instanceof Float) {
            Float f = (Float) object;
            return Double.valueOf(f.doubleValue());
        }
        if (object instanceof Long) {
        	Long l = (Long) object;
        	return Double.valueOf(l.doubleValue());
        }

        throw new RuntimeException("Cannot convert object " + object + " to double, and therefore cannot interpolate!");
    }

    @Override
    public int getMinimumDataCount() {
        return 2;
    }

}
