package org.n52.ifgicopter.spf.input.mk.gui.common;

import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.n52.ifgicopter.javamk.MKConstants;

/**
 * @author Matthes Rieke <m.rieke@uni-muenster.de>
 *
 */
public class JPropertiesDialog extends JDialog {
	
	private static final Log log = LogFactory.getLog(MKConstants.LOGGER_ID);
	private static final long serialVersionUID = 1L;

	private Object[][] tabbedProperties;
	private int tabCount;
	private String[] prefixes;
	private Properties properties;

	private List<PropertyRow> propertyRows = new ArrayList<PropertyRow>();

	private String fileName;

	private int maxCount;

	/**
	 * Creates a dialog representation of the
	 * given Properties.
	 * if prefixes is not null it tries to split
	 * the properties using
	 * the prefixes and group them to a JTabbedPane.
	 * 
	 * @param props the properties
	 * @param pref prefixes used for this properties
	 * @param fileName the path to the used file
	 * @param parent the parent frame
	 */
	public JPropertiesDialog(Properties props, String[] pref, String fileName, Frame parent) {
		super(parent);
		this.prefixes = pref;
		this.properties = props;
		this.fileName = fileName;
		
		Set<Object> keys = new HashSet<Object>(props.keySet());
		
		if (prefixes != null && prefixes.length > 0) {
			tabbedProperties = new Object[prefixes.length + 1][]; 
			
			List<Object> tmp = new ArrayList<Object>();
			Set<Object> toRemove = new HashSet<Object>();
			for (int i = 0; i < prefixes.length; i++) {
				for (Object key : keys) {
					if (key.toString().startsWith(prefixes[i])) {
						tmp.add(key.toString());
						toRemove.add(key);
					}
				}
				keys.removeAll(toRemove);
				
				tabbedProperties[i] = tmp.toArray();
				
				if (tmp.size() > maxCount) {
					maxCount = tmp.size();
				}
				
				tmp.clear();
			}
			
			if (!keys.isEmpty()) {
				tabbedProperties[tabbedProperties.length - 1] = keys.toArray();
			}
			
			initDialog();
		}
	}

	private void initDialog() {
		if (tabbedProperties[tabbedProperties.length - 1] != null) {
			tabCount = tabbedProperties.length;
		}
		else {
			tabCount = tabbedProperties.length - 1;
		}
		
		this.setModal(true);
		this.setResizable(false);
		this.setTitle("JavaMKOperator settings");
		
		/*
		 * the tabs
		 */
		JTabbedPane tabs = new JTabbedPane(JTabbedPane.TOP);
		
		for (int i = 0; i < tabCount; i++) {
			JPanel newPanel = new JPanel();
			GridLayout gl = new GridLayout(0, 2);
			gl.setHgap(5);
			gl.setVgap(5);
			newPanel.setLayout(gl);
			newPanel.setBorder(new EmptyBorder(2, 2, 2, 2));
			
			for (int j = 0; j < tabbedProperties[i].length; j++) {
				Object key = tabbedProperties[i][j];
				String value = this.properties.getProperty(key.toString());
				PropertyRow pr = new PropertyRow(key.toString(), value);
				newPanel.add(pr.getKeyLabel());
				newPanel.add(pr.getValueField());
				this.propertyRows.add(pr);
				
			}
			
			int delta = maxCount - tabbedProperties[i].length;
			for (int j = 0; j < delta; j++) {
				newPanel.add(new JLabel());
				newPanel.add(new JLabel());
			}
			
			
			if (prefixes.length >= i + 1) {
				tabs.addTab(prefixes[i], newPanel);
			}
			else {
				tabs.addTab("Other", newPanel);
			}
		}
		
		this.getContentPane().setLayout(new BoxLayout(this.getContentPane(), BoxLayout.Y_AXIS));
		
		this.getContentPane().add(tabs);
		
		/*
		 * buttons
		 */
		ExitDialogAction exitAction = new ExitDialogAction();
		
		JPanel buttons = new JPanel();
		
		JButton save = new JButton("Save");
		save.addActionListener(exitAction);
		save.setActionCommand("save");
		
		buttons.add(save);
		
		JButton cancel = new JButton("Cancel");
		cancel.addActionListener(exitAction);
		cancel.setActionCommand("cancel");
		
		buttons.add(cancel);
		
		this.getContentPane().add(buttons);
		
		JLabel info = new JLabel("See \""+ fileName +"\" for details.");
		info.getPreferredSize().height = 25;
		this.getContentPane().add(info);
		
		this.pack();
		this.setLocationRelativeTo(null);
		this.setVisible(true);
	}
	
	
	private class PropertyRow extends JPanel {
		
		private static final long serialVersionUID = 1L;

		private String key;
		private JTextField valueField;
		private JLabel keyLabel;
		
		public PropertyRow(String key, String value) {
			super();
			
			FlowLayout fl = new FlowLayout(FlowLayout.LEFT, 2, 2);
			this.setLayout(fl);
			
			this.keyLabel = new JLabel(key);
			this.add(this.keyLabel);
			this.valueField = new JTextField(value);
			this.add(this.valueField);
			this.key = key;

		}

		/**
		 * @return the key
		 */
		public String getKey() {
			return key;
		}

		/**
		 * @return the valueField
		 */
		public String getValue() {
			return valueField.getText();
		}

		/**
		 * @return the valueField
		 */
		public JTextField getValueField() {
			return valueField;
		}

		/**
		 * @return the keyLabel
		 */
		public JLabel getKeyLabel() {
			return keyLabel;
		}
		
		
	}
	
	private class ExitDialogAction implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			if (e.getActionCommand().equals("save")) {
				/*
				 * save all properties
				 */
				for (PropertyRow pr : propertyRows) {
					properties.setProperty(pr.getKey(), pr.getValue());
				}
				
				try {
					JMKOConfig.getInstance().storeProperties();
				} catch (FileNotFoundException e1) {
					log.warn(e1.getMessage(), e1);
				} catch (IOException e1) {
					log.warn(e1.getMessage(), e1);
				}
				
			}
			
			setVisible(false);
			dispose();
		}
		
	}

}
