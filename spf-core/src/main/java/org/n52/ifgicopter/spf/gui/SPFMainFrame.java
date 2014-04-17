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
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.n52.ifgicopter.spf.SPFRegistry;
import org.n52.ifgicopter.spf.common.IModule;
import org.n52.ifgicopter.spf.common.IOutputMessageListener;
import org.n52.ifgicopter.spf.common.IStatusChangeListener;
import org.n52.ifgicopter.spf.gui.ModuleGUI;
import org.n52.ifgicopter.spf.gui.ModuleGUI.SimpleInputPluginGUI;
import org.n52.ifgicopter.spf.gui.PluginRegistrationDialog.TablePanel;
import org.n52.ifgicopter.spf.gui.map.ImageMapMarker;
import org.n52.ifgicopter.spf.input.IInputPlugin;
import org.n52.ifgicopter.spf.output.IOutputPlugin;
import org.n52.ifgicopter.spf.xml.Plugin;
import org.n52.ifgicopter.spf.xml.PluginXMLTools;

import com.nexes.wizard.Wizard;

/**
 * This is a simple gui representation of the SPFramework. It can be extended by any InputPlugin which wants
 * to provide an own user interface.
 * 
 * @author Matthes Rieke <m.rieke@uni-muenster.de>
 * 
 */
public class SPFMainFrame extends JFrame implements IStatusChangeListener, IOutputMessageListener {

	static ImageIcon RED_IMAGE;
	static ImageIcon GREEN_IMAGE;
	static ImageIcon BLUE_IMAGE;

	static {
		try {
			InputStream in = null;
			File f = new File("img/point_red.png");
			if (!f.exists()) {
				in = SPFMainFrame.class.getResourceAsStream("/img/point_red.png");
			}else {
				in = new FileInputStream(f);
			}

			RED_IMAGE = new ImageIcon(ImageIO.read(in));

			f = new File("img/point_blue.png");
			if (!f.exists()) {
				in = SPFMainFrame.class.getResourceAsStream("/img/point_blue.png");
			}else {
				in = new FileInputStream(f);
			}

			BLUE_IMAGE = new ImageIcon(ImageIO.read(in));

			f = new File("img/point_green.png");
			if (!f.exists()) {
				in = SPFMainFrame.class.getResourceAsStream("/img/point_green.png");
			}else {
				in = new FileInputStream(f);
			}

			GREEN_IMAGE = new ImageIcon(ImageIO.read(in));
		}
		catch (Exception e) {
			LogFactory.getLog(SPFMainFrame.class).warn(e.getMessage(), e);
		}

	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * the version of the framework
	 */
	public static final String VERSION = "v 0.5";


	/**
	 * the public return code for indicating an application restart.
	 * SPF needs be run using provided scripts.
	 */
	public static final int RESTART_CODE = 100;

	final Log log = LogFactory.getLog(SPFMainFrame.class);

	private JTabbedPane pane;
	private JMenuBar menu;
	private JMenu inputPluginMenu;
	private FrameworkCorePanel corePanel;

	private Map<IInputPlugin, ModuleGUI> inputPluginPanels = new HashMap<IInputPlugin, ModuleGUI>();
	private Map<IOutputPlugin, ModuleGUI> outputPluginPanels = new HashMap<IOutputPlugin, ModuleGUI>();

	JLabel statusLabel;
	JLabel outputLabel;
	AboutDialog aboutDialog;
	JCheckBoxMenuItem pnp;
	boolean inputChanged;
	protected boolean dontShow;
	private MapPanel mapPanel;
	private JMenu outputPluginMenu;

	/**
	 * the gui representation of the framework
	 */
	public SPFMainFrame() {

		/*
		 * handled by worker thread.
		 */
		this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		this.setTitle("Sensor Platform Framework");

		this.menu = new JMenuBar();

		try {
			File f = new File("img/icon.png");
			InputStream in;
			if (!f.exists()) {
				in = ImageMapMarker.class.getResourceAsStream("/img/icon.png");
			}else {
				in = new FileInputStream(f);
			}

			this.setIconImage(ImageIO.read(in));
		}
		catch (IOException e) {
			this.log.warn(e.getMessage(), e);
		}

		/*
		 * simple menu bar
		 */
		JMenu file = new JMenu("File");
		JMenuItem exit = new JMenuItem("Exit");

		/*
		 * shutdown the engine if closed
		 */
		exit.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				shutdownFrame();
			}
		});

		this.pnp = new JCheckBoxMenuItem("Plug'n'Play mode");
		this.pnp.setSelected(false);

		/*
		 * switch the pnp mode
		 */
		this.pnp.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				boolean flag = SPFMainFrame.this.pnp.isSelected();

				if (flag && !SPFMainFrame.this.dontShow) {
					JCheckBox checkbox = new JCheckBox("Do not show this message again.");
					String message = "During Plug'n'Play mode the output generation is blocked.";
					Object[] params = {message, checkbox};
					JOptionPane.showMessageDialog(SPFMainFrame.this, params);
					SPFMainFrame.this.dontShow = checkbox.isSelected();
				}

				/*
				 * check if we need to restart the output plugins
				 */
				if ( !flag && SPFMainFrame.this.inputChanged) {
					SPFRegistry.getInstance().restartOutputPlugins();
				}

				SPFRegistry.getInstance().setPNPMode(flag);
			}
		});

		JMenuItem restart = new JMenuItem("Restart");
		restart.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				shutdownFrame(RESTART_CODE);
			}
		});

		JMenuItem managePlugins = new JMenuItem("Manage available Plugins");
		managePlugins.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				PluginRegistrationDialog prd = new PluginRegistrationDialog(SPFMainFrame.this);

				if (prd.isCanceled()) return;

				updateConfigurationFile(prd.getSelectedNewPlugins(), prd.getSelectedOldPlugins());

				int ret = JOptionPane.showConfirmDialog(SPFMainFrame.this, "<html><body><div>" +
						"Changes will have effect after restart of the application. " +
						"</div><div>A restart is highly recommended due to memory usage." +
						"</div><div><br />Restart now?</div></body></html>",
						"Restart application", JOptionPane.YES_NO_OPTION);

				if (ret == 0) {
					shutdownFrame(RESTART_CODE);
				}
			}
		});
		file.add(managePlugins);

		file.add(this.pnp);
		file.add(new FixedSeparator());
		file.add(restart);
		file.add(new FixedSeparator());
		file.add(exit);
		this.menu.add(file);
		this.inputPluginMenu = new JMenu("InputPlugins");
		this.outputPluginMenu = new JMenu("OutputPlugins");
		this.menu.add(this.inputPluginMenu);
		this.menu.add(this.outputPluginMenu);

		/*
		 * help
		 */
		this.aboutDialog = new AboutDialog(SPFMainFrame.this);
		JMenu help = new JMenu("Help");
		JMenuItem about = new JMenuItem("About");
		about.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				SPFMainFrame.this.aboutDialog.showSelf(SPFMainFrame.this);
			}
		});

		help.add(about);
		this.menu.add(help);
		this.setJMenuBar(this.menu);

		/*
		 * the tabbed pane. every tab represents a input plugin
		 */
		this.pane = new JTabbedPane();
		this.pane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);

		/*
		 * shutdown the engine if closed
		 */
		this.addWindowListener(new WindowAdapter() {

			@Override
			public void windowClosing(WindowEvent e) {
				SPFMainFrame.this.shutdownFrame();
			}

		});

		/*
		 * the framework core tab
		 */
		this.corePanel = new FrameworkCorePanel();
		this.addTab(this.corePanel, "Framework Core", BLUE_IMAGE);

		/*
		 * the map panel
		 */
		if (Boolean.parseBoolean(SPFRegistry.getInstance().getConfigProperty(SPFRegistry.OVERVIEW_MAP_ENABLED))) {
			this.mapPanel = new MapPanel();
			this.addTab(this.mapPanel, "Overview Map", BLUE_IMAGE);
		}
		

		/*
		 * other stuff
		 */
		this.getContentPane().add(this.pane);

		JPanel statusBar = new JPanel();
		statusBar.setLayout(new BorderLayout());
		statusBar.setPreferredSize(new Dimension(200, 25));
		JPanel statusBarWrapper = new JPanel(new BorderLayout());
		statusBarWrapper.add(Box.createRigidArea(new Dimension(3, 3)), BorderLayout.WEST);
		statusBarWrapper.add(statusBar);
		statusBarWrapper.add(Box.createRigidArea(new Dimension(3, 3)), BorderLayout.EAST);

		this.statusLabel = new JLabel("SPFramework startup finished.");
		statusBar.add(this.statusLabel, BorderLayout.EAST);

		this.outputLabel = new JLabel("(no output yet)");
		statusBar.add(this.outputLabel, BorderLayout.WEST);

		this.getContentPane().add(statusBarWrapper, BorderLayout.SOUTH);

		this.getContentPane().setBackground(this.pane.getBackground());

		this.setPreferredSize(new Dimension(1280, 720));
		this.pack();

		/*
		 * full screen?
		 */
		 if (Boolean.parseBoolean(SPFRegistry.getInstance().getConfigProperty(SPFRegistry.MAXIMIZED))) {
			 this.setExtendedState(this.getExtendedState() | JFrame.MAXIMIZED_BOTH);
		 }

		 this.setLocationRelativeTo(null);
	}

	/**
	 * @param newPlugins used inputs plugins
	 * @param oldPlugins used output plugins
	 */
	protected void updateConfigurationFile(Map<String, TablePanel> newPlugins,
			List<String> oldPlugins) {
		String inString = SPFRegistry.getInstance().getConfigProperty(SPFRegistry.INPUT_PLUGINS_PROP);
		String outString = SPFRegistry.getInstance().getConfigProperty(SPFRegistry.OUTPUT_PLUGINS_PROP);

		String[] inArray = inString.split(SPFRegistry.LIST_SEPARATOR);
		String[] outArray = outString.split(SPFRegistry.LIST_SEPARATOR);

		/*
		 * cycle through currently registered plugins of the config
		 */
		StringBuilder sbIn = new StringBuilder();
		for (String string : inArray) {
			string = string.trim();
			if (oldPlugins.contains(string)) {
				/*
				 * add as original setup
				 */
				sbIn.append(string);
				sbIn.append(SPFRegistry.LIST_SEPARATOR);
			}

		}

		StringBuilder sbOut = new StringBuilder();
		for (String string : outArray) {
			string = string.trim();
			if (oldPlugins.contains(string)) {
				/*
				 * add as original setup
				 */
				sbOut.append(string);
				sbOut.append(SPFRegistry.LIST_SEPARATOR);
			}

		}

		/*
		 * add the new ones
		 */
		for (String string : newPlugins.keySet()) {
			TablePanel params = newPlugins.get(string);

			/*
			 * input?
			 */
			if (!params.isInputPlugin()) continue;

			if (params.isDummy()) {
				sbIn.append(string);
			} else {
				sbIn.append(string.substring(0, string.indexOf("(") + 1));

				List<String> values = params.getValues();
				for (int i = 0; i < values.size() - 1; i++) {
					String s = values.get(i);
					sbIn.append(s);
					sbIn.append(SPFRegistry.PARAMETER_SEPARATOR);
				}
				sbIn.append(values.get(values.size() - 1));
				sbIn.append(")");
			}

			sbIn.append(SPFRegistry.LIST_SEPARATOR);
		}

		for (String string : newPlugins.keySet()) {
			TablePanel params = newPlugins.get(string);

			/*
			 * input?
			 */
			if (params.isInputPlugin()) continue;

			if (params.isDummy()) {
				sbOut.append(string);
			} else {
				sbOut.append(string.substring(0, string.indexOf("(") + 1));

				List<String> values = params.getValues();
				for (int i = 0; i < values.size() - 1; i++) {
					String s = values.get(i);
					sbOut.append(s);
					sbOut.append(SPFRegistry.PARAMETER_SEPARATOR);
				}
				sbOut.append(values.get(values.size() - 1));
				sbOut.append(")");
			}

			sbOut.append(SPFRegistry.LIST_SEPARATOR);
		}

		SPFRegistry.getInstance().setConfigProperty(SPFRegistry.INPUT_PLUGINS_PROP, sbIn.toString());
		SPFRegistry.getInstance().setConfigProperty(SPFRegistry.OUTPUT_PLUGINS_PROP, sbOut.toString());

		try {
			SPFRegistry.getInstance().saveConfiguration();
		} catch (IOException e) {
			log.warn(e.getMessage(), e);
		}
	}

	protected void shutdownFrame(final int returnCode) {
		SwingWorker<String, Object> worker = new SwingWorker<String, Object>() {

			@Override
			protected String doInBackground() throws Exception {
				SPFMainFrame.this.statusLabel.setText("SPFramework shutting down... this could take a while.");

				ShutdownDialog dialog = new ShutdownDialog(SPFMainFrame.this);
				new Thread(dialog).start();


				try {
					SPFRegistry.getInstance().shutdownSystem();
				}
				catch (Exception e1) {
					SPFMainFrame.this.log.warn(e1.getMessage(), e1);

					//check if we have a restart returnCode, then do not error
					if (returnCode == 0) System.exit(1);
				} finally {
					System.exit(returnCode);
				}

				return null;
			}
		};

		worker.execute();	
	}

	protected void shutdownFrame() {
		shutdownFrame(0);
	}

	/**
	 * This adds an {@link IInputPlugin} to the gui.
	 * 
	 * @param ip
	 *        the input plugin
	 */
	public void addInputPlugin(final IInputPlugin ip) {
		this.corePanel.addInputPlugin(ip);

		ModuleGUI ui = ip.getUserInterface();

		/*
		 * if the plugin does not have a gui, create a default one.
		 */
		if (ui == null) {
			ui = createDefaultInputUI(ip);
		}
		else {
			/*
			 * add the tab
			 */
			this.addTab(ui.getGui(), ip.getName(), RED_IMAGE);
		}

		this.inputPluginPanels.put(ip, ui);

		/*
		 * menu if it has one
		 */
		JMenu men = ui.getMenu();
		if (men == null) {
			men = new JMenu();
		}
		men.setText(ip.getName());

		/*
		 * add an entry for the metadata and xml export
		 */
		JMenuItem metas = new JMenuItem("Edit Metadata");
		metas.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Plugin plug = SPFRegistry.getInstance().getPluginForName(ip.getName());
				InputMetadataDialog dial = new InputMetadataDialog(plug.getMetadata(), SPFMainFrame.this);
				dial.setVisible(true);
			}
		});
		men.add(metas);

		JMenuItem export = new JMenuItem("Export XML description");
		export.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Plugin plug = SPFRegistry.getInstance().getPluginForName(ip.getName());
				String string = PluginXMLTools.toXML(plug);

				String tmp = "plugin-description-" + plug.getName() + ".xml";
				tmp = tmp.replace(":", "_");
				File file = new File(tmp);

				try {
					if ( !file.exists()) {
						boolean b = file.createNewFile();
						if ( !b)
							SPFMainFrame.this.log.warn("File already exists!");
					}
					FileWriter fw = new FileWriter(file);

					fw.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
					fw.append(System.getProperty("line.separator"));
					fw.write(string);
					fw.flush();
					fw.close();
					SPFMainFrame.this.statusLabel.setText("XML written to " + file.getAbsolutePath());
				}
				catch (IOException e1) {
					SPFMainFrame.this.log.warn(e1.getMessage(), e1);
					SPFMainFrame.this.statusLabel.setText("Failed writing XML description. (" + e1.getMessage() + ")");
				}

			}
		});
		men.add(export);

		this.inputPluginMenu.add(men);
	}

	/**
	 * this method creates a default UI for an input plugin that does not provide its own.
	 * 
	 * @param ip
	 *        the input plugin
	 * @return a simple gui
	 */
	private ModuleGUI createDefaultInputUI(IInputPlugin ip) {
		ModuleGUI ipgui = new ModuleGUI();
		SimpleInputPluginGUI panel = ipgui.new SimpleInputPluginGUI(ip);

		JMenu newMen = new JMenu(ip.getName());
		JMenuItem newItem = new JMenuItem("(no actions)");
		newMen.add(newItem);
		newItem.setEnabled(false);

		ipgui.setMenu(newMen);

		ipgui.setGui(panel);
		return ipgui;
	}

	/**
	 * creates a small panel for each output plugin
	 * 
	 * @param op
	 *        the output plugin
	 */
	public void addOutputPlugin(IOutputPlugin op) {
		this.corePanel.addOutputPlugin(op);

		ModuleGUI ui = op.getUserInterface();

		/*
		 * if the plugin does not have a gui, create a default one.
		 */
		if (ui != null) {
			/*
			 * add the tab
			 */
			this.addTab(ui.getGui(), op.getName(), GREEN_IMAGE);

			this.outputPluginPanels.put(op, ui);

			/*
			 * menu if it has one
			 */
			JMenu men = ui.getMenu();
			if (men != null) {
				men.setText(op.getName());
				this.outputPluginMenu.add(men);
			}
		}
	}

	/**
	 * helper method. creates tab labels with icons on the left side.
	 */
	private void addTab(Component tab, String title, Icon icon) {
		this.pane.add(tab);

		// Create bespoke component for rendering the tab.
		JLabel lbl = new JLabel(title);
		lbl.setIcon(icon);

		// Add some spacing between text and icon, and position text to the RHS.
		lbl.setIconTextGap(2);
		lbl.setHorizontalTextPosition(SwingConstants.RIGHT);

		this.pane.setTabComponentAt(this.pane.getTabCount() - 1, lbl);
	}

	@Override
	public void statusChanged(int status, IModule module) {

		if (module instanceof IInputPlugin) {
			this.corePanel.statusChanged(status, (IInputPlugin) module);
			IInputPlugin iip = (IInputPlugin) module;
			ModuleGUI ipgui = this.inputPluginPanels.get(iip);
			if (ipgui.getGui() instanceof SimpleInputPluginGUI) {
				((SimpleInputPluginGUI) ipgui.getGui()).setStatusText(iip.getStatusString(), status);
			}
			this.statusLabel.setText(iip.getName() + ": " + iip.getStatusString());
		}
		else if (module instanceof IOutputPlugin) {
			IOutputPlugin iop = (IOutputPlugin) module;
			this.corePanel.statusChanged(status, iop);
			this.statusLabel.setText(iop.getName() + ": " + iop.getStatusString());
		}

	}

	@Override
	public void newOutput(String message) {
		this.outputLabel.setText(message);
	}

	/**
	 * @param key
	 *        the property
	 * @return the used dialog instance which holds the metadata
	 */
	public PNPDialog doPNP(String key) {
		PNPDialog result = new PNPDialog(this, key);

		if (result.isCanceled()) {
			int ret = JOptionPane.showConfirmDialog(this, "You cancelled the metadata dialog. "
					+ "Do you want to turn off the Plug'n'Play mode?", "Turn of P'n'P?", JOptionPane.YES_NO_OPTION);
			if (ret == 0) {
				/*
				 * perhaps the input has changed anyhow
				 */
				if (this.inputChanged) {
					SPFRegistry.getInstance().restartOutputPlugins();
				}

				result.setPNPOff();
				this.pnp.setSelected(false);
			}
		}
		else if (result.getReturn() == Wizard.FINISH_RETURN_CODE) {
			this.inputChanged = true;
		}

		return result;
	}

	/**
	 * @return the mapPanel
	 */
	public MapPanel getMapPanel() {
		return this.mapPanel;
	}

	private class AboutDialog extends JDialog {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		Desktop desktop;

		public AboutDialog(JFrame parent) {
			this.setResizable(false);
			this.setModal(true);
			this.setIconImage(parent.getIconImage());
			this.setTitle("About");

			Image img = null;
			try {
				File f = new File("img/splash.png");
				InputStream in;
				if (!f.exists()) {
					in = ImageMapMarker.class.getResourceAsStream("/img/splash.png");
				}else {
					in = new FileInputStream(f);
				}

				img = ImageIO.read(in);
			}
			catch (IOException e1) {
				e1.printStackTrace();
			}

			JLabel imgLabel;
			if (img != null) {
				imgLabel = new JLabel(new ImageIcon(img));
			}
			else {
				imgLabel = new JLabel();
			}
			imgLabel.setPreferredSize(new Dimension(200, 300));
			imgLabel.setBorder(BorderFactory.createEtchedBorder());

			/*
			 * check if we are allowed to open the browser on link-clicks
			 */
			if (Desktop.isDesktopSupported()) {
				this.desktop = Desktop.getDesktop();
			}

			JPanel aboutPanel = new JPanel();
			aboutPanel.setLayout(new GridLayout(0, 1));
			JEditorPane jep = new JEditorPane("text/html",
					"<html><div style=\"font-family: sans-serif; font-size:10pt\"><h3>Sensor Platform Framework "
					+ SPFMainFrame.VERSION
					+ "</h3><p>by Matthes Rieke (<a href=\"mailto:m.rieke@uni-muenster.de\">m.rieke@uni-muenster.de</a>)<br /></p>"
					+ "<p>Developed as part of the Diploma Thesis"
					+ "<div>'<strong>Entwicklung eines Frameworks zur Anbindung von "
					+ "Multi-Sensor-Plattformen an das Sensor Web</strong>'</div></p>"
					+ "<h4>Links</h4><ul><li><a href=\"http://swsl.uni-muenster.de/research/ifgicopter/\">ifgicopter</a></li>"
					+ "<li><a href=\"http://ifgi.uni-muenster.de/~m_riek02/\">Matthes Rieke</a></li></ul>"
					+ "</div></html>");
			jep.setEditable(false);

			jep.addHyperlinkListener(new HyperlinkListener() {

				@Override
				public void hyperlinkUpdate(HyperlinkEvent e) {
					if (e.getEventType().equals(HyperlinkEvent.EventType.ACTIVATED)) {
						if (AboutDialog.this.desktop != null) {
							try {
								AboutDialog.this.desktop.browse(e.getURL().toURI());
							}
							catch (IOException e1) {
								SPFMainFrame.this.log.warn(e1.getMessage(), e1);
							}
							catch (URISyntaxException e1) {
								SPFMainFrame.this.log.warn(e1.getMessage(), e1);
							}
						}
					}
				}
			});
			aboutPanel.add(jep);

			aboutPanel.setPreferredSize(new Dimension(150, 100));

			this.getContentPane().add(imgLabel, BorderLayout.CENTER);
			this.getContentPane().add(aboutPanel, BorderLayout.EAST);

			this.pack();
			this.repaint();

			this.setLocationRelativeTo(parent);

		}

		/**
		 * use to activate multiple times
		 * 
		 * @param parent
		 *        the parent
		 */
		public void showSelf(JFrame parent) {
			this.setLocationRelativeTo(parent);
			this.setVisible(true);
		}

	}

	static class FixedSeparator extends JSeparator {
		private static final long serialVersionUID = 1L;

		public Dimension getPreferredSize() {
			Dimension d = super.getPreferredSize();
			if (d.height == 0) d.height = 4;
			return d;
		}
	}

	/**
	 * switch to the default tab, set in configuration
	 */
	public void switchToDefaultTab() {
		String val = SPFRegistry.getInstance().getConfigProperty(SPFRegistry.DEFAULT_ACTIVE_PLUGIN);


		ModuleGUI tab = null;
		outer:
			if (!val.equals("core")) {
				for (IOutputPlugin op : this.outputPluginPanels.keySet()) {
					if (op.getClass().getName().equals(val)) {
						tab = this.outputPluginPanels.get(op);
						break outer;
					}
				}

				for (IInputPlugin ip : this.inputPluginPanels.keySet()) {
					if (ip.getClass().getName().equals(val)) {
						tab = this.inputPluginPanels.get(ip);
						break outer;
					}
				}
			}

		if (tab != null) {
			this.pane.setSelectedComponent(tab.getGui());
		}
	}

}
