package org.n52.ifgicopter.spf.input.mk.gui.common;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;

import javax.swing.border.Border;

/**
 * Custom fancy top-rounded {@link Border} implementation.
 * 
 * @author Matthes Rieke <m.rieke@uni-muenster.de>
 *
 */
public class TopRoundedBorder implements Border {

	private final Color bgColor;
	private final Color borderColor;
	private final Color fgColor;
	private final int radius;
	private final int stroke;

	/**
	 * @param background color outside of the (out)line
	 * @param outline actual color of the border
	 * @param foreground color inside the (out)line. should be the same as the
	 * 		parents background color
	 * @param radius the radius of a rounded corner
	 * @param stroke line width of the border
	 */
	public TopRoundedBorder(Color background, Color outline, Color foreground, int radius, int stroke) {
		this.bgColor = background;
		this.borderColor = outline;
		this.fgColor = foreground;
		this.radius = radius;
		this.stroke = stroke;
	}

	/* (non-Javadoc)
	 * @see javax.swing.border.Border#paintBorder(java.awt.Component, java.awt.Graphics, int, int, int, int)
	 */
	public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
		Graphics2D g2d = (Graphics2D) g.create();
		g2d.getRenderingHints().put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		int dia = radius * 2;

		// background in corners
		g2d.setColor(bgColor);
		g2d.fillRect(0,              0,               radius, radius);
		g2d.fillRect(width - radius, 0,               radius, radius);
		g2d.fillRect(0,              height - radius, radius, radius);
		g2d.fillRect(width - radius, height - radius, radius, radius);

		// fill corners
		int arcLeft = 0;
		int arcTop = 0;
		int arcRight = width - dia - 1;
		int arcBottom = height - dia - 1;
		int arcDia = dia;
		
		int sideWidth = width - dia;
		int sideHeight = height - dia;

		g2d.setColor(fgColor);
		g2d.fillArc(arcLeft,  arcTop,    arcDia, arcDia, 90, 90);
		g2d.fillArc(arcRight, arcTop,    arcDia, arcDia, 0, 90);
		g2d.fillArc(arcLeft,  arcBottom, arcDia, arcDia, 180, 90);
		g2d.fillArc(arcRight, arcBottom, arcDia, arcDia, 270, 90);

		// fill sides
		g2d.fillRect(radius,         0,               sideWidth, radius);
		g2d.fillRect(radius,         height - radius, sideWidth, radius);
		g2d.fillRect(0,              radius,          radius,    sideHeight);
		g2d.fillRect(width - radius, radius,          radius,    sideHeight);

		// prepare the arc lines
		if(stroke > 0) {
			g2d.setColor(borderColor);
			g2d.setStroke(new BasicStroke(stroke));
			int halfStroke = (stroke) / 2;

			// stroke corners
			int strokeDia = dia - stroke;
			int leftStroke = halfStroke;
			int rightStroke = width - dia + halfStroke;
			int topStroke = halfStroke;
			
			g2d.drawArc(leftStroke,  topStroke,    strokeDia, strokeDia, 90, 90);
			g2d.drawArc(rightStroke, topStroke,    strokeDia, strokeDia, 0,  90);

			// stroke sides
			int sideBottom = height - stroke;
			int sideTop = 0;
			int sideLeft = 0;
			int sideRight = width - stroke;
			
			g2d.fillRect(radius,    sideTop,    sideWidth, stroke);
			g2d.fillRect(0,    sideBottom, sideWidth+dia, stroke);
			g2d.fillRect(sideLeft,  radius,     stroke,    sideHeight+radius);
			g2d.fillRect(sideRight, radius,     stroke,    sideHeight+radius);
		}
	}

	/* (non-Javadoc)
	 * @see javax.swing.border.Border#getBorderInsets(java.awt.Component)
	 */
	public Insets getBorderInsets(Component c) {
		return new Insets(radius, radius, radius, radius);
	}

	/* (non-Javadoc)
	 * @see javax.swing.border.Border#isBorderOpaque()
	 */
	public boolean isBorderOpaque() {
		return false;
	}

}

