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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.StyledDocument;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

import org.n52.ifgicopter.spf.gui.ModuleGUI;
import org.n52.ifgicopter.spf.output.IOutputPlugin;
import org.n52.ifgicopter.spf.xml.Plugin;

/**
 * Simple CSV file writer plugin. This class works only like a charm if the input is always the same. It
 * determines the input-fields the first time it gets data. If data differs from the property-set of the first
 * run it will ignore writing this as a deformed CSV would be the result.
 * 
 * @author Matthes Rieke <m.rieke@uni-muenster.de>
 * 
 */
public class FileWriterPlugin implements IOutputPlugin {

    private final Log log = LogFactory.getLog(FileWriterPlugin.class);

    private File file;
    private FileOutputStream fos;
    private String delimiter;
    private boolean firstRun;

    private ArrayList<String> outputFields = new ArrayList<String>();

    private JTextPane console;

    private StyledDocument consoleDoc;

    private ModuleGUI gui;

    private JPanel panel;

    private JCheckBox scrollLock;

    
    /**
     * default constructor using '|' as the delimiter
     */
    public FileWriterPlugin() {
    	this("|");
    }
    
    /**
     * @param delimiter
     *        the separator between each field.
     */
    @ConstructorParameters({"The CSV delimiter"})
    public FileWriterPlugin(String delimiter) {
        this.delimiter = delimiter;
    }

    @Override
    public void init() throws Exception {
        String time = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH_mm_ss").print(System.currentTimeMillis());
        this.file = new File("csv-logger-" + time + ".csv");
        boolean b = this.file.createNewFile();
        if ( !b)
            this.log.warn("File already exists!");

        this.firstRun = true;

        synchronized (this) {
            this.fos = new FileOutputStream(this.file);
        }

        this.gui = new ModuleGUI();
        this.panel = new JPanel(new BorderLayout());
        this.gui.setGui(this.panel);

        this.scrollLock = new JCheckBox("Scroll lock");
        this.scrollLock.setSelected(true);

        this.console = new JTextPane() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean getScrollableTracksViewportWidth() {
                return false;
            }
        };
        this.consoleDoc = this.console.getStyledDocument();

        this.console.setFont(Font.getFont(Font.MONOSPACED));
        this.console.setEditable(false);
        this.console.setBackground(Color.WHITE);

        JScrollPane jsp = new JScrollPane(this.console);

        jsp.setBackground(Color.white);

        this.panel.add(jsp);
        this.panel.add(this.scrollLock, BorderLayout.NORTH);
        this.panel.add(new JLabel("Writing to file '" + this.file.getAbsolutePath() + "'."), BorderLayout.SOUTH);
    }

    @Override
    public void shutdown() throws Exception {
        this.log.info("shutting down " + this.getClass().getSimpleName());

        synchronized (this) {
            this.fos.flush();
            this.fos.close();
        }

    }

    @Override
    public int processData(Map<Long, Map<String, Object>> map, Plugin plugin) {

        for (Long timestamp : map.keySet()) {
            Map<String, Object> items = map.get(timestamp);

            processSingleData(items, timestamp, plugin);
        }

        return STATUS_RUNNING;
    }

    @Override
    public int processSingleData(Map<String, Object> data, Long timestamp, Plugin plugin) {
        Long time;
        if (timestamp == null) {
            time = (Long) data.get(plugin.getTime().getProperty());
        }
        else {
            time = timestamp;
        }

        /*
         * check if we have to write the headings
         */
        synchronized (this) {
            if (this.firstRun) {
                StringBuilder firstLine = new StringBuilder();
                firstLine.append("time");
                firstLine.append(this.delimiter);

                List<String> allNeededOutputs = plugin.getOutputProperties();
                for (String item : allNeededOutputs) {
                    this.outputFields.add(item);
                    firstLine.append(item);
                    firstLine.append(this.delimiter);
                }

                for (String item : data.keySet()) {
                    /*
                     * if its in the output, we already have it
                     */
                    if (allNeededOutputs.contains(item))
                        continue;

                    this.outputFields.add(item);
                    firstLine.append(item);
                    firstLine.append(this.delimiter);
                }

                // firstLine = firstLine.substring(0, firstLine.length() - this.delimiter.length());
                firstLine.replace(firstLine.length() - this.delimiter.length(), firstLine.length(), "");

                firstLine.append(System.getProperty("line.separator"));

                writeToOutputs(firstLine.toString());

                this.firstRun = false;
            }
        }

        /*
         * create the new line
         */
        String str = new DateTime(time).toString() + this.delimiter;

        for (String key : this.outputFields) {
            Object value = data.get(key);

            if (key.equals(plugin.getTime().getProperty())) {
                value = new DateTime(value);
            }

            str += value + this.delimiter;
        }

        str = str.substring(0, str.length() - this.delimiter.length());
        str += System.getProperty("line.separator");

        writeToOutputs(str);

        return STATUS_RUNNING;
    }

    private synchronized void writeToOutputs(String line) {
        try {
            this.fos.write(line.getBytes());
            this.fos.flush();

            this.consoleDoc.insertString(this.consoleDoc.getLength(), line, null);
            if (this.scrollLock.isSelected()) {
                this.console.setCaretPosition(this.consoleDoc.getLength());
            }
        }
        catch (IOException e) {
            this.log.warn(e.getMessage(), e);
        }
        catch (BadLocationException e) {
            this.log.warn(e.getMessage(), e);
        }
    }

    @Override
    public String getStatusString() {
        return "FileWriterPlugin running.";
    }

    @Override
    public String getName() {
        return "Simple File Writer Plugin";
    }

    @Override
    public int getStatus() {
        return 1;
    }

    @Override
    public void restart() throws Exception {
        shutdown();
        this.outputFields.clear();
        init();
    }

    @Override
    public ModuleGUI getUserInterface() {
        return this.gui;
    }

}
