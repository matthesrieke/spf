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

import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

/**
 * Abstract class for an interpolation algorithm.
 * 
 * @author Matthes Rieke <m.rieke@uni-muenster.de>
 *
 */
public abstract class AbstractInterpolator {

	/**
	 * @param data the sorted map
	 * @param timestamp the timestamp to get the data
	 * @return one data set for the timestamp
	 */
	public abstract Map<String, Object> interpolateForTimestamp(Map<String,
			SortedMap<Long, Object>> data, long timestamp);

	/**
	 * @param data the sorted map
	 * @param timestamp the timestamp to get the data
	 * @param forItems only create data for this items
	 * @return map with interpolated values
	 */
	public abstract Map<String, Object> interpolateForTimestamp(
			Map<String, SortedMap<Long, Object>> data,
			Long timestamp, Set<String> forItems);

	/**
	 * @return the number of data values needed for interpolation
	 */
	public abstract int getMinimumDataCount();

}
