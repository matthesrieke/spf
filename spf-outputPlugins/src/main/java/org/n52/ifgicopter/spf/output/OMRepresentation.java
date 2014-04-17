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

package org.n52.ifgicopter.spf.output;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.joda.time.DateTime;
import org.n52.ifgicopter.spf.xml.Plugin;

import net.opengis.gml.DirectPositionType;
import net.opengis.gml.PointType;
import net.opengis.gml.TimeInstantType;
import net.opengis.gml.TimePositionType;
import net.opengis.om.x10.ObservationDocument;
import net.opengis.om.x10.ObservationType;
import net.opengis.swe.x101.ItemPropertyType;
import net.opengis.swe.x101.QuantityDocument.Quantity;
import net.opengis.swe.x101.RecordType;
import net.opengis.swe.x101.UomPropertyType;

/**
 * This class represents a measurement as on Observation & Measurements (O&M) document.
 * 
 * @author Matthes Rieke <m.rieke@uni-muenster.de>
 * 
 */
public class OMRepresentation {

    private ObservationDocument observation;
    private Plugin plugin;

    /**
     * Constructor with the data to be encoded.
     * 
     * @param data
     *        data map to be encoded
     * @param plugin
     *        the underlying plugin
     */
    public OMRepresentation(Map<String, Object> data, Plugin plugin) {
        this.observation = ObservationDocument.Factory.newInstance();
        this.plugin = plugin;

        createDocument(data);
    }

    private void createDocument(Map<String, Object> data) {
        ObservationType obs = this.observation.addNewObservation();

        /*
         * location
         */
        PointType point = PointType.Factory.newInstance();
        DirectPositionType pos = point.addNewPos();
        String lat = String.valueOf(data.get(this.plugin.getLocation().getFirstCoordinateName()));
        String lon = String.valueOf(data.get(this.plugin.getLocation().getSecondCoordinateName()));
        pos.setStringValue(lat + " " + lon);

        obs.addNewLocation().set(point);

        /*
         * time
         */
        TimeInstantType tit = TimeInstantType.Factory.newInstance();

        Object tmp = data.get(this.plugin.getTime().getProperty());
        if (tmp instanceof Long) {
            DateTime dateTime = new DateTime(data.get(this.plugin.getTime().getProperty()));
            TimePositionType timePos = tit.addNewTimePosition();
            timePos.setStringValue(dateTime.toString());
        }

        obs.addNewSamplingTime().set(tit);

        /*
         * data items
         */
        XmlObject result = obs.addNewResult();

        // cursor need because om:result is of type "anyType"
        XmlCursor resultC = result.newCursor();
        resultC.toNextToken();
        // begin the element hard-coded. TODO is there any other way?
        resultC.beginElement("Record", "http://www.opengis.net/swe/1.0.1");

        RecordType record = RecordType.Factory.newInstance();
        // get the cursor of the Record. now it is at
        // the starting position
        XmlCursor recordC = record.newCursor();

        /*
         * add other data
         */
        for (Entry<String, Object> entry : data.entrySet()) {

            String key = entry.getKey();
            if (key.equals(this.plugin.getTime().getProperty())
                    || key.equals(this.plugin.getLocation().getFirstCoordinateName())
                    || key.equals(this.plugin.getLocation().getSecondCoordinateName())) {
                continue;
            }

            Object itemData = entry.getValue();

            /*
             * did we find the value ?
             */
            if (itemData != null) {
                /*
                 * <swe:Quantity definition="urn:ogc:def:phenomenon:OGC:1.0.30:temperature"> <swe:uom
                 * code="[degF]"/> </swe:Quantity>
                 */
                ItemPropertyType field = record.addNewField();

                Quantity q = Quantity.Factory.newInstance();
                q.setDefinition(key);
                UomPropertyType uom = q.addNewUom();
                uom.setCode("");
                q.setValue(Double.parseDouble(itemData.toString()));

                field.addNewItem().set(q);
            }
        }

        // move the contents of record to result
        recordC.moveXmlContents(resultC);
    }

    /**
     * @return the xmlbeans document
     */
    public ObservationDocument getObservation() {
        return this.observation;
    }

    /**
     * @return a pretty printed version of this O&M
     */
    public String toXML() {
        XmlOptions opts = new XmlOptions();
        opts.setSavePrettyPrint();
        opts.setSaveNamespacesFirst();

        HashMap<String, String> prefixes = new HashMap<String, String>();
        prefixes.put("http://www.opengis.net/om/1.0", "om");
        prefixes.put("http://www.opengis.net/gml", "gml");
        prefixes.put("http://www.opengis.net/swe/1.0.1", "swe");
        opts.setSaveSuggestedPrefixes(prefixes);

        opts.setSaveNoXmlDecl();

        return this.observation.xmlText(opts);
    }
}
