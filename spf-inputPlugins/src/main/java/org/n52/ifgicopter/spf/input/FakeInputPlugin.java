/**
 * ﻿Copyright (C) 2012
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

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.n52.ifgicopter.spf.common.IModule;
import org.n52.ifgicopter.spf.gui.ModuleGUI;
import org.n52.ifgicopter.spf.input.IInputPlugin;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

/**
 * @author Daniel Nüst (d.nuest@52north.org)
 * 
 */
public class FakeInputPlugin implements IInputPlugin, Runnable {

    private static final double ALTITUDE_INCREMENT = 5.0d;

    private static final double ALTITUDE_RAND_FACTOR = 2.0d;

    private static final double ANGLE_INCREMENT = 0.02d;

    private static final double DEFAULT_ALTITUDE = 50.0d;

    private static final double DEFAULT_CENTER_LAT = 52.0d;

    private static final double DEFAULT_CENTER_LON = 7.0d;

    private static final double DEFAULT_RADIUS = 0.01d;

    protected static Log log = LogFactory.getLog(FakeInputPlugin.class);

    private static final String NAME = "Fake Input";

    private static final double RADIUS_INCREMENT = 0.000005d;

    private static final int[] RANDOM_INT_RANGE = new int[] { -10, 20};

    private static final double SECONDS_TO_MILLISECONDS = 1000.0d;

    private static final long THREAD_SLEEP_TIME_MASTER_WHILE_LOOP = 100;

    /**
     * 
     * @param args
     */
    public static void main(String[] args) {
        FakeInputPlugin fip = new FakeInputPlugin(null);
        try {
            fip.init();
        }
        catch (Exception e) {
            log.error(e);
            return;
        }

        for (int i = 0; i < 100; i++) {
            Point p = fip.getNextPosition();
            log.debug(p);
        }
    }

    private double altitude;

    private double angle = 0.0d;

    private Point center;

    private String configFile;

    private JPanel controlPanel;

    private int counter = 0;

    private GeometryFactory factory = new GeometryFactory();

    private ModuleGUI gui;

    private boolean initialized = false;

    private JMenu menu;

    private List<Map<String, Object>> newData;

    private JPanel panel;

    private double radius;

    private Random rand = new Random(5585l);

    private boolean running = false;

    protected long sleepTimeMillis = 500l;

    private static final long DEFAULT_INTERVAL_MILLIS = 3000l;

    private int status = IModule.STATUS_NOT_RUNNING;

    protected JLabel statusLabel;

    private String statusString = "Fake data, fake status!";

    protected StringBuilder statusStringBuilder;

    private double initialTemperature;

    private double lastPollutantValue = 42.0d;

    /**
     * 
     * @param configFile
     */
    public FakeInputPlugin(String configFile) {
        this.configFile = configFile;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.n52.ifgicopter.spf.input.IInputPlugin#getConfigFile()
     */
    @Override
    public InputStream getConfigFile() {
        try {
            return new FileInputStream(new File(this.configFile));
        }
        catch (FileNotFoundException e) {
            log.error(e);
        }

        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.n52.ifgicopter.spf.input.IInputPlugin#getName()
     */
    @Override
    public String getName() {
        return NAME;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.n52.ifgicopter.spf.input.IInputPlugin#getNewData()
     */
    @Override
    public List<Map<String, Object>> getNewData() {
        List<Map<String, Object>> currentNewData = new ArrayList<Map<String, Object>>();
        currentNewData.addAll(this.newData);
        this.newData.clear();

        return currentNewData;
    }

    /**
     * http://stackoverflow.com/questions/674225/calculating-point-on-a-circles-circumference-from-angle-in-
     * 
     * @return
     */
    public Point getNextPosition() {
        double x = this.center.getX() + this.radius * Math.cos(this.angle);
        double y = this.center.getY() + this.radius * Math.sin(this.angle);
        double z = this.altitude;

        this.angle += ANGLE_INCREMENT;
        this.altitude += ALTITUDE_INCREMENT + (ALTITUDE_RAND_FACTOR * (this.rand.nextDouble() - 0.5));
        this.radius += RADIUS_INCREMENT;

        return this.factory.createPoint(new Coordinate(x, y, z));
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.n52.ifgicopter.spf.input.IInputPlugin#getStatus()
     */
    @Override
    public int getStatus() {
        return this.status;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.n52.ifgicopter.spf.input.IInputPlugin#getStatusString()
     */
    @Override
    public String getStatusString() {
        return this.statusString;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.n52.ifgicopter.spf.input.IInputPlugin#getUserInterface()
     */
    @Override
    public ModuleGUI getUserInterface() {
        return this.gui;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.n52.ifgicopter.spf.input.IInputPlugin#hasNewData()
     */
    @Override
    public boolean hasNewData() {
        if (this.newData != null)
            return this.newData.size() > 0;
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.n52.ifgicopter.spf.common.IModule#init()
     */
    @Override
    public void init() throws Exception {
        this.newData = new ArrayList<Map<String, Object>>();

        this.center = this.factory.createPoint(new com.vividsolutions.jts.geom.Coordinate(DEFAULT_CENTER_LON,
                                                                                          DEFAULT_CENTER_LAT,
                                                                                          DEFAULT_ALTITUDE));
        this.altitude = DEFAULT_ALTITUDE;
        this.radius = DEFAULT_RADIUS;
        this.initialTemperature = GpxUtil.randomNumber(this.rand, RANDOM_INT_RANGE[0], RANDOM_INT_RANGE[1]);

        // gui
        this.gui = new ModuleGUI();

        // panel
        this.gui.setGui(makePanel());

        // menu
        this.gui.setMenu(makeMenu());

        ExecutorService exec = Executors.newSingleThreadExecutor();
        exec.submit(this);

        this.initialized = true;
        this.status = IModule.STATUS_RUNNING;
    }

    /**
     * 
     */
    private void innerRun() {
        Map<String, Object> newDataSet = new HashMap<String, Object>();

        Point p = getNextPosition();
        Date now = new Date();

        newDataSet.put("time", Long.valueOf(now.getTime()));
        newDataSet.put("latitude", Double.valueOf(p.getCoordinate().y));
        newDataSet.put("longitude", Double.valueOf(p.getCoordinate().x));
        newDataSet.put("altitude", Double.valueOf(p.getCoordinate().z));

        /*
         * http://en.wikipedia.org/wiki/Altitude#Relation_between_temperature_and_altitude_in_Earth.27s_atmosphere
         * 
         * temperature lapse rate: 6.49 K(°C)/1,000m --> 0.00649 K / 1m
         */
        double t = this.initialTemperature + 0.00649 * p.getCoordinate().z;

        // a little bit randomization
        double value2 = getPollutantValue(p.getCoordinate().y, p.getCoordinate().x, p.getCoordinate().z);

        newDataSet.put("temperature", Double.valueOf(t));
        newDataSet.put("pollutant", Double.valueOf(value2));

        log.debug("SENDING DATA: " + Arrays.deepToString(newDataSet.entrySet().toArray()));
        this.newData.add(newDataSet);

        makeStatusLabel();
        this.counter++;
    }

    private double getPollutantValue(double lat, double lon, double alt) {
        double randomItALittleBit = (this.rand.nextDouble() - 0.5) * 1.17;

        double value = Double.NaN;
        if (alt <= 1000.0 || alt >= 5000.0)
            value = 0.0d;
        else {
            value = Math.abs(Math.cos(lat + lon));
            value += randomItALittleBit;
            value = this.lastPollutantValue + value;
            this.lastPollutantValue = value;
        }

        value = GpxUtil.roundToDecimals(value, 3);
        return value;
    }

    /**
     * 
     * @return
     */
    private JPanel makeControlPanel() {
        if (this.controlPanel == null) {
            this.controlPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));

            JButton startButton = new JButton("Start");
            startButton.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    EventQueue.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            start();
                        }
                    });
                }
            });
            this.controlPanel.add(startButton);
            JButton stopButton = new JButton("Stop");
            stopButton.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    EventQueue.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            stop();
                        }
                    });
                }
            });
            this.controlPanel.add(stopButton);

            JLabel sleepTimeLabel = new JLabel("Time between points in seconds:");
            this.controlPanel.add(sleepTimeLabel);
            double spinnerMin = 0.1d;
            double spinnerMax = 10000.0d;
            SpinnerModel model = new SpinnerNumberModel(DEFAULT_INTERVAL_MILLIS / SECONDS_TO_MILLISECONDS,
                                                        spinnerMin,
                                                        spinnerMax,
                                                        0.1d);
            JSpinner sleepTimeSpinner = new JSpinner(model);
            sleepTimeSpinner.setToolTipText("Select time using controls or manual input within the range of "
                    + spinnerMin + " to " + spinnerMax + ".");

            sleepTimeSpinner.addChangeListener(new ChangeListener() {

                @Override
                public void stateChanged(ChangeEvent e) {
                    Object source = e.getSource();
                    if (source instanceof JSpinner) {
                        final JSpinner spinner = (JSpinner) source;

                        EventQueue.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                Double value = (Double) spinner.getValue();
                                value = Double.valueOf(value.doubleValue() * SECONDS_TO_MILLISECONDS);
                                FakeInputPlugin.this.sleepTimeMillis = value.longValue();
                            }
                        });
                    }
                    else
                        log.warn("Unsupported ChangeEvent, need JSpinner as source: " + e);
                }
            });
            // catch text change events without loosing the focus
            // JSpinner.DefaultEditor editor = (DefaultEditor) sleepTimeSpinner.getEditor();
            // not implemented, can be done using KeyEvent, but then it hast to be checked where in the text
            // field the keystroke was etc. --> too complicated.

            this.controlPanel.add(sleepTimeSpinner);
        }

        return this.controlPanel;
    }

    /**
     * 
     * @return
     */
    private JMenu makeMenu() {
        if (this.menu == null) {
            this.menu = new JMenu();
            JMenuItem start = new JMenuItem("Start");
            start.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    EventQueue.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            start();
                        }
                    });
                }
            });
            this.menu.add(start);
            JMenuItem stop = new JMenuItem("Stop");
            stop.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    EventQueue.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            stop();
                        }
                    });
                }
            });
            this.menu.add(stop);
            this.menu.addSeparator();

        }

        return this.menu;
    }

    /**
     * 
     * @return
     */
    private JPanel makePanel() {
        if (this.panel == null) {
            this.panel = new JPanel(new BorderLayout());
            this.panel.add(makeControlPanel(), BorderLayout.NORTH);
            this.panel.add(makeStatusLabel(), BorderLayout.SOUTH);
        }

        return this.panel;
    }

    /**
     * 
     * @return
     */
    private JLabel makeStatusLabel() {
        if (this.statusLabel == null)
            this.statusLabel = new JLabel();

        this.statusStringBuilder = new StringBuilder();
        this.statusStringBuilder.append("<html><p>");
        this.statusStringBuilder.append("<b>Status: </b>");
        this.statusStringBuilder.append(this.statusString);
        this.statusStringBuilder.append("</p><p><b>Center: </b>");
        this.statusStringBuilder.append(this.center);
        this.statusStringBuilder.append("</p><p><b>Angle: </b>");
        this.statusStringBuilder.append(this.angle);
        this.statusStringBuilder.append("</p><p><b>Altitude: </b>");
        this.statusStringBuilder.append(this.altitude);
        this.statusStringBuilder.append("</p></html>");

        EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                FakeInputPlugin.this.statusLabel.setText(FakeInputPlugin.this.statusStringBuilder.toString());
            }
        });
        return this.statusLabel;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {
        while ( !this.initialized) {
            try {
                Thread.sleep(THREAD_SLEEP_TIME_MASTER_WHILE_LOOP);
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        try {
            while (true) {
                if (this.running) {

                    innerRun();

                    try {
                        Thread.sleep(this.sleepTimeMillis);
                    }
                    catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                else {
                    try {
                        Thread.sleep(THREAD_SLEEP_TIME_MASTER_WHILE_LOOP);
                    }
                    catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        catch (Exception e) {
            log.error("error in run()", e);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.n52.ifgicopter.spf.common.IModule#shutdown()
     */
    @Override
    public void shutdown() throws Exception {
        //
    }

    /**
     * 
     */
    protected void start() {
        if (this.running)
            return;

        this.running = true;
        this.statusString = "Started!";
        makeStatusLabel();
        log.info("START with " + this.configFile);
    }

    /**
     * 
     */
    protected void stop() {
        if ( !this.running)
            return;

        this.running = false;
        this.statusString = "Stopped! (after " + this.counter + " points)";
        makeStatusLabel();
        log.info("STOP");
    }

}