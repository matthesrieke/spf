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

package org.n52.ifgicopter.spf.gui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.n52.ifgicopter.spf.common.IModule;
import org.n52.ifgicopter.spf.gui.map.ImageMapMarker;
import org.n52.ifgicopter.spf.input.IInputPlugin;
import org.n52.ifgicopter.spf.output.IOutputPlugin;

/**
 * The framework has its own panel gui.
 * 
 * @author Matthes Rieke <m.rieke@uni-muenster.de>
 * 
 */
public class FrameworkCorePanel extends JPanel {

    /**
	 * 
	 */
    private static final long serialVersionUID = 1L;
    private static final Log log = LogFactory.getLog(FrameworkCorePanel.class);

    protected static final Font ITEM_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 12);
    protected static final Font SUB_ITEM_FONT = ITEM_FONT.deriveFont(Font.PLAIN, 10);
    protected static final Color SELECTED_COLOR = new Color(235, 235, 235);
    protected static final Color DEFAULT_COLOR = Color.white;
    protected static final String WELCOME_PANEL = "WELCOME_PANEL";

    protected static ImageIcon INPUT_IMAGE;
    protected static ImageIcon OUTPUT_IMAGE;

    static {
        InputStream in = null;
        File f;
        try {
            f = new File("img/inputp.png");
            if ( !f.exists()) {
                in = ImageMapMarker.class.getResourceAsStream("/img/inputp.png");
            }
            else {
                in = new FileInputStream(f);
            }

            INPUT_IMAGE = new ImageIcon(ImageIO.read(in));
        }
        catch (Exception e) {
            log.warn(e.getMessage(), e);
            INPUT_IMAGE = new ImageIcon();
        }
        try {
            in = null;
            f = new File("img/outputp.png");
            if ( !f.exists()) {
                in = ImageMapMarker.class.getResourceAsStream("/img/outputp.png");
            }
            else {
                in = new FileInputStream(f);
            }

            OUTPUT_IMAGE = new ImageIcon(ImageIO.read(in));
        }
        catch (Exception e) {
            log.warn(e.getMessage(), e);
            OUTPUT_IMAGE = new ImageIcon();
        }
    }

    private Map<IOutputPlugin, PluginPanel> outputPluginsTable = new HashMap<IOutputPlugin, PluginPanel>();
    protected JPanel pluginsPanel;
    private Map<IInputPlugin, PluginPanel> inputPluginsTable = new HashMap<IInputPlugin, PluginPanel>();
    private GridBagConstraints globalGBC;
    private JPanel contentPanel;
    private PluginPanel currentSelected;

    /**
     * default constructor to create the framework core panel.
     */
    public FrameworkCorePanel() {

        /*
         * INPUT PLUGIN LIST
         */
        this.pluginsPanel = new JPanel();
        this.pluginsPanel.setLayout(new GridBagLayout());
        this.pluginsPanel.setBackground(DEFAULT_COLOR);

        this.globalGBC = new GridBagConstraints();
        this.globalGBC.gridx = 0;
        this.globalGBC.gridy = 0;
        this.globalGBC.weightx = 0.0;
        this.globalGBC.weighty = 0.0;
        this.globalGBC.anchor = GridBagConstraints.NORTHWEST;
        this.globalGBC.fill = GridBagConstraints.HORIZONTAL;

        this.pluginsPanel.add(new JLabel("<html><body><div><strong>" + "Active Plugins</strong></div></body></html>"),
                              this.globalGBC);

        this.globalGBC.gridy++;
        this.globalGBC.gridwidth = GridBagConstraints.REMAINDER;
        this.globalGBC.insets = new Insets(0, 0, 5, 0);
        this.pluginsPanel.add(new JSeparator(), this.globalGBC);
        this.globalGBC.insets = new Insets(0, 0, 0, 0);

        this.globalGBC.weightx = 1.0;
        this.globalGBC.gridy = 0;
        this.globalGBC.gridx = 1;
        this.pluginsPanel.add(Box.createHorizontalGlue(), this.globalGBC);
        this.globalGBC.weightx = 0.0;
        this.globalGBC.gridx = 0;
        this.globalGBC.gridy = 1;

        /*
         * the contentpanel
         */
        this.contentPanel = new JPanel();
        this.contentPanel.setLayout(new CardLayout());
        this.contentPanel.setBorder(BorderFactory.createEtchedBorder());
        this.contentPanel.add(WelcomePanel.getInstance(), WELCOME_PANEL);
        // ((CardLayout) contentPanel.getLayout()).show(contentPanel, WELCOME_PANEL);

        JScrollPane startupS = new JScrollPane(this.contentPanel);
        startupS.setViewportBorder(null);

        /*
         * wrap the plugins to ensure top alignment
         */
        JPanel pluginsPanelWrapper = new JPanel(new BorderLayout());
        pluginsPanelWrapper.add(this.pluginsPanel, BorderLayout.NORTH);
        pluginsPanelWrapper.add(Box.createVerticalGlue(), BorderLayout.CENTER);
        pluginsPanelWrapper.setBorder(BorderFactory.createEtchedBorder());
        pluginsPanelWrapper.setBackground(this.pluginsPanel.getBackground());
        JScrollPane wrapperS = new JScrollPane(pluginsPanelWrapper);
        wrapperS.setViewportBorder(null);

        /*
         * add both scroll panes to the split pane
         */
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, wrapperS, startupS);
        splitPane.setOneTouchExpandable(true);
        splitPane.setDividerLocation(250);
        splitPane.setContinuousLayout(true);

        Dimension minimumSize = new Dimension(100, 100);
        startupS.setMinimumSize(minimumSize);

        this.setLayout(new BorderLayout());
        this.add(splitPane);
    }

    /**
     * @param op
     *        new output plugin
     */
    public void addOutputPlugin(IOutputPlugin op) {
        PluginPanel opui = new PluginPanel(op);
        opui.setStatusText(op.getStatusString(), op.getStatus());

        this.outputPluginsTable.put(op, opui);

        synchronized (this) {
            this.globalGBC.gridy++;
            this.pluginsPanel.add(opui, this.globalGBC);
        }

    }

    /**
     * @param ip
     *        the new input plugin
     */
    public void addInputPlugin(IInputPlugin ip) {
        PluginPanel ipui = new PluginPanel(ip);
        ipui.setStatusText(ip.getStatusString(), IModule.STATUS_RUNNING);

        this.inputPluginsTable.put(ip, ipui);
        synchronized (this) {
            this.globalGBC.gridy++;
            this.pluginsPanel.add(ipui, this.globalGBC);
        }

    }

    /**
     * @param status
     *        new status
     * @param iip
     *        the {@link IInputPlugin}
     */
    public void statusChanged(int status, IInputPlugin iip) {
        PluginPanel ipgui = this.inputPluginsTable.get(iip);
        ipgui.setStatusText(iip.getStatusString(), status);
    }

    /**
     * @param status
     *        new status
     * @param iop
     *        the {@link IOutputPlugin}
     */
    public void statusChanged(int status, IOutputPlugin iop) {
        PluginPanel opgui = this.outputPluginsTable.get(iop);
        opgui.setStatusText(iop.getStatusString(), status);
    }

    public void onPanelSelection(PluginPanel pluginPanel) {
        if (this.currentSelected != null) {
            this.currentSelected.panelDeselected();
        }
        if (this.currentSelected != pluginPanel) {
            pluginPanel.panelSelected();
            this.currentSelected = pluginPanel;
        }
        else {
            this.currentSelected = null;
            /*
             * selection cleared
             */
            // TODO do something with contentPanel
        }

    }

    /**
     * A small private class for output plugin gui component.
     * 
     * @author Matthes Rieke <m.rieke@uni-muenster.de>
     * 
     */
    private class PluginPanel extends JPanel implements MouseListener {

        /**
		 * 
		 */
        private static final long serialVersionUID = 1L;
        private JLabel statusLabel;

        public PluginPanel(IOutputPlugin op) {
            this(op.getName(), op.getStatusString(), false);
        }

        public PluginPanel(IInputPlugin ip) {
            this(ip.getName(), ip.getStatusString(), true);
        }

        public PluginPanel(String name, String status, boolean input) {
            this.setLayout(new GridBagLayout());
            this.setBackground(FrameworkCorePanel.this.pluginsPanel.getBackground());

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 2;
            gbc.gridy = 0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.anchor = GridBagConstraints.NORTHWEST;
            gbc.weightx = 1.0;
            gbc.weighty = 0.0;

            this.add(Box.createHorizontalGlue(), gbc);
            gbc.gridx = 1;
            gbc.weightx = 0.0;

            JLabel nameLabel = new JLabel(name);
            nameLabel.setFont(ITEM_FONT);
            this.add(nameLabel, gbc);
            gbc.gridy++;

            this.statusLabel = new JLabel(status);
            this.statusLabel.setFont(SUB_ITEM_FONT);
            this.add(this.statusLabel, gbc);

            gbc.gridx = 0;
            gbc.gridy = 0;
            if (input) {
                this.add(new JLabel(INPUT_IMAGE), gbc);
            }
            else {
                this.add(new JLabel(OUTPUT_IMAGE), gbc);
            }

            addMouseListener(this);
        }

        /**
         * should be called if the status of the plugin changed.
         * 
         * @param text
         *        the new text
         * @param status
         *        the status
         */
        public void setStatusText(String text, int status) {
            if (status == IModule.STATUS_NOT_RUNNING) {
                this.statusLabel.setIcon(SPFMainFrame.RED_IMAGE);
            }
            else {
                this.statusLabel.setIcon(SPFMainFrame.GREEN_IMAGE);
            }
            this.statusLabel.setText(text);
        }

        public void panelSelected() {
            this.setBackground(SELECTED_COLOR);
        }

        public void panelDeselected() {
            this.setBackground(DEFAULT_COLOR);
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            onPanelSelection(this);
        }

        @Override
        public void mousePressed(MouseEvent e) {
            //
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            //
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            //
        }

        @Override
        public void mouseExited(MouseEvent e) {
            //
        }

    }

    /**
     * 
     */
    private static class WelcomePanel extends JPanel {

        private static final Log log2 = LogFactory.getLog(WelcomePanel.class);

        private static final long serialVersionUID = 1L;
        private static WelcomePanel _instance;

        private WelcomePanel() {
            setLayout(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();

            InputStream in = null;
            File f;
            ImageIcon bgImg;
            try {
                f = new File("img/spf-background.png");
                if ( !f.exists()) {
                    in = ImageMapMarker.class.getResourceAsStream("/img/spf-background.png");
                }
                else {
                    in = new FileInputStream(f);
                }

                bgImg = new ImageIcon(ImageIO.read(in));
            }
            catch (Exception e) {
                log2.warn(e.getMessage(), e);
                bgImg = new ImageIcon();
            }

            gbc.gridx = 1;
            gbc.gridy = 1;
            gbc.insets = new Insets(0, 0, 50, 50);

            this.add(new JLabel(bgImg), gbc);

            gbc.gridy = 0;
            gbc.gridx = 0;
            gbc.weightx = 1.0;
            gbc.weighty = 1.0;
            gbc.insets.bottom = 0;
            gbc.insets.right = 0;

            this.add(Box.createGlue(), gbc);
        }

        public static synchronized WelcomePanel getInstance() {
            if (_instance == null) {
                _instance = new WelcomePanel();
            }
            return _instance;
        }

    }


}
