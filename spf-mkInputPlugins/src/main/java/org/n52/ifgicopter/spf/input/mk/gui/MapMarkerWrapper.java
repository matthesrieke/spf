package org.n52.ifgicopter.spf.input.mk.gui;

import java.util.HashMap;
import java.util.Map;

import org.n52.ifgicopter.spf.input.mk.gui.map.ImageMapMarker;
import org.openstreetmap.gui.jmapviewer.interfaces.MapMarker;

/**
 * A wrapper class for {@link MapMarker} which can hold metadata.
 * 
 * @author matthes rieke
 *
 */
public class MapMarkerWrapper extends ImageMapMarker {
	
	/**
	 * altitude key
	 */
	public static final String ALTITUDE = "altitude";
	
	/**
	 * tolerance radius key
	 */
	public static final String RADIUS = "radius";
	
	/**
	 * hold time key
	 */
	public static final String HOLD_TIME = "holdTime";
	
	private Map<String, Object> meta;

	/**
	 * @param lat latitude
	 * @param lon longitude
	 */
	public MapMarkerWrapper(double lat, double lon) {
		super(lat, lon, ImageMapMarker.BLUE_ICON);
		this.meta = new HashMap<String, Object>();
		
		/*
		 * add some default values 
		 */
		addMetaField(ALTITUDE, 15d);
		addMetaField(HOLD_TIME, 2);
		addMetaField(RADIUS, 5);
	}

	/**
	 * add a metadata property.
	 * 
	 * @param key the key
	 * @param value the value
	 */
	public void addMetaField(String key, Object value) {
		this.meta.put(key, value);
	}
	
	/**
	 * @param key the key
	 * @return the value of the key
	 */
	public Object getMetaField(String key) {
		return this.meta.get(key);
	}


}
