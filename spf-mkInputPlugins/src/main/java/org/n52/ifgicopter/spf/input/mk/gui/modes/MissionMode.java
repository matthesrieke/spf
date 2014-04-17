package org.n52.ifgicopter.spf.input.mk.gui.modes;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.n52.ifgicopter.javamk.incoming.FollowMeData;
import org.n52.ifgicopter.javamk.incoming.OSDData;
import org.n52.ifgicopter.spf.input.mk.gui.GPSPanel;
import org.n52.ifgicopter.spf.input.mk.gui.MapMarkerWrapper;
import org.n52.ifgicopter.spf.input.mk.gui.listener.IPositionUpdateListener;
import org.n52.ifgicopter.spf.input.mk.gui.map.ImageMapMarker;

/**
 * Mission modus for managing an automatic waypoint flight (also with multiple
 * packages of waypoints as MK has limited waypoint number count).
 * 
 * @author matthes rieke
 *
 */
public class MissionMode implements IPositionUpdateListener {

	private List<MapMarkerWrapper> waypoints;
	private ImageMapMarker activeWaypoint;
	private int activeWaypointIndex;
	private GPSPanel gpsPanel;
	private static final Log log = LogFactory.getLog(MissionMode.class);

	/*
	 * 
	 * Do something to register self at WaypointsSender if new package of waypoints
	 * is sent, to get informed aboout success!
	 * 
	 */
	/**
	 * @param waypoints the waypoints for this mission
	 * @param panel the map GUI panel
	 */
	public MissionMode(List<MapMarkerWrapper> waypoints, GPSPanel panel) {
		if (waypoints.isEmpty()) throw new IllegalArgumentException("MissionMode needs waypoints.");
		
		this.gpsPanel = panel;
		this.waypoints = waypoints;
		
		for (MapMarkerWrapper wp : waypoints) {
			if (!(wp.getMetaField(MapMarkerWrapper.ALTITUDE) instanceof Double)) {
				throw new IllegalArgumentException("Every waypoint needs altitude information.");
			}
		}
		
		this.activeWaypoint = waypoints.get(0);
		this.activeWaypoint.setIcon(ImageMapMarker.BLUE_ACTIVE_ICON);
		this.gpsPanel.addCustomMapMarker(this.activeWaypoint);
	}
	
	
	@Override
	public void processOSDData(OSDData recv) {
		if (this.activeWaypointIndex != recv.getNaviData().getWaypointIndex()) {
			this.activeWaypointIndex = recv.getNaviData().getWaypointIndex();
			
			this.gpsPanel.removeCustomMapMarker(this.activeWaypoint);
			
			this.activeWaypoint = this.waypoints.get(recv.getNaviData().getWaypointIndex());
			this.activeWaypoint.setIcon(ImageMapMarker.BLUE_ACTIVE_ICON);
			
			this.gpsPanel.addCustomMapMarker(this.activeWaypoint);
			
			log.info("Active waypoint: "+this.activeWaypointIndex);
		}
		
	}

	@Override
	public void processFollowMeData(FollowMeData recv) {
		// TODO Auto-generated method stub
		
	}
	
	

}
