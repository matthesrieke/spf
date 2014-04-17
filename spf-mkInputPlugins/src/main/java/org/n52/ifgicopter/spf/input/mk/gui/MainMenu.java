package org.n52.ifgicopter.spf.input.mk.gui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.filechooser.FileFilter;

import org.openstreetmap.gui.jmapviewer.MemoryTileCache;
import org.openstreetmap.gui.jmapviewer.interfaces.MapMarker;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.n52.ifgicopter.javamk.MKDataHandler;
import org.n52.ifgicopter.javamk.outgoing.SendTargetPositionCommand;
import org.n52.ifgicopter.javamk.outgoing.SendWaypointCommand;
import org.n52.ifgicopter.spf.input.mk.gui.actions.AnalogValueSender;
import org.n52.ifgicopter.spf.input.mk.gui.common.ActionDelegationManager;
import org.n52.ifgicopter.spf.input.mk.gui.common.ActionDelegationReceiver;
import org.n52.ifgicopter.spf.input.mk.gui.common.JMKOConfig;
import org.n52.ifgicopter.spf.input.mk.gui.common.JPropertiesDialog;
import org.n52.ifgicopter.spf.input.mk.gui.map.JMapWithOverlay;
import org.n52.ifgicopter.spf.input.mk.gui.map.OfflineMapWithOverlay;
import org.n52.ifgicopter.spf.input.mk.gui.modes.MissionMode;
import org.n52.ifgicopter.spf.input.mk.gui.sender.Request3DDataSender;
import org.n52.ifgicopter.spf.input.mk.gui.sender.RequestDebugSender;
import org.n52.ifgicopter.spf.input.mk.gui.sender.RequestOSDThread;
import org.n52.ifgicopter.spf.input.mk.gui.sender.RequestWaypointsSender;
import org.n52.ifgicopter.spf.input.mk.gui.sender.SendWaypointsSender;
import org.n52.ifgicopter.spf.input.mk.gui.tools.DecodeCommandDialog;
import org.n52.ifgicopter.spf.input.mk.gui.tools.ImportCsvWaypointsDialog;

/**
 * Class holding the main menu of the JavaMKOperator gui.
 * 
 * @author Matthes Rieke <m.rieke@uni-muenster.de>
 *
 */
public class MainMenu extends JMenu implements ActionDelegationReceiver {

	private static final long serialVersionUID = 1L;
	private static final Log log = LogFactory.getLog(MainMenu.class.getName());

	private MKDataHandler dataHandler;
	private JavaMKMainPanel mainPanel;

	private int kopterHeight;
	private JMenu communicationMenu;
	private JMenu toolsMenu;
	private JMenu mapMenu;
	private JCheckBoxMenuItem getDebug;
	private JCheckBoxMenuItem osdData;

	/**
	 * @param handler the data handler instance
	 * @param mainFrame the main frame that uses this menu
	 */
	public MainMenu(MKDataHandler handler, final JavaMKMainPanel mainFrame) {
		super("Ifgicopter IP");
		
		/*
		 * register at action deligator to receive debug switches from the menu
		 */
		ActionDelegationManager.getInstance().registerForAction(ActionDelegationManager.DEBUG_SWITCH_ACTION, this);
		ActionDelegationManager.getInstance().registerForAction(ActionDelegationManager.GPS_SWITCH_ACTION, this);
		
		this.dataHandler = handler;
		this.mainPanel = mainFrame;
		

		/*
		 * commands menu
		 */
		communicationMenu = new JMenu("Communication");
		
		JMenuItem startComm = new JMenuItem("Initialize communication");
		startComm.addActionListener(new InitCommAction());

		getDebug = new JCheckBoxMenuItem("(switch) GetDebug");
		getDebug.addActionListener(RequestDebugSender.getInstance(dataHandler));
		getDebug.setSelected(false);

		JCheckBoxMenuItem get3D = new JCheckBoxMenuItem("(switch) Request 3D Data");
		get3D.addActionListener(Request3DDataSender.getInstance(dataHandler));
		get3D.setSelected(false);

		JMenuItem sendWP = new JMenuItem("Send waypoints");
		sendWP.addActionListener(SendWaypointsSender.getInstance(this.mainPanel));

		JMenuItem resetWaypoints = new JMenuItem("Reset waypointList");
		resetWaypoints.addActionListener(new ResetWaypointsSender());

		JMenuItem requestWaypoints = new JMenuItem("Request all waypoints");
		requestWaypoints.addActionListener(new RequestWaypointsAction());

		JMenuItem gpsTarget = new JMenuItem("SendTarget command");
		gpsTarget.addActionListener(new GPSTargetSender());

		JMenuItem analogValues = new JMenuItem("Request all analog labels");
		analogValues.addActionListener(new AnalogValueSender(handler));

		osdData = new JCheckBoxMenuItem("(switch) Request OSD Data");
		osdData.addActionListener(RequestOSDThread.getInstance(dataHandler));
		osdData.setSelected(false);

		communicationMenu.add(startComm);
		communicationMenu.add(new JSeparator(JSeparator.HORIZONTAL));
		communicationMenu.add(getDebug);
		communicationMenu.add(analogValues);
		communicationMenu.add(get3D);
		communicationMenu.add(sendWP);
		communicationMenu.add(requestWaypoints);
		communicationMenu.add(resetWaypoints);
		communicationMenu.add(gpsTarget);
		communicationMenu.add(osdData);

		this.add(communicationMenu);

		/*
		 * tools menu
		 */
		toolsMenu = new JMenu("Tools");

		JMenuItem rawCommand = new JMenuItem("Decode raw command");
		rawCommand.addActionListener(new DisplayDecodeCommandDialog());
		
		JCheckBoxMenuItem switchFlightDisplay = new JCheckBoxMenuItem("(switch) show flight route on map");
		switchFlightDisplay.setSelected(false);
		switchFlightDisplay.addActionListener(mainFrame.getGpsPanel().new ClearWaypointsAction());
		switchFlightDisplay.setActionCommand("route");
		
		JMenuItem exportRoute = new JMenuItem("Export route to text-file");
		exportRoute.addActionListener(mainFrame.getGpsPanel().new ExportRouteAction());
		
		JMenuItem config = new JMenuItem("Settings");
		config.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JMKOConfig cfg = JMKOConfig.getInstance();
				new JPropertiesDialog(cfg.getProperties(), cfg.getPrefixes(),
						cfg.getFileName(), getOurParent());	
			}
		});

		toolsMenu.add(rawCommand);
		toolsMenu.add(switchFlightDisplay);
		toolsMenu.add(exportRoute);
		toolsMenu.add(new JSeparator(JSeparator.HORIZONTAL));
		toolsMenu.add(config);

		this.add(toolsMenu);
		
		/*
		 * map menu
		 */
		mapMenu = new JMenu("Map");
		
		JMenuItem snapshot = new JMenuItem("Export geotagged Map");
		snapshot.addActionListener(mainFrame.getGpsPanel().new MakeSnapshot());
		
		OpenMapDialog mapListener = new OpenMapDialog();
		JMenuItem offlineMap = new JMenuItem("Open offline map");
		offlineMap.addActionListener(mapListener);
		
		JMenuItem wmsMap = new JMenuItem("Load WMS map for current bbox");
		wmsMap.addActionListener(mainFrame.getGpsPanel().new LoadWMSMap());
		
		JMenuItem osmMap = new JMenuItem("Change to OSM (online) map");
		osmMap.addActionListener(mapListener);
		osmMap.setActionCommand("OSM");
		
		mapMenu.add(snapshot);
		mapMenu.add(offlineMap);
		mapMenu.add(wmsMap);
		mapMenu.add(osmMap);
		
		this.add(mapMenu);
		
		/*
		 * other actions
		 */
		JMenuItem importWaypoints = new JMenuItem("Import Waypoints from Flightplanning");
		
		importWaypoints.addActionListener(new DisplayImportDialog());
		this.add(importWaypoints);
		
		/*
		 * mission mode
		 */
		JMenuItem aerialSurvey = new JMenuItem("Start Aerial Survey monitoring");
		
		aerialSurvey.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				List<MapMarkerWrapper> markers = new ArrayList<MapMarkerWrapper>();
				
				List<MapMarker> tmp;
				if (mainFrame.getGpsPanel().getMKWaypointMarkers().size() > 0) {
					tmp = mainFrame.getGpsPanel().getMKWaypointMarkers();
				}
				else tmp = mainFrame.getGpsPanel().getWaypointMarkers();
				
				for (MapMarker m : tmp) {
					markers.add(new MapMarkerWrapper(m.getLat(), m.getLon()));
				}
				
				MissionMode mm = new MissionMode(markers, mainFrame.getGpsPanel());
				mainFrame.addPositionListener(mm);
			}
		});
		this.add(aerialSurvey);
	}



	/**
	 * calles when new height data is available
	 * 
	 * @param altitude the new altitude in mm
	 */
	public void setKopterHeight(int altitude) {
		this.kopterHeight = altitude;
	}

	/**
	 * @return the currently stored height of the MK
	 */
	public double getKopterHeight() {
		return this.kopterHeight;
	}





	/*
	 * 
	 * 
	 * ACTION LISTENER
	 * 
	 * 
	 */
	
	private class InitCommAction implements ActionListener {

		
		@Override
		public void actionPerformed(ActionEvent e) {
			final JMKOConfig config = JMKOConfig.getInstance();
			
			final JDialog dialog = new JDialog(getOurParent(), "Port and COMM Layer");
			
			Container contentPane = dialog.getContentPane();
			contentPane.setLayout(new BorderLayout());
	        dialog.setResizable(false);
	        
	        final JTextField portField = new JTextField(config.getValue("COMM_PORT_MK"));
	        final JTextField portFieldSensors = new JTextField(config.getValue("COMM_PORT_SENSORS"));
	        final JComboBox layerCombo = new JComboBox(new String[] {"RXTX", "Javax COMM"});
	        
	        layerCombo.setSelectedIndex(config.getIntegerValue("COMM_LAYER"));
	        JButton okButton = new JButton("OK");
	        
	        portField.setPreferredSize(new Dimension(130, 25));
	        layerCombo.setPreferredSize(new Dimension(130, 25));
	        okButton.setPreferredSize(new Dimension(70, 30));
	        okButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					dialog.setVisible(false);
					dialog.dispose();
					
					/*
					 * set the new comm-layer and port
					 */
					try {
						JMKOConfig config = JMKOConfig.getInstance();
						config.setProperty("COMM_PORT_SENSORS",
								portFieldSensors.getText());
						config.setProperty("COMM_PORT_MK", portField.getText());
						config.setProperty("COMM_LAYER", layerCombo.getSelectedIndex());
						config.storeProperties();
						mainPanel.newCommLayerInit();
					} catch (Exception e1) {
						log.warn(e1.getMessage(), e1);
					}
				}
			});
	        
	        JPanel buttonPanel = new JPanel();
	        buttonPanel.add(okButton);
	        
	        JPanel fieldPanel = new JPanel();
	        fieldPanel.setLayout(new GridLayout(0, 2));
			
	        fieldPanel.add(new JLabel("Port to MK"));
	        fieldPanel.add(portField);
	        fieldPanel.add(new JLabel("Port to MK's sensors"));
	        fieldPanel.add(portFieldSensors);
	        fieldPanel.add(new JLabel("Used COMM Layer"));
	        fieldPanel.add(layerCombo);
	        
	        contentPane.add(fieldPanel, BorderLayout.CENTER);
	        contentPane.add(buttonPanel, BorderLayout.SOUTH);
	        
	        dialog.pack();
	        dialog.setLocationRelativeTo(dialog.getParent());
			dialog.setVisible(true);
		}
		
	}

	private class ResetWaypointsSender implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					MainMenu.this.dataHandler.sendCommand(new SendWaypointCommand(true));
					mainPanel.getGpsPanel().removeMKWaypoints();
				}
			}).start();
		}
	}

	private class RequestWaypointsAction implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			RequestWaypointsSender.getInstance(dataHandler).perform();
		}
	}

	private class GPSTargetSender implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					MapMarker marker = mainPanel.getGpsPanel().getSendTargetMarker();
					SendTargetPositionCommand cmd = new SendTargetPositionCommand(marker.getLon(),
							marker.getLat(), kopterHeight*1E-3, 2);
					MainMenu.this.dataHandler.sendCommand(cmd);
				}
			}).start();
		}

	}



	private class DisplayDecodeCommandDialog implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent arg0) {
			new DecodeCommandDialog(MainMenu.this.getOurParent());
		}

	}


	private class DisplayImportDialog implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent arg0) {
			new ImportCsvWaypointsDialog(MainMenu.this.mainPanel.getGpsPanel(), MainMenu.this.getOurParent());
		}

	}
	
	private class OpenMapDialog implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			if (e.getActionCommand().equals("OSM")) {
				
				MainMenu.this.mainPanel.getGpsPanel().initMap(
						new JMapWithOverlay(new MemoryTileCache(), 4));
				MainMenu.this.mainPanel.getGpsPanel().validate();
				
				return;
			}
			
			JFileChooser fc = new JFileChooser();
			fc.setPreferredSize(new Dimension(640, 480));
			fc.setCurrentDirectory(new File("."));
			
			fc.setFileFilter(new FileFilter() {
				
				@Override
				public String getDescription() {
					return "JPEG Map Files (Geoinformation needed)";
				}
				
				@Override
				public boolean accept(File f) {
					String name = f.getName();
					String ext = name.substring(name.lastIndexOf('.')+1,
							name.length()).toLowerCase();
					return (f.isDirectory() || 
							ext.equals("jpg") || ext.equals("jpeg")) ? true:false;
				}
			});

			int returnVal = fc.showOpenDialog(MainMenu.this);

	        if (returnVal == JFileChooser.APPROVE_OPTION) {
	            File file = fc.getSelectedFile();
	            try {
					MainMenu.this.mainPanel.getGpsPanel().initMap(
							new OfflineMapWithOverlay(file));
					MainMenu.this.mainPanel.getGpsPanel().validate();
				} catch (IOException e1) {
					log.warn(e1.getMessage(), e1);
				}
	        }
			
		}
		
	}

	/**
	 * @return a frame that holds this panel in one of
	 * its components 
	 */
	protected Frame getOurParent() {
		Container tmp = this.mainPanel;
		
		int maxCount = 20;
		int count = 0;
		while (!(tmp instanceof Frame) && count++ < maxCount) {
			tmp = tmp.getParent();
		}
		return (Frame) tmp;
	}



	@Override
	public void receiveAction(ActionEvent e) {
		if (e.getActionCommand().equals(ActionDelegationManager.DEBUG_SWITCH_ACTION) && !e.getSource().equals(getDebug)) {
			this.getDebug.setSelected(!this.getDebug.isSelected());
		}
		else if (e.getActionCommand().equals(ActionDelegationManager.GPS_SWITCH_ACTION) && !e.getSource().equals(osdData)) {
			this.osdData.setSelected(!this.osdData.isSelected());
		}		
	}


}
