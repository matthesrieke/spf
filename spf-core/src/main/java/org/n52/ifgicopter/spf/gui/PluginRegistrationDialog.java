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
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.n52.ifgicopter.spf.SPFRegistry;
import org.n52.ifgicopter.spf.common.IModule;
import org.n52.ifgicopter.spf.common.PluginRegistry;
import org.n52.ifgicopter.spf.common.IModule.ConstructorParameters;
import org.n52.ifgicopter.spf.input.IInputPlugin;

import com.nexes.wizard.Wizard;
import com.nexes.wizard.WizardPanelDescriptor;

/**
 * A Wizard style dialog for searching and configuring plugins.
 * 
 * @author matthes rieke
 * 
 */
public class PluginRegistrationDialog {

    protected static final Log log = LogFactory.getLog(PluginRegistrationDialog.class);

    protected PluginRegistry pr = new PluginRegistry();

    private static final String FIRST_DESCRIPTOR = "INTRO";
    private static final String SECOND_DESCRIPTOR = "RESULT";
    private static final String THIRD_DESCRIPTOR = "CHOICE";
    private static final String FOURTH_DESCRIPTOR = "SETUP";
    private int ret;
    private boolean canceled;
    protected ResultPanel result;
    protected PluginChoicePanel choice;
    protected ConstructorSetupPanel setup;
    protected Object searchingMutex = new Object();
    protected boolean searching;
    protected boolean searchAlreadyFinished = false;

    /**
     * @param frame
     *        the parent frame
     */
    public PluginRegistrationDialog(JFrame parent) {
        Wizard wizard = new Wizard(parent);

        WizardPanelDescriptor intro = new IntroPanel();
        wizard.registerWizardPanel(FIRST_DESCRIPTOR, intro);

        wizard.setCurrentPanel(FIRST_DESCRIPTOR);
        wizard.setTitle("Configure Plugins");

        this.result = new ResultPanel();
        wizard.registerWizardPanel(SECOND_DESCRIPTOR, this.result);

        this.choice = new PluginChoicePanel();
        wizard.registerWizardPanel(THIRD_DESCRIPTOR, this.choice);

        this.setup = new ConstructorSetupPanel();
        wizard.registerWizardPanel(FOURTH_DESCRIPTOR, this.setup);

        wizard.getDialog().setIconImage(parent.getIconImage());

        this.ret = wizard.showModalDialog();

        /*
         * this is a cancel
         */
        if (this.ret == Wizard.CANCEL_RETURN_CODE) {
            this.canceled = true;
        }

    }

    /**
     * @return all selected new plugins
     */
    public Map<String, TablePanel> getSelectedNewPlugins() {
        HashMap<String, TablePanel> map = new HashMap<String, TablePanel>();

        for (Object o : this.choice.newInputList.getSelectedValues()) {
            String s = o.toString();
            if (this.choice.tables.containsKey(s)) {
                map.put(s, this.choice.tables.get(s));
            }
        }

        for (Object o : this.choice.newOutputList.getSelectedValues()) {
            String s = o.toString();
            if (this.choice.tables.containsKey(s)) {
                map.put(s, this.choice.tables.get(s));
            }
        }

        return map;
    }

    /**
     * @return all selected old plugins
     */
    public List<String> getSelectedOldPlugins() {
        List<String> list = new ArrayList<String>();

        for (Object o : this.choice.outputList.getSelectedValues()) {
            list.add(o.toString());
        }

        for (Object o : this.choice.inputList.getSelectedValues()) {
            list.add(o.toString());
        }

        return list;
    }

    /**
     * @return the canceled
     */
    public boolean isCanceled() {
        return this.canceled;
    }

    /**
     * the first panel showing the introduction text.
     */
    private class IntroPanel extends WizardPanelDescriptor {

        private JPanel introPanel;

        public IntroPanel() {
            this.introPanel = new JPanel(new CardLayout());

            String text = "<html><body><div>This tool searches the \"plugins\" folder"
                    + " for available Input- and OutputPlugins.</div><div>A restart of the application"
                    + " is required afterwards.<br/></div><div>"
                    + "Put plugin implementations as Java jar files into the \"plugins\" folder "
                    + "in<br/>order to enable their usage.</div></body></html>";

            JLabel description = new JLabel(text);
            this.introPanel.add(description, "1");

            this.introPanel.setBorder(new EmptyBorder(2, 2, 2, 2));
            this.introPanel.setPreferredSize(new Dimension(this.introPanel.getPreferredSize().width + 100, 350));

            this.setPanelComponent(this.introPanel);
            this.setPanelDescriptorIdentifier(FIRST_DESCRIPTOR);
        }

        /*
         * (non-Javadoc)
         * 
         * @see com.nexes.wizard.WizardPanelDescriptor#getNextPanelDescriptor()
         */
        @Override
        public Object getNextPanelDescriptor() {
            return SECOND_DESCRIPTOR;
        }

        /*
         * (non-Javadoc)
         * 
         * @see com.nexes.wizard.WizardPanelDescriptor#aboutToHidePanel()
         */
        @Override
        public void aboutToHidePanel() {
            if (PluginRegistrationDialog.this.searchAlreadyFinished)
                return;

            synchronized (PluginRegistrationDialog.this.searchingMutex) {
                PluginRegistrationDialog.this.searching = true;
            }

            String text = "<html><body><div>Searching...</div></body></html>";

            final JLabel description = new JLabel(text);
            this.introPanel.add(description, "2");
            ((CardLayout) this.introPanel.getLayout()).show(this.introPanel, "2");

            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    try {
                        /*
                         * call the actual plugin search algorithm
                         */
                        PluginRegistrationDialog.this.pr.lookupPlugins();

                        synchronized (PluginRegistrationDialog.this.searchingMutex) {
                            PluginRegistrationDialog.this.searching = false;

                            // this prevents the app to repeatingly search
                            PluginRegistrationDialog.this.searchAlreadyFinished = true;

                            description.setText("<html><body><div>Finished searching.</div></body></html>");

                            // call waiters!
                            PluginRegistrationDialog.this.searchingMutex.notifyAll();
                        }
                    }
                    catch (IOException e) {
                        log.warn(e.getMessage(), e);
                    }
                }
            });
        }

    }

    /**
     * 
     * Panel for displaying the search results.
     * 
     * 
     */
    private class ResultPanel extends WizardPanelDescriptor {

        protected JPanel panel;
        protected JLabel info;
        protected JList inputList;
        protected JList outputList;

        public ResultPanel() {
            this.panel = new JPanel();
            this.panel.setLayout(new CardLayout());

            this.info = new JLabel("<html><body><div>Searching...</div></body></html>");
            this.panel.add(this.info, "1");

            this.setPanelComponent(this.panel);
            this.setPanelDescriptorIdentifier(SECOND_DESCRIPTOR);
        }

        /*
         * (non-Javadoc)
         * 
         * @see com.nexes.wizard.WizardPanelDescriptor#aboutToDisplayPanel()
         */
        @Override
        public void aboutToDisplayPanel() {
            if (PluginRegistrationDialog.this.searchAlreadyFinished)
                return;

            // disable buttons as long as searching
            getWizard().setBackButtonEnabled(false);
            getWizard().setNextFinishButtonEnabled(false);

            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    synchronized (PluginRegistrationDialog.this.searchingMutex) {
                        ResultPanel.this.info.setText("Searching... ");
                        while (PluginRegistrationDialog.this.searching) {
                            try {
                                // wait.. hopefully gets notified, heh!
                                PluginRegistrationDialog.this.searchingMutex.wait();
                            }
                            catch (InterruptedException e) {
                                log.warn(e.getMessage(), e);
                            }
                        }
                    }

                    displayResults();
                }
            });
        }

        protected void displayResults() {
            // search finished -> reactivate buttons
            getWizard().setBackButtonEnabled(true);
            getWizard().setNextFinishButtonEnabled(true);

            this.inputList = new JList(PluginRegistrationDialog.this.pr.getInputPlugins().toArray());
            this.inputList.addListSelectionListener(PluginRegistrationDialog.this.choice);
            this.outputList = new JList(PluginRegistrationDialog.this.pr.getOutputPlugins().toArray());

            JPanel listPanel = new JPanel();
            listPanel.setLayout(new GridBagLayout());

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.fill = GridBagConstraints.BOTH;
            gbc.anchor = GridBagConstraints.NORTHWEST;
            gbc.weightx = 1.0;
            gbc.weighty = 0.0;
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.insets = new Insets(1, 1, 1, 1);

            // first row
            gbc.gridwidth = 2;
            JLabel fin = new JLabel("<html><body><strong>Finished! "
                    + PluginRegistrationDialog.this.pr.getInputPlugins().size() + " InputPlugins, "
                    + PluginRegistrationDialog.this.pr.getOutputPlugins().size()
                    + " OutputPlugins found.</strong></body></html>");
            listPanel.add(fin, gbc);
            gbc.gridwidth = 1;

            // second row
            gbc.gridy++;
            gbc.weightx = 1.0;
            listPanel.add(new JLabel("InputPlugins"), gbc);
            gbc.gridx++;
            listPanel.add(new JLabel("OutputPlugins"), gbc);

            // third row
            gbc.gridy++;
            gbc.gridx = 0;
            gbc.weighty = 1.0;
            listPanel.add(new JScrollPane(this.inputList), gbc);
            gbc.gridx++;
            listPanel.add(new JScrollPane(this.outputList), gbc);

            // fourth row
            gbc.weighty = 0.0;
            gbc.gridy++;
            gbc.gridx = 0;
            gbc.gridwidth = 2;
            listPanel.add(new JLabel("<html><body><strong>Please select the plugins you want to use.</strong></body></html>"),
                          gbc);

            String s = "2";
            this.panel.add(listPanel, s);
            ((CardLayout) this.panel.getLayout()).show(this.panel, s);
        }

        /*
         * (non-Javadoc)
         * 
         * @see com.nexes.wizard.WizardPanelDescriptor#getBackPanelDescriptor()
         */
        @Override
        public Object getBackPanelDescriptor() {
            return FIRST_DESCRIPTOR;
        }

        /*
         * (non-Javadoc)
         * 
         * @see com.nexes.wizard.WizardPanelDescriptor#getNextPanelDescriptor()
         */
        @Override
        public Object getNextPanelDescriptor() {
            return THIRD_DESCRIPTOR;
        }

    }

    /**
     * 
     * Panel providing all available plugins for choosing by the user.
     * 
     * 
     */
    private class PluginChoicePanel extends WizardPanelDescriptor implements ListSelectionListener {

        private JPanel panel;
        private GridBagConstraints gbc;
        protected JList inputList;
        protected JList outputList;
        protected JList newInputList;
        protected JList newOutputList;
        private boolean alreadyDrawn;
        protected Map<String, TablePanel> tables = new HashMap<String, TablePanel>();
        private boolean modelUpdated = true;

        public PluginChoicePanel() {
            this.panel = new JPanel();
            this.panel.setLayout(new GridBagLayout());

            this.gbc = new GridBagConstraints();
            this.gbc.fill = GridBagConstraints.BOTH;
            this.gbc.anchor = GridBagConstraints.NORTHWEST;
            this.gbc.weightx = 1.0;
            this.gbc.weighty = 0.0;
            this.gbc.gridx = 0;
            this.gbc.gridy = 0;
            this.gbc.insets = new Insets(1, 1, 1, 1);

            // zero row
            this.gbc.gridwidth = 2;
            JLabel fin = new JLabel("<html><body>Currently enabled Plugins</body></html>");
            this.panel.add(fin, this.gbc);
            this.gbc.gridwidth = 1;

            this.setPanelComponent(this.panel);
            this.setPanelDescriptorIdentifier(THIRD_DESCRIPTOR);
        }

        /*
         * (non-Javadoc)
         * 
         * @see com.nexes.wizard.WizardPanelDescriptor#aboutToDisplayPanel()
         */
        @Override
        public void aboutToDisplayPanel() {
            List<String> newInputs = new ArrayList<String>();
            List<String> newOutputs = new ArrayList<String>();

            if (this.modelUpdated) {
                if (this.alreadyDrawn) {
                    ((DefaultListModel) (this.newInputList.getModel())).removeAllElements();
                    ((DefaultListModel) (this.newOutputList.getModel())).removeAllElements();
                }

                /*
                 * check constructors do it with reflections
                 */
                searchForConstructor(newInputs, PluginRegistrationDialog.this.result.inputList);
                searchForConstructor(newOutputs, PluginRegistrationDialog.this.result.outputList);

                /*
                 * re-set the items
                 */
                if (this.alreadyDrawn) {
                    DefaultListModel model = ((DefaultListModel) this.newInputList.getModel());
                    for (String string : newInputs) {
                        model.add(model.getSize(), string);
                    }
                    DefaultListModel model2 = ((DefaultListModel) this.newOutputList.getModel());
                    for (String string : newOutputs) {
                        model2.add(model2.getSize(), string);
                    }
                }

                this.modelUpdated = false;
            }

            /*
             * do not redraw the UI
             */
            if (this.alreadyDrawn)
                return;

            /*
             * currently active plugins
             */
            // first
            this.gbc.gridy++;
            this.gbc.weightx = 1.0;
            JLabel inLabel = new JLabel("InputPlugins");
            JLabel outLabel = new JLabel("OutputPlugins");

            this.panel.add(inLabel, this.gbc);
            this.gbc.gridx++;
            this.panel.add(outLabel, this.gbc);

            // second
            List<String> inputs = new ArrayList<String>();
            List<String> outputs = new ArrayList<String>();

            /*
             * retrieve currentyl registered plugins
             */
            for (String s : SPFRegistry.getInstance().getConfigProperty(SPFRegistry.INPUT_PLUGINS_PROP).split(SPFRegistry.LIST_SEPARATOR)) {
                if (s.trim().length() > 0)
                    inputs.add(s.trim());
            }
            for (String s : SPFRegistry.getInstance().getConfigProperty(SPFRegistry.OUTPUT_PLUGINS_PROP).split(SPFRegistry.LIST_SEPARATOR)) {
                if (s.trim().length() > 0)
                    outputs.add(s.trim());
            }

            int[] indices = new int[Math.max(inputs.size(), outputs.size())];
            for (int i = 0; i < indices.length; i++) {
                indices[i] = i;
            }

            this.gbc.gridy++;
            this.gbc.gridx = 0;
            this.gbc.weighty = 1.0;
            DefaultListModel inputModel = new DefaultListModel();
            for (String string : inputs) {
                inputModel.add(inputModel.getSize(), string);
            }
            this.inputList = new JList(inputModel);

            if (inputModel.getSize() > 0) {
                this.inputList.setSelectedIndices(indices);
            }
            this.panel.add(new JScrollPane(this.inputList), this.gbc);

            this.gbc.gridx++;
            DefaultListModel outputModel = new DefaultListModel();
            for (String string : outputs) {
                outputModel.add(outputModel.getSize(), string);
            }
            this.outputList = new JList(outputModel);

            if (outputModel.size() > 0) {
                this.outputList.setSelectedIndices(indices);
            }
            this.panel.add(new JScrollPane(this.outputList), this.gbc);

            /*
             * new plugins
             */
            // first row
            this.gbc.gridy++;
            this.gbc.gridx = 0;
            this.gbc.gridwidth = 2;
            this.gbc.weighty = 0.0;
            JLabel fin = new JLabel("<html><body>Found constructors for plugins:</body></html>");
            this.panel.add(fin, this.gbc);
            this.gbc.gridwidth = 1;
            this.gbc.weighty = 1.0;

            // third row
            this.gbc.gridy++;
            this.gbc.gridx = 0;
            this.gbc.weighty = 1.0;
            DefaultListModel newInputModel = new DefaultListModel();
            for (String string : newInputs) {
                newInputModel.add(newInputModel.getSize(), string);
            }
            this.newInputList = new JList(newInputModel);
            this.newInputList.addListSelectionListener(PluginRegistrationDialog.this.setup);
            this.panel.add(new JScrollPane(this.newInputList), this.gbc);

            this.gbc.gridx++;
            DefaultListModel newOutputModel = new DefaultListModel();
            for (String string : newOutputs) {
                newOutputModel.add(newOutputModel.getSize(), string);
            }
            this.newOutputList = new JList(newOutputModel);
            this.newOutputList.addListSelectionListener(PluginRegistrationDialog.this.setup);
            this.panel.add(new JScrollPane(this.newOutputList), this.gbc);

            // fourth row
            this.gbc.weighty = 0.0;
            this.gbc.gridy++;
            this.gbc.gridx = 0;
            this.gbc.gridwidth = 2;
            this.panel.add(new JLabel("<html><body><strong>Selected plugins will be used at the next"
                    + " application start.</strong></body></html>"), this.gbc);

            this.alreadyDrawn = true;
        }

        private List<String> searchForConstructor(List<String> targetList, JList sourceList) {
            for (Object o : sourceList.getSelectedValues()) {
                try {
                    Class< ? > clazz = Class.forName(o.toString(), false, getClass().getClassLoader());
                    Constructor< ? >[] cons = clazz.getConstructors();

                    for (Constructor< ? > con : cons) {
                        if (con.getParameterTypes().length > 0) {
                            /*
                             * lets try to receive parameter names. this is done using runtime-available
                             * annotations for the plugins constructor. if no annotation is present default
                             * labels are used.
                             */
                            ConstructorParameters params = con.getAnnotation(IModule.ConstructorParameters.class);
                            Object[][] paramNames = new String[con.getParameterTypes().length][2];
                            if (params == null) {
                                for (int i = 0; i < paramNames.length; i++) {
                                    paramNames[i][0] = "Parameter " + (i + 1);
                                    paramNames[i][1] = "";
                                }
                            }
                            else {
                                for (int i = 0; i < paramNames.length; i++) {
                                    paramNames[i][0] = params.value()[i];
                                    paramNames[i][1] = "";
                                }
                            }

                            /*
                             * create a TablePanel using the names
                             */
                            TablePanel paramTable = new TablePanel(paramNames);
                            paramTable.setInputPlugin(sourceList == PluginRegistrationDialog.this.result.inputList);
                            String pseudoSyntax = con.getName() + "(";
                            if (paramNames.length == 1) {
                                pseudoSyntax += paramNames[0][0] + ")";
                            }
                            else {
                                for (int i = 0; i < paramNames.length - 1; i++) {
                                    pseudoSyntax += paramNames[i][0] + ", ";
                                }
                                pseudoSyntax += paramNames[paramNames.length - 1][0] + ")";
                            }

                            this.tables.put(pseudoSyntax, paramTable);
                            targetList.add(pseudoSyntax);
                        }
                        else {
                            // create a dummy table instead. needed for internal model
                            TablePanel paramTable = new TablePanel();
                            paramTable.setInputPlugin(sourceList == PluginRegistrationDialog.this.result.inputList);
                            this.tables.put(o.toString(), paramTable);
                            targetList.add(o.toString());
                        }
                    }
                }
                catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
                catch (SecurityException e) {
                    e.printStackTrace();
                }
            }

            return targetList;
        }

        /*
         * (non-Javadoc)
         * 
         * @see com.nexes.wizard.WizardPanelDescriptor#getBackPanelDescriptor()
         */
        @Override
        public Object getBackPanelDescriptor() {
            return SECOND_DESCRIPTOR;
        }

        /*
         * (non-Javadoc)
         * 
         * @see com.nexes.wizard.WizardPanelDescriptor#getNextPanelDescriptor()
         */
        @Override
        public Object getNextPanelDescriptor() {
            return FOURTH_DESCRIPTOR;
        }

        /**
         * this is called whenever the selection of the results panel changed
         */
        @Override
        public void valueChanged(ListSelectionEvent e) {
            this.modelUpdated = true;
        }
    }

    /**
     * 
     * Last Panel for providing parameter fields for every selected constructor.
     * 
     * 
     */
    private class ConstructorSetupPanel extends WizardPanelDescriptor implements ListSelectionListener {

        private JPanel panel;
        private JLabel infoLabel;
        private GridBagConstraints globalGBC;
        private boolean modelUpdated = false;
        private JScrollPane pane;
        private JPanel wrapper;

        public ConstructorSetupPanel() {
            this.wrapper = new JPanel();
            this.wrapper.setLayout(new BorderLayout());

            this.panel = new JPanel();
            this.panel.setLayout(new GridBagLayout());
            this.globalGBC = new GridBagConstraints();
            this.globalGBC.gridx = 0;
            this.globalGBC.gridy = 0;
            this.globalGBC.anchor = GridBagConstraints.NORTHWEST;
            this.globalGBC.insets = new Insets(0, 0, 0, 0);
            this.globalGBC.weighty = 0.0;

            this.infoLabel = new JLabel("Plugins will be used at the next application startup.");
            this.wrapper.add(this.infoLabel, BorderLayout.NORTH);

            this.pane = new JScrollPane(this.panel);
            this.pane.setViewportBorder(null);
            this.pane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
            this.pane.getVerticalScrollBar().setBorder(BorderFactory.createEmptyBorder());
            this.pane.getHorizontalScrollBar().setBorder(BorderFactory.createEmptyBorder());
            this.wrapper.add(this.pane, BorderLayout.CENTER);

            this.setPanelComponent(this.wrapper);
            this.setPanelDescriptorIdentifier(FOURTH_DESCRIPTOR);
        }

        /*
         * (non-Javadoc)
         * 
         * @see com.nexes.wizard.WizardPanelDescriptor#aboutToDisplayPanel()
         */
        @Override
        public void aboutToDisplayPanel() {
            if ( !this.modelUpdated)
                return;

            /*
             * make it left aligned
             */
            this.globalGBC.fill = GridBagConstraints.HORIZONTAL;
            this.globalGBC.gridx = 1;
            this.globalGBC.gridy = 0;
            this.globalGBC.weightx = 1.0;
            this.panel.add(Box.createHorizontalGlue(), this.globalGBC);
            this.globalGBC.fill = GridBagConstraints.BOTH;
            this.globalGBC.weightx = 0.0;

            /*
             * reset grids
             */
            this.globalGBC.gridx = 0;
            this.globalGBC.gridy = 0;

            this.infoLabel.setText("<html><body><div><strong>"
                    + "The following plugins need parameters for instantiating them.</strong></div><div>"
                    + "All values will be treated as Strings.</div></body></html>");

            this.globalGBC.gridy++;
            this.panel.add(new JSeparator(), this.globalGBC);
            this.globalGBC.gridy++;

            boolean setupNeeded = false;
            for (Object o : PluginRegistrationDialog.this.choice.newInputList.getSelectedValues()) {
                if (createAndAddParameterPanel(o.toString()))
                    setupNeeded = true;
            }

            for (Object o : PluginRegistrationDialog.this.choice.newOutputList.getSelectedValues()) {
                if (createAndAddParameterPanel(o.toString()))
                    setupNeeded = true;
            }

            this.globalGBC.weighty = 10.0;
            this.panel.add(Box.createVerticalGlue(), this.globalGBC);
            this.globalGBC.weighty = 0.0;

            if ( !setupNeeded) {
                this.infoLabel.setText("Plugins will be used at the next application startup.");
            }

            this.modelUpdated = false;
        }

        private boolean createAndAddParameterPanel(String plugin) {
            if (PluginRegistrationDialog.this.choice.tables.containsKey(plugin)
                    && !PluginRegistrationDialog.this.choice.tables.get(plugin).isDummy()) {
                JPanel panel2 = new JPanel();
                panel2.setBorder(BorderFactory.createEmptyBorder());
                panel2.setLayout(new BorderLayout());
                panel2.add(new JLabel(plugin.substring(0, plugin.indexOf("("))), BorderLayout.NORTH);
                panel2.add(PluginRegistrationDialog.this.choice.tables.get(plugin), BorderLayout.CENTER);
                panel2.add(new JSeparator(), BorderLayout.SOUTH);

                this.panel.add(panel2, this.globalGBC);
                this.globalGBC.gridy++;
                return true;
            }
            return false;
        }

        /*
         * (non-Javadoc)
         * 
         * @see com.nexes.wizard.WizardPanelDescriptor#getBackPanelDescriptor()
         */
        @Override
        public Object getBackPanelDescriptor() {
            return THIRD_DESCRIPTOR;
        }

        /*
         * (non-Javadoc)
         * 
         * @see com.nexes.wizard.WizardPanelDescriptor#getNextPanelDescriptor()
         */
        @Override
        public Object getNextPanelDescriptor() {
            return FINISH;
        }

        @Override
        public void valueChanged(ListSelectionEvent e) {
            this.modelUpdated = true;
            this.panel.removeAll();
        }

    }

    /**
     * Helper panel creating a simple input gui for one constructor.
     */
    public class TablePanel extends JPanel {
        private List<JTextField> values;
        private boolean inputPlugin;
        private boolean dummy;

        /**
         * @return true if representation of {@link IInputPlugin}
         */
        public boolean isInputPlugin() {
            return this.inputPlugin;
        }

        protected void setInputPlugin(boolean inputPlugin) {
            this.inputPlugin = inputPlugin;
        }

        /**
         * @param paramNames
         *        2d array of parameters
         */
        public TablePanel(Object[][] paramNames) {
            this.setLayout(new GridBagLayout());
            this.setBorder(BorderFactory.createEmptyBorder());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.anchor = GridBagConstraints.NORTHWEST;
            gbc.insets = new Insets(1, 1, 1, 1);

            this.values = new ArrayList<JTextField>();

            for (int i = 0; i < paramNames.length; i++) {
                gbc.gridx = 0;
                this.add(new JLabel(paramNames[i][0].toString()), gbc);
                this.values.add(new JTextField(paramNames[i][1].toString()));
                this.values.get(i).setPreferredSize(new Dimension(150, this.values.get(i).getPreferredSize().height));

                gbc.gridx++;
                this.add(this.values.get(i), gbc);
                gbc.gridy++;
            }
        }

        /**
         * dummy panel constructor
         */
        public TablePanel() {
            this.dummy = true;
        }

        /**
         * @return true if this is a dummy panel (holding no actual data; used for zero-parameter
         *         constructors)
         */
        public boolean isDummy() {
            return this.dummy;
        }

        /**
         * @return the values as set by the user
         */
        public List<String> getValues() {
            ArrayList<String> resultList = new ArrayList<String>();

            for (JTextField string : this.values) {
                String val = string.getText();

                if (val == null || val.equals("")) {
                    resultList.add("null");
                }
                else {
                    resultList.add(val.trim());
                }
            }

            return resultList;
        }

        private static final long serialVersionUID = 1L;

    }

}
