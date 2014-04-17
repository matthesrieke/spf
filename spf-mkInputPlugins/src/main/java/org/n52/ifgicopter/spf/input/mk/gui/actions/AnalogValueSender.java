package org.n52.ifgicopter.spf.input.mk.gui.actions;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.n52.ifgicopter.javamk.MKDataHandler;
import org.n52.ifgicopter.javamk.outgoing.AnalogValueCommand;

/**
 * retrieves all analog labels
 * 
 * @author matthes rieke
 *
 */
public class AnalogValueSender implements ActionListener {

	private MKDataHandler dataHandler;
	private static final Log log = LogFactory.getLog(AnalogValueSender.class.getName());

	/**
	 * @param h the data handler
	 */
	public AnalogValueSender(MKDataHandler h) {
		this.dataHandler = h;
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		new Thread(new Runnable() {
			@Override
			public void run() {

				for (int i = 0; i < 32; i++) {
					dataHandler.sendCommand(new AnalogValueCommand(i));
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						log.warn(e.getMessage(), e);
					}
				}

			}
		}).start();
	}

}