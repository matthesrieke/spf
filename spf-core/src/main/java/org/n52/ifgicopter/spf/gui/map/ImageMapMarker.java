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
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.openstreetmap.gui.jmapviewer.interfaces.MapMarker;


/**
 * Class implementing {@link MapMarker}
 * with a {@link BufferedImage} used.
 * 
 * @author Matthes Rieke <m.rieke@uni-muenster.de>
 *
 */
public class ImageMapMarker implements MapMarker {


	private static final Logger log = Logger.getLogger(ImageMapMarker.class);

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

	private static BufferedImage RED_IMAGE;
	private static BufferedImage YELLOW_IMAGE;
	private static BufferedImage BLUE_IMAGE;
	private static BufferedImage GREEN_IMAGE;

	static {
		try {
			InputStream in = null;
			File f = new File("img/point_red.png");
			if (!f.exists()) {
				in = ImageMapMarker.class.getResourceAsStream("/img/point_red.png");
			}else {
				in = new FileInputStream(f);
			}
			
			RED_IMAGE = ImageIO.read(in);

			f = new File("img/point_blue.png");
			if (!f.exists()) {
				in = ImageMapMarker.class.getResourceAsStream("/img/point_blue.png");
			}else {
				in = new FileInputStream(f);
			}
			
			BLUE_IMAGE = ImageIO.read(in);
			
			f = new File("img/point_yellow.png");
			if (!f.exists()) {
				in = ImageMapMarker.class.getResourceAsStream("/img/point_yellow.png");
			}else {
				in = new FileInputStream(f);
			}
			
			YELLOW_IMAGE = ImageIO.read(in);
			
			f = new File("img/point_green.png");
			if (!f.exists()) {
				in = ImageMapMarker.class.getResourceAsStream("/img/point_green.png");
			}else {
				in = new FileInputStream(f);
			}
			
			GREEN_IMAGE = ImageIO.read(in);
		} catch (IOException e) {
			log.log(Level.WARN, e.getMessage(), e);
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
		default:
			this.image = RED_IMAGE;
			break;
		}

		this.width = this.image.getWidth();
		this.height = this.image.getHeight();
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

	@Override
	public void paint(Graphics g, Point position) {
		g.drawImage(this.image, position.x - this.width/2,
				position.y - this.height/2, null);
	}

	/**
	 * @return the image
	 */
	public BufferedImage getImage() {
		return this.image;
	}
	
	

}
