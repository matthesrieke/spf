package org.n52.ifgicopter.spf.input.mk.gui.map;

import java.awt.Point;
import java.util.List;

import org.openstreetmap.gui.jmapviewer.Coordinate;
import org.openstreetmap.gui.jmapviewer.interfaces.MapMarker;

/**
 * Interface holding the methods needed for a {@link JMKOMap}.
 * This interface can be used to implement a custom map view.
 * 
 * @author Matthes Rieke <m.rieke@uni-muenster.de>
 *
 */
public interface JMKOMap {

	
	/**
	 * removes a marker from the map.
	 * 
	 * @param marker marker to be removed
	 */
	void removeMapMarker(MapMarker marker);
	
	/**
	 * adds a marker to the map.
	 * 
	 * @param marker marker to be added
	 */
	void addMapMarker(MapMarker marker);

	/**
	 * Returns lat/lon position using a display position.
	 * 
	 * @param point position on map display
	 * @return lat/lon as {@link Coordinate}
	 */
	Coordinate getPosition(Point point);

	/**
	 * Adds an overlay to the map that is displayed
	 * always.
	 * 
	 * @param kopterImgOverlay the new overlay
	 */
	void addOverlay(MapOverlay kopterImgOverlay);

	/**
	 * adds a new waypoint marker to the map. waypoint
	 * markers are connected with a line.
	 * 
	 * @param marker a new waypoint marker
	 */
	void addWaypointMarker(MapMarker marker);

	/**
	 * adds a new waypoint marker received by NaviCtrl
	 * to the map. waypoint markers are connected with a line.
	 * 
	 * @param marker a new waypoint marker
	 */
	void addMKWaypointMarker(MapMarker marker);
	
	/**
	 * @return the waypoint marker list.
	 */
	List<MapMarker> getWaypointMarker();
	
	/**
	 * @return the waypoint marker list received from the NaviCtrl.
	 */
	List<MapMarker> getMKWaypointMarker();
	
	/**
	 * sets lat/lon data of coordinate-overlay on the map
	 * 
	 * @param coordinate the new coordinate
	 */
	void setDisplayLatLon(Coordinate coordinate);
	
}
