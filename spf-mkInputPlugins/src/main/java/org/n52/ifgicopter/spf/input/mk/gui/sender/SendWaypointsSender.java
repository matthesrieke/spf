package org.n52.ifgicopter.spf.input.mk.gui.sender;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import org.openstreetmap.gui.jmapviewer.interfaces.MapMarker;

import org.n52.ifgicopter.javamk.MKDataHandler;
import org.n52.ifgicopter.javamk.outgoing.SendWaypointCommand;
import org.n52.ifgicopter.javamk.sender.WaypointsSender;
import org.n52.ifgicopter.spf.input.mk.gui.JavaMKMainPanel;
import org.n52.ifgicopter.spf.input.mk.gui.MapMarkerWrapper;
import org.n52.ifgicopter.spf.input.mk.gui.common.JMKOConfig;

/**
 * Class implementing {@link ActionListener} to start
 * sending all waypoints set on the map.
 * 
 * @author Matthes Rieke <m.rieke@uni-muenster.de>
 *
 */
public class SendWaypointsSender implements ActionListener {

	private JavaMKMainPanel mainFrame;

	private static SendWaypointsSender _instance;

	private SendWaypointsSender(JavaMKMainPanel mainFrame) {
		this.mainFrame = mainFrame;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		JMKOConfig config = JMKOConfig.getInstance();
		
		List<MapMarker> waypoints = null;
		waypoints = new ArrayList<MapMarker>(mainFrame.getGpsPanel().getWaypointMarkers());

		if (waypoints == null || waypoints.size() == 0) return;

		MKDataHandler handler = mainFrame.getDataHandler();

		double targetHeight;
		Double heightValue = config.getDoubleValue("NC_ALTITUDE");
		if (heightValue == null) heightValue = 15.0
		;
		if (config.getValue("NC_ALT_MODE").equals("ADD")) {
			targetHeight = mainFrame.getMenu().getKopterHeight()*1E-3 + heightValue;
		}
		else {
			targetHeight = heightValue;
		}

		SendWaypointCommand[] wpCommands = new SendWaypointCommand[waypoints.size()];

		Integer tolerance = config.getIntegerValue("NC_TOLERANCE_RADIUS");
		Integer holdTime = config.getIntegerValue("NC_HOLD_TIME");
		
		if (tolerance == null) tolerance = 5;
		if (holdTime == null) holdTime = 5;
		
		MapMarker m;
		for (int i = 0; i < wpCommands.length; i++) {
			m = waypoints.get(i);

			wpCommands[i] = new SendWaypointCommand(m.getLon(), m.getLat(),
					m instanceof MapMarkerWrapper ? (Double) ((MapMarkerWrapper) m).getMetaField(MapMarkerWrapper.ALTITUDE) : targetHeight,
					tolerance, holdTime, i+1);
		}
		new WaypointsSender(wpCommands, handler).start();


	}


	/**
	 * @param frame mainFrame holding the data handler and the map
	 * @return the singleton instance
	 */
	public static synchronized SendWaypointsSender getInstance(JavaMKMainPanel frame) {
		if (_instance == null) {
			_instance = new SendWaypointsSender(frame);
		}
		return _instance;
	}


}