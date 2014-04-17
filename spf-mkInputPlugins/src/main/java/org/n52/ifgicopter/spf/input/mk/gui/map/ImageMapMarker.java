package org.n52.ifgicopter.spf.input.mk.gui.map;

import java.awt.Graphics;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.openstreetmap.gui.jmapviewer.interfaces.MapMarker;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.n52.ifgicopter.javamk.MKConstants;

/**
 * Class implementing {@link MapMarker}
 * with a {@link BufferedImage} used.
 * 
 * @author Matthes Rieke <m.rieke@uni-muenster.de>
 *
 */
public class ImageMapMarker implements MapMarker {


	private static final Log log = LogFactory.getLog(MKConstants.LOGGER_ID);

	/**
	 * use a red image
	 */
	public static final int RED_ICON = 0;

	/**
	 * use a blue image
	 */
	public static final int BLUE_ICON = 1;

	/**
	 * use a yellow image
	 */
	public static final int YELLOW_ICON = 2;

	/**
	 * use a green image
	 */
	public static final int GREEN_ICON = 3;
	
	/**
	 * use the start image
	 */
	public static final int START_ICON = 4;
	
	/**
	 * use the finish image
	 */
	public static final int FINISH_ICON = 5;
	
	/**
	 * use a trans blue image
	 */
	public static final int BLUE_TRANS_ICON = 6;
	
	/**
	 * use an avtive blue image
	 */
	public static final int BLUE_ACTIVE_ICON = 7;

	private static BufferedImage RED_IMAGE;
	private static BufferedImage YELLOW_IMAGE;
	private static BufferedImage BLUE_IMAGE;
	private static BufferedImage GREEN_IMAGE;
	private static BufferedImage FINISH_IMAGE;
	private static BufferedImage START_IMAGE;
	private static BufferedImage BLUE_TRANS_IMAGE;
	private static BufferedImage BLUE_ACTIVE_IMAGE;

	static {
		try {
			RED_IMAGE = ImageIO.read(ImageMapMarker.class.getResourceAsStream("point_red.png"));
			BLUE_IMAGE = ImageIO.read(ImageMapMarker.class.getResourceAsStream("point_blue.png"));
			YELLOW_IMAGE = ImageIO.read(ImageMapMarker.class.getResourceAsStream("point_yellow.png"));
			GREEN_IMAGE = ImageIO.read(ImageMapMarker.class.getResourceAsStream("point_green.png"));
			START_IMAGE = ImageIO.read(ImageMapMarker.class.getResourceAsStream("point_start.png"));
			FINISH_IMAGE = ImageIO.read(ImageMapMarker.class.getResourceAsStream("point_finish.png"));
			BLUE_TRANS_IMAGE = ImageIO.read(ImageMapMarker.class.getResourceAsStream("point_blue_trans.png"));
			BLUE_ACTIVE_IMAGE = ImageIO.read(ImageMapMarker.class.getResourceAsStream("point_blue_active.png"));
		} catch (IOException e) {
			log.warn(e.getMessage(), e);
		}
	}

	private double lat;
	private double lon;
	private BufferedImage image;
	private int width;
	private int height;

	/**
	 * @param lat latitude of marker
	 * @param lon longitude of marker
	 * @param style style/color
	 */
	public ImageMapMarker(double lat, double lon, int style) {
		this.lat = lat;
		this.lon = lon;

		setIcon(style);
	}

	@Override
	public double getLat() {
		return this.lat;
	}

	@Override
	public double getLon() {
		return this.lon;
	}

	/**
	 * @param lat the lat to set
	 */
	public void setLat(double lat) {
		this.lat = lat;
	}

	/**
	 * @param lon the lon to set
	 */
	public void setLon(double lon) {
		this.lon = lon;
	}
	
	/**
	 * Use to change the style of the marker.
	 * 
	 * @param style the style of the marker
	 */
	public void setIcon(int style) {
		switch (style) {
		case RED_ICON:
			this.image = RED_IMAGE;
			break;
		case BLUE_ICON:
			this.image = BLUE_IMAGE;
			break;
		case YELLOW_ICON:
			this.image = YELLOW_IMAGE;
			break;
		case GREEN_ICON:
			this.image = GREEN_IMAGE;
			break;
		case START_ICON:
			this.image = START_IMAGE;
			break;
		case FINISH_ICON:
			this.image = FINISH_IMAGE;
			break;
		case BLUE_TRANS_ICON:
			this.image = BLUE_TRANS_IMAGE;
			break;
		case BLUE_ACTIVE_ICON:
			this.image = BLUE_ACTIVE_IMAGE;
			break;
		default:
			this.image = RED_IMAGE;
			break;
		}

		this.width = this.image.getWidth();
		this.height = this.image.getHeight();
	}

	@Override
	public void paint(Graphics g, Point position) {
		g.drawImage(this.image, position.x - this.width/2,
				position.y - this.height/2, null);
	}

}
