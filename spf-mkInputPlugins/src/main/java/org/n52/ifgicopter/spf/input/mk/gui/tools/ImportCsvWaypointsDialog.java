package org.n52.ifgicopter.spf.input.mk.gui.tools;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;


import org.n52.ifgicopter.spf.input.mk.gui.GPSPanel;
import org.n52.ifgicopter.spf.input.mk.gui.MapMarkerWrapper;
import org.openstreetmap.gui.jmapviewer.interfaces.MapMarker;

/**
 * Extending {@link JDialog} having the capability
 * of decoding a raw command to an int-array.
 * 
 * @author Matthes Rieke <m.rieke@uni-muenster.de>
 *
 */
public class ImportCsvWaypointsDialog extends JDialog {

	private static final long serialVersionUID = 1L;
	private JTextArea input;
	private GPSPanel gpsPanel;
	private JButton importButton;


	/**
	 * @param gpsPanel the parent component
	 * @param frame the parent
	 */
	public ImportCsvWaypointsDialog(GPSPanel gpsPanel, Frame frame) {
		super(frame);
		this.gpsPanel = gpsPanel;
		this.setModal(true);
		this.setTitle("Import Waypoints");

		input = new JTextArea() {
			private static final long serialVersionUID = 1L;

			@Override
			public boolean getScrollableTracksViewportWidth() {
				return false;
			}
		};
		input.setBackground(this.getBackground());

		importButton = new JButton("Import");
		importButton.addActionListener(new ImportWaypointsAction());

		this.setLayout(new BorderLayout());

		JScrollPane scroll = new JScrollPane(this.input);
		scroll.setPreferredSize(new Dimension(400, 300));
		scroll.setBackground(Color.white);

		this.add(scroll);
		this.add(importButton, BorderLayout.SOUTH);

		this.pack();
		this.setLocationRelativeTo(gpsPanel);
		this.setVisible(true);
	}


	private class ImportWaypointsAction implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			importButton.setEnabled(false);
			importButton.setText("importing ...");
			String text = input.getText();
			String[] lines;
			if (text.contains(System.getProperty("line.separator"))) {
				lines = input.getText().split(System.getProperty("line.separator"));
			} else if (text.contains("\n\r")) {
				lines = input.getText().split("\n\r");
			} else if (text.contains("\n")) {
				lines = input.getText().split("\n");
			} else {
				return;
			}

			List<MapMarker> wps = new ArrayList<MapMarker>();
			String[] values;
			for (String l : lines) {
				values = l.split(";");
				if (values.length != 3) continue;

				/*
				 * lon; lat; alt
				 */
				double lon = Double.parseDouble(values[0]);
				double lat = Double.parseDouble(values[1]);
				double alt = Double.parseDouble(values[2]);

				MapMarkerWrapper marker = new MapMarkerWrapper(lat, lon);
				marker.addMetaField(MapMarkerWrapper.ALTITUDE, alt);
				wps.add(marker);

			}
			gpsPanel.setWaypointMarkers(wps);

			ImportCsvWaypointsDialog.this.setVisible(false);
			if (wps.size() > 0) gpsPanel.centerOnPosition(wps.get(0));
		}

	}
}
