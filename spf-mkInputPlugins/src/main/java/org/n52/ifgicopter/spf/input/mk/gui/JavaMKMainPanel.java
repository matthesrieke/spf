package org.n52.ifgicopter.spf.input.mk.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.LineBorder;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.n52.ifgicopter.javamk.CommandReceive;
import org.n52.ifgicopter.javamk.MKConstants;
import org.n52.ifgicopter.javamk.MKDataHandler;
import org.n52.ifgicopter.javamk.incoming.AnalogValue;
import org.n52.ifgicopter.javamk.incoming.Data3D;
import org.n52.ifgicopter.javamk.incoming.DebugData;
import org.n52.ifgicopter.javamk.incoming.FollowMeData;
import org.n52.ifgicopter.javamk.incoming.NumberOfWaypoints;
import org.n52.ifgicopter.javamk.incoming.OSDData;
import org.n52.ifgicopter.javamk.incoming.RequestWaypointResponse;
import org.n52.ifgicopter.javamk.incoming.SerialLinkEcho;
import org.n52.ifgicopter.javamk.incoming.VersionInfo;
import org.n52.ifgicopter.javamk.listener.ICommandListener;
import org.n52.ifgicopter.spf.input.mk.IfgicopterInputPluginMK;
import org.n52.ifgicopter.spf.input.mk.gui.actions.AnalogValueSender;
import org.n52.ifgicopter.spf.input.mk.gui.listener.IPositionUpdateListener;
import org.n52.ifgicopter.spf.input.mk.gui.sender.GetVersionSender;
import org.n52.ifgicopter.spf.input.mk.gui.sender.RequestDebugSender;
import org.n52.ifgicopter.spf.input.mk.gui.sender.RequestOSDThread;
import org.n52.ifgicopter.spf.input.mk.gui.sender.RequestWaypointsSender;

/**
 * @author Matthes Rieke <m.rieke@uni-muenster.de>
 *
 */
public class JavaMKMainPanel extends JPanel implements ICommandListener {

	private static final long serialVersionUID = 1L;
	
	private MKDataHandler dataHandler;
	private DebugPanel debugPanel;
	private GPSPanel gpsPanel;
	private JLabel statusLabel;
	private ConsolePanel consolePanel;
	private List<IPositionUpdateListener> positionListener = new ArrayList<IPositionUpdateListener>();

	private MainMenu menu;

	private IfgicopterInputPluginMK spfPlugin;

	private IconBar iconBar;

	private static final Log log = LogFactory.getLog(MKConstants.LOGGER_ID);
	


	/**
	 * default constructor
	 * @param mkDataHandler the mk data handler
	 * @param spfPlugin the input plugin
	 */
	public JavaMKMainPanel(MKDataHandler mkDataHandler, IfgicopterInputPluginMK spfPlugin) {
		super();
		
		this.spfPlugin = spfPlugin;
		this.dataHandler = mkDataHandler;
		this.setLayout(new BorderLayout());

		
		/*
		 * mainPanel
		 */
		JPanel mainPanel = new JPanel();
		mainPanel.setBorder(new LineBorder(Color.gray));
		mainPanel.setLayout(new GridBagLayout());
		
		this.debugPanel = new DebugPanel();
		JScrollPane debugScroll = new JScrollPane(this.debugPanel);
		debugScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		debugScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		debugScroll.setMinimumSize(new Dimension(debugScroll.getPreferredSize().width, 40));
		
		this.gpsPanel = new GPSPanel(this);

		this.consolePanel = new ConsolePanel();

		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.gridx = 0;
		c.gridy = 0;
		c.weighty = 0.7;
		mainPanel.add(debugScroll, c);

		c.gridx = 1;
		c.gridy = 0;
		mainPanel.add(this.getGpsPanel(), c);

		c.gridx = 0;
		c.gridy = 1;
		c.gridwidth = 2;
		c.weighty = 0.3;
		c.weightx = 2.0;
		mainPanel.add(this.consolePanel, c);

		JPanel statusBar = new JPanel();
		this.statusLabel = new JLabel("version N/A yet");
		this.statusLabel.setFont(new Font(Font.DIALOG, 0, 12));
//		statusBar.add(this.statusLabel);

		this.iconBar = new IconBar(this.getDataHandler(), gpsPanel);
		
		this.add(iconBar, BorderLayout.NORTH);
		this.add(mainPanel, BorderLayout.CENTER);
		this.add(statusBar, BorderLayout.SOUTH);

		/*
		 * MENU
		 */
		menu = new MainMenu(this.getDataHandler(), this);
//		this.setJMenuBar(menu);


		
//		try {
//			this.setIconImage(ImageIO.read(in));
//		} catch (IOException e) {
//			log.warn(e.getMessage(), e);
//		}
//		
//		this.setLocationRelativeTo(null);
		
		
		/*
		 * init complete - register self
		 * as listener
		 */
		this.getDataHandler().registerCommandListener(this);
		
		this.setVisible(true);
		
		/*
		 * request the version of active UART
		 */
		GetVersionSender.getInstance(this.getDataHandler()).perform();
		
		
		this.positionListener.add(this.gpsPanel);
		this.positionListener.add(this.iconBar);
		
		/*
		 * probably this should be activated in the config
		 */
		RequestDebugSender.getInstance(this.getDataHandler()).actionPerformed(new ActionEvent(this, 0, ""));
		RequestOSDThread.getInstance(this.getDataHandler()).actionPerformed(new ActionEvent(this, 0, ""));
		new AnalogValueSender(dataHandler).actionPerformed(new ActionEvent(this, 0, ""));
	}


	/**
	 * @param l the listener to be added
	 */
	public void addPositionListener(IPositionUpdateListener l) {
		this.positionListener.add(l);
	}

	/**
	 * @param l the listener to be removed
	 */
	public void removePositionListener(IPositionUpdateListener l) {
		this.positionListener.remove(l);
	}

	/**
	 * @return the gps panel holding the map
	 */
	public GPSPanel getGpsPanel() {
		return gpsPanel;
	}


	/**
	 * @return the data handler instance
	 */
	public MKDataHandler getDataHandler() {
		return dataHandler;
	}
	


	/**
	 * @return the menu
	 */
	public MainMenu getMenu() {
		return menu;
	}


	@Override
	public void processCommand(CommandReceive recv) {
		if (recv instanceof DebugData) {
			/*
			 * call the listener
			 */
			this.debugPanel.processDebugData(((DebugData) recv).getAnalogData());
			this.getGpsPanel().processDebugData((DebugData) recv);
		}
		else if (recv instanceof Data3D) {
			this.getGpsPanel().processData3D((Data3D) recv);
		}
		else if (recv instanceof SerialLinkEcho) {
			log.info("Received serial link echo: "+ 
					((SerialLinkEcho) recv).isValid());
		}
		else if (recv instanceof AnalogValue) {
			/*
			 * call the listener
			 */
			this.debugPanel.processAnalogLabel((AnalogValue) recv);
		}
		else if (recv instanceof VersionInfo) {
			this.statusLabel.setText("Active Control Unit: "+ ((VersionInfo) recv).toString());
			GetVersionSender.getInstance(this.getDataHandler()).setRunning(false);
		}
		else if (recv instanceof OSDData) {
			/*
			 * call the listener
			 */
			OSDData osd = (OSDData) recv;
			RequestWaypointsSender.getInstance(getDataHandler()).setCount(
					osd.getNaviData().getWaypointNumber());
			
			for (IPositionUpdateListener l : this.positionListener) {
				l.processOSDData((OSDData) recv);
			}
		}
		else if (recv instanceof FollowMeData) {
			/*
			 * call the listener
			 */
			FollowMeData followMe = (FollowMeData) recv;
			this.menu.setKopterHeight(followMe.getWaypoint().getPosition().getAltitude());
			
			for (IPositionUpdateListener l : this.positionListener) {
				l.processFollowMeData((FollowMeData) recv);
			}
		}
		else if (recv instanceof RequestWaypointResponse) {
			RequestWaypointResponse rwr = (RequestWaypointResponse) recv;
			
			/*
			 * call the listener
			 */
			this.getGpsPanel().processWaypointMarker(rwr);
			RequestWaypointsSender.getInstance(getDataHandler()).setCount(rwr.getWaypointCount());
		} else if (recv instanceof NumberOfWaypoints) {
			NumberOfWaypoints num = (NumberOfWaypoints) recv;
			
			RequestWaypointsSender.getInstance(getDataHandler()).setCount(num.getWaypointCount());
		}
	}



	/**
	 * method gets called when a new communication
	 * layer gets initialised.
	 */
	public void newCommLayerInit() {
		this.spfPlugin.activeNewCommLayer();
	}



	/**
	 * @return the global iconbar
	 */
	public IconBar getIconBar() {
		return this.iconBar;
	}


	

}
