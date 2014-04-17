package org.n52.ifgicopter.spf.input.mk.gui;

import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.text.DecimalFormat;

import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.n52.ifgicopter.javamk.MKConstants;
import org.n52.ifgicopter.javamk.MKDataHandler;
import org.n52.ifgicopter.javamk.incoming.FollowMeData;
import org.n52.ifgicopter.javamk.incoming.OSDData;
import org.n52.ifgicopter.javamk.outgoing.RedirectUARTCommand;
import org.n52.ifgicopter.spf.input.mk.gui.common.ActionDelegationManager;
import org.n52.ifgicopter.spf.input.mk.gui.common.ActionDelegationReceiver;
import org.n52.ifgicopter.spf.input.mk.gui.listener.IPositionUpdateListener;
import org.n52.ifgicopter.spf.input.mk.gui.sender.GetVersionSender;
import org.n52.ifgicopter.spf.input.mk.gui.sender.RequestDebugSender;
import org.n52.ifgicopter.spf.input.mk.gui.sender.RequestOSDThread;

/**
 * @author Matthes Rieke <m.rieke@uni-muenster.de>
 *
 */
public class IconBar extends JPanel implements ActionDelegationReceiver, IPositionUpdateListener {

	
	private static final long serialVersionUID = 1L;
	private static final Log log = LogFactory.getLog(MKConstants.LOGGER_ID);
	private JToggleButton debugSwitch;
	private JToggleButton gpsSwitch;
	private JToggleButton centerOnMKPos;
	private DecimalFormat degreeFormat = new DecimalFormat("#0.00000");
	private JLabel latLabel = new JLabel(formatDouble(0.0));
	private JLabel lonLabel = new JLabel(formatDouble(99.99999));
	private JLabel altLabel = new JLabel(formatDouble(-120.0));
	private JToggleButton newWaypointButton;
	private JToggleButton sendTargetButton;
	private GPSPanel gpsPanel;

	/**
	 * @param dataHandler mk comm
	 * @param gps the {@link GPSPanel} instance
	 */
	public IconBar(final MKDataHandler dataHandler, GPSPanel gps) {
		this.gpsPanel = gps;
		/*
		 * register at action deligator to receive debug switches from the menu
		 */
		ActionDelegationManager.getInstance().registerForAction(ActionDelegationManager.DEBUG_SWITCH_ACTION, this);
		ActionDelegationManager.getInstance().registerForAction(ActionDelegationManager.GPS_SWITCH_ACTION, this);
		
		JButton naviCtrlButton = new JButton("NC");
		naviCtrlButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				dataHandler.resetUARTtoNaviCtrl();
				try {
					GetVersionSender.getInstance().perform();
				} catch (Exception e1) {
					log.warn(e1.getMessage(), e1);
				}
			}
		});
		
		JButton flightCtrlButton = new JButton("FC");
		flightCtrlButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				dataHandler.sendCommand(new RedirectUARTCommand(0));
				try {
					GetVersionSender.getInstance().perform();
				} catch (Exception e1) {
					log.warn(e1.getMessage(), e1);
				}
			}
		});
		
		JButton mk3MagButton = new JButton("MK3");
		mk3MagButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				dataHandler.sendCommand(new RedirectUARTCommand(1));
				try {
					GetVersionSender.getInstance().perform();
				} catch (Exception e1) {
					log.warn(e1.getMessage(), e1);
				}
			}
		});
		
		JButton mkgGpsButton = new JButton("MKGPS");
		mkgGpsButton.setEnabled(false);
		mkgGpsButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				dataHandler.sendCommand(new RedirectUARTCommand(2));
				try {
					GetVersionSender.getInstance().perform();
				} catch (Exception e1) {
					log.warn(e1.getMessage(), e1);
				}
			}
		});
		
		debugSwitch = null;
		try {
			debugSwitch = new JToggleButton(new ImageIcon(ImageIO.read(getClass().getResourceAsStream("debug.png"))));
		} catch (IOException e1) {
			log.warn(e1.getMessage(), e1);
			debugSwitch = new JToggleButton("Debug");
		}
		debugSwitch.setSelected(false);
		debugSwitch.setToolTipText("Toggle request of Debug data from the MK");
		debugSwitch.addActionListener(RequestDebugSender.getInstance(dataHandler));
		
		gpsSwitch = null;
		try {
			gpsSwitch = new JToggleButton(new ImageIcon(ImageIO.read(getClass().getResourceAsStream("gps.png"))));
		} catch (IOException e1) {
			log.warn(e1.getMessage(), e1);
			gpsSwitch = new JToggleButton("GPS");
		}
		gpsSwitch.setSelected(false);
		gpsSwitch.setToolTipText("Toggle request of position data from the MK");
		gpsSwitch.addActionListener(RequestOSDThread.getInstance(dataHandler));

		((FlowLayout)this.getLayout()).setAlignment(FlowLayout.LEFT);
		
		this.add(debugSwitch);
		this.add(gpsSwitch);
		
		/*
		 * currently not needed
		 *
		this.add(naviCtrlButton);
		this.add(flightCtrlButton);
		this.add(mk3MagButton);
		this.add(mkgGpsButton);
		 *
		 * 
		 */
		
		/*
		 * those buttons moved here from GPSPanel
		 */
		try {
			centerOnMKPos = new JToggleButton(new ImageIcon(ImageIO.read(getClass().getResourceAsStream("center-mk.png"))));
		} catch (IOException e1) {
			log.warn(e1.getMessage(), e1);
			centerOnMKPos = new JCheckBox("Center MK");
		}
		
		centerOnMKPos.setToolTipText("Toggle centering on Mikrokopter when new position data arrives");
		centerOnMKPos.setSelected(true);

		JPanel coordPanel = new JPanel(new FlowLayout());
		coordPanel.add(centerOnMKPos);
		Font f = new Font(Font.MONOSPACED, Font.PLAIN, 12);
		lonLabel.setFont(f);
		latLabel.setFont(f);
		altLabel.setFont(f);

		coordPanel.add(new JLabel("|"));
		coordPanel.add(this.latLabel);
		coordPanel.add(new JLabel("|"));
		coordPanel.add(this.lonLabel);
		coordPanel.add(new JLabel("|"));
		coordPanel.add(this.altLabel);
		coordPanel.add(new JLabel("|"));

		try {
			newWaypointButton = new JToggleButton(new ImageIcon(ImageIO.read(getClass().getResourceAsStream("wp-add.png"))));
		} catch (IOException e1) {
			log.warn(e1.getMessage(), e1);
			newWaypointButton = new JToggleButton("Add WP");
		}
		newWaypointButton.setSelected(true);
		newWaypointButton.setToolTipText("Toggle adding a new waypoint when clicking on the map");

		JButton clearWaypoints;
		try {
			clearWaypoints = new JButton(new ImageIcon(ImageIO.read(getClass().getResourceAsStream("wp-clear.png"))));
		} catch (IOException e1) {
			log.warn(e1.getMessage(), e1);
			clearWaypoints = new JButton("clear WPs on map");
		}

		clearWaypoints.setToolTipText("Clear all waypoints on the map (does not clear on MK)");
		clearWaypoints.addActionListener(gpsPanel.new ClearWaypointsAction());
		clearWaypoints.setActionCommand("local");

		try {
			sendTargetButton = new JToggleButton(new ImageIcon(ImageIO.read(getClass().getResourceAsStream("wp-target.png"))));
		} catch (IOException e1) {
			log.warn(e1.getMessage(), e1);
			sendTargetButton = new JToggleButton("Send Target");
		}
		sendTargetButton.setToolTipText("Toggle setting a new target waypoint when clicking on the map");

		ButtonGroup mapButtonGroup = new ButtonGroup();
		mapButtonGroup.add(newWaypointButton);
		mapButtonGroup.add(sendTargetButton);

		coordPanel.add(newWaypointButton);
		coordPanel.add(sendTargetButton);		
		coordPanel.add(clearWaypoints);
		
		this.add(coordPanel);
	}
	
	/**
	 * @param enabled the state of the debug switch
	 */
	public void switchDebugData(boolean enabled) {
		this.debugSwitch.setSelected(enabled);
	}
	
	/**
	 * @param enabled the state of the gps switch
	 */
	public void switchGPSData(boolean enabled) {
		this.gpsSwitch.setSelected(enabled);
	}

	@Override
	public void receiveAction(ActionEvent e) {
		if (e.getActionCommand().equals(ActionDelegationManager.DEBUG_SWITCH_ACTION) && !e.getSource().equals(debugSwitch)) {
			this.debugSwitch.setSelected(!this.debugSwitch.isSelected());
		}
		else if (e.getActionCommand().equals(ActionDelegationManager.GPS_SWITCH_ACTION) && !e.getSource().equals(gpsSwitch)) {
			this.gpsSwitch.setSelected(!this.gpsSwitch.isSelected());
		}
	}
	
	
	private String formatDouble(double d) {
		String str = degreeFormat.format(d);

		int delta = 10 - str.length();
		if (delta > 0) {

			String add = " ";
			for (int i = 1; i < delta; i++) {
				add += " ";
			}

			str = add + str;
		}
		return str;
	}
	
	
	/*
	 * 
	 * 
	 * methods moved here from GPSPanel
	 * 
	 * 
	 */
	
	/**
	 * @param currpos 3-element array (lon, lat, alt)
	 * @param sysTime the system time when the update occured
	 */
	private void updateGPSData(double[] currpos) {
		this.lonLabel.setText(formatDouble(currpos[0]));
		this.latLabel.setText(formatDouble(currpos[1]));
		this.altLabel.setText(formatDouble(currpos[2]));

	}


	/**
	 * @param recv new OSD data from NaviCtrl
	 */
	public void processOSDData(OSDData recv) {
		double[] currpos = recv.getNaviData().getCurrentPosition().getAsDegrees();
		updateGPSData(currpos);
	}



	/**
	 * @param recv the response received from MK.
	 */
	public void processFollowMeData(FollowMeData recv) {
		double[] currpos = recv.getWaypoint().getPosition().getAsDegrees();
		updateGPSData(currpos);
	}

	/**
	 * @return the switchbutton for centering the map on incoming position data
	 */
	public AbstractButton getCenterOnMKPos() {
		return this.centerOnMKPos;
	}

	/**
	 * @return the switchbutton for adding a new waypoint
	 */
	public AbstractButton getNewWaypointButton() {
		return this.newWaypointButton;
	}

	/**
	 * @return the switchbutton for creating a sendtarget command
	 */
	public AbstractButton getSendTargetButton() {
		return this.sendTargetButton;
	}

}
