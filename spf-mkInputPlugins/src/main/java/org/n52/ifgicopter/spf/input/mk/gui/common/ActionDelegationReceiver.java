package org.n52.ifgicopter.spf.input.mk.gui.common;

import java.awt.event.ActionEvent;

/**
 * Simple interface for receiving an {@link ActionEvent}.
 * 
 * @author matthes rieke
 *
 */
public interface ActionDelegationReceiver {

	/**
	 * see {@link ActionDelegationManager}.
	 * 
	 * @param e the action event
	 */
	void receiveAction(ActionEvent e);

}
