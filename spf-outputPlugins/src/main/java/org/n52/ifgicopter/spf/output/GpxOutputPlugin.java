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
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;

import net.boplicity.xmleditor.XmlTextPane;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlOptions;
import org.n52.ifgicopter.spf.common.IModule;
import org.n52.ifgicopter.spf.gui.ModuleGUI;
import org.n52.ifgicopter.spf.output.IOutputPlugin;
import org.n52.ifgicopter.spf.xml.Location;
import org.n52.ifgicopter.spf.xml.Plugin;

import com.topografix.gpx.x1.x1.GpxDocument;
import com.topografix.gpx.x1.x1.WptType;

/**
 * 
 * http://de.wikipedia.org/wiki/GPS_Exchange_Format
 * 
 * @author Daniel Nüst (d.nuest@52north.org)
 * 
 */
public class GpxOutputPlugin implements IOutputPlugin {

    /**
     * 
     * http://www.java2s.com/Code/Java/Swing-JFC/AverticallayoutmanagersimilartojavaawtFlowLayout.htm
     * 
     * A vertical layout manager similar to java.awt.FlowLayout. Like FlowLayout components do not expand to
     * fill available space except when the horizontal alignment is <code>BOTH</code> in which case components
     * are stretched horizontally. Unlike FlowLayout, components will not wrap to form another column if there
     * isn't enough space vertically. VerticalLayout can optionally anchor components to the top or bottom of
     * the display area or center them between the top and bottom.
     * 
     * Revision date 12th July 2001
     * 
     * @author Colin Mummery e-mail: colin_mummery@yahoo.com Homepage:www.kagi.com/equitysoft - Based on
     *         'FlexLayout' in Java class libraries Vol 2 Chan/Lee Addison-Wesley 1998
     * 
     *         THIS PROGRAM IS PROVIDED "AS IS" WITHOUT ANY WARRANTIES (OR CONDITIONS), EXPRESS OR IMPLIED
     *         WITH RESPECT TO THE PROGRAM, INCLUDING THE IMPLIED WARRANTIES (OR CONDITIONS) OF
     *         MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE ENTIRE RISK ARISING OUT OF USE OR
     *         PERFORMANCE OF THE PROGRAM AND DOCUMENTATION REMAINS WITH THE USER.
     */
    protected class VerticalLayout implements LayoutManager {

        /**
         * The horizontal alignment constant that designates stretching the component horizontally.
         */
        public final static int BOTH = 3;
        /**
         * The anchoring constant that designates anchoring to the bottom of the display area
         */
        public final static int BOTTOM = 2;
        /**
         * The horizontal alignment constant that designates centering. Also used to designate center
         * anchoring.
         */
        public final static int CENTER = 0;
        /**
         * The horizontal alignment constant that designates left justification.
         */
        public final static int LEFT = 2;

        /**
         * The horizontal alignment constant that designates right justification.
         */
        public final static int RIGHT = 1;
        /**
         * The anchoring constant that designates anchoring to the top of the display area
         */
        public final static int TOP = 1;
        private int alignment; // LEFT, RIGHT, CENTER or BOTH...how the components are justified
        private int anchor; // TOP, BOTTOM or CENTER ...where are the components positioned in an overlarge
                            // space
        private int vgap; // the vertical vgap between components...defaults to 5

        // private Hashtable comps;

        /**
         * Constructs an instance of VerticalLayout with a vertical vgap of 5 pixels, horizontal centering and
         * anchored to the top of the display area.
         */
        public VerticalLayout() {
            this(5, CENTER, TOP);
        }

        /**
         * Constructs a VerticalLayout instance with horizontal centering, anchored to the top with the
         * specified vgap
         * 
         * @param vgap
         *        An int value indicating the vertical seperation of the components
         */
        public VerticalLayout(int vgap) {
            this(vgap, CENTER, TOP);
        }

        /**
         * Constructs a VerticalLayout instance anchored to the top with the specified vgap and horizontal
         * alignment
         * 
         * @param vgap
         *        An int value indicating the vertical seperation of the components
         * @param alignment
         *        An int value which is one of <code>RIGHT, LEFT, CENTER, BOTH</code> for the horizontal
         *        alignment.
         */
        public VerticalLayout(int vgap, int alignment) {
            this(vgap, alignment, TOP);
        }

        /**
         * Constructs a VerticalLayout instance with the specified vgap, horizontal alignment and anchoring
         * 
         * @param vgap
         *        An int value indicating the vertical seperation of the components
         * @param alignment
         *        An int value which is one of <code>RIGHT, LEFT, CENTER, BOTH</code> for the horizontal
         *        alignment.
         * @param anchor
         *        An int value which is one of <code>TOP, BOTTOM, CENTER</code> indicating where the
         *        components are to appear if the display area exceeds the minimum necessary.
         */
        public VerticalLayout(int vgap, int alignment, int anchor) {
            this.vgap = vgap;
            this.alignment = alignment;
            this.anchor = anchor;
        }

        /*
         * Not used by this class
         * 
         * (non-Javadoc)
         * 
         * @see java.awt.LayoutManager#addLayoutComponent(java.lang.String, java.awt.Component)
         */
        @Override
        public void addLayoutComponent(String name, Component comp) {
            //
        }

        // -----------------------------------------------------------------------------
        /**
         * Lays out the container.
         */
        @Override
        public void layoutContainer(Container parent) {
            Insets insets = parent.getInsets();
            synchronized (parent.getTreeLock()) {
                int n = parent.getComponentCount();
                Dimension pd = parent.getSize();
                int y = 0;
                // work out the total size
                for (int i = 0; i < n; i++) {
                    Component c = parent.getComponent(i);
                    Dimension d = c.getPreferredSize();
                    y += d.height + this.vgap;
                }
                y -= this.vgap; // otherwise there's a vgap too many
                // Work out the anchor paint
                if (this.anchor == TOP)
                    y = insets.top;
                else if (this.anchor == CENTER)
                    y = (pd.height - y) / 2;
                else
                    y = pd.height - y - insets.bottom;
                // do layout
                for (int i = 0; i < n; i++) {
                    Component c = parent.getComponent(i);
                    Dimension d = c.getPreferredSize();
                    int x = insets.left;
                    int wid = d.width;
                    if (this.alignment == CENTER)
                        x = (pd.width - d.width) / 2;
                    else if (this.alignment == RIGHT)
                        x = pd.width - d.width - insets.right;
                    else if (this.alignment == BOTH)
                        wid = pd.width - insets.left - insets.right;
                    c.setBounds(x, y, wid, d.height);
                    y += d.height + this.vgap;
                }
            }
        }

        // ----------------------------------------------------------------------------
        private Dimension layoutSize(Container parent, boolean minimum) {
            Dimension dim = new Dimension(0, 0);
            Dimension d;
            synchronized (parent.getTreeLock()) {
                int n = parent.getComponentCount();
                for (int i = 0; i < n; i++) {
                    Component c = parent.getComponent(i);
                    if (c.isVisible()) {
                        d = minimum ? c.getMinimumSize() : c.getPreferredSize();
                        dim.width = Math.max(dim.width, d.width);
                        dim.height += d.height;
                        if (i > 0)
                            dim.height += this.vgap;
                    }
                }
            }
            Insets insets = parent.getInsets();
            dim.width += insets.left + insets.right;
            dim.height += insets.top + insets.bottom + this.vgap + this.vgap;
            return dim;
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.awt.LayoutManager#minimumLayoutSize(java.awt.Container)
         */
        @Override
        public Dimension minimumLayoutSize(Container parent) {
            return layoutSize(parent, false);
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.awt.LayoutManager#preferredLayoutSize(java.awt.Container)
         */
        @Override
        public Dimension preferredLayoutSize(Container parent) {
            return layoutSize(parent, false);
        }

        /*
         * Not used by this class
         * 
         * (non-Javadoc)
         * 
         * @see java.awt.LayoutManager#removeLayoutComponent(java.awt.Component)
         */
        @Override
        public void removeLayoutComponent(Component comp) {
            //
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return getClass().getName() + "[vgap=" + this.vgap + " align=" + this.alignment + " anchor=" + this.anchor
                    + "]";
        }
    }

    private static final String DEFAULT_GPX_FILE_PATH = "data/out.gpx";

    private static final int DEFAULT_INDENT = 4;

    public static final XmlOptions GPX_OPTIONS = new XmlOptions();

    protected static Log log = LogFactory.getLog(GpxOutputPlugin.class);

    private static final String NO_DATA_LIST_ELEMENT = "[NO DATA]";

    private static final String PLUGIN_NAME = "GPX Output";

    private static final String SAVE_CURRENT_FILE_TEXT = "Save Current File";

    private static final String SELECT_GPX_FILE_TEXT = "Select GPX File";

    static {
        GPX_OPTIONS.setSavePrettyPrint();
        GPX_OPTIONS.setSavePrettyPrintIndent(DEFAULT_INDENT);
        HashMap<String, String> suggestedPrefixes = new HashMap<String, String>();
        suggestedPrefixes.put("http://www.topografix.com/GPX/1/1", "gpx");

        GPX_OPTIONS.setSaveSuggestedPrefixes(suggestedPrefixes);
    }

    protected Set<String> availableDataKeys = new HashSet<String>();

    private int counter = 0;

    private GpxDocument gpx;

    private String gpxFilePath = null;

    private ModuleGUI gui;

    private JMenu menu;

    protected JLabel outputFileLabel;

    private JPanel panel;

    protected XmlTextPane textPane;

    protected Collection<String> valuesToSave = new ArrayList<String>();

    protected JList valuesToSaveList;

    /**
     * 
     */
    @ConstructorParameters({})
    public GpxOutputPlugin() {
        this(DEFAULT_GPX_FILE_PATH);
    }

    /**
     * 
     */
    @ConstructorParameters({"The output file (including path), where to store the generated GPX file. Defaults to '"
            + DEFAULT_GPX_FILE_PATH + "'."})
    public GpxOutputPlugin(String outputPath) {
        log.info("NEW " + this);
        setGpxFilePath(outputPath);
    }

    /**
     * @return the gpxFilePath
     */
    public String getGpxFilePath() {
        return this.gpxFilePath;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.n52.ifgicopter.spf.output.IOutputPlugin#getName()
     */
    @Override
    public String getName() {
        return PLUGIN_NAME;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.n52.ifgicopter.spf.output.IOutputPlugin#getStatus()
     */
    @Override
    public int getStatus() {
        return IModule.STATUS_RUNNING;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.n52.ifgicopter.spf.output.IOutputPlugin#getStatusString()
     */
    @Override
    public String getStatusString() {
        return "Target file: " + this.gpxFilePath;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.n52.ifgicopter.spf.output.IOutputPlugin#getUserInterface()
     */
    @Override
    public ModuleGUI getUserInterface() {
        return this.gui;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.n52.ifgicopter.spf.common.IModule#init()
     */
    @Override
    public void init() throws Exception {
        this.gpx = GpxDocument.Factory.newInstance();
        this.gpx.addNewGpx();

        this.gui = new ModuleGUI();

        this.gui.setMenu(makeMenu());
        this.gui.setGui(makeGUI());

        setGpxFilePath(DEFAULT_GPX_FILE_PATH);

        updateGUI();
    }

    /**
     * 
     * @return
     */
    private JPanel makeGUI() {
        if (this.panel == null) {
            this.panel = new JPanel(new BorderLayout());
            JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
            JButton selectFileButton = new JButton(SELECT_GPX_FILE_TEXT);
            selectFileButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    selectGpxFileAction();
                }
            });
            controlPanel.add(selectFileButton);

            this.outputFileLabel = new JLabel();
            controlPanel.add(this.outputFileLabel);

            JButton saveFileButton = new JButton(SAVE_CURRENT_FILE_TEXT);
            saveFileButton.setToolTipText("Does also save manual changes undless model changes were made.");
            saveFileButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    saveCurrentFile();
                }
            });
            controlPanel.add(saveFileButton);

            // JCheckBox scrollLockCheckBox = new JCheckBox("scroll lock");
            // scrollLockCheckBox.setSelected(true);
            // scrollLockCheckBox.addActionListener(new ActionListener() {
            // @Override
            // public void actionPerformed(final ActionEvent e) {
            // EventQueue.invokeLater(new Runnable() {
            //
            // @Override
            // public void run() {
            // JCheckBox source = (JCheckBox) e.getSource();
            // // GpxOutputPlugin.this.textPane.setAutoscrolls(source.isSelected());
            // }
            // });
            // }
            // });
            // controlPanel.add(scrollLockCheckBox);

            this.panel.add(controlPanel, BorderLayout.NORTH);

            JPanel valuesPanel = new JPanel(new VerticalLayout(5, VerticalLayout.LEFT));
            DefaultListModel listModel = new DefaultListModel();
            listModel.addElement(NO_DATA_LIST_ELEMENT);
            this.valuesToSaveList = new JList(listModel);
            this.valuesToSaveList.setVisibleRowCount(2);
            this.valuesToSaveList.setToolTipText("Select (multiple) values to include in the GPX file [Multiple using <Ctrl>].");
            this.valuesToSaveList.addListSelectionListener(new ListSelectionListener() {

                @Override
                public void valueChanged(final ListSelectionEvent e) {
                    EventQueue.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            JList list = (JList) e.getSource();
                            // DefaultListModel listModel = (DefaultListModel) list.getModel();
                            Object[] selectedValues = list.getSelectedValues();
                            log.debug("New selection in value list:" + Arrays.toString(selectedValues));

                            GpxOutputPlugin.this.valuesToSave.clear();

                            for (Object o : selectedValues) {
                                if (o instanceof String) {
                                    String s = (String) o;
                                    if ( !GpxOutputPlugin.this.valuesToSave.contains(s))
                                        GpxOutputPlugin.this.valuesToSave.add(s);
                                }
                            }
                        }
                    });
                }
            });
            this.valuesToSaveList.setSize(200, 22);
            this.valuesToSaveList.setAlignmentY(Component.CENTER_ALIGNMENT);
            valuesPanel.add(new JLabel("Saved attributes:"));
            valuesPanel.add(this.valuesToSaveList);
            valuesPanel.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
            this.panel.add(valuesPanel, BorderLayout.EAST);

            this.textPane = new XmlTextPane();
            this.textPane.setEditable(true);
            this.textPane.setFont(Font.getFont(Font.MONOSPACED));

            JScrollPane scrollPane = new JScrollPane(this.textPane);
            this.panel.add(scrollPane, BorderLayout.CENTER);
            JButton refreshTextFieldButton = new JButton("Update");
            refreshTextFieldButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    EventQueue.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            updateGUI();
                        }
                    });
                }
            });
            this.panel.add(refreshTextFieldButton, BorderLayout.SOUTH);

            updateGUI();
        }

        return this.panel;
    }

    /**
     * 
     * @return
     */
    private JMenu makeMenu() {
        if (this.menu == null) {
            this.menu = new JMenu();
            JMenuItem selectFile = new JMenuItem(SELECT_GPX_FILE_TEXT);
            selectFile.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    selectGpxFileAction();
                }
            });
            this.menu.add(selectFile);
            JMenuItem saveFile = new JMenuItem(SAVE_CURRENT_FILE_TEXT);
            saveFile.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    saveCurrentFile();
                }
            });
            this.menu.add(saveFile);
        }

        return this.menu;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.n52.ifgicopter.spf.output.IOutputPlugin#processData(java.util.Map,
     * org.n52.ifgicopter.spf.xml.Plugin)
     */
    @Override
    public int processData(Map<Long, Map<String, Object>> data, Plugin plugin) {
        for (Entry<Long, Map<String, Object>> entry : data.entrySet()) {
            int i = processSingleData(entry.getValue(), entry.getKey(), plugin);
            if (i != IModule.STATUS_RUNNING)
                log.warn("Processing of data had error: " + entry);
        }

        return IModule.STATUS_RUNNING;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.n52.ifgicopter.spf.output.IOutputPlugin#processSingleData(java.util.Map, java.lang.Long,
     * org.n52.ifgicopter.spf.xml.Plugin)
     */
    @Override
    public int processSingleData(Map<String, Object> data, Long timestamp, Plugin plugin) {
        if (log.isDebugEnabled())
            log.debug("New data for GPX output: " + data + " @ " + timestamp);

        this.availableDataKeys.addAll(data.keySet());

        WptType newWpt = this.gpx.getGpx().addNewWpt();

        newWpt.setName(plugin.getName() + " [" + Integer.toString(this.counter) + "]");
        this.counter++;

        Date d = new Date(timestamp.longValue());
        Calendar cal = Calendar.getInstance();
        cal.setTime(d);
        newWpt.setTime(cal);

        // use plugin.getLocation() instead of hard coded keys for the data map
        Location location = plugin.getLocation();
        Double lat = (Double) data.get(location.getFirstCoordinateName());
        Double lon = (Double) data.get(location.getSecondCoordinateName());
        Double alt = (Double) data.get(location.getAltitudeName());
        newWpt.setLat(new BigDecimal(lat.toString()));
        newWpt.setLon(new BigDecimal(lon.toString()));
        newWpt.setEle(new BigDecimal(alt.toString()));

        HashMap<String, Object> dataToSave = new HashMap<String, Object>();
        for (Entry<String, Object> entry : data.entrySet()) {
            if (this.valuesToSave.contains(entry.getKey()))
                dataToSave.put(entry.getKey(), entry.getValue());
        }

        String values = encodeDataString(dataToSave);
        newWpt.setDesc(values);

        updateGUI();
        return IModule.STATUS_RUNNING;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.n52.ifgicopter.spf.output.IOutputPlugin#restart()
     */
    @Override
    public void restart() throws Exception {
        log.info("Creating new empty document because of restart!");
        this.gpx = GpxDocument.Factory.newInstance();
    }

    /**
     * @throws IOException
     */
    protected void saveCurrentFile() {
        File f = new File(this.gpxFilePath);
        
        try {
        	if (!f.exists()) {
        		f.mkdirs();
        		f.createNewFile();
        	}
        	
            String text = this.textPane.getText();

            GpxDocument gpxToSave = GpxDocument.Factory.parse(text);

            boolean valid = this.gpx.validate();
            if (log.isDebugEnabled()) {
                log.debug("Created GPX file from text pane, is valid=" + valid);
            }

            gpxToSave.save(f, GPX_OPTIONS);
        }
        catch (IOException e) {
            log.error("Could not save GPX output file " + f.getAbsolutePath(), e);
        }
        catch (XmlException e) {
            log.error("Could not save GPX output file " + f.getAbsolutePath(), e);
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
                    if ( !file.getName().endsWith(".gpx") || !file.getName().endsWith(".xml"))
                        setGpxFilePath(file.getAbsolutePath() + ".gpx");
                    else
                        setGpxFilePath(file.getAbsolutePath());
                }
                else {
                    log.debug("Open command cancelled by user.");
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
        this.gpxFilePath = gpxFilePath;
        if (log.isDebugEnabled())
            log.debug("New output file path " + this.gpxFilePath);

        updateGUI();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.n52.ifgicopter.spf.common.IModule#shutdown()
     */
    @Override
    public void shutdown() throws Exception {
        saveCurrentFile();
        this.gpx = null;
    }

    /**
     * 
     */
    protected void updateGUI() {
        if (this.gpx == null)
            return;

        final String t = this.gpx.xmlText(GPX_OPTIONS);
        EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                GpxOutputPlugin.this.textPane.setText(t);

                GpxOutputPlugin.this.outputFileLabel.setText("<html><b>Output file: </b>" + getGpxFilePath()
                        + "</html>");

                DefaultListModel model = (DefaultListModel) GpxOutputPlugin.this.valuesToSaveList.getModel();
                // update list
                if (model.getSize() < GpxOutputPlugin.this.availableDataKeys.size()) {
                    if (model.contains(NO_DATA_LIST_ELEMENT))
                        model.remove(model.indexOf(NO_DATA_LIST_ELEMENT));

                    for (String s : GpxOutputPlugin.this.availableDataKeys) {
                        if ( !model.contains(s)) {
                            model.addElement(s);
                        }
                    }
                }
            }
        });
    }

    private String encodeDataString(Map<String, Object> data) {
        StringBuilder values = new StringBuilder();
        for (Entry<String, Object> entry : data.entrySet()) {
            values.append(entry.getKey());
            values.append("=");
            values.append(entry.getValue().toString());
            values.append(";");
        }

        return values.toString();
    }

}
