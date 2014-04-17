package org.n52.ifgicopter.spf.input.mk.gui.sender;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.n52.ifgicopter.javamk.MKConstants;
import org.n52.ifgicopter.javamk.MKDataHandler;
import org.n52.ifgicopter.javamk.incoming.DebugData;
import org.n52.ifgicopter.javamk.outgoing.RequestDebugCommand;
import org.n52.ifgicopter.spf.input.mk.gui.common.ActionDelegationManager;

/**
 * Holds a thread that requests {@link DebugData} every
 * 3 seconds.
 * 
 * @author Matthes Rieke <m.rieke@uni-muenster.de>
 *
 */
public class RequestDebugSender implements ActionListener, Runnable {

	private static final Log log = LogFactory.getLog(MKConstants.LOGGER_ID);
	
	private MKDataHandler dataHandler;
	private boolean running = false;
	private static RequestDebugSender _instance;

	private RequestDebugSender(MKDataHandler handler) {
		this.dataHandler = handler;
	}
	
	/**
	 * @param handler the data handler
	 * @return the singleton instance
	 */
	public static RequestDebugSender getInstance(MKDataHandler handler) {
		if (_instance == null) {
			_instance = new RequestDebugSender(handler);
		}
		
		return _instance;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		synchronized (_instance) {
			ActionEvent delEvent = new ActionEvent(e.getSource(), 0, ActionDelegationManager.DEBUG_SWITCH_ACTION);
			ActionDelegationManager.getInstance().delegateAction(delEvent);
			
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
		RequestDebugCommand send = new RequestDebugCommand(1000);
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