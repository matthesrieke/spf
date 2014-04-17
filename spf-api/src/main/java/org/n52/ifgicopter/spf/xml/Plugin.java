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

package org.n52.ifgicopter.spf.xml;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.opengis.sensorML.x101.SensorMLDocument;
import net.opengis.sensorML.x101.SensorMLDocument.SensorML;

/**
 * @author Matthes Rieke <m.rieke@uni-muenster.de>
 * 
 */
public class Plugin {

    /**
     * the namespace for the SPF Plugin schema
     */
    public static final String SPF_PLUGIN_NAMESPACE = "http://ifgi.uni-muenster.de/~m_riek02/spf/0.1";

    public static final String SENSORML_NAMESPACE = "http://www.opengis.net/sensorML/1.0.1";

    public static final String PERIOD_BEHAVIOUR = "period";
    public static final String AVAILABLE_BEHAVIOUR = "available";

    public static final String TIME_DEFAULT_NAME = "time";

    private String name;
    private Map<String, Item> items = new HashMap<String, Item>();
    private String outputType;
    private int timeDelta = Integer.MIN_VALUE;

    private List<String> outputProperties = new ArrayList<String>();
    private List<String> mandatoryProperties = new ArrayList<String>();

    /**
     * this list holds all incoming properties. e.g., a CompoundField holds the incoming properties in its
     * leafs (which are SimpleFields)
     */
    private List<String> inputProperties = new ArrayList<String>();

    private Time time;
    private Location location;
    private SensorML sensorML;
    private boolean mobile = true;

    private PluginMetadata metadata;

    /**
     * Recursive method to find a leaf of a CompoundItem.
     * 
     * @param leafName
     *        the leaf name
     * @param tree
     *        the tree to search. if null this.items.values() will be used
     * @return the leaf
     */
    public Item getLeafItemOfCompound(String leafName, Collection<Item> tree) {
        Collection<Item> tempTree = tree;
        if (tempTree == null) {
            tempTree = this.items.values();
        }
        for (Item item : tempTree) {
            if (item instanceof CompoundItem) {
                return getLeafItemOfCompound(leafName, ((CompoundItem) item).getCompoundItems());
            }
            if (item.getProperty().equals(leafName)) {
                return item;
            }
        }

        return null;
    }

    /**
     * @return the name of the plugin
     */
    public String getName() {
        return this.name;
    }

    /**
     * @return items hold in this inputplugin
     */
    public Map<String, Item> getItems() {
        return this.items;
    }

    /**
     * @param itemName
     *        the name of the item
     * @return the item with the name
     */
    public Item getItem(String itemName) {
        return this.items.get(itemName);
    }

    /**
     * @return behaviour type. push data periodly?
     */
    public String getOutputType() {
        return this.outputType;
    }

    /**
     * @return the behaviour corresponding value (period time)
     */
    public int getTimeDelta() {
        return this.timeDelta;
    }

    /**
     * @return the list of output properties
     */
    public List<String> getOutputProperties() {
        return this.outputProperties;
    }

    /**
     * @return the mandatory items for output
     */
    public List<String> getMandatoryProperties() {
        return this.mandatoryProperties;
    }

    /**
     * @return all input properties of this plugin. see {@link Plugin#inputProperties}.
     */
    public List<String> getInputProperties() {
        return this.inputProperties;
    }

    /**
     * @return the time Item for this plugin
     */
    public Time getTime() {
        return this.time;
    }

    /**
     * @return the location Item for this plugin
     */
    public Location getLocation() {
        return this.location;
    }

    /**
     * @return the metadata
     */
    public PluginMetadata getMetadata() {
        return this.metadata;
    }

    /**
     * @param metadata
     *        the metadata to set
     */
    public void setMetadata(PluginMetadata metadata) {
        this.metadata = metadata;
    }

    /**
     * @return the mobile
     */
    public boolean isMobile() {
        return this.mobile;
    }

    /**
     * @return xmlbeans representation of this plugins SensorML
     */
    public String getSensorMLString() {
        SensorMLDocument doc = SensorMLDocument.Factory.newInstance();
        doc.setSensorML(this.getSensorML());
        return doc.toString();
    }

    /**
     * @param items
     *        the items to set
     */
    public void setItems(Map<String, Item> items) {
        this.items = items;
    }

    /**
     * @param outputProperties
     *        the outputProperties to set
     */
    public void setOutputProperties(List<String> outputProperties) {
        this.outputProperties = outputProperties;
    }

    /**
     * @param mandatoryProperties
     *        the mandatoryProperties to set
     */
    public void setMandatoryProperties(List<String> mandatoryProperties) {
        this.mandatoryProperties = mandatoryProperties;
    }

    /**
     * @param inputProperties
     *        the inputProperties to set
     */
    public void setInputProperties(List<String> inputProperties) {
        this.inputProperties = inputProperties;
    }

    /**
     * @param name
     *        the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @param outputType
     *        the outputType to set
     */
    public void setOutputType(String outputType) {
        this.outputType = outputType;
    }

    /**
     * @param timeDelta
     *        the timeDelta to set
     */
    public void setTimeDelta(int timeDelta) {
        this.timeDelta = timeDelta;
    }

    /**
     * @param location
     *        the location to set
     */
    public void setLocation(Location location) {
        this.location = location;
    }

    /**
     * @param sensorML
     *        the sensorML to set
     */
    public void setSensorML(SensorML sensorML) {
        this.sensorML = sensorML;
    }

    /**
     * @param mobile
     *        the mobile to set
     */
    public void setMobile(boolean mobile) {
        this.mobile = mobile;
    }

    /**
     * adds a new output property at runtime.
     * 
     * @param item
     *        the new item
     */
    public void addOutputProperty(Item item) {
        this.items.put(item.getProperty(), item);
        this.outputProperties.add(item.getProperty());
        this.inputProperties.add(item.getProperty());
    }

    /**
     * adds a new mandatory property at runtime
     * 
     * @param item
     *        the new item
     */
    public void addMandatoryProperty(Item item) {
        this.items.put(item.getProperty(), item);
        this.mandatoryProperties.add(item.getProperty());
        this.inputProperties.add(item.getProperty());
    }

    /**
     * adds a normal input property
     * 
     * @param item
     *        the new item
     */
    public void addInputProperty(Item item) {
        this.items.put(item.getProperty(), item);
        this.inputProperties.add(item.getProperty());
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("plugin-name: '");
        sb.append(this.name);
        sb.append("'; input-properties: ");
        sb.append(this.inputProperties);

        return sb.toString();
    }

    public void setTime(Time time2) {
        this.time = time2;
    }

    public SensorML getSensorML() {
        return this.sensorML;
    }

}
