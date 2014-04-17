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
package org.n52.ifgicopter.spf.output.kml;

import java.util.HashMap;

public class GxConstants {

    public static final String GX_NAMESPACE_URI = "http://www.google.com/kml/ext/2.2";

    public static final String GX_SCHEMA_PREFIX = "gx";

    private static final String GX_SCHEMA_FILE_LOCATION = "https://developers.google.com/kml/schema/kml22gx.xsd";

    public static String getSchemaLocation() {
        return GX_NAMESPACE_URI + " " + GX_SCHEMA_FILE_LOCATION;
    }

    public static void addSuggestedPrefix(HashMap<String, String> suggestedPrefixes) {
        suggestedPrefixes.put(GX_NAMESPACE_URI, GX_SCHEMA_PREFIX);
    }

}
