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

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import org.n52.ifgicopter.spf.xml.PluginMetadata;


/**
 * This dialog shows and saves a Metadata description.
 * 
 * @author Matthes Rieke <m.rieke@uni-muenster.de>
 *
 */
public class InputMetadataDialog extends JDialog {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	JTextField nameField;
	JTextField uniqueField;
	JTextField contactField;
	JCheckBox mobileBox;
	JTextField posField;
	PluginMetadata metadata;

	/**
	 * @param metas the plugin metadata
	 * @param frame the owner
	 */
	public InputMetadataDialog(PluginMetadata metas, JFrame frame) {
		this.metadata = metas;
		
		JPanel newPanel = new JPanel();
		GridLayout gl = new GridLayout(0, 2);
		gl.setHgap(5);
		gl.setVgap(5);
		newPanel.setLayout(gl);
		newPanel.setBorder(new EmptyBorder(2, 2, 2, 2));
		
		this.nameField = new JTextField(this.metadata.getName());
		newPanel.add(new JLabel("Name"));
		newPanel.add(this.nameField);
		
		this.uniqueField = new JTextField(this.metadata.getUniqueID());
		newPanel.add(new JLabel("uniqueID"));
		newPanel.add(this.uniqueField);
		
		this.contactField = new JTextField(this.metadata.getContactEmail());
		newPanel.add(new JLabel("contact email address"));
		newPanel.add(this.contactField);
		
		this.mobileBox = new JCheckBox();
		this.mobileBox.setSelected(this.metadata.isMobile());
		newPanel.add(new JLabel("is mobile platform?"));
		newPanel.add(this.mobileBox);
		
		this.posField = new JTextField(this.metadata.getPosition());
		newPanel.add(new JLabel("position (e.g. \"52.0 7.1\")"));
		newPanel.add(this.posField);
		
		JButton canc = new JButton("Cancel");
		canc.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setVisible(false);
			}
		});
		newPanel.add(canc);
		
		JButton ok = new JButton("OK");
		ok.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				/*
				 * save the data
				 */
				InputMetadataDialog.this.metadata.setContactEmail(InputMetadataDialog.this.contactField.getText());
				InputMetadataDialog.this.metadata.setName(InputMetadataDialog.this.nameField.getText());
				InputMetadataDialog.this.metadata.setUniqueID(InputMetadataDialog.this.uniqueField.getText());
				InputMetadataDialog.this.metadata.setPosition(InputMetadataDialog.this.posField.getText());
				InputMetadataDialog.this.metadata.setMobile(InputMetadataDialog.this.mobileBox.isSelected());
				setVisible(false);
			}
		});
		newPanel.add(ok);
		newPanel.setBorder(new EmptyBorder(2, 2, 2, 2));
		
		this.setTitle("Plugin Metadata");
		this.getContentPane().add(newPanel);
		this.pack();
		this.setLocationRelativeTo(frame);
	}

}
