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

package org.n52.ifgicopter.spf.gui.map;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import javax.swing.border.Border;

import org.openstreetmap.gui.jmapviewer.Coordinate;
import org.openstreetmap.gui.jmapviewer.JMapViewer;
import org.openstreetmap.gui.jmapviewer.MemoryTileCache;
import org.openstreetmap.gui.jmapviewer.interfaces.MapMarker;

/**
 * This map instance extends {@link JMapViewer} and
 * is using OSM tiles to display a WGS84 map with
 * overlays.
 * 
 * @author Matthes Rieke <m.rieke@uni-muenster.de>
 *
 */
public class JMapWithOverlay extends JMapViewer implements JMKOMap {

	private static final long serialVersionUID = 1L;
	
	private List<MapOverlay> overlays = new ArrayList<MapOverlay>();
	private CoordinateOverlay coordOverlay;

	private boolean firstRun = true;
	private boolean overlayActive = true;

	private ArrayList<MapMarker> waypointMarkers;
	private ArrayList<MapMarker> mKwaypointMarkers;

	/**
	 * see {@link JMapViewer#JMapViewer(org.openstreetmap.gui.jmapviewer.interfaces.TileCache, int)}
	 * 
	 * @param memoryTileCache see {@link JMapViewer}
	 * @param i see {@link JMapViewer}
	 */
	public JMapWithOverlay(MemoryTileCache memoryTileCache, int i) {
		super(memoryTileCache, i);
		
		/*
		 * dont display the map markers
		 * we are doing it ourselves because of
		 * the connecting lines.
		 */
		this.setMapMarkerVisible(false);
		
		this.waypointMarkers = new ArrayList<MapMarker>();
		this.mKwaypointMarkers = new ArrayList<MapMarker>();
		
		this.coordOverlay = new CoordinateOverlay(MapOverlay.BOTTOM_CENTER, this);
		this.addOverlay(this.coordOverlay);
	}

	/* (non-Javadoc)
	 * @see org.openstreetmap.gui.jmapviewer.JMapViewer#paintComponent(java.awt.Graphics)
	 */
	@Override
	protected void paintComponent(Graphics g) {
		if (this.firstRun ) {
			((Graphics2D) this.getGraphics()).setRenderingHint(
					RenderingHints.KEY_ANTIALIASING,
	                RenderingHints.VALUE_ANTIALIAS_ON);
			this.firstRun = false;
		}
		super.paintComponent(g);
		
		Graphics2D g2 = (Graphics2D) g;
		
		/*
		 * draw local waypoints
		 */
		MapMarker tmp = null;
        if (this.waypointMarkers != null) {
            for (MapMarker marker : this.waypointMarkers) {
                if (tmp != null) {
                	Point p1 = getMapPosition(tmp.getLat(), tmp.getLon());
                	Point p2 = getMapPosition(marker.getLat(), marker.getLon());
                	
                	if (p1 != null && p2 != null) {
                		g2.drawLine(p1.x, p1.y, p2.x, p2.y);
                	}
                }
                tmp = marker;
            }
            
            for (MapMarker marker : this.waypointMarkers) {
            	paintMarker(g, marker);
			}
        }
        
        /*
         * draw waypoints received from NaviCtrl
         */
        if (this.mKwaypointMarkers != null) {
            for (MapMarker marker : this.mKwaypointMarkers) {
                if (tmp != null) {
                	Point p1 = getMapPosition(tmp.getLat(), tmp.getLon());
                	Point p2 = getMapPosition(marker.getLat(), marker.getLon());
                	
                	if (p1 != null && p2 != null) {
                		g2.drawLine(p1.x, p1.y, p2.x, p2.y);
                	}
                }
                tmp = marker;
            }
        	
            for (MapMarker marker : this.mKwaypointMarkers) {
                paintMarker(g, marker);
            }
        }
        
        /*
         * draw other markers (FollowMe or SendTarget)
         */
        if (this.mapMarkerList != null) {
        	for (MapMarker marker : this.mapMarkerList) {
				paintMarker(g, marker);
			}
        }
		
        /*
         * the overlays
         */
		if (!this.overlayActive ) return;
		
		for (MapOverlay ol : this.overlays) {
			ol.draw(g2);
		}
	}
	
	
	/**
	 * Creates a snapshot of the current map
	 * without overlays and mapcontrol shown.
	 * 
	 * @return a snapshot of the current map
	 */
	public BufferedImage getSnapshot() {
		BufferedImage result = new BufferedImage(this.getSize().width, 
				this.getSize().height, BufferedImage.TYPE_INT_RGB);
		
		Border oldBorder = this.getBorder();
		this.setBorder(null);
		this.overlayActive = false;
		this.setZoomContolsVisible(false);
		
		this.paint(result.getGraphics());

		this.setZoomContolsVisible(true);
		this.setBorder(oldBorder);
		this.overlayActive = true;
		
		return result;
	}
	
	@Override
	public void addOverlay(MapOverlay ol) {
		this.overlays.add(ol);
	}

	@Override
	public synchronized void addMKWaypointMarker(MapMarker marker) {
		this.mKwaypointMarkers.add(marker);
	}

	@Override
	public synchronized void addWaypointMarker(MapMarker marker) {
		this.waypointMarkers.add(marker);			
	}

	@Override
	public synchronized List<MapMarker> getMKWaypointMarker() {
		return this.mKwaypointMarkers;
	}

	@Override
	public synchronized List<MapMarker> getWaypointMarker() {
		return this.waypointMarkers;
	}

	@Override
	public void setDisplayLatLon(Coordinate coordinate) {
		if (this.coordOverlay != null) {
			this.coordOverlay.setLatLon(coordinate);
		}
	}
}
