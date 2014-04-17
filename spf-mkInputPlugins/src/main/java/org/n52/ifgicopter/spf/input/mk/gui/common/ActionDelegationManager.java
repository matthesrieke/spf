package org.n52.ifgicopter.spf.input.mk.gui.common;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Class for delegating certain actions to interested classes.
 * see {@link ActionDelegationReceiver}.
 * 
 * @author matthes rieke
 *
 */
public class ActionDelegationManager {
	
	private static ActionDelegationManager _instance = new ActionDelegationManager();
	
	/*
	 * static action identifiers
	 */
	
	/**
	 * the debug data switch id
	 */
	public static final String DEBUG_SWITCH_ACTION = "DEBUG_SWITCH";
	
	/**
	 * the gps switch id
	 */
	public static final String GPS_SWITCH_ACTION = "GPS_SWITCH";
	
	
	private Map<String, List<ActionDelegationReceiver>> registrations = new HashMap<String, List<ActionDelegationReceiver>>();
	
	private ActionDelegationManager() {
		
	}
	
	/**
	 * @return the singleton instance
	 */
	public static ActionDelegationManager getInstance() {
		return _instance;
	}
	
	/**
	 * after registering for an action, the receiver will be informed
	 * of any action with this id sent through the {@link ActionDelegationManager}.
	 * 
	 * @param id the action id
	 * @param rec the receiver instance
	 */
	public void registerForAction(String id, ActionDelegationReceiver rec) {
		synchronized (this) {
			if (!this.registrations.containsKey(id)) {
				this.registrations.put(id, new ArrayList<ActionDelegationReceiver>());
			}
			
			this.registrations.get(id).add(rec);
		}
	}
	
	/**
	 * This method informs all registered receivers.
	 * 
	 * @param e the action
	 */
	public void delegateAction(ActionEvent e) {
		if (this.registrations.containsKey(e.getActionCommand())) {
			List<ActionDelegationReceiver> list = this.registrations.get(e.getActionCommand());
			
			for (ActionDelegationReceiver actionDeligationReceiver : list) {
				actionDeligationReceiver.receiveAction(e);
			}
		}
	}

}
