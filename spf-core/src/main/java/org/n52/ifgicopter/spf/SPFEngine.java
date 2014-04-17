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

package org.n52.ifgicopter.spf;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;
import org.n52.ifgicopter.spf.common.IModule;
import org.n52.ifgicopter.spf.common.IOutputMessageListener;
import org.n52.ifgicopter.spf.common.IPositionListener;
import org.n52.ifgicopter.spf.common.IStatusChangeListener;
import org.n52.ifgicopter.spf.gui.PNPDialog;
import org.n52.ifgicopter.spf.gui.SPFMainFrame;
import org.n52.ifgicopter.spf.input.IInputPlugin;
import org.n52.ifgicopter.spf.input.InputPluginCollector;
import org.n52.ifgicopter.spf.output.IOutputPlugin;
import org.n52.ifgicopter.spf.xml.Plugin;
import org.n52.ifgicopter.spf.xml.PluginXMLTools;



/**
 * Main class of the framework. InputPlugins register here
 * and push data in. OutputPlugins register here and data is forwarded to
 * them.
 * 
 * @author Matthes Rieke <m.rieke@uni-muenster.de>
 *
 */
public class SPFEngine {

	protected final Log log = LogFactory.getLog(SPFEngine.class);
	Map<IInputPlugin, InputPluginCollector> inputPlugins = new HashMap<IInputPlugin, InputPluginCollector>();	
	List<IOutputPlugin> outputPlugins = new ArrayList<IOutputPlugin>();
	protected boolean running = true;

	Map<IInputPlugin, Integer> inputPluginStatus = new HashMap<IInputPlugin, Integer>();
	private Map<IOutputPlugin, Integer> outputPluginStatus = new HashMap<IOutputPlugin, Integer>();


	/**
	 * mutex object for output synchronization
	 */
	private final Object mutex = new Object();
	private boolean mutexState = false;
	List<IStatusChangeListener> statusListeners;
	private List<IOutputMessageListener> outputMessageListeners;
	private SPFMainFrame mainFrame;
	private List<IPositionListener> positionListeners = new ArrayList<IPositionListener>();


	/**
	 * Default constructor
	 */
	SPFEngine() {
	    //
	}

	/**
	 * Registers a new InputPlugin at this  engine instance.
	 * 
	 * @param iplugin the new plugin
	 * @param dataProcessors 
	 * @throws Exception if parsing of properties failes
	 */
	protected void registerInputPlugin(IInputPlugin iplugin,
			List<Class<?>> dataProcessors) throws Exception {	
		InputStream is = iplugin.getConfigFile();

		if (is == null || is.available() == 0) {
			throw new IllegalArgumentException("Could not read InputPlugin description!");
		}

		Plugin pl = PluginXMLTools.parsePlugin(is);
		
		InputPluginCollector ipc = new InputPluginCollector(pl, this);
		ipc.setDataProcessors(dataProcessors);
		
		this.inputPlugins.put(iplugin, ipc);

		this.inputPluginStatus.put(iplugin, Integer.valueOf(IModule.STATUS_RUNNING));

		if (this.log.isInfoEnabled()) {
			this.log.info("New InputPlugin registered succesfully at engine: " + ipc.getPlugin().toString());
		}
	}


	/**
	 * Registers a new OutputPlugin
	 * 
	 * @param oplugin the new plugin
	 */
	protected void registerOutputPlugin(IOutputPlugin oplugin) {
		this.outputPlugins.add(oplugin);

		this.outputPluginStatus.put(oplugin, Integer.valueOf(IModule.STATUS_RUNNING));
		
		if (this.log.isInfoEnabled()) {
            this.log.info("New OutputPlugin registered succesfully at engine: " + oplugin.toString());
        }
	}


	/**
	 * Calls the pnp mechanism to recognize a new property.
	 * 
	 * @param key the property name
	 * @return an item description
	 */
	public PNPDialog doPNP(String key) {
		PNPDialog result = this.mainFrame.doPNP(key);
		
		if (result.isPnpOff()) {
			setPNPMode(false);
		}
		
		return result;
	}

	/**
	 * This method is called from an {@link IInputPlugin}.
	 * 
	 * @param newData processed data from the {@link IInputPlugin}
	 * @param plugin the plugin which has generated the output
	 * @deprecated use {@link SPFEngine#doSingleOutput(Map, Plugin)} instead
	 */
	@Deprecated
	public void doOutput(Map<Long, Map<String, Object>> newData, Plugin plugin) {
		if (this.log.isDebugEnabled()) {
			this.log.debug("Doing output.");
		}
		synchronized (this.mutex) {
			for (IOutputPlugin out : SPFEngine.this.outputPlugins) {
				out.processData(newData, plugin);
			}
		}
	}

	/**
	 * This method is called from the SPFEngine-main-thread to push
	 * out a single data set.
	 * 
	 * @param newData processed data from the {@link IInputPlugin}
	 * @param plugin the plugin which has generated the output
	 */
	public void doSingleOutput(Map<String, Object> newData, Plugin plugin) {
		if (this.log.isDebugEnabled()) {
			this.log.debug("Doing singleOutput: "+newData);
		}
		Long time = Long.valueOf(0L);
		synchronized (this.mutex) {
			/*
			 * the engine could be locked
			 * no output can be generated then
			 */
			while (this.mutexState) {
				try {
					this.mutex.wait();
				} catch (InterruptedException e) {
					this.log.warn(e.getMessage());
				}
			}
			
			if (newData != null) {
				time = (Long) newData.get(plugin.getTime().getProperty());
				newData.remove(plugin.getTime().getProperty());
				for (IOutputPlugin out : SPFEngine.this.outputPlugins) {
					
					/*
					 * send to plugin and get its status
					 */
					int stat = out.processSingleData(newData, time, plugin);
					Integer oldStat = this.outputPluginStatus.put(out, Integer.valueOf(stat));
					
					if (oldStat != null && oldStat.intValue() != stat) {

						/*
						 * inform all status listeners
						 */
						for (IStatusChangeListener iscl : this.statusListeners) {
							iscl.statusChanged(stat, out);
						}
					}
				}
			}
		}
		
		for (IOutputMessageListener oms : this.outputMessageListeners) {
			oms.newOutput("Last output at "+ new DateTime(time));
		}
	}

	/**
	 * @param name the Plugin name where the status change occured
	 * @param debugMsg the debug message
	 * @param status status of the plugin ( != 1 -> crashed).
	 */
	public void onOutputPluginStatus(String name, String debugMsg, int status) {
		this.log.warn("An error occured in OutputPlugin '"+ name +"': "+ debugMsg +". Status of " +
				"plugin: "+ (status==1 ? "running":"not running"));
	}

	/**
	 * Starts the engine. In a cycle
	 */
	public void start() {
		this.statusListeners = SPFRegistry.getInstance().getStatusChangeListeners();
		this.outputMessageListeners = SPFRegistry.getInstance().getOutputMessageListeners();

		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {

				List<Map<String, Object>> data = null;

				while (SPFEngine.this.running) {
					/*
					 * infinite loop over all IInputPlugin instances
					 */
					for (IInputPlugin iip : SPFEngine.this.inputPlugins.keySet()) {

						/*
						 * check if we have new data at the plugin
						 */
						if (iip.hasNewData()) {
							/*
							 * pull new data of the plugin
							 */
							data = iip.getNewData();

							for (Map<String, Object> map : data) {
								SPFEngine.this.inputPlugins.get(iip).addNewData(map);
							}
						}
						else {
							/*
							 * was there an error last time?
							 */
							int stat = iip.getStatus();
							Integer oldStat = SPFEngine.this.inputPluginStatus.put(iip, Integer.valueOf(stat));
							if (oldStat != null && oldStat.intValue() != stat) {
								/*
								 * this is a status change
								 */
								for (IStatusChangeListener	iscl : SPFEngine.this.statusListeners) {
									iscl.statusChanged(stat, iip);
								}

							}

							/*
							 * if its not running do not get data
							 */
							if (stat == IModule.STATUS_NOT_RUNNING) {
								/*
								 * plugin is not running, skip.
								 */
								continue;
							}
						}
						
						/*
						 * the thread is taking 100% of a dualcore system
						 * fix: let it sleep for 50 ms. should do the job.
						 */
						try {
							Thread.sleep(50);
						} catch (InterruptedException e) {
							SPFEngine.this.log.warn(e.getMessage(), e);
						}
					}
				}
			}
		});	
		t.setName("SPFEngine-main-thread");
		t.start();
	}

	/**
	 * shutdown the engine.
	 */
	public void shutdown() {
		this.running = false;
	}

	/**
	 * @param mainFrame the gui of the framework
	 */
	public void setMainFrame(SPFMainFrame mainFrame) {
		this.mainFrame = mainFrame;
	}

	/**
	 * set the pnp mode in all IPCs
	 * 
	 * @param selected active or not
	 */
	public void setPNPMode(boolean selected) {
		/*
		 * prevent output generation
		 */
		if (selected) {
			lockEngine();
		}
		else {
			freeEngine();
		}
		
		for (InputPluginCollector ipc : this.inputPlugins.values()) {
			ipc.setPlugAndPlayBehaviour(selected);
		}
	}

	/**
	 * Retunrs the plugin description for a plugin name.
	 * 
	 * @param name the plugin name
	 * @return the description
	 */
	public Plugin getPluginForName(String name) {
		for (IInputPlugin ipc : this.inputPlugins.keySet()) {
			if (ipc.getName().equals(name)) {
				return this.inputPlugins.get(ipc).getPlugin();
			}
		}
		return null;
	}

	/**
	 * restarts all output plugins. locking the engine output for this time
	 */
	public void restartOutputPlugins() {
		this.log.info("Restarting output plugins...");
		synchronized (this.mutex) {
			for (IOutputPlugin mod : this.outputPlugins) {
				try {
					mod.restart();
				} catch (Exception e) {
					this.log.warn(e.getMessage(), e);
				}
			}
		}
		this.log.info("Finished restarting output plugins.");		
	}
	
	/**
	 * locks the engine. must be followed
	 * by a freeEngine()-call eventually.
	 */
	public void lockEngine() {
		synchronized (this.mutex) {
			this.mutexState = true;
		}
	}
	
	/**
	 * frees the engine after a lockEngine()-call.
	 */
	public void freeEngine() {
		synchronized (this.mutex) {
			this.mutexState = false;
			this.mutex.notifyAll();
		}
	}

	/**
	 * @return the list of {@link IPositionListener}s.
	 */
	public List<IPositionListener> getPositionListeners() {
		return this.positionListeners;
	}
	
	/**
	 * @param ipl the new listener
	 */
	public void addPositionListener(IPositionListener ipl) {
		this.positionListeners.add(ipl);
	}


}
