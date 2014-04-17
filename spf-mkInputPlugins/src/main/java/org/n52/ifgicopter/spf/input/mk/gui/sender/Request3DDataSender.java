package org.n52.ifgicopter.spf.input.mk.gui.sender;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.n52.ifgicopter.javamk.MKConstants;
import org.n52.ifgicopter.javamk.MKDataHandler;
import org.n52.ifgicopter.javamk.incoming.Data3D;
import org.n52.ifgicopter.javamk.outgoing.Set3DDataIntervalCommand;

/**
 * Holds a thread that requests {@link Data3D}
 * every 3 seconds.
 * 
 * @author Matthes Rieke <m.rieke@uni-muenster.de>
 *
 */
public class Request3DDataSender implements ActionListener, Runnable {

	private static final Log log = LogFactory.getLog(MKConstants.LOGGER_ID);
	
	private MKDataHandler dataHandler;
	private boolean running = false;
	private static Request3DDataSender _instance;

	private Request3DDataSender(MKDataHandler handler) {
		this.dataHandler = handler;
	}
	
	/**
	 * @param handler the data handler
	 * @return the singleton instance
	 */
	public static Request3DDataSender getInstance(MKDataHandler handler) {
		if (_instance == null) {
			_instance = new Request3DDataSender(handler);
		}
		
		return _instance;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		synchronized (_instance) {
			if (running) {
				this.running = false;
			}
			else {
				this.running = true;
				Thread t = new Thread(this);
				t.setName("RequestDebugSender thread");
				t.start();
			}
			
		}
	}

	@Override
	public void run() {
		Set3DDataIntervalCommand send = new Set3DDataIntervalCommand(10);
		while (this.running) {
			this.dataHandler.sendCommand(send);
			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				log.warn(e.getMessage(), e);
			}
		}
	}

}