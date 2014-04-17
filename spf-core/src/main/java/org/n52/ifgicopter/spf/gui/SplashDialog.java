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

package org.n52.ifgicopter.spf.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Point;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.n52.ifgicopter.spf.gui.map.ImageMapMarker;



/**
 * The splash dialog is displayed while
 * loading the program.
 * 
 * @author Matthes Rieke <m.rieke@uni-muenster.de>
 *
 */
public class SplashDialog extends JDialog implements Runnable {

	private static final long serialVersionUID = 1L;

	private static final int SPLASH_WIDTH = 200;
	private static final int SPLASH_HEIGHT = 315;
	
	private List<Object> waitObject = new ArrayList<Object>();

	private JProgressBar progress;

	private int finishedProcesses;

	/**
	 * Remove self from screen
	 */
	public void removeSelf() {
		synchronized (this.waitObject) {
			this.waitObject.add(new Object());
			this.waitObject.notifyAll();
		}
	}

	@Override
	public void run() {
		this.setUndecorated(true);
		this.setResizable(false);
		this.setLocationRelativeTo(null);
//		this.setAlwaysOnTop(true);
		
		/*
		 * move to center of screen
		 */
		Point loc = getLocation();
		loc.x -= SPLASH_WIDTH/2;
		loc.y -= SPLASH_HEIGHT/2;
		this.setLocation(loc);
		
		this.progress = new JProgressBar(SwingConstants.HORIZONTAL);
		this.progress.setPreferredSize(new Dimension(200, 15));
		this.progress.setIndeterminate(true);
		
		/*
		 * transparency
		 */
		
		Image img = null;
		try {
			File f = new File("img/splash.png");
			InputStream in;
			if (!f.exists()) {
				in = ImageMapMarker.class.getResourceAsStream("/img/splash.png");
			}else {
				in = new FileInputStream(f);
			}
			
			img = ImageIO.read(in);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		JLabel imgLabel;
		if (img != null) {
			imgLabel = new JLabel(new ImageIcon(img));
		}
		else {
			imgLabel = new JLabel();
		}
	    imgLabel.setPreferredSize(new Dimension(200, 300));
	    imgLabel.setBorder(BorderFactory.createEtchedBorder());
		
		this.getContentPane().add(imgLabel, BorderLayout.CENTER);
		this.getContentPane().add(this.progress, BorderLayout.SOUTH);
		this.pack();
		this.setVisible(true);
		this.repaint();
		
		synchronized (this.waitObject) {
			while (this.waitObject.size() == 0)
				try {
					this.waitObject.wait();
				} catch (InterruptedException e) {
					Logger.getLogger(SplashDialog.class.getName()).log(Level.WARN, null, e);
				}
		}
		
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			Logger.getLogger(SplashDialog.class.getName()).log(Level.WARN, null, e);
		}
		
		this.setVisible(false);
		this.dispose();
	}
	
	/**
	 * call this method if the application can determine how many
	 * processes need to load.
	 * 
	 * @param count the number of processes
	 */
	public void activateProgressBar(int count) {
		this.progress.setIndeterminate(false);
		this.progress.setMaximum(count);
		this.finishedProcesses = 0;
	}
	
	/**
	 * call this if a process has finished loading
	 * -> progress bar progresses
	 */
	public void processFinished() {
		this.progress.setValue(++this.finishedProcesses);
	}
	
}
