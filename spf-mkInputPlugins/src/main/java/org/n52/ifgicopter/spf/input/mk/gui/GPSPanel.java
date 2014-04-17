package org.n52.ifgicopter.spf.input.mk.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import javax.imageio.ImageIO;
import javax.swing.JPanel;
import javax.swing.border.LineBorder;

import org.openstreetmap.gui.jmapviewer.Coordinate;
import org.openstreetmap.gui.jmapviewer.DefaultMapController;
import org.openstreetmap.gui.jmapviewer.MemoryTileCache;
import org.openstreetmap.gui.jmapviewer.interfaces.MapMarker;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.n52.ifgicopter.javamk.MKConstants;
import org.n52.ifgicopter.javamk.incoming.Data3D;
import org.n52.ifgicopter.javamk.incoming.DebugData;
import org.n52.ifgicopter.javamk.incoming.FollowMeData;
import org.n52.ifgicopter.javamk.incoming.OSDData;
import org.n52.ifgicopter.javamk.incoming.RequestWaypointResponse;
import org.n52.ifgicopter.spf.input.mk.gui.listener.IPositionUpdateListener;
import org.n52.ifgicopter.spf.input.mk.gui.map.ImageMapMarker;
import org.n52.ifgicopter.spf.input.mk.gui.map.JMKOMap;
import org.n52.ifgicopter.spf.input.mk.gui.map.JMapWithOverlay;
import org.n52.ifgicopter.spf.input.mk.gui.map.MapOverlay;
import org.n52.ifgicopter.spf.input.mk.gui.map.OfflineMapWithOverlay;
import org.n52.ifgicopter.spf.input.mk.gui.tools.JPEGCommentIO;

/**
 * @author Matthes Rieke <m.rieke@uni-muenster.de>
 *
 */
public class GPSPanel extends JPanel implements IPositionUpdateListener {

	private static final long serialVersionUID = 1L;
	private static final Log log = LogFactory.getLog(MKConstants.LOGGER_ID);

	private JMKOMap map;
	private Map<Long, MapMarker> followMeMarkers = new HashMap<Long, MapMarker>();
	private MapMarker latestMarker;
	private MapMarker sendTargetMarker;

	private MapOverlay kopterImgOverlay;

	private boolean logFlight = false;
	private JavaMKMainPanel mainPanel;



	/**
	 * default constructor
	 * @param javaMKMainPanel the mainpanel, holding buttons
	 */
	public GPSPanel(JavaMKMainPanel javaMKMainPanel) {
		this.mainPanel = javaMKMainPanel;

		this.setLayout(new BorderLayout());

		initMap(new JMapWithOverlay(new MemoryTileCache(), 4));

		this.setPreferredSize(new Dimension(600, 500));

	}

	/**
	 * Inits the map and adds it to the {@link GPSPanel} components.
	 * 
	 * @param newMap the new map object
	 */
	public void initMap(JMKOMap newMap) {
		List<MapMarker> oldMakers = null;
		if (this.map != null) {
			oldMakers = getWaypointMarkers();

			for (MapMarker marker : oldMakers) {
				newMap.addWaypointMarker(marker);	
			}

			for (MapMarker marker : followMeMarkers.values()) {
				newMap.addMapMarker(marker);
			}

			/*
			 * there was a map before, remove it from the panel
			 */
			if (this.map instanceof JMapWithOverlay) {
				this.remove((JMapWithOverlay) this.map);
			}
			else if (this.map instanceof OfflineMapWithOverlay) {
				this.remove((OfflineMapWithOverlay) this.map);
			}
		}

		this.map = newMap;

		/*
		 * handle JMapWithOverlay
		 */
		if (this.map instanceof JMapWithOverlay) {
			new DefaultMapController((JMapWithOverlay) this.map) {
				@Override
				public void mouseClicked(MouseEvent e) {
					if(!GPSPanel.this.mainPanel.getIconBar().getNewWaypointButton().isSelected() &&
							!GPSPanel.this.mainPanel.getIconBar().getSendTargetButton().isSelected()) {
						super.mousePressed(e);
					}
					else {
						handleMouseClick(e);
					}
				}

				@Override
				public void mouseMoved(MouseEvent e) {
					GPSPanel.this.map.setDisplayLatLon(GPSPanel.this.map.getPosition(e.getPoint()));
					super.mouseMoved(e);
				}


			};

			((JMapWithOverlay) this.map).setBorder(new LineBorder(Color.GRAY));

			if (this.map.getWaypointMarker().size() > 0) {
				MapMarker c = this.map.getWaypointMarker().get(0);
				((JMapWithOverlay) this.map).setDisplayPositionByLatLon(c.getLat(), 
						c.getLon(), 17);
			} else {
				((JMapWithOverlay) this.map).setDisplayPositionByLatLon(51.971296,
						7.600267, 17);
			}

			this.add((JMapWithOverlay) this.map, BorderLayout.CENTER);

		}

		/*
		 * handle OfflineMapWithOverlay
		 */
		else if (this.map instanceof OfflineMapWithOverlay) {
			((OfflineMapWithOverlay) this.map).addMouseMotionListener(new MouseMotionListener() {
				@Override
				public void mouseMoved(MouseEvent e) {
					Coordinate coords = map.getPosition(e.getPoint());

					if (coords != null)	map.setDisplayLatLon(coords);
				}

				@Override
				public void mouseDragged(MouseEvent e) {
				}
			});
			((OfflineMapWithOverlay) this.map).addMouseListener(new MouseListener() {

				@Override
				public void mouseReleased(MouseEvent e) {
				}

				@Override
				public void mousePressed(MouseEvent e) {
				}

				@Override
				public void mouseExited(MouseEvent e) {
				}

				@Override
				public void mouseEntered(MouseEvent e) {
				}

				@Override
				public void mouseClicked(MouseEvent e) {
					handleMouseClick(e);
				}
			});

			this.add((OfflineMapWithOverlay) this.map, BorderLayout.CENTER);
		}

		/*
		 * add overlays
		 */
		BufferedImage img;
		try {
			InputStream in = null;
			File f = new File("mk_overlay2.png");
			if (!f.exists()) {
				in = getClass().getResourceAsStream("mk_overlay2.png");
			} else {
				in = new FileInputStream(f);
			}

			img = ImageIO.read(in);
			this.kopterImgOverlay = new MapOverlay(MapOverlay.BOTTOM_RIGHT, img, this.map);
			this.map.addOverlay(kopterImgOverlay);
		} catch (IOException e1) {
			log.warn(e1.getMessage(), e1);
		}

		//		try {
		//			this.signalOverlay = new SignalOverlay(MapOverlay.TOP_RIGHT, this.map);
		//			this.map.addOverlay(this.signalOverlay);
		//		} catch (IOException e1) {
		//			log.log(Level.WARN, e1.getMessage(), e1);
		//		}

		this.repaint();
	}

	/**
	 * Method to handle a mouse click inside
	 * the map.
	 * 
	 * @param e the event
	 */
	protected void handleMouseClick(MouseEvent e) {
		if (e.getButton() == MouseEvent.BUTTON1) {
			if (GPSPanel.this.mainPanel.getIconBar().getNewWaypointButton().isSelected()) {
				Coordinate p = GPSPanel.this.map.getPosition(e.getPoint());

				if (p == null) return;

				MapMarker marker = new ImageMapMarker(p.getLat(),
						p.getLon(), ImageMapMarker.BLUE_ICON);

				GPSPanel.this.map.addWaypointMarker(marker);
			}
			else {
				Coordinate p = GPSPanel.this.map.getPosition(e.getPoint());
				if (sendTargetMarker != null) {
					GPSPanel.this.map.removeMapMarker(sendTargetMarker);
				}

				sendTargetMarker = new ImageMapMarker(p.getLat(), p.getLon(),
						ImageMapMarker.YELLOW_ICON);
				GPSPanel.this.map.addMapMarker(sendTargetMarker);	
			}
		}

		this.repaint();
	}


	/**
	 * @param m a custom map marker
	 */
	public void addCustomMapMarker(MapMarker m) {
		this.map.addMapMarker(m);
	}
	
	/**
	 * @param m a custom map marker previously added
	 */
	public void removeCustomMapMarker(MapMarker m) {
		this.map.removeMapMarker(m);
	}
	
	/**
	 * @return the waypointMarkers
	 */
	public List<MapMarker> getWaypointMarkers() {
		return this.map.getWaypointMarker();
	}

	/**
	 * Adds all waypoint markers to a map instance.
	 * 
	 * @param markers the new markers
	 */
	public void setWaypointMarkers(List<MapMarker> markers) {
		this.map.getWaypointMarker().clear();
		for (MapMarker marker : markers) {
			this.map.addWaypointMarker(marker);	
		}

	}

	/**
	 * @return the sendTargetMarker
	 */
	public MapMarker getSendTargetMarker() {
		return sendTargetMarker;
	}

	/**
	 * removes all waypoints received from the NaviCtrl.
	 */
	public void removeMKWaypoints() {
		map.getMKWaypointMarker().clear();	
	}


	/**
	 * @param currpos 3-element array (lon, lat, alt)
	 * @param sysTime the system time when the update occured
	 */
	private void updateGPSData(double[] currpos, long sysTime) {

		/*
		 * create a MapMarker
		 */
		MapMarker marker = new ImageMapMarker(currpos[1], currpos[0],
				ImageMapMarker.RED_ICON);
		this.map.addMapMarker(marker);
		this.followMeMarkers.put(sysTime, marker);

		/*
		 * remove the old marker
		 */
		if (!logFlight) {
			this.map.removeMapMarker(this.latestMarker);
		}
		this.latestMarker = marker;

		/*
		 * center on the current position?
		 */
		if (mainPanel.getIconBar().getCenterOnMKPos().isSelected()) {
			if (this.map instanceof JMapWithOverlay) {
				((JMapWithOverlay) this.map).setDisplayPositionByLatLon(currpos[1], currpos[0],
						((JMapWithOverlay) this.map).getZoom());
			}
		}
	}


	/**
	 * @param recv new OSD data from NaviCtrl
	 */
	public void processOSDData(OSDData recv) {
		double[] currpos = recv.getNaviData().getCurrentPosition().getAsDegrees();
		updateGPSData(currpos, recv.getSystemTime());
	}


	/**
	 * @param wpr the response received from MK.
	 */
	public void processWaypointMarker(RequestWaypointResponse wpr) {
		double[] pos = wpr.getWaypoint().getPosition().getAsDegrees();
		MapMarker marker = new ImageMapMarker(pos[1], pos[0],
				ImageMapMarker.GREEN_ICON);

		this.map.addMKWaypointMarker(marker);
	}


	/**
	 * @param recv the response received from MK.
	 */
	public void processFollowMeData(FollowMeData recv) {
		double[] currpos = recv.getWaypoint().getPosition().getAsDegrees();
		updateGPSData(currpos, recv.getSystemTime());
	}

	/**
	 * @param d3d the response received from MK.
	 */
	public void processData3D(Data3D d3d) {
		if (kopterImgOverlay != null) {
			kopterImgOverlay.setRotation(d3d.getData3D().getHeading());
			this.repaint();
		}
	}

	/**
	 * @param dd the response received from MK.
	 */
	public void processDebugData(DebugData dd) {
		if (kopterImgOverlay != null) {
			if (dd.getAddress() == MKConstants.NAVI_CTRL_CHAR) {
				kopterImgOverlay.setRotation(dd.getAnalogData(10)*10);	
			}
			else if (dd.getAddress() == MKConstants.FLIGHT_CTRL_CHAR) {
				kopterImgOverlay.setRotation(dd.getAnalogData(8)*10);
			}
			this.repaint();
		}
	}


	/**
	 * @param mapMarker the marker where the map should be centered
	 */
	public void centerOnPosition(MapMarker mapMarker) {
		if (this.map instanceof JMapWithOverlay) {
			((JMapWithOverlay) this.map).setDisplayPositionByLatLon(mapMarker.getLat(), mapMarker.getLon(),
					((JMapWithOverlay) this.map).getZoom());
		}
	}

	/**
	 * Public class for creating a snapshot of an OSM map.
	 * 
	 * @author Matthes Rieke <m.rieke@uni-muenster.de>
	 *
	 */
	public class MakeSnapshot implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			/*
			 * snapshot with spatial envelope
			 */
			if (!(map instanceof JMapWithOverlay)) return;

			JMapWithOverlay jmwo = (JMapWithOverlay) map;

			BufferedImage img = jmwo.getSnapshot();
			File file = new File("map-" + System.currentTimeMillis() +".jpg");
			try {
				file.createNewFile();
			} catch (IOException e2) {
				log.warn(e2.getMessage(), e2);
			}
			try {
				ImageIO.write(img, "jpg", file);
				JPEGCommentIO comm = new JPEGCommentIO(file);

				Coordinate topLeft = jmwo.getPosition(0, 0);
				Coordinate lowRight = jmwo.getPosition(jmwo.getWidth(), jmwo.getHeight());

				comm.appendGeoInfo(topLeft, lowRight);
			} catch (IOException e1) {
				log.warn(e1.getMessage(), e1);
			}			
		}

	}


	/**
	 * Public class for retrieving a WMS map from a server.
	 * 
	 * @author Matthes Rieke <m.rieke@uni-muenster.de>
	 *
	 */
	public class LoadWMSMap implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					if (!(map instanceof JMapWithOverlay)) return;

					JMapWithOverlay jmwo = (JMapWithOverlay) map;

					Coordinate topLeft = jmwo.getPosition(0, 0);
					Coordinate lowRight = jmwo.getPosition(jmwo.getWidth(), jmwo.getHeight());

					StringBuilder url = new StringBuilder("http://www.wms.nrw.de/geobasis/DOP?SERVICE=WMS&");
					url.append("REQUEST=GetMap&LAYERS=WMS&FORMAT=image%2Fjpeg&TRANSPARENT=TRUE&");
					url.append("HEIGHT=");
					url.append(jmwo.getHeight());
					url.append("&WIDTH=");
					url.append(jmwo.getWidth());
					url.append("&SRS=EPSG:4326&STYLES=&VERSION=1.1.1&BBOX=");
					// 7.08782,50.60378,7.10215,50.61265
					url.append(topLeft.getLon());
					url.append(",");
					url.append(lowRight.getLat());
					url.append(",");
					url.append(lowRight.getLon());
					url.append(",");
					url.append(topLeft.getLat());

					ConsolePanel.getDefaultLogger().info("Loading WMS - "+ url.toString());
					
					try {
						URL conn = new URL(url.toString());
						File f = new File("wms-"+ System.currentTimeMillis() +".jpg");

						log.info("Retrieving WMS map from: "+ conn.toString());

						BufferedImage img = ImageIO.read(conn);
						if (img == null) {
							ConsolePanel.getDefaultLogger().warn("WMS loading failed.");
							return;
						}
						ImageIO.write(img, "jpg", f);

						JPEGCommentIO comm = new JPEGCommentIO(f);
						comm.appendGeoInfo(topLeft, lowRight);

						initMap(new OfflineMapWithOverlay(f));
						validate();
					} catch (MalformedURLException e1) {
						log.warn(e1.getMessage(), e1);
						ConsolePanel.getDefaultLogger().warn("WMS loading failed.");
					} catch (IOException e1) {
						log.warn(e1.getMessage(), e1);
						ConsolePanel.getDefaultLogger().warn("WMS loading failed.");
					}
				}
			}).start();

		}

	}


	/**
	 * Public class implementing {@link ActionListener}
	 * to clear all waypoints on the map.
	 * also used to switch route logging and to remove old
	 * route markers.
	 * 
	 * @author Matthes Rieke <m.rieke@uni-muenster.de>
	 *
	 */
	public class ClearWaypointsAction implements ActionListener {
		public void actionPerformed(ActionEvent e) {

			if (e.getActionCommand().equals("local")) {
				/*
				 * clear the waypoints in map
				 */
				map.getWaypointMarker().clear();

				GPSPanel.this.repaint();	
			}
			else if (e.getActionCommand().equals("route")) {
				/*
				 * switch flight route logging and remove or add
				 * old positions
				 */
				logFlight = !logFlight;
				log.info("Display of flight route set to: " +logFlight);

				if (logFlight == false) {
					for (MapMarker marker : followMeMarkers.values()) {
						map.removeMapMarker(marker);
					}
				} else {
					for (MapMarker marker : followMeMarkers.values()) {
						map.addMapMarker(marker);
					}
				}
			}
		}
	}

	/**
	 * Public class to export the logged Route.
	 * 
	 * @author Matthes Rieke <m.rieke@uni-muenster.de>
	 *
	 */
	public class ExportRouteAction implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				File f = new File("route-log-" + System.currentTimeMillis() +".txt");
				f.createNewFile();

				FileWriter fw = new FileWriter(f);

				TreeSet<Long> times = new TreeSet<Long>(followMeMarkers.keySet());
				SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-mm-dd HH:mm:ss.S");

				for (Long timeStamp : times) {
					MapMarker marker = followMeMarkers.get(timeStamp);
					fw.write(marker.getLat() +","+ marker.getLon() +","+ dateFormat.format(new Date(timeStamp)));
					fw.write("\n");
				}

				fw.flush();
				fw.close();
			} catch (Exception e1) {
				log.warn(e1.getMessage(), e1);
			}
		}

	}

	/**
	 * @return the waypoints stored at the MK
	 */
	public List<MapMarker> getMKWaypointMarkers() {
		return this.map.getMKWaypointMarker();
	}


}
