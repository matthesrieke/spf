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
 * the terms of the GNU General Public License serviceVersion 2 as published by the
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

package org.n52.ifgicopter.spf.output;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.n52.ifgicopter.spf.gui.ModuleGUI;
import org.n52.ifgicopter.spf.output.IOutputPlugin;
import org.n52.ifgicopter.spf.xml.Plugin;

/**
 * O&M file outputter.
 * 
 * @author Matthes Rieke <m.rieke@uni-muenster.de>
 * 
 */
public class OMFilePlugin implements IOutputPlugin {

    private final Log log = LogFactory.getLog(OMFilePlugin.class);
    private String logDir;

    /**
     * @param logDir
     *        absoulte or relative path to the directory for log files.
     */
    public OMFilePlugin(String logDir) {
        this.logDir = logDir;
    }

    @Override
    public void init() throws Exception {
        boolean b = new File(this.logDir).mkdir();
        if ( !b)
            this.log.warn("Directory was NOT created!");

    }

    @Override
    public void shutdown() throws Exception {
        this.log.info("shutting down " + this.getClass().getSimpleName());
    }

    @Override
    public int processData(Map<Long, Map<String, Object>> data, Plugin plugin) {
        return STATUS_RUNNING;
    }

    @Override
    public int processSingleData(Map<String, Object> data, Long timestamp, Plugin plugin) {
        File f = new File(this.logDir + File.pathSeparator + "om-output-" + data.get(plugin.getTime().getProperty())
                + ".xml");
        try {
            f.createNewFile();
        }
        catch (IOException e) {
            this.log.warn(null, e);
            return STATUS_NOT_RUNNING;
        }

        if ( !f.exists())
            return STATUS_NOT_RUNNING;

        FileOutputStream fos;
        try {
            fos = new FileOutputStream(f);

            fos.write("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>".getBytes());
            fos.write(System.getProperty("line.separator").getBytes());
            fos.write(new OMRepresentation(data, plugin).toXML().getBytes());

            fos.flush();
            fos.close();
        }
        catch (FileNotFoundException e) {
            this.log.warn(null, e);
        }
        catch (IOException e) {
            this.log.warn(null, e);
        }

        return STATUS_RUNNING;
    }

    @Override
    public String getStatusString() {
        return "OMFilePlugin running.";
    }

    @Override
    public String getName() {
        return "O&M File Plugin";
    }

    @Override
    public int getStatus() {
        return 1;
    }

    @Override
    public void restart() throws Exception {
        // TODO Auto-generated method stub

    }

    @Override
    public ModuleGUI getUserInterface() {
        return null;
    }

}
