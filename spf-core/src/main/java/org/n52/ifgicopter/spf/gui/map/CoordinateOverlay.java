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

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.text.DecimalFormat;
import java.text.NumberFormat;


import org.openstreetmap.gui.jmapviewer.Coordinate;

/**
 * Class extending {@link MapOverlay} to display
 * coordinates of the mouse position.
 * 
 * @author Matthes Rieke <m.rieke@uni-muenster.de>
 *
 */
public class CoordinateOverlay extends MapOverlay {

	private static final long serialVersionUID = 1L;
	private NumberFormat formatter = new DecimalFormat("#0.000000");
	private Coordinate currentPosition;
	private Graphics2D graphics;
	private int descent;
	private final Color trans = new Color(0, 0, 0, 140);

	/**
	 * @param orientation position on the map
	 * @param observ the map
	 */
	public CoordinateOverlay(int orientation, JMKOMap observ) {
		super(orientation, observ);
		Font font = new Font(Font.MONOSPACED, Font.BOLD, 14);
		
		this.overlay = new BufferedImage(200, 18, BufferedImage.TYPE_INT_ARGB);
		
		this.graphics = this.overlay.createGraphics();
		this.graphics.setFont(font);
		FontMetrics metrics = this.graphics.getFontMetrics();
		
		this.width = metrics.stringWidth("-180.000000 : -90.000000");
		this.height = metrics.getHeight() + 4;
		
		this.descent = metrics.getMaxDescent();
		
		/*
		 * workaround - needed twice because FontMetrics are
		 * not available until image is created.
		 */
		this.overlay = new BufferedImage(this.width, this.height, BufferedImage.TYPE_INT_ARGB);
		
		this.graphics = this.overlay.createGraphics();
		this.graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
		this.graphics.setFont(font);
		
		setLatLon(new Coordinate(0.0, 0.0));
	}

	/**
	 * should be called whenever new coordinate information
	 * is available
	 * 
	 * @param coordinate the new coords
	 */
	public void setLatLon(Coordinate coordinate) {
		this.currentPosition = coordinate;
		/*
		 * remove the old
		 */
		this.graphics.setComposite(AlphaComposite.Clear);
		this.graphics.fillRect(0,0,this.width, this.height);
		this.graphics.setComposite(AlphaComposite.SrcOver);

		/*
		 * rounded rect half transparent
		 */
		this.graphics.setColor(this.trans);
		this.graphics.fillRoundRect(0, 0, this.width, this.height, 5, 5);
		this.graphics.setColor(Color.white);
		
		/*
		 * draw the new
		 */
		String str = this.formatter.format(coordinate.getLat()) +" : "+
				this.formatter.format(coordinate.getLon());
		int delta = 24 - str.length();
		
		/*
		 * do my best to center the string ;-)
		 */
		if (delta > 0) {
			for (int i = 0; i < delta; i++) {
				if (i % 2 == 0) {
					str = " "+ str;
				}
				else {
					str = str +" ";
				}
			}
		}
		
		this.graphics.drawString(str, 0, this.height - this.descent - 2);
		
		this.observer.repaint();
	}

	/* (non-Javadoc)
	 * @see de.ifgi.lehre.thesisRieke.javamk.gui.map.MapOverlay#draw(java.awt.Graphics2D)
	 */
	@Override
	public void draw(Graphics2D g2d) {
		super.draw(g2d);
	}

	/**
	 * @return the currentPosition
	 */
	public Coordinate getCurrentPosition() {
		return this.currentPosition;
	}
	
	

}
