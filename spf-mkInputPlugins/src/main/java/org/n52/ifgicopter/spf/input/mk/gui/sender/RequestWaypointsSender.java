package org.n52.ifgicopter.spf.input.mk.gui.sender;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.n52.ifgicopter.javamk.MKConstants;
import org.n52.ifgicopter.javamk.MKDataHandler;
import org.n52.ifgicopter.javamk.outgoing.RequestWaypointCommand;

/**
 * If performed this class retrieves all waypoints
 * stored on the NaviCtrl.
 * 
 * @author Matthes Rieke <m.rieke@uni-muenster.de>
 *
 */
public class RequestWaypointsSender implements Runnable {

	private static final Log log = LogFactory.getLog(MKConstants.LOGGER_ID);
	
	private static RequestWaypointsSender _instance;
	private MKDataHandler dataHandler;
	private boolean running = false;
	
	private int count;

	private RequestWaypointsSender(MKDataHandler handler) {
		this.dataHandler = handler;
	}
	
	/**
	 * method to start a new thread with this {@link RequestWaypointsSender}
	 * instance.
	 */
	public synchronized void perform() {
		if (this.running) return;
		
		this.running = true;
		Thread t = new Thread(this);
		t.setName("RequestWaypointsSender Thread");
		t.start();
	}

	@Override
	public void run() {
		while (this.running) {
			if (this.count == -1) {
				this.running = false;
				break;
			}
			
			for (int i = 1; i <= this.count; i++) {
				this.dataHandler.sendCommand(new RequestWaypointCommand(i));
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					log.warn(e.getMessage(), e);
				}
			}
			this.running = false;
		}
	}

	/**
	 * @param running the running to set
	 */
	public void setRunning(boolean running) {
		this.running = running;
	}
	
	
	
	/**
	 * @param count the count to set
	 */
	public void setCount(int count) {
		this.count = count;
	}

	/**
	 * this method must be called once as it stores the {@link MKDataHandler}
	 * in a field.
	 * 
	 * @param handler {@link MKDataHandler} to send the commands to
	 * @return the singleton instance of this {@link RequestWaypointsSender}
	 */
	public static synchronized RequestWaypointsSender getInstance(MKDataHandler handler) {
		if (_instance == null) {
			_instance = new RequestWaypointsSender(handler);
		}
		return _instance;
	}
	
	/**
	 * @return the singleton instance of this {@link RequestWaypointsSender}
	 * @throws Exception if instance is not initialised
	 */
	public static synchronized RequestWaypointsSender getInstance() throws Exception {
		if (_instance == null) {
			throw new Exception("no instance available. need to" +
					"create one using getInstance(MKDataHandler handler");
		}
		return _instance;
	}

}
