package org.n52.ifgicopter.spf.input.mk.gui.sender;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.n52.ifgicopter.javamk.MKConstants;
import org.n52.ifgicopter.javamk.MKDataHandler;
import org.n52.ifgicopter.javamk.incoming.OSDData;
import org.n52.ifgicopter.javamk.outgoing.RequestOSDCommand;
import org.n52.ifgicopter.spf.input.mk.gui.common.ActionDelegationManager;

/**
 * Holds a thread that requests {@link OSDData} every
 * 2 seconds.
 * 
 * @author Matthes Rieke <m.rieke@uni-muenster.de>
 *
 */
public class RequestOSDThread implements ActionListener, Runnable {

	private static final Log log = LogFactory.getLog(MKConstants.LOGGER_ID);
	
	private MKDataHandler dataHandler;
	private boolean running = false;
	private static RequestOSDThread _instance;

	private RequestOSDThread(MKDataHandler handler) {
		this.dataHandler = handler;
	}
	
	/**
	 * @param handler the data handler
	 * @return the singleton instance
	 */
	public static RequestOSDThread getInstance(MKDataHandler handler) {
		if (_instance == null) {
			_instance = new RequestOSDThread(handler);
		}
		
		return _instance;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		synchronized (_instance) {
			ActionEvent delEvent = new ActionEvent(e.getSource(), 0, ActionDelegationManager.GPS_SWITCH_ACTION);
			ActionDelegationManager.getInstance().delegateAction(delEvent);
			
			if (running) {
				this.running = false;
			}
			else {
				this.running = true;
				Thread t = new Thread(this);
				t.setName("RequestOSD thread");
				t.start();
			}
			
		}
	}

	@Override
	public void run() {
		while (this.running) {
			this.dataHandler.sendCommand(new RequestOSDCommand(100));
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				log.warn(e.getMessage(), e);
			}
		}
	}

}