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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.text.DefaultCaret;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.n52.ifgicopter.spf.gui.ModuleGUI;
import org.n52.ifgicopter.spf.input.IInputPlugin;
import org.postgis.Geometry;
import org.postgis.PGgeometry;
import org.postgis.Point;

/**
 * 
 * 
 * @author Daniel Nüst (d.nuest@52north.org)
 * 
 */
public abstract class PostgisInputPlugin implements IInputPlugin {

    private static class Connector implements Callable<Connection> {

        private Log logger = LogFactory.getLog(Connector.class);

        private PostgisInputPlugin plugin;

        /**
         * 
         * @param postgisInputPlugin
         */
        public Connector(PostgisInputPlugin postgisInputPlugin) {
            this.plugin = postgisInputPlugin;
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.util.concurrent.Callable#call()
         */
        @Override
        public Connection call() throws Exception {
            Connection c = null;

            try {
                String connectionUrl = "jdbc:postgresql:"
                        + (this.plugin.host != null ? ("//" + this.plugin.host)
                                + (this.plugin.port != null ? ":" + this.plugin.port : "") + "/" : "")
                        + this.plugin.databaseName;
                c = DriverManager.getConnection(connectionUrl, this.plugin.user, this.plugin.password);

                this.logger.debug("Database connection established!");
            }
            catch (SQLException e) {
                this.logger.error(e);
                List<Map<String, Object>> currentNewData = new ArrayList<Map<String, Object>>();
                Map<String, Object> map = new HashMap<String, Object>();
                map.put("ERROR", e.getMessage());
                map.put("", e.toString());
                currentNewData.add(map);
                this.plugin.appendToDataDisplay(currentNewData);
            }

            return c;
        }

    }

    private static final String CONFIG_FILE = "D:/workspace/SensorVis/config/spf/input-noise.xml";

    private static Log log = LogFactory.getLog(PostgisInputPlugin.class);

    private static final String PLUGIN_NAME = "PostGIS Input Plugin";

    private Connection connection = null;

    protected String databaseName = "postgis";

    protected String driver = "org.postgresql.Driver";

    protected boolean extensiveLog = false;

    private Future<Connection> futureConnection;

    protected ModuleGUI gui;

    protected String host = "localhost";

    protected Timestamp lastDataTimestamp;

    protected JPanel panel;

    protected String password = "postgres";

    protected String port = "5432";

    protected int status = STATUS_NOT_RUNNING;

    protected JLabel statusLabel;

    protected String statusString = "...";

    protected String tableName;

    private JTextArea textArea;

    protected String timeColumnName;

    protected String user = "postgres";

    protected String where = null;

    /**
     * 
     * @param driver
     * @param host
     * @param port
     * @param database
     * @param user
     * @param password
     */
    @ConstructorParameters({"host", "port", "database", "user", "password", "table", "timeColumn"})
    public PostgisInputPlugin(String host,
                              String port,
                              String database,
                              String user,
                              String password,
                              String table,
                              String timeColumn) {
        this(host, port, database, user, password, table, timeColumn, null);
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
    public PostgisInputPlugin(String host,
                              String port,
                              String database,
                              String user,
                              String password,
                              String table,
                              String timeColumn,
                              String where) {
        this.host = host;
        this.port = port;
        this.databaseName = database;
        this.user = user;
        this.password = password;
        this.tableName = table;
        this.timeColumnName = timeColumn;
        this.where = where;
    }

    /**
     * 
     * @param currentNewData
     */
    protected void appendToDataDisplay(List<Map<String, Object>> currentNewData) {
        String s = this.textArea.getText();

        StringBuilder sb = new StringBuilder();
        sb.append(s);
        for (Map<String, Object> map : currentNewData) {
            sb.append(Arrays.toString(map.entrySet().toArray()));
            sb.append("\n");
        }

        this.textArea.setText(sb.toString());
    }

    /**
     * 
     */
    protected void clearDataDisplay() {
        this.textArea.setText("");
    }

    /**
     * @param resultSet
     * @return
     * @throws SQLException
     */
    protected ArrayList<String> getColumnNames(ResultSet resultSet) throws SQLException {
        ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
        int cols = resultSetMetaData.getColumnCount();

        // get the column names
        ArrayList<String> columnNames = new ArrayList<String>();
        for (int i = 1; i <= cols; i++) {
            columnNames.add(resultSetMetaData.getColumnName(i));
        }
        if (log.isDebugEnabled())
            log.debug("Found columns: " + Arrays.toString(columnNames.toArray()));
        return columnNames;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.n52.ifgicopter.spf.input.IInputPlugin#getConfigFile()
     */
    @Override
    public InputStream getConfigFile() {
        try {
            return new FileInputStream(new File(CONFIG_FILE));
        }
        catch (FileNotFoundException e) {
            log.error(e);
        }
        return null;
    }

    /**
     * @return the connection
     */
    protected Connection getConnection() {
        if (this.connection == null) {
            try {
                this.connection = this.futureConnection.get();
            }
            catch (Exception e) {
                log.fatal(e);
            }
        }

        return this.connection;
    }

    /**
     * 
     * @param resultSet
     * @return
     */
    protected List<Map<String, Object>> getData(ResultSet resultSet) {
        List<Map<String, Object>> data = new ArrayList<Map<String, Object>>();

        try {
            ArrayList<String> columnNames = getColumnNames(resultSet);

            while (resultSet.next()) {
                Map<String, Object> dataElem = getDataElement(resultSet, columnNames);
                data.add(dataElem);
            } // while(resultSet.next())
        }
        catch (SQLException e) {
            log.error(e);
        }

        return data;
    }

    /**
     * @param resultSet
     * @param columnNames
     * @return
     * @throws SQLException
     */
    protected Map<String, Object> getDataElement(ResultSet resultSet, ArrayList<String> columnNames) throws SQLException {
        Map<String, Object> dataElem = new HashMap<String, Object>();

        for (String c : columnNames) {
            Object o = resultSet.getObject(c);
            if (log.isDebugEnabled() && this.extensiveLog)
                log.debug("Adding " + c + " = " + o);

            if (o instanceof Timestamp) {
                Timestamp t = (Timestamp) o;
                this.lastDataTimestamp = t;
                if (log.isDebugEnabled() && this.extensiveLog)
                    log.debug("Latest data as of " + this.lastDataTimestamp);

                // dataElem.put(c, t);
                dataElem.put("time", Long.valueOf(t.getTime()));
            }
            else if (o instanceof PGgeometry) {
                PGgeometry geom = (PGgeometry) o;
                Geometry geometry = geom.getGeometry();

                if (geometry.getType() == Geometry.POINT) {
                    Point p = (Point) geometry;
                    dataElem.put("x", Double.valueOf(p.getX()));
                    dataElem.put("y", Double.valueOf(p.getY()));
                    dataElem.put("z", Double.valueOf(p.getZ()));
                    dataElem.put("SRID", Integer.valueOf(p.getSrid()));
                }
                else
                    log.warn("unhandled geometry: " + geom);

                // dataElem.put(c, geometry);
            }
            else
                dataElem.put(c, o);
        }

        if (log.isDebugEnabled() && this.extensiveLog)
            log.debug("Added new data element: " + Arrays.toString(dataElem.entrySet().toArray()));
        return dataElem;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.n52.ifgicopter.spf.input.IInputPlugin#getName()
     */
    @Override
    public String getName() {
        return PLUGIN_NAME;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.n52.ifgicopter.spf.input.IInputPlugin#getNewData()
     */
    @Override
    public abstract List<Map<String, Object>> getNewData();

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
    public abstract boolean hasNewData();

    /*
     * (non-Javadoc)
     * 
     * @see org.n52.ifgicopter.spf.common.IModule#init()
     */
    @Override
    public void init() throws Exception {
        log.info("initializing ...");

        // create GUI
        this.gui = new ModuleGUI();
        this.panel = new JPanel();
        this.panel.setLayout(new BorderLayout());
        this.panel.add(makeControlPanel(), BorderLayout.NORTH);
        this.panel.add(makeStatusLabel(), BorderLayout.SOUTH);
        this.textArea = new JTextArea();
        this.textArea.setEditable(false);
        DefaultCaret caret = (DefaultCaret) this.textArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        JScrollPane scrollPane = new JScrollPane(this.textArea);
        this.panel.add(scrollPane, BorderLayout.CENTER);

        this.gui.setMenu(makeMenu());
        this.gui.setGui(this.panel);

        // try loading the database driver
        try {
            Class.forName(this.driver);
        }
        catch (ClassNotFoundException e) {
            log.error(e);
        }

        // create database connection
        Connector connector = new Connector(this);
        this.futureConnection = Executors.newSingleThreadExecutor().submit(connector);

        log.info("initialized.");
    }

    /**
     * 
     * @return
     */
    protected abstract JPanel makeControlPanel();

    /**
     * 
     * @return
     */
    protected abstract JMenu makeMenu();

    /**
     * 
     * @return
     */
    protected JLabel makeStatusLabel() {
        if (this.statusLabel == null)
            this.statusLabel = new JLabel();

        StringBuilder sb = new StringBuilder();
        sb.append("<html><p>");
        sb.append("<b>Status: </b>");
        sb.append(this.statusString);
        sb.append("</p><p><b>Database host: </b>");
        sb.append(this.host);
        sb.append("</p><p><b>Database name: </b>");
        sb.append(this.databaseName);
        sb.append("</p><p><b>Table name: </b>");
        sb.append(this.tableName);
        sb.append("</p><p><b>Time column: </b>");
        sb.append(this.timeColumnName);
        sb.append("</p></html>");

        this.statusLabel.setText(sb.toString());
        return this.statusLabel;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.n52.ifgicopter.spf.common.IModule#shutdown()
     */
    @Override
    public void shutdown() throws Exception {
        // close database connection, might block if the connection is not established yet.
        Connection conn = getConnection();
        if (conn != null)
            conn.close();
    }
}
