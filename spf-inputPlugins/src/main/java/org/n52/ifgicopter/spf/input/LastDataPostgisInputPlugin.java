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

import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

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

/**
 * 
 * reads all available fields from given database table using the timeColumn to request only the latest value
 * 
 * @author Daniel Nüst (d.nuest@52north.org)
 * 
 */
public class LastDataPostgisInputPlugin extends PostgisInputPlugin {

    protected static Log log = LogFactory.getLog(LastDataPostgisInputPlugin.class);

    private JPanel controlPanel;

    private long lastNewDataRequest = System.currentTimeMillis();

    private JMenu menu;

    private long minimumMillisBetweenRequests = 500l;

    private boolean running;

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
    public LastDataPostgisInputPlugin(String host,
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
    public LastDataPostgisInputPlugin(String host,
                                      String port,
                                      String database,
                                      String user,
                                      String password,
                                      String table,
                                      String timeColumn,
                                      String where) {
        super(host, port, database, user, password, table, timeColumn, where);
    }

    private String getLatestRecordQuery() {
        StringBuilder sb = new StringBuilder();

        sb.append("SELECT * FROM ");
        sb.append(this.tableName);
        if (this.where != null) {
            sb.append(" WHERE ");
            sb.append(this.where);
        }

        sb.append(" ORDER BY ");
        sb.append(this.timeColumnName);
        sb.append(" DESC LIMIT 1");

        return sb.toString();
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.ifgi.nuest.sensorVis.spf.PostgisInputPlugin#getNewData()
     */
    @Override
    public List<Map<String, Object>> getNewData() {
        Statement statement;
        try {
            statement = getConnection().createStatement();
        }
        catch (SQLException e) {
            log.error(e);
            return null;
        }

        String query = getLatestRecordQuery();
        ResultSet resultSet;
        try {
            resultSet = statement.executeQuery(query);
        }
        catch (SQLException e) {
            log.error(e);
            return null;
        }

        // create data list from result set
        List<Map<String, Object>> newData = getData(resultSet);

        // close stuff
        try {
            resultSet.close();
            statement.close();
        }
        catch (SQLException e) {
            log.error(e);
        }

        this.statusString = "Received " + newData.size() + " records with query <i>" + getLatestRecordQuery() + "</i>";
        makeStatusLabel();

        appendToDataDisplay(newData);

        return newData;
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.ifgi.nuest.sensorVis.spf.PostgisInputPlugin#hasNewData()
     */
    @Override
    public boolean hasNewData() {
        if ( !this.running)
            return false;

        // ensure minimum time between requests
        long diff = System.currentTimeMillis() - this.lastNewDataRequest;
        if (diff < this.minimumMillisBetweenRequests) {
            return false;
        }
        this.lastNewDataRequest = System.currentTimeMillis();

        if (log.isDebugEnabled() && this.extensiveLog)
            log.debug("Checking database for new data: " + this.host);

        String query = getLatestRecordQuery();
        Statement statement;
        try {
            statement = getConnection().createStatement();
        }
        catch (SQLException e) {
            log.error(e);
            return false;
        }

        ResultSet resultSet;
        try {
            resultSet = statement.executeQuery(query);
        }
        catch (SQLException e) {
            log.error(e);
            return false;
        }

        Timestamp t;
        try {
            // no lines returned
            if ( !resultSet.next())
                return false;

            t = resultSet.getTimestamp(this.timeColumnName);

            if (log.isDebugEnabled() && this.extensiveLog)
                log.debug("Got newest data for " + t + ", last data was " + this.lastDataTimestamp);
        }
        catch (SQLException e) {
            log.error(e);
            return false;
        }

        if (t.equals(this.lastDataTimestamp))
            return false;

        return true;
    }

    @Override
    public void init() throws Exception {
        super.init();
        this.status = STATUS_RUNNING;
    }

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

            JLabel minTimeLabel = new JLabel("Mimium time database queries in milliseconds:");
            this.controlPanel.add(minTimeLabel);
            SpinnerModel model = new SpinnerNumberModel(this.minimumMillisBetweenRequests, 10d, 3600000d, 10d);
            JSpinner sleepTimeSpinner = new JSpinner(model);
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
                                setMinimumMillisBetweenRequests(value.longValue());
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

    /**
     * @param minimumMillisBetweenRequests
     *        the minimumMillisBetweenRequests to set
     */
    protected void setMinimumMillisBetweenRequests(long minimumMillisBetweenRequests) {
        this.minimumMillisBetweenRequests = minimumMillisBetweenRequests;
    }

    /**
     * 
     */
    public void start() {
        if (this.running)
            return;

        this.running = true;
        this.status = STATUS_RUNNING;
        this.statusString = "Started!";

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

        makeStatusLabel();
        log.info("stop");
    }

}
