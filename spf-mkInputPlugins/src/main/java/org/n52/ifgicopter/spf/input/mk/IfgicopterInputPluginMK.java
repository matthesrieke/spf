package org.n52.ifgicopter.spf.input.mk;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.n52.ifgicopter.javamk.CommandReceive;
import org.n52.ifgicopter.javamk.MKDataHandler;
import org.n52.ifgicopter.javamk.incoming.Data3D;
import org.n52.ifgicopter.javamk.incoming.FollowMeData;
import org.n52.ifgicopter.javamk.incoming.MK3MagWinkel;
import org.n52.ifgicopter.javamk.incoming.OSDData;
import org.n52.ifgicopter.javamk.listener.ICommandListener;
import org.n52.ifgicopter.mksensors.IMKSensorListener;
import org.n52.ifgicopter.mksensors.MKSensorReceiver;
import org.n52.ifgicopter.spf.common.IModule;
import org.n52.ifgicopter.spf.gui.ModuleGUI;
import org.n52.ifgicopter.spf.input.IInputPlugin;
import org.n52.ifgicopter.spf.input.mk.gui.JavaMKMainPanel;
import org.n52.ifgicopter.spf.input.mk.gui.common.JMKOConfig;


/**
 * Class for collecting all data. Interpolation is also
 * calculated here.
 * 
 * @author Matthes Rieke <m.rieke@uni-muenster.de>
 *
 */
public class IfgicopterInputPluginMK implements ICommandListener, IMKSensorListener, IInputPlugin {

	private static final Log log = LogFactory.getLog(IfgicopterInputPluginMK.class.getName());

	private static final String TIME_IDENTIFIER = "time";

	private MKSensorReceiver sensorReceiver;

	private MKDataHandler mkDataHandler;


	private List<Map<String, Object>> dataBuffer = new ArrayList<Map<String,Object>>();

	private ModuleGUI gui;

	private int status = IModule.STATUS_RUNNING;

	private String errorString = "";

	@Override
	public void processCommand(CommandReceive recv) {
		if (recv instanceof FollowMeData) {
			FollowMeData fmd = (FollowMeData) recv;
			/*
			 * GPS data
			 */
			HashMap<String, Object> data = new HashMap<String, Object>();
			data.put("latitude", fmd.getWaypoint().getPosition().getLatitude() / 1E7);
			data.put("longitude", fmd.getWaypoint().getPosition().getLongitude() / 1E7);
			data.put("altitude", fmd.getWaypoint().getPosition().getAltitude() / 1E3);
			data.put(TIME_IDENTIFIER, fmd.getSystemTime());

			this.populateNewData(data);
		}
		else if (recv instanceof OSDData) {
			OSDData fmd = (OSDData) recv;
			/*
			 * GPS data
			 */
			HashMap<String, Object> data = new HashMap<String, Object>();
			data.put("latitude", fmd.getNaviData().getCurrentPosition().getLatitude() / 1E7);
			data.put("longitude", fmd.getNaviData().getCurrentPosition().getLongitude() / 1E7);
			data.put("altitude", fmd.getNaviData().getCurrentPosition().getAltitude() / 1E3);
			data.put(TIME_IDENTIFIER, fmd.getSystemTime());

			this.populateNewData(data);
		}
		else if (recv instanceof Data3D) {
			Data3D heading = (Data3D) recv;
			/*
			 * heading data
			 */
			HashMap<String, Object> data = new HashMap<String, Object>();
			data.put("heading", heading.getData3D().getHeading() / 10.0);
			data.put(TIME_IDENTIFIER, heading.getSystemTime());

			this.populateNewData(data);
		}
		else if (recv instanceof MK3MagWinkel) {
			//			MK3MagWinkel heading = (MK3MagWinkel) recv;
			//			/*
			//			 * heading data
			//			 */
			//			HashMap<String, Object> data = new HashMap<String, Object>();
			//			data.put("heading", heading.getWinkel(). getHeading() / 10.0);
			//			data.put(IInputPlugin.TIME_IDENTIFIER, heading.getSystemTime());
			//			
			//			this.sdfInstance.populateNewData(data);

		}
	}

	private synchronized void populateNewData(Map<String, Object> data) {
		this.dataBuffer.add(data);
	}

	@Override
	public InputStream getConfigFile() {
		File f = new File("config/spf/input-ifgicopter.xml");
		if (f.exists()) {
			try {
				return new FileInputStream(f);
			} catch (FileNotFoundException e) {
				log.warn(e.getMessage(), e);
			}
		}
		
		return getClass().getResourceAsStream("/config/spf/input-ifgicopter.xml");
	}

	@Override
	public void processSensorData(byte[] data) {
		String str = new String(data).trim();
		log.info("new MK sensor data: "+ str);

		//Temp=22.75;Humi=60.84;
		String[] items = str.split(";");
		if (items.length != 2) {
			return;
		}

		double temperature, humi;
		try {
			temperature = Double.parseDouble(
					items[0].substring(items[0].indexOf("=") + 1));
			humi = Double.parseDouble(
					items[1].substring(items[1].indexOf("=") + 1));
		} catch (NumberFormatException e) {
			log.warn("Could not parse to number: " +items[0] + "; "+ items[1]);
			return;
		}

		Map<String,Object> newData = new HashMap<String, Object>();
		newData.put("temperature", temperature);
		newData.put("humidity", humi);
		newData.put(TIME_IDENTIFIER, System.currentTimeMillis());

		this.populateNewData(newData);
	}

	@Override
	public void init() throws Exception {
		initCommunication();
		/*
		 * 
		 * init the GUI
		 * 
		 */
		JavaMKMainPanel panel = new JavaMKMainPanel(this.mkDataHandler, this);
		this.gui = new ModuleGUI();
		this.gui.setGui(panel);
		this.gui.setMenu(panel.getMenu());
		
//		final Random rand = new Random();
//		
//		new Thread(new Runnable() {
//			
//			@Override
//			public void run() {
//				while (true) {
//					HashMap<String, Object> data = new HashMap<String, Object>();
//					data.put(TIME_IDENTIFIER, System.currentTimeMillis());
//					data.put("temperature", -3.2 + rand.nextDouble()*2);
//					data.put("humidity", 88.0 + rand.nextDouble()*2);
//					
//					populateNewData(data);
//					
//					try {
//						Thread.sleep(2000);
//					} catch (InterruptedException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}	
//				}
//				
//			}
//		}).start();
	}

	@Override
	public void shutdown() throws Exception {
		this.mkDataHandler.shutdown();
		if (this.sensorReceiver != null) {
			this.sensorReceiver.shutdown();
		}
	}


	/**
	 * must be called from a class which knows the comm layer
	 */
	public void initCommunication() {
		JMKOConfig config = JMKOConfig.getInstance();
		
		if (config.getBooleanValue("COMM_PORT_AUTO_FIND")) {
			this.mkDataHandler = new MKDataHandler(true);
		}
		else {
			this.mkDataHandler = new MKDataHandler(config.getValue("COMM_PORT_MK"),
					config.getIntegerValue("COMM_LAYER"));
		}
		
		
		this.mkDataHandler.registerCommandListener(this);
		
		if (!this.mkDataHandler.isReady()) {
			this.status = IModule.STATUS_NOT_RUNNING;
			this.errorString = "Could not open COM port '" 
				+ config.getValue("COMM_PORT_MK") + "' to MK!";
			return;
		}
		
		try {
			this.sensorReceiver = new MKSensorReceiver(this.mkDataHandler.getCommLayer(),
					config.getValue("COMM_PORT_SENSORS"));
			this.sensorReceiver.registerListener(this);
			this.status = IModule.STATUS_RUNNING;
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
			this.status = IModule.STATUS_NOT_RUNNING;
			this.errorString = "Could not open COM port '" 
				+ config.getValue("COMM_PORT_SENSORS") + "' to MK sensors!";
		}
		
		this.mkDataHandler.initializationComplete();
	}


	@Override
	public synchronized boolean hasNewData() {
		return !this.dataBuffer.isEmpty();
	}

	@Override
	public synchronized List<Map<String, Object>> getNewData() {
		ArrayList<Map<String, Object>> result = new ArrayList<Map<String, Object>>(this.dataBuffer);
		this.dataBuffer.clear();
		return result;
	}

	@Override
	public int getStatus() {
		return this.status;
	}

	@Override
	public String getStatusString() {
		if (this.status == IInputPlugin.STATUS_RUNNING) {
			return "Mikrokopter Input-Plugin running normally.";
		} else {
			return this.errorString;
		}
	}

	@Override
	public String getName() {
		return "Mikrokopter Input-Plugin";
	}

	@Override
	public ModuleGUI getUserInterface() {
		return this.gui;
	}

	/**
	 * method gets called if a new communication layer
	 * is initialised
	 */
	public void activeNewCommLayer() {
		initCommunication();
	}

}
