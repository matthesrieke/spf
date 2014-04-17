package org.n52.ifgicopter.spf.input.mk.gui.map;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.JPanel;

import org.n52.ifgicopter.spf.input.mk.gui.tools.JPEGCommentIO;
import org.openstreetmap.gui.jmapviewer.Coordinate;
import org.openstreetmap.gui.jmapviewer.interfaces.MapMarker;


/**
 * Map that implements {@link JMKOMap} using
 * an offline georeferenced image in WGS84 reference system.
 * 
 * @author Matthes Rieke <m.rieke@uni-muenster.de>
 *
 */
public class OfflineMapWithOverlay extends JPanel implements JMKOMap {
	
	private static final long serialVersionUID = 1L;
	private Image backgroundMap;
	
	private List<MapOverlay> overlays = new ArrayList<MapOverlay>();
	private boolean overlayActive = true;
	private Coordinate topLeft;
	private Coordinate bottomRight;
	private List<MapMarker> markers = new ArrayList<MapMarker>();
	private double ratio;
	private double imageWidth;
	private double imageHeight;
	private double latDelta;
	private double lonDelta;
	private List<MapMarker> waypointMarkers = new ArrayList<MapMarker>();
	private List<MapMarker> mKwaypointMarkers = new ArrayList<MapMarker>();
	private CoordinateOverlay coordOverlay;
	protected Image scaled;

	/**
	 * @param file the file holding the image
	 * @throws IOException if error occures
	 */
	public OfflineMapWithOverlay(File file) throws IOException {
		this.backgroundMap = ImageIO.read(file);
		this.imageWidth = (double) this.backgroundMap.getWidth(this);
		this.imageHeight = (double) this.backgroundMap.getHeight(this);
		this.ratio =  1.0;
		scaled = backgroundMap;
		
		Dimension min = new Dimension(this.backgroundMap.getWidth(null) / 2, this.backgroundMap.getHeight(null) / 2);
		setMinimumSize(min);
        setPreferredSize(new Dimension(400, 400));
		
		/*
		 * read the comment and parse it.
		 */
		parseGeoInfo(new JPEGCommentIO(file).readComment());
		
		this.latDelta = this.topLeft.getLat() - this.bottomRight.getLat();
		this.lonDelta = this.bottomRight.getLon() - this.topLeft.getLon();
		
		coordOverlay = new CoordinateOverlay(MapOverlay.BOTTOM_CENTER, this);
		this.addOverlay(coordOverlay);
		
		this.addComponentListener(new ComponentListener() {
			
			@Override
			public void componentShown(ComponentEvent e) {
			}
			
			@Override
			public void componentResized(ComponentEvent e) {
				/*
				 * calculate the ratio of the image
				 */
				double ratioX = imageWidth / getSize().width;
				double ratioY = imageHeight / getSize().height;
				
				/*
				 * apply the ratio. this way the aspect ratio
				 * remains the same
				 */
				if (ratioX > ratioY) {
					ratio = ratioX;
					scaled = backgroundMap.getScaledInstance(
							(int) (imageWidth/ratioX),
							(int) (imageHeight/ratioX), 0);
				}
				else if (ratioX < ratioY){
					ratio = ratioY;
					scaled = backgroundMap.getScaledInstance(
							(int) (imageWidth/ratioY),
							(int) (imageHeight/ratioY), 0);
				}
				else {
					/*
					 * map not resized yet
					 */
					ratio = 1.0;
					scaled = backgroundMap;
				}
			}
			
			@Override
			public void componentMoved(ComponentEvent e) {
			}
			
			@Override
			public void componentHidden(ComponentEvent e) {
			}
		});
	}
	
	
	
	/**
	 * parses the GEOInformation contained in the comment
	 * part of the image.
	 * 
	 * @param geoInfo the String containing the GEOInformation
	 */
	private void parseGeoInfo(String geoInfo) {
		String sub = geoInfo.substring(geoInfo.indexOf(',') + 1,
				geoInfo.lastIndexOf(','));
		String[] coords = sub.split(",");
		
		if (coords.length != 4) throw new IllegalArgumentException("Wrong count of coordinates.");
		
		String[] corner = coords[1].split(":");
		
		topLeft = new Coordinate(java.lang.Double.parseDouble(corner[0]), 
				java.lang.Double.parseDouble(corner[1]));
		
		corner = coords[2].split(":");
		
		bottomRight = new Coordinate(java.lang.Double.parseDouble(corner[0]), 
				java.lang.Double.parseDouble(corner[1]));
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		
		Graphics2D g2 = (Graphics2D) g;
		
		g2.drawImage(scaled, 0, 0, this);
		
		/*
		 * draw local waypoints
		 */
		MapMarker tmp = null;
        if (waypointMarkers != null) {
            for (MapMarker marker : waypointMarkers) {
                if (tmp != null) {
                	Point p1 = getMapPosition(tmp.getLat(), tmp.getLon());
                	Point p2 = getMapPosition(marker.getLat(), marker.getLon());
                	
                	g2.drawLine(p1.x, p1.y, p2.x, p2.y);
                }
                tmp = marker;
            }
            
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
                	
                	g2.drawLine(p1.x, p1.y, p2.x, p2.y);
                }
                tmp = marker;
            }
        	
            for (MapMarker marker : mKwaypointMarkers) {
                paintMarker(g, marker);
            }
        }
        
		/*
		 * draw other markers
		 */
        if (markers != null) {
        	for (MapMarker marker : markers) {
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
     * paints the marker using the given Graphics.
     * 
     * @param g the used {@link Graphics}
     * @param marker the marker to paint
     */
    protected void paintMarker(Graphics g, MapMarker marker) {
        Point p = getMapPosition(marker.getLat(), marker.getLon());
        if (p != null) {
            marker.paint(g, p);
        }
    }

	/**
	 * Calculates the display position
	 * using lat/lon coordiantes
	 * 
	 * @param lat latitude
	 * @param lon longitude
	 * @return a Point in display coordinates
	 */
	private Point getMapPosition(double lat, double lon) {
		double tempLat = lat - this.bottomRight.getLat();
		double tempLon = lon - this.topLeft.getLon();
		
		if (tempLat >= 0 && tempLon >= 0) {
			double tmp = this.lonDelta / tempLon;
			double x = this.imageWidth/this.ratio/tmp;
			
			tmp = this.latDelta / tempLat;
			double y = this.imageHeight/this.ratio - this.imageHeight/this.ratio/tmp;
			return new Point((int) x, (int) y);
		}
		
		return null;
	}

	@Override
	public void addMapMarker(MapMarker marker) {
		this.markers.add(marker);
		this.repaint();
	}

	@Override
	public void addOverlay(MapOverlay ol) {
		this.overlays.add(ol);
	}

	@Override
	public Coordinate getPosition(Point point) {
		if (point.x > this.imageWidth/this.ratio || point.y 
				> this.imageHeight/this.ratio) {
			return null;
		}
		
		double tmp = this.imageWidth/this.ratio / point.x;
		double lon = this.topLeft.getLon() + this.lonDelta/tmp;
		
		tmp = this.imageHeight/this.ratio / point.y;
		double lat = this.topLeft.getLat() - this.latDelta/tmp;
		
		return new Coordinate(lat, lon);
	}

	@Override
	public void removeMapMarker(MapMarker marker) {
		this.markers.remove(marker);
	}
	
	@Override
	public void addMKWaypointMarker(MapMarker marker) {
		this.mKwaypointMarkers.add(marker);
	}

	@Override
	public void addWaypointMarker(MapMarker marker) {
		this.waypointMarkers.add(marker);
	}

	@Override
	public List<MapMarker> getMKWaypointMarker() {
		return mKwaypointMarkers;
	}

	@Override
	public List<MapMarker> getWaypointMarker() {
		return waypointMarkers;
	}

	@Override
	public void setDisplayLatLon(Coordinate coordinate) {
		if (coordOverlay != null) {
			coordOverlay.setLatLon(coordinate);
		}		
	}

}
