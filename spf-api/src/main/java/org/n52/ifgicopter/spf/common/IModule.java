/**
 * ﻿Copyright (C) 2009
 * by 52 North Initiative for Geospatial Open Source Software GmbH
 *
 * Contact: Andreas Wytzisk
 * 52 North Initiative for Geospatial Open Source Software GmbH
 * Martin-Luther-King-Weg 24
 * 48155 Muenster, Germany
 * info@52north.org
 *
 * This program is free software; you can redistribute and/or modify it under
 * the terms of the GNU General Public License version 2 as published by the
 * Free Software Foundation.
 *
 * This program is distributed WITHOUT ANY WARRANTY; even without the implied
 * WARRANTY OF MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program (see gnu-gpl v2.txt). If not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA or
 * visit the Free Software Foundation web page, http://www.fsf.org.
 */

package org.n52.ifgicopter.spf.common;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;


/**
 * Interface that classes should implement which are
 * available in the {@link SPFRegistry} and need
 * a init and shutdown (when the programm is exited).
 * 
 * @author Matthes Rieke <m.rieke@uni-muenster.de>
 * 
 */
public interface IModule {
	
	/**
	 * use this if the plugin is running
	 */
	public static final int STATUS_RUNNING = 1;
	
	/**
	 * use this if the plugin is not running
	 */
	public static final int STATUS_NOT_RUNNING = 0;
	
	/**
	 * Initialises the module.
	 * 
	 * @throws Exception
	 */
	public void init() throws Exception;
	
	/**
	 * shuts the module down.
	 * 
	 * @throws Exception
	 */
	public void shutdown() throws Exception;

	/**
	 * Use this annotation to describe parameters of extensions.
	 * These descriptions will be used within dynamic plugin setup.
	 * 
	 * @author matthes rieke
	 *
	 */
	@Retention(RetentionPolicy.RUNTIME)
	public static @interface ConstructorParameters {

		String[] value();
		
	}
}
