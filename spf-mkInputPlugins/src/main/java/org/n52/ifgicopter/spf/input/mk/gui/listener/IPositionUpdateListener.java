package org.n52.ifgicopter.spf.input.mk.gui.listener;

import org.n52.ifgicopter.javamk.incoming.FollowMeData;
import org.n52.ifgicopter.javamk.incoming.OSDData;

/**
 * Interface for receiption of GPS position updates
 * of the MK.
 * 
 * @author matthes rieke
 *
 */
public interface IPositionUpdateListener {
	
	/**
	 * @param recv GPS pos wrapped in an OSDData black
	 */
	public abstract void processOSDData(OSDData recv);
	
	/**
	 * @param recv GPS pos wrapped in a FollowMe block
	 */
	public abstract void processFollowMeData(FollowMeData recv);

}
