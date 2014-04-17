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

package org.n52.ifgicopter.spf.xmlrpc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.xmlrpc.server.PropertyHandlerMapping;
import org.apache.xmlrpc.server.XmlRpcServer;
import org.apache.xmlrpc.server.XmlRpcServerConfigImpl;
import org.apache.xmlrpc.webserver.WebServer;
import org.n52.ifgicopter.spf.common.IModule;
import org.n52.ifgicopter.spf.gui.ModuleGUI;
import org.n52.ifgicopter.spf.input.IInputPlugin;

/**
 * This class provides an language independent interface using XML-RPC. Using this class an enduser of the
 * framework can push data to it using XML remote procedure calls. <br />
 * A combination of collecting data with XML-RPC and in addition with Java can be realized by extending this
 * class and calling {@link XMLRPCInputPlugin#populateData(Map)} inside the extending class. <br />
 * See doc/xml-rpc-example1.xml and doc/xml-rpc-example2.xml for example usage of this interface.
 * 
 * @author Matthes Rieke <m.rieke@uni-muenster.de>
 * 
 */
public class XMLRPCInputPlugin implements IInputPlugin {

    int status = IModule.STATUS_RUNNING;
    private String name;
    String errorString;
    private Properties config;
    private File file;
    List<Map<String, Object>> availableData = new ArrayList<Map<String, Object>>();
    private int port;

    /**
     * @param filename
     *        the config file
     * @throws IOException
     */
    @ConstructorParameters({"filename"})
    public XMLRPCInputPlugin(String filename) throws IOException {
        this.file = new File(filename);

        if ( !this.file.exists()) {
            throw new IllegalArgumentException("File '" + filename + "' does not exist.");
        }

        FileInputStream fis = new FileInputStream(this.file);
        this.config = new Properties();
        this.config.load(fis);

        String configPath = this.config.getProperty("PATH_TO_PLUGIN_DESCRIPTION");

        this.file = new File(configPath);

        if ( !this.file.exists()) {
            throw new IllegalArgumentException("Input Plugin description '" + configPath + "' does not exist.");
        }

        this.name = "XML-RPC-Plugin '" + this.config.getProperty("PLUGIN_NAME") + "'";
        this.port = Integer.parseInt(this.config.getProperty("PLUGIN_PORT"));
        
        fis.close();
    }

    @Override
    public InputStream getConfigFile() {
        try {
            return new FileInputStream(this.file);
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public List<Map<String, Object>> getNewData() {
        ArrayList<Map<String, Object>> result = null;
        synchronized (this.availableData) {
            result = new ArrayList<Map<String, Object>>(this.availableData);
            this.availableData.clear();
        }
        return result;
    }

    @Override
    public int getStatus() {
        return this.status;
    }

    @Override
    public String getStatusString() {
        if (this.status == IModule.STATUS_RUNNING) {
            return this.name + " running normally.";
        }
        return this.errorString;

    }

    @Override
    public ModuleGUI getUserInterface() {
        return null;
    }

    @Override
    public boolean hasNewData() {
        boolean result = false;
        synchronized (this.availableData) {
            result = !this.availableData.isEmpty();
        }
        return result;
    }

    @Override
    public void init() throws Exception {
        /*
         * start the mini HTTP server and wait for incoming connections
         */
        WebServer webServer = new WebServer(this.port);

        XmlRpcServer xmlRpcServer = webServer.getXmlRpcServer();

        PropertyHandlerMapping phm = new PropertyHandlerMapping();

        /*
         * use a factory (object based instead of class based) so we enable listening on multiple ports, one
         * per plugin
         */
        phm.setRequestProcessorFactoryFactory(new RPCListenerProcessorFactoryFactory(this.new RPCListener()));

        phm.addHandler("RPCListener", RPCListener.class);

        xmlRpcServer.setHandlerMapping(phm);

        XmlRpcServerConfigImpl serverConfig = (XmlRpcServerConfigImpl) xmlRpcServer.getConfig();
        serverConfig.setEnabledForExtensions(true);
        serverConfig.setContentLengthOptional(false);

        webServer.start();
    }

    @Override
    public void shutdown() throws Exception {
        //
    }

    /**
     * call this method to push new data to the plugin.
     * 
     * @param data
     *        new data as key-value-pairs
     * @return true if successed
     */
    protected boolean populateData(Map<String, Object> data) {
        boolean result = false;

        synchronized (this.availableData) {
            result = this.availableData.add(data);
        }
        return result;
    }

    /**
     * call this method to set the status of the plugin.
     * 
     * @param status
     *        new status: 1=running, 0=stopped
     * @param statusString
     *        description of the new status
     */
    protected void setStatus(int status, String statusString) {
        this.status = status;
        this.errorString = statusString;
    }

    /**
     * Class for accesing with RPC.
     * 
     * @author Matthes Rieke <m.rieke@uni-muenster.de>
     * 
     */
    public class RPCListener {

        /**
         * call this method to push new data to the plugin.
         * 
         * @param data
         *        new data as key-value-pairs
         * @return true if success
         */
        public boolean populateData(Map<String, Object> data) {
            return XMLRPCInputPlugin.this.populateData(data);
        }

        /**
         * call this method to set the status of the plugin.
         * 
         * @param status
         *        new status: 1=running, 0=stopped
         * @param statusString
         *        description of the new status
         * @return true if success
         */
        public boolean setStatus(int status, String statusString) {
            XMLRPCInputPlugin.this.setStatus(status, statusString);
            return true;
        }

    }
}
