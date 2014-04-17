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
/**
 * 
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
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;

import net.boplicity.xmleditor.XmlTextPane;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xmlbeans.XmlException;
import org.n52.ifgicopter.spf.gui.ModuleGUI;
import org.n52.ifgicopter.spf.input.IInputPlugin;

import com.topografix.gpx.x1.x1.GpxDocument;
import com.topografix.gpx.x1.x1.RteType;
import com.topografix.gpx.x1.x1.TrkType;
import com.topografix.gpx.x1.x1.TrksegType;
import com.topografix.gpx.x1.x1.WptType;

/**
 * 
 * Base class for waypoints-based feeding of positional data with generated values.
 * 
 * @author Daniel Nüst (d.nuest@52north.org)
 * 
 */
public abstract class GpxInputPlugin implements IInputPlugin, Runnable {

    private static Log log = LogFactory.getLog(GpxInputPlugin.class);

    private static final double SECONDS_TO_MILLISECONDS = 1000.0d;

    private static final long THREAD_SLEEP_TIME_MASTER_WHILE_LOOP = 100;

    /**
     * 
     * @return
     */
    protected static Log getLog() {
        return log;
    }

    private String configFile = "";

    private JPanel controlPanel;

    private int counter = 0;

    private GpxDocument gpx;

    /*
     * set an invalid default file
     */
    private String gpxFilePath = "";

    private ModuleGUI gui;

    private boolean initialized = false;

    private JMenu menu;

    private List<Map<String, Object>> newData = new ArrayList<Map<String, Object>>();

    private JPanel panel;

    protected String pluginName = "GPX Input";

    private boolean running = false;

    private long sleepTimeMillis = 1000 * 2;

    private int status = STATUS_NOT_RUNNING;

    private JLabel statusLabel;

    private String statusString = "...";

    private XmlTextPane textPane;

    @SuppressWarnings("unused")
    private WptType[] trackPoints;

    private boolean useTrackpoints = false;

    private List<WptType> wayPoints = new ArrayList<WptType>();

    private ListIterator<WptType> wptIter;

    /**
     * 
     */
    @ConstructorParameters({"configuration file"})
    public GpxInputPlugin(String configFile) {
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

    /**
     * 
     * @param p
     * @return
     */
    protected double[] getCoordinates(WptType p) {
        BigDecimal lat = p.getLat();
        BigDecimal lon = p.getLon();
        BigDecimal ele = p.getEle();

        return new double[] {lat.doubleValue(), lon.doubleValue(), ele.doubleValue()};
    }

    /**
     * @param currentWaypoint
     * @return
     */
    protected abstract Map<String, Object> getDataSet(WptType currentWaypoint);

    /**
     * @return the gpxFilePath
     */
    public String getGpxFilePath() {
        return this.gpxFilePath;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.n52.ifgicopter.spf.input.IInputPlugin#getName()
     */
    @Override
    public String getName() {
        return this.pluginName;
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

    /**
     * 
     * @param currentWaypoint
     * @return
     */
    protected Map<String, Object> getTimeAndCoordinateDataSet(WptType currentWaypoint) {
        Map<String, Object> newDataSet = new HashMap<String, Object>();
        Date now = new Date();
        double[] coords = getCoordinates(currentWaypoint);

        newDataSet.put("time", Long.valueOf(now.getTime()));
        newDataSet.put("latitude", Double.valueOf(coords[0]));
        newDataSet.put("longitude", Double.valueOf(coords[1]));
        newDataSet.put("altitude", Double.valueOf(coords[2]));

        return newDataSet;
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
        // gui
        this.gui = new ModuleGUI();

        // panel
        this.gui.setGui(makePanel());

        // menu
        this.gui.setMenu(makeMenu());

        ExecutorService exec = Executors.newSingleThreadExecutor();
        exec.submit(this);

        this.status = STATUS_RUNNING;
        this.initialized = true;

        // if the plugin was given a path at startup, read it NOW.
        if (this.gpxFilePath != null && this.gpx == null)
            readGpx();
    }

    /**
     * 
     */
    private void innerRun() {
        if (this.wptIter == null)
            return;

        if (this.wptIter.hasNext()) {
            WptType currentWaypoint = this.wptIter.next();

            Map<String, Object> newDataSet = getDataSet(currentWaypoint);

            log.debug("SENDING DATA: " + Arrays.deepToString(newDataSet.entrySet().toArray()));
            this.newData.add(newDataSet);

            this.counter++;
            this.statusString = this.counter + " of " + this.wayPoints.size() + " waypoints processed.";
            makeStatusLabel();
        }
        else {
            log.info("No more waypoints!");
            this.status = STATUS_NOT_RUNNING;
            stop(); // already sets a status, overwrite again:
            this.statusString = "No more waypoints!";
            makeStatusLabel();
        }
    }

    /**
     * @return
     * 
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
            JButton selectFileButton = new JButton("Select GPX File");
            selectFileButton.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    selectGpxFileAction();
                }
            });
            this.controlPanel.add(selectFileButton);

            JLabel sleepTimeLabel = new JLabel("Time between points in seconds:");
            this.controlPanel.add(sleepTimeLabel);
            double spinnerMin = 0.5d;
            double spinnerMax = 10000.0d;
            SpinnerModel model = new SpinnerNumberModel(2.0d, spinnerMin, spinnerMax, 0.1d);
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
                                setSleepTimeMillis(value.longValue());
                            }
                        });
                    }
                    else
                        getLog().warn("Unsupported ChangeEvent, need JSpinner as source: " + e);
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
     * @return
     * 
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

            JMenuItem selectFile = new JMenuItem("Select GPX File");
            selectFile.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    selectGpxFileAction();
                }
            });
            this.menu.add(selectFile);
            this.menu.addSeparator();
        }

        return this.menu;
    }

    /**
     * @return
     * 
     */
    private JPanel makePanel() {
        if (this.panel == null) {
            this.panel = new JPanel(new BorderLayout());
            this.panel.add(makeControlPanel(), BorderLayout.NORTH);
            this.panel.add(makeStatusLabel(), BorderLayout.SOUTH);
            this.textPane = new XmlTextPane();
            this.textPane.setEditable(false);
            JScrollPane scrollPane = new JScrollPane(this.textPane);
            this.panel.add(scrollPane, BorderLayout.CENTER);
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

        StringBuilder sb = new StringBuilder();
        sb.append("<html><p>");
        sb.append("<b>Status: </b>");
        sb.append(this.statusString);
        sb.append("</p><p><b>GPX File: </b>");
        sb.append(this.gpxFilePath);
        sb.append("</p><p><b>useTrackpoints: </b>");
        sb.append(this.useTrackpoints);
        sb.append("</p></html>");

        this.statusLabel.setText(sb.toString());
        return this.statusLabel;
    }

    /**
     * 
     */
    private void readGpx() {
        // read points from gpx
        File gpxfile = new File(this.gpxFilePath);
        if ( !gpxfile.isFile()) {
            String msg = "Not a valid file: " + gpxfile;
            log.warn(msg);
            this.statusString = msg;
            makeStatusLabel();
            return;
        }

        this.counter = 0;
        this.wayPoints = null;
        this.wayPoints = new ArrayList<WptType>();
        this.newData.clear();

        this.wptIter = null;
        this.gpx = null;

        try {
            this.gpx = GpxDocument.Factory.parse(gpxfile);

            boolean valid = this.gpx.validate();
            if (log.isDebugEnabled()) {
                log.debug("read gpx file, is valid=" + valid);
            }
        }
        catch (XmlException e) {
            e.printStackTrace();
            return;
        }
        catch (IOException e) {
            e.printStackTrace();
            return;
        }

        WptType[] waypointArray = this.gpx.getGpx().getWptArray();
        TrkType[] trackArray = this.gpx.getGpx().getTrkArray();
        RteType[] routeArray = this.gpx.getGpx().getRteArray();

        log.info("Loaded " + trackArray.length + " tracks and " + waypointArray.length + " waypoints and "
                + routeArray.length + " routes.");

        if (trackArray.length > 0) {
            TrkType track = trackArray[0];
            TrksegType[] trackSegmentArray = track.getTrksegArray();
            TrksegType trackSegment = trackSegmentArray[0];

            WptType[] trackPointArray = trackSegment.getTrkptArray();
            log.info("loaded tracksegment with " + trackPointArray.length + " trackpoints");

            this.trackPoints = trackPointArray;
        }

        if (waypointArray.length > 0) {
            this.wayPoints = Arrays.asList(waypointArray);
        }

        this.textPane.setText(this.gpx.xmlText());
        this.wptIter = this.wayPoints.listIterator();

        this.statusString = "Loaded " + waypointArray.length + " waypoints from file " + this.gpxFilePath;
        makeStatusLabel();
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

    /**
     * 
     */
    protected void selectGpxFileAction() {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                JFileChooser fc = new JFileChooser();
                fc.setDialogTitle("Select GPX File");
                fc.setFileFilter(new FileFilter() {

                    @Override
                    public boolean accept(File f) {
                        if (f.getName().toLowerCase().endsWith("gpx") || f.getName().toLowerCase().endsWith("xml"))
                            return true;
                        if (f.isDirectory())
                            return true;

                        return false;
                    }

                    @Override
                    public String getDescription() {
                        return "GPX file";
                    }
                });
                File gpxfile = new File(getGpxFilePath());
                if (gpxfile.isFile()) {
                    fc.setCurrentDirectory(gpxfile.getParentFile());
                }

                int returnVal = fc.showOpenDialog(getUserInterface().getGui());

                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    File file = fc.getSelectedFile();
                    setGpxFilePath(file.getAbsolutePath());
                }
                else {
                    getLog().debug("Open command cancelled by user.");
                }

                fc = null;
            }
        });
    }

    /**
     * @param gpxFilePath
     *        the gpxFilePath to set
     */
    public void setGpxFilePath(String gpxFilePath) {
        log.info("Setting file path: " + gpxFilePath);
        this.gpxFilePath = gpxFilePath;

        makeStatusLabel();

        // if the plugin was given a path at startup, read it LATER.
        if (this.initialized)
            readGpx();
    }

    /**
     * @param sleepTime
     *        the sleepTime to set
     */
    public void setSleepTimeMillis(long sleepTime) {
        this.sleepTimeMillis = sleepTime;
        // if (log.isDebugEnabled())
        // log.debug("Changed sleep time to " + this.sleepTimeMillis);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.n52.ifgicopter.spf.common.IModule#shutdown()
     */
    @Override
    public void shutdown() throws Exception {
        this.gpx = null;
    }

    /**
     * 
     */
    protected void start() {
        if (this.running)
            return;

        if (this.gpx == null) {
            this.statusString = "No input file set!";
        }
        else {
            this.running = true;
            this.statusString = "Started!";
            log.info("START with " + this.configFile);
        }

        makeStatusLabel();
    }

    /**
     * 
     */
    protected void stop() {
        if ( !this.running)
            return;

        this.running = false;
        this.statusString = "Stopped! (after " + this.counter + " of " + this.wayPoints.size() + " waypoints)";

        makeStatusLabel();
        log.info("STOP");
    }

}
