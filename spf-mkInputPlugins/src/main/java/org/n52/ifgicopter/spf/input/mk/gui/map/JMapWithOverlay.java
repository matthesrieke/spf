package org.n52.ifgicopter.spf.input.mk.gui.map;

import java.awt.Color;
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
		
		coordOverlay = new CoordinateOverlay(MapOverlay.BOTTOM_CENTER, this);
		this.addOverlay(coordOverlay);
	}

	/* (non-Javadoc)
	 * @see org.openstreetmap.gui.jmapviewer.JMapViewer#paintComponent(java.awt.Graphics)
	 */
	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		
		Graphics2D g2 = (Graphics2D) g;
		g2.setRenderingHint(
				RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
		/*
		 * draw local waypoints
		 */
		MapMarker tmp = null;
        if (waypointMarkers != null) {
        	
        	Color oldcolor = g2.getColor();
        	g2.setColor(Color.RED);
            for (MapMarker marker : waypointMarkers) {
                if (tmp != null) {
                	Point p1 = getMapPosition(tmp.getLat(), tmp.getLon());
                	Point p2 = getMapPosition(marker.getLat(), marker.getLon());
                	
                	if (p1 == null && p2 == null) {
                		
                	}
                	else {
                		if (p1 == null) {
                			p1 = getMapPosition(tmp.getLat(), tmp.getLon(), false);
                		}
                		else {
                			p2 = getMapPosition(marker.getLat(), marker.getLon(), false);
                		}
                	}
                	
                	if (p1 != null && p2 != null) {
                		g2.drawLine(p1.x, p1.y, p2.x, p2.y);
                	}
                }
                tmp = marker;
            }
            g2.setColor(oldcolor);

            for (MapMarker marker : waypointMarkers) {
            	paintMarker(g, marker);
			}
        }
        
        /*
         * draw waypoints received from NaviCtrl
         */
        if (mKwaypointMarkers != null) {
            for (MapMarker marker : mKwaypointMarkers) {
                if (tmp != null) {
                	Point p1 = getMapPosition(tmp.getLat(), tmp.getLon());
                	Point p2 = getMapPosition(marker.getLat(), marker.getLon());
                	
                	if (p1 != null && p2 != null) {
                		g2.drawLine(p1.x, p1.y, p2.x, p2.y);
                	}
                }
                tmp = marker;
            }
        	
            for (MapMarker marker : mKwaypointMarkers) {
                paintMarker(g, marker);
            }
        }
        
        /*
         * draw other markers (FollowMe or SendTarget)
         */
        if (mapMarkerList != null) {
        	for (MapMarker marker : mapMarkerList) {
				paintMarker(g, marker);
			}
        }
		
        /*
         * the overlays
         */
		if (!overlayActive ) return;
		
		for (MapOverlay ol : overlays) {
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
		mKwaypointMarkers.add(marker);
		updateStartFinishMarkers(mKwaypointMarkers, true, ImageMapMarker.GREEN_ICON);
	}

	@Override
	public synchronized void addWaypointMarker(MapMarker marker) {
		waypointMarkers.add(marker);			
		updateStartFinishMarkers(waypointMarkers, true, ImageMapMarker.BLUE_ICON);
	}

	@Override
	public synchronized List<MapMarker> getMKWaypointMarker() {
		return mKwaypointMarkers;
	}

	@Override
	public synchronized List<MapMarker> getWaypointMarker() {
		return waypointMarkers;
	}

	@Override
	public void setDisplayLatLon(Coordinate coordinate) {
		if (coordOverlay != null) {
			coordOverlay.setLatLon(coordinate);
		}
	}
	
	private void updateStartFinishMarkers(List<MapMarker> theList, boolean added, int defaultStyle) {
		if (added) {
			if (theList.size() > 1) {
				((ImageMapMarker)theList.get(theList.size()-1)).setIcon(ImageMapMarker.FINISH_ICON);
				((ImageMapMarker)theList.get(0)).setIcon(ImageMapMarker.START_ICON);
				
				if (theList.size() > 2) {
					((ImageMapMarker)theList.get(theList.size()-2)).setIcon(ImageMapMarker.BLUE_ICON);
				}
			}
		} else {
			if (theList.size() > 1) {
				((ImageMapMarker)theList.get(theList.size()-1)).setIcon(ImageMapMarker.FINISH_ICON);
				((ImageMapMarker)theList.get(0)).setIcon(ImageMapMarker.START_ICON);
			}
		}
	}
}
