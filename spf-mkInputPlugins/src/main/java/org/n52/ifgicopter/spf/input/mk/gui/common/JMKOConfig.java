package org.n52.ifgicopter.spf.input.mk.gui.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.n52.ifgicopter.javamk.MKConstants;

/**
 * Configuration file with all needed application
 * parameters.
 * 
 * @author Matthes Rieke <m.rieke@uni-muenster.de>
 *
 */
public class JMKOConfig {
	
	private static final Log log = LogFactory.getLog(MKConstants.LOGGER_ID);
	private static JMKOConfig _instance;
	private Properties properties;
	private File file;
	private String comment;
	
	private JMKOConfig() {
		this.properties = new Properties();
		this.file = new File("config/jmko.cfg");
		InputStream in = null;
		
		if (!file.exists()) {
			in = getClass().getResourceAsStream("/config/jmko.cfg");
		} else {
			try {
				in = new FileInputStream(file);
			} catch (FileNotFoundException e) {
				log.warn(e.getMessage(), e);
			}
		}
		
		try {
			this.properties.load(in);
		} catch (IOException e) {
			log.warn(e.getMessage(), e);
		}
		
		String newline = System.getProperty("line.separator");
		
		this.comment = "### Config for JavaMKOperator. One parameter per line"+ newline+
				"###"+ newline+
				"### COM port and protocol layer"+ newline+
				"### DEFAULT_PORT_MK: string with the port name for mk port"+ newline+
				"### DEFAULT_PORT_MK: string with the port name for sensor port"+ newline+
				"### possible DEFAULT_COMM_LAYER values: RXTX=0, JAVA=1"+ newline+
				"###"+ newline+
				"### Prefix: NC_"+ newline+
				"### NC_TOLERANCE_RADIUS: integer value for waypoint tolerenace in meter"+ newline+
				"### NC_HOLD_TIME: integer value for waypoint holtime in seconds"+ newline+
				"### NC_ALTITUDE: double value for waypoint altitude"+ newline+
				"### possible NC_ALT_MODE values: SET (use ALTITUDE), ADD (add ALTITUDE to current height)"+ newline+
				"###"+ newline;
	}
	
	/**
	 * @param key the key
	 * @return the value specified for the key
	 */
	public String getValue(String key) {
		return this.properties.getProperty(key);
	}
	
	/**
	 * @param key the key
	 * @return the value specified for the key as Integer
	 */
	public Integer getIntegerValue(String key) {
		try {
		return Integer.parseInt(this.properties.getProperty(key));
		} catch (NumberFormatException e) {
			log.warn("Configuration file malformed", e);
			return null;
		}
	}
	
	/**
	 * @param key the key
	 * @return the value specified for the key as Double
	 */
	public Double getDoubleValue(String key) {
		try {
		return Double.parseDouble(this.properties.getProperty(key));
		} catch (NumberFormatException e) {
			log.warn("Configuration file malformed", e);
			return null;
		}
	}
	
	/**
	 * @param key the key
	 * @return the value specified for the key as Boolean
	 */
	public Boolean getBooleanValue(String key) {
		try {
		return Boolean.parseBoolean(this.properties.getProperty(key));
		} catch (NumberFormatException e) {
			log.warn("Configuration file malformed", e);
			return null;
		}
	}
	

	/**
	 * @return the properties
	 */
	public Properties getProperties() {
		return properties;
	}

	/**
	 * @return the singleton instance of {@link JMKOConfig}
	 */
	public static synchronized JMKOConfig getInstance() {
		if (_instance == null) {
			_instance = new JMKOConfig();
		}
		
		return _instance;
	}

	/**
	 * saves the properties to its file
	 * 
	 * @throws FileNotFoundException if file is not existing
	 * @throws IOException if error occured
	 */
	public void storeProperties() throws FileNotFoundException, IOException {
		if (!file.exists()) {
			if (!file.createNewFile()) {
				throw new IOException("Could not create file: '"+ file.getAbsolutePath() +"'.");
			}
		}
		
		this.properties.store(new FileOutputStream(file), this.comment);
	}

	/**
	 * @return prefixes for dialog representations
	 */
	public String[] getPrefixes() {
		return new String[] {"NC_", "COMM_"};
	}

	/**
	 * @return the absolute path to the config file.
	 */
	public String getFileName() {
		return this.file.getAbsolutePath();
	}

	/**
	 * sets a property
	 * 
	 * @param string the property
	 * @param text the value
	 */
	public void setProperty(String string, Object text) {
		this.properties.setProperty(string, text.toString());
	}

}
