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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.n52.ifgicopter.spf.SPFRegistry;
import org.n52.ifgicopter.spf.common.IModule;



/**
 * Global singleton class for providing a
 * static number of working threads.
 * 
 * @author Matthes Rieke <m.rieke@uni-muenster.de>
 *
 */
public class SPFThreadPool implements IModule {

	private final Log log = LogFactory.getLog(SPFThreadPool.class);
	private ExecutorService executor = Executors.newFixedThreadPool(Integer.parseInt(
			SPFRegistry.getInstance().getConfigProperty(SPFRegistry.PROCESSOR_COUNT_PROP)));
	private static SPFThreadPool _instance;

	/**
	 * default local constructor
	 */
	private SPFThreadPool() {
	    //
	}
	
	/**
	 * @param task the task to be executed.
	 */
	public void submitTask(Runnable task) {
		if (!this.executor.isShutdown()) {
			this.executor.submit(task);
		}
	}
	
	@Override
	public void init() throws Exception {
	    //
	}

	@Override
	public void shutdown() throws InterruptedException {
		if (this.log.isInfoEnabled()) {
			this.log.info("shutting down SPF thread pool");
		}
		this.executor.shutdown();
		this.executor.awaitTermination(3, TimeUnit.SECONDS);
		this.executor.shutdownNow();
	}
	
	/**
	 * @return the singleton instance
	 */
	public static synchronized SPFThreadPool getInstance() {
		if (_instance == null) {
			_instance = new SPFThreadPool();
		}
		
		return _instance;
	}


}
