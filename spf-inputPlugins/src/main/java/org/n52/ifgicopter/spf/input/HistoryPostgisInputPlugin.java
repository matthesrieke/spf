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

import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * 
 * plugin loads all data from a specified database table, the query can be filtered in the user interface. the
 * loaded data can be made available in a specified update interval to simulate real-time updates.
 * 
 * @author Daniel Nüst (d.nuest@52north.org)
 * 
 */
public class HistoryPostgisInputPlugin extends PostgisInputPlugin implements Runnable {

    protected static Log log = LogFactory.getLog(HistoryPostgisInputPlugin.class);

    private static final long THREAD_SLEEP_TIME_MASTER_WHILE_LOOP = 100;

    private Queue<Map<String, Object>> allData = new LinkedBlockingQueue<Map<String, Object>>();

    private int allDataAtStartCount = 0;

    private JPanel controlPanel;

    private int counter = 0;

    private boolean initialized = false;

    private JMenu menu;

    private Collection<Map<String, Object>> newData = new ArrayList<Map<String, Object>>();

    private boolean running;

    private long sleepTimeMillis = 1000 * 2;

    private JButton whereButton;

    private JTextField whereField;

    /**
     * 
     * @param host
     * @param port
     * @param database
     * @param user
     * @param password
     * @param table
     * @param timeColumn
     */
    @ConstructorParameters({"host", "port", "database", "user", "password", "table", "timeColumn"})
    public HistoryPostgisInputPlugin(String host,
                                     String port,
                                     String database,
                                     String user,
                                     String password,
                                     String table,
                                     String timeColumn) {
        super(host, port, database, user, password, table, timeColumn);
    }

    /**
     * 
     * @param host
     * @param port
     * @param database
     * @param user
     * @param password
     * @param table
     * @param timeColumn
     * @param where
     */
    @ConstructorParameters({"host", "port", "database", "user", "password", "table", "timeColumn", "where clause"})
    public HistoryPostgisInputPlugin(String host,
                                     String port,
                                     String database,
                                     String user,
                                     String password,
                                     String table,
                                     String timeColumn,
                                     String where) {
        super(host, port, database, user, password, table, timeColumn, where);
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.ifgi.nuest.sensorVis.spf.PostgisInputPlugin#getNewData()
     */
    @Override
    public List<Map<String, Object>> getNewData() {
        List<Map<String, Object>> currentNewData = new ArrayList<Map<String, Object>>();
        currentNewData.addAll(this.newData);
        this.newData.clear();

        appendToDataDisplay(currentNewData);

        return currentNewData;
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.ifgi.nuest.sensorVis.spf.PostgisInputPlugin#hasNewData()
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
     * @see de.ifgi.nuest.sensorVis.spf.PostgisInputPlugin#init()
     */
    @Override
    public void init() throws Exception {
        super.init();

        ExecutorService exec = Executors.newSingleThreadExecutor();
        exec.submit(this);
        this.initialized = true;
    }

    /**
     * 
     */
    protected void innerRun() {
        if (this.allData.peek() != null) {
            Map<String, Object> polled = this.allData.poll();

            // Date now = new Date();
            // polled.put("time", Long.valueOf(now.getTime()));

            this.newData.add(polled);

            this.counter++;
            this.statusString = this.counter + " of " + this.allDataAtStartCount + " datasets processed.";
            makeStatusLabel();
        }
        else {
            log.info("No more datasets!");
            this.status = STATUS_NOT_RUNNING;
            stop(); // already sets a status, overwrite again:
            this.statusString = "No more data!";
            makeStatusLabel();
        }
    }

    /**
     * load all data from the table, sorted by date, ascending
     * 
     * @throws SQLException
     */
    protected void loadAllData() {
        this.allData.clear();
        this.newData.clear();
        clearDataDisplay();
        this.allDataAtStartCount = 0;
        this.counter = 0;

        StringBuilder sb = new StringBuilder();

        sb.append("SELECT * FROM ");
        sb.append(this.tableName);
        if (this.where != null && !this.where.isEmpty()) {
            if (log.isDebugEnabled())
                log.debug("Filtering: " + this.where);

            sb.append(" WHERE ");
            sb.append(this.where);
        }
        sb.append(" ORDER BY ");
        sb.append(this.timeColumnName);

        List<Map<String, Object>> data = null;
        try {
            if (log.isDebugEnabled())
                log.debug("Query: " + sb.toString());

            Connection conn = getConnection();
            if (conn == null) {
                log.error("Connection is null!");
                this.statusString = "<html><font color='red'>Connection is null!</font></html>";
                makeStatusLabel();
                return;
            }
            Statement statement = conn.createStatement();
            ResultSet resultSet = statement.executeQuery(sb.toString());
            data = getData(resultSet);
        }
        catch (SQLException e) {
            log.error(e);
            return;
        }

        this.allData.addAll(data);
        this.allDataAtStartCount = this.allData.size();
        this.statusString = "Loaded data (" + this.allDataAtStartCount + " rows) with query <i>" + sb.toString()
                + "</i>";
        makeStatusLabel();
    }

    /**
     * @return
     * 
     */
    @Override
    protected JPanel makeControlPanel() {
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
                                value = Double.valueOf(value.doubleValue() * 1000d);
                                setSleepTimeMillis(value.longValue());
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

            JLabel whereLabel = new JLabel("Where clause: ");
            this.whereField = new JTextField(this.where);
            this.whereField.setPreferredSize(new Dimension(200, 20));
            this.whereField.setToolTipText("Insert a valid SQL 'WHERE' clause here.");
            this.whereButton = new JButton("Load data");
            this.whereButton.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    EventQueue.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            updateWhere();

                            loadAllData();
                        }
                    });
                }
            });

            this.controlPanel.add(whereLabel);
            this.controlPanel.add(this.whereField);
            this.controlPanel.add(this.whereButton);
        }

        return this.controlPanel;
    }

    /**
     * @return
     * 
     */
    @Override
    protected JMenu makeMenu() {
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
        }

        return this.menu;
    }

    /*
     * 
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
     * @param sleepTime
     *        the sleepTime to set
     */
    public void setSleepTimeMillis(long sleepTime) {
        this.sleepTimeMillis = sleepTime;
    }

    /**
     * 
     */
    public void start() {
        if (this.allData.isEmpty()) {
            loadAllData();
        }

        if (this.running)
            return;

        this.running = true;
        this.status = STATUS_RUNNING;
        this.statusString = "Started!";
        this.whereButton.setEnabled(false);
        this.whereField.setEditable(false);

        makeStatusLabel();
        log.info("start");
    }

    /**
     * 
     */
    public void stop() {
        if ( !this.running)
            return;

        this.running = false;
        this.status = STATUS_NOT_RUNNING;
        this.statusString = "Stopped!";
        this.whereButton.setEnabled(true);
        this.whereField.setEditable(true);

        makeStatusLabel();
        log.info("stop");
    }

    /**
     * 
     */
    protected void updateWhere() {
        this.where = this.whereField.getText().trim();
    }

}
