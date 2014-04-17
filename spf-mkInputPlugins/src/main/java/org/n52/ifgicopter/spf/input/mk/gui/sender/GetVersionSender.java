package org.n52.ifgicopter.spf.input.mk.gui.sender;



import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.n52.ifgicopter.javamk.MKConstants;
import org.n52.ifgicopter.javamk.MKDataHandler;
import org.n52.ifgicopter.javamk.outgoing.GetVersionCommand;

/**
 * This class holds a thread that can be started to retrieve
 * version information from the MK. it can be stopped
 * via calling the {@link GetVersionSender#setRunning(boolean)}
 * method (for instance if version info was received from MK).
 * Get the instance and call {@link GetVersionSender#perform()}
 * to start the thread.
 * 
 * @author Matthes Rieke <m.rieke@uni-muenster.de>
 *
 */
public class GetVersionSender implements Runnable {
	
	private static final Log log = LogFactory.getLog(MKConstants.LOGGER_ID);
	
	private static GetVersionSender _instance;
	private MKDataHandler dataHandler;
	private boolean running = false;

	private GetVersionSender(MKDataHandler handler) {
		this.dataHandler = handler;
	}
	
	/**
	 * method to start a new thread with this {@link GetVersionSender}
	 * instance.
	 */
	public synchronized void perform() {
		if (this.running) return;
		
		this.running = true;
		Thread t = new Thread(this);
		t.setName("GetVersionSender Thread");
		t.start();
	}

	@Override
	public void run() {
		while (this.running) {
			this.dataHandler.sendCommand(new GetVersionCommand());
			
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				log.warn(e.getMessage(), e);
			}
			
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				log.warn(e.getMessage(), e);
			}
		}
	}

	/**
	 * @param running the running to set
	 */
	public void setRunning(boolean running) {
		this.running = running;
	}
	
	/**
	 * this method must be called once as it stores the {@link MKDataHandler}
	 * in a field.
	 * 
	 * @param handler {@link MKDataHandler} to send the commands to
	 * @return the singleton instance of this {@link GetVersionSender}
	 */
	public static synchronized GetVersionSender getInstance(MKDataHandler handler) {
		if (_instance == null) {
			_instance = new GetVersionSender(handler);
		}
		return _instance;
	}
	
	/**
	 * @return the singleton instance of this {@link GetVersionSender}
	 * @throws Exception if instance is not initialised
	 */
	public static synchronized GetVersionSender getInstance() throws Exception {
		if (_instance == null) {
			throw new Exception("no instance available. need to" +
					"create one using getInstance(MKDataHandler handler");
		}
		return _instance;
	}
	
}