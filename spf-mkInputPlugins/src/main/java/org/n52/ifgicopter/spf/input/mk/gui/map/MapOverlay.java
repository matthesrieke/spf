package org.n52.ifgicopter.spf.input.mk.gui.map;

import java.awt.Canvas;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

import javax.swing.JComponent;

/**
 * General Overlay to be displayed on a {@link JMKOMap} instance.
 * 
 * @author Matthes Rieke <m.rieke@uni-muenster.de>
 *
 */
public class MapOverlay extends Canvas {
	
	private static final long serialVersionUID = 1L;

	/**
	 * orientation
	 */
	public static final int TOP_RIGHT = 1;
	
	/**
	 * orientation
	 */
	public static final int BOTTOM_RIGHT = 3;
	
	/**
	 * orientation
	 */
	public static final int BOTTOM_CENTER = 5;
	
	protected BufferedImage overlay;
	protected JComponent observer;
	protected double rotation = 0.0;
	protected int width;
	protected int height;

	private Point pos;

	private int posType;
	
	/**
	 * @param i orientation
	 * @param observ the map
	 */
	public MapOverlay(int i, JMKOMap observ) {
		this.observer = (JComponent) observ;
		this.posType = i;
	}
	
	/**
	 * @param i orientation
	 * @param overlay the image to overlay
	 * @param observ the map
	 */
	public MapOverlay(int i, BufferedImage overlay, JMKOMap observ) {
		this.overlay = overlay;
		this.observer = (JComponent) observ;
		this.posType = i;
		this.width = this.overlay.getWidth(this);
		this.height = this.overlay.getHeight(this);
	}
	
	/**
	 * draws this overlay to the given graphics
	 * 
	 * @param g2d the graphics
	 */
	public void draw(Graphics2D g2d) {
		if (this.overlay == null) return;
		
		switch (posType) {
		case TOP_RIGHT:
			this.pos = new Point(this.observer.getWidth() -
					this.width - 5, 5);
			break;
		case BOTTOM_RIGHT:
			this.pos = new Point(this.observer.getWidth() -
					this.width - 5, this.observer.getHeight() - this.height - 5);
			break;
		case BOTTOM_CENTER:
			this.pos = new Point(this.observer.getWidth() / 2 -
					this.width / 2, this.observer.getHeight() - this.height - 5);
			break;
		default:
			this.pos = new Point(5, 5);
			break;
		}
		
		AffineTransform oldAT = null;
		if (this.rotation != 0.0) {
			 oldAT = g2d.getTransform();
			g2d.rotate(Math.toRadians(this.rotation/10), this.pos.x+this.width/2, this.pos.y+this.height/2);
		}
		
		g2d.drawImage(this.overlay, this.pos.x, this.pos.y, this);
		
		if (this.rotation != 0.0) {
			g2d.setTransform(oldAT);
		}
	}

	/**
	 * used to rotate the overlay
	 * 
	 * @param rot the value in degree*10
	 */
	public void setRotation(double rot) {
		this.rotation = rot;
	}

	/**
	 * @param overlay the overlay to set
	 */
	public void setOverlay(BufferedImage overlay) {
		this.overlay = overlay;
		this.width = this.overlay.getWidth(this.observer);
		this.height = this.overlay.getHeight(this.observer);
	}
	
	

}
