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

package org.n52.ifgicopter.spf.input;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.n52.ifgicopter.spf.common.IModule;
import org.n52.ifgicopter.spf.gui.ModuleGUI;
import org.n52.ifgicopter.spf.input.IInputPlugin;

/**
 * Simple test class.
 * 
 * @author Matthes Rieke <m.rieke@uni-muenster.de>
 * 
 */
public class DummyInputPlugin implements IInputPlugin {

	protected static final Log log = LogFactory
			.getLog(DummyInputPlugin.class);

	private static final long THREAD_SLEEP_TIME_MASTER_WHILE_LOOP = 100;

	/*
	 * seconds the loop will run
	 */
	protected static final int LOOP_SEC = 500;

	/*
	 * set this to true to autostart the plugin
	 */
	protected boolean running = false;

	private List<Map<String, Object>> dataBuffer = new ArrayList<Map<String, Object>>();

	int status = IModule.STATUS_RUNNING;

	private ModuleGUI gui;

	protected boolean initialized = false;

	protected long sleeptime = 500;

	public DummyInputPlugin() {
		// TODO Auto-generated constructor stub
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.n52.ifgicopter.spf.input.IInputPlugin#getConfigFile()
	 */
	@Override
	public InputStream getConfigFile() {
		try {
			return new FileInputStream(new File(
					"config/spf/input-ifgicopter.xml"));
		} catch (FileNotFoundException e) {
			log.error(e);
		}
		return null;
	}

	@Override
	public void init() throws Exception {
		this.gui = new ModuleGUI();
		JPanel panel = new JPanel(new FlowLayout());
		JLabel label = new JLabel(
				"<html><h1>Dummy InputPlugin</h1><p>Running time: "
						+ LOOP_SEC
						+ " seconds with a new value interval of "
						+ this.sleeptime
						+ " milliseconds.</p><p></p><p>Once started, you cannot stop it.</p></html>");
		panel.add(label);
		
		final JButton startbut = new JButton("Start");
		startbut.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				start();
				startbut.setEnabled(false);
			}
		});
		panel.add(startbut);
		
		this.gui.setGui(panel);


		new Thread(new Runnable() {

			@Override
			public void run() {
				while (!DummyInputPlugin.this.initialized) {
					try {
						Thread.sleep(THREAD_SLEEP_TIME_MASTER_WHILE_LOOP);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}

				try {
					while (true) {
						if (DummyInputPlugin.this.running) {

							innerRun();

						} else {
							try {
								Thread.sleep(THREAD_SLEEP_TIME_MASTER_WHILE_LOOP);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}
					}
				} catch (Exception e) {
					log.error("error in run()", e);
				}
			}

			private void innerRun() {
				double altDelta = 0.5;
				int humicount = 0;
				Random rand = new Random();

				Map<String, Object> posData = null;
				Map<String, Object> sensorData = null;

				long start = System.currentTimeMillis();
				posData = new HashMap<String, Object>();

				/*
				 * do loop of LOOP_SEC seconds
				 */
				log.debug("starting time series for " + LOOP_SEC + " seconds.");
				while (System.currentTimeMillis() - start < LOOP_SEC * 1000) {

					posData.put("time",
							Long.valueOf(System.currentTimeMillis()));
					posData.put("latitude",
							Double.valueOf(52.0 + rand.nextDouble() * 0.0025));
					posData.put("longitude",
							Double.valueOf(7.0 + rand.nextDouble() * 0.0025));
					posData.put("altitude",
							Double.valueOf((altDelta*humicount) + 34.0 + rand.nextDouble() * 7));

					DummyInputPlugin.this.populateNewData(posData);

					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						log.error(e);
					}

					/*
					 * now with humidity
					 */
					sensorData = new HashMap<String, Object>();

					sensorData.put("time",
							Long.valueOf(System.currentTimeMillis()));
					sensorData.put("humidity",
							Double.valueOf(70.2 + (rand.nextInt(200) / 10.0)));
					sensorData.put("humiditay",
							Double.valueOf(70.2 + (rand.nextInt(200) / 10.0)));
					sensorData.put("temperature",
							Double.valueOf(20.0 + (rand.nextInt(40) / 10.0)));
					humicount++;

					DummyInputPlugin.this.populateNewData(sensorData);

					try {
						Thread.sleep(DummyInputPlugin.this.sleeptime);
					} catch (InterruptedException e) {
						log.error(e);
					}

				}

				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					log.error(e);
				}

				posData.put("time", Long.valueOf(System.currentTimeMillis()));
				DummyInputPlugin.this.populateNewData(posData);

				log.info("humicount=" + humicount);
				
				// just one run!
				stop();
			}

		}).start();

		this.initialized = true;
	}

	/**
     * 
     */
	public void start() {
		this.running = true;
		log.info("start");
	}

	/**
     * 
     */
	public void stop() {
		this.running = false;
		this.status = IModule.STATUS_NOT_RUNNING;
		log.info("stop");
	}

	@Override
	public void shutdown() throws Exception {
		this.running = false;
	}

	protected synchronized void populateNewData(Map<String, Object> data) {
		this.dataBuffer.add(data);
	}

	@Override
	public synchronized boolean hasNewData() {
		return !this.dataBuffer.isEmpty();
	}

	@Override
	public synchronized List<Map<String, Object>> getNewData() {
		ArrayList<Map<String, Object>> result = new ArrayList<Map<String, Object>>(
				this.dataBuffer);
		this.dataBuffer.clear();
		log.debug(result);
		return result;
	}

	@Override
	public int getStatus() {
		return this.status;
	}

	@Override
	public String getStatusString() {
		if (this.status == IModule.STATUS_RUNNING) {
			return "everything fine";
		}
		return "stopped...";
	}

	@Override
	public String getName() {
		return "Dummy InputPlugin";
	}

	@Override
	public ModuleGUI getUserInterface() {
		return this.gui;
	}

}
