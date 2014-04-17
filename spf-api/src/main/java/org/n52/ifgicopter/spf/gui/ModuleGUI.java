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

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JPanel;

import org.apache.commons.logging.LogFactory;
import org.n52.ifgicopter.spf.common.IModule;
import org.n52.ifgicopter.spf.input.IInputPlugin;


/**
 * A simple GUI Pojo for an Input- or Output-Plugin.
 * 
 * @author Matthes Rieke <m.rieke@uni-muenster.de>
 *
 */
public class ModuleGUI {
	
	static ImageIcon RED_IMAGE;
	static ImageIcon GREEN_IMAGE;
	static ImageIcon BLUE_IMAGE;
	
	static {
		try {
            InputStream in = null;
			File f = new File("img/point_red.png");
			if (!f.exists()) {
				in = ModuleGUI.class.getResourceAsStream("/img/point_red.png");
			}else {
				in = new FileInputStream(f);
			}
			
			RED_IMAGE = new ImageIcon(ImageIO.read(in));
            
			f = new File("img/point_blue.png");
			if (!f.exists()) {
				in = ModuleGUI.class.getResourceAsStream("/img/point_blue.png");
			}else {
				in = new FileInputStream(f);
			}
			
			BLUE_IMAGE = new ImageIcon(ImageIO.read(in));
			
			f = new File("img/point_green.png");
			if (!f.exists()) {
				in = ModuleGUI.class.getResourceAsStream("/img/point_green.png");
			}else {
				in = new FileInputStream(f);
			}
			
			GREEN_IMAGE = new ImageIcon(ImageIO.read(in));
        }
        catch (Exception e) {
            LogFactory.getLog(ModuleGUI.class).warn(e.getMessage(), e);
        }

	}

	private JPanel gui;
	
	private JMenu menu;

	/**
	 * @return the gui
	 */
	public JPanel getGui() {
		return this.gui;
	}

	/**
	 * @param gui the gui to set
	 */
	public void setGui(JPanel gui) {
		this.gui = gui;
	}

	/**
	 * @return the menu
	 */
	public JMenu getMenu() {
		return this.menu;
	}

	/**
	 * @param menu the menu to set
	 */
	public void setMenu(JMenu menu) {
		this.menu = menu;
	}
	
	/**
	 * A simple implementation.
	 * 
	 * @author Matthes Rieke <m.rieke@uni-muenster.de>
	 *
	 */
	public class SimpleInputPluginGUI extends JPanel {


		private JLabel status;

		/**
		 * default constructor creates a simple gui
		 * 
		 * @param ip the input plugin
		 */
		public SimpleInputPluginGUI(IInputPlugin ip) {
			this.add(new JLabel(ip.getName() +": "));
			this.status = new JLabel(ip.getStatusString());
			this.status.setIcon(GREEN_IMAGE);
			this.add(this.status);
		}
		
		/**
		 * @param text the new status text
		 * @param status the new status
		 */
		public void setStatusText(String text, int status) {
			if (status == IModule.STATUS_NOT_RUNNING) {
				this.status.setIcon(RED_IMAGE);
			}
			else {
				this.status.setIcon(GREEN_IMAGE);
			}
			this.status.setText(text);
		}

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		
	}

}
