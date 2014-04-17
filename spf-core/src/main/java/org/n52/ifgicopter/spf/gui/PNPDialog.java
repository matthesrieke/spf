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

import java.awt.Frame;
import java.awt.GridLayout;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import com.nexes.wizard.Wizard;
import com.nexes.wizard.WizardPanelDescriptor;

/**
 * Extending {@link JDialog} having the capability of decoding a raw command to an int-array.
 * 
 * @author Matthes Rieke <m.rieke@uni-muenster.de>
 * 
 */
public class PNPDialog {

    /**
     * id for first panel
     */
    public static final String FIRST_DESCRIPTOR = "INTRO";

    /**
     * id for second panel
     */
    public static final String SECOND_DESCRIPTOR = "UOM";

    private String uom;

    private String defintion;

    private Class< ? > datatype;

    private boolean output;

    private boolean mandatory;

    private boolean canceled = false;

    private boolean pnpOff = false;

    private int ret;

    /**
     * @param parent
     *        the parent component
     * @param property
     *        the unknown property
     */
    public PNPDialog(Frame parent, String property) {

        Wizard wizard = new Wizard(parent);
        wizard.getDialog().setTitle("Test Wizard Dialog");

        WizardPanelDescriptor intro = new IntroPanel(property);
        wizard.registerWizardPanel(FIRST_DESCRIPTOR, intro);

        MetadataPanel meta = new MetadataPanel(property);
        wizard.registerWizardPanel(SECOND_DESCRIPTOR, meta);

        wizard.setCurrentPanel(FIRST_DESCRIPTOR);
        wizard.setTitle("New unknown property '" + property + "'");

        wizard.getDialog().setIconImage(parent.getIconImage());

        this.ret = wizard.showModalDialog();

        /*
         * this is a cancel
         */
        if (this.ret == Wizard.CANCEL_RETURN_CODE) {
            this.canceled = true;
        }

        this.uom = meta.getUomField().getText();
        this.defintion = meta.getDefField().getText();
        this.output = meta.getOutputBox().isSelected();
        this.mandatory = meta.getMandatoryBox().isSelected();

        String type = meta.getTypeBox().getSelectedItem().toString();

        if (type.equals("double")) {
            this.datatype = Double.class;
        }
        else if (type.equals("text")) {
            this.datatype = String.class;
        }
        else if (type.equals("boolean")) {
            this.datatype = Boolean.class;
        }
    }

    /**
     * @param args
     *        unused
     */
    public static void main(String[] args) {
        JFrame frame = new JFrame();
        frame.setLocationRelativeTo(null);
        frame.getContentPane().add(new JLabel("asdas"));
        frame.pack();
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        @SuppressWarnings("unused")
        PNPDialog pnpDialog = new PNPDialog(frame, "asdasd");
    }

    /**
     * @return the uom
     */
    public String getUom() {
        return this.uom;
    }

    /**
     * @return the defintion
     */
    public String getDefintion() {
        return this.defintion;
    }

    /**
     * @return the datatype
     */
    public Class< ? > getDatatype() {
        return this.datatype;
    }

    /**
     * @return the output
     */
    public boolean isOutput() {
        return this.output;
    }

    /**
     * @return the mandatory
     */
    public boolean isMandatory() {
        return this.mandatory;
    }

    /**
     * @return the canceled
     */
    public boolean isCanceled() {
        return this.canceled;
    }

    /**
     * set the pnp flag
     */
    public void setPNPOff() {
        this.pnpOff = true;
    }

    /**
     * @return the pnpOff
     */
    public boolean isPnpOff() {
        return this.pnpOff;
    }

    /**
     * @return the return status of the dialog
     */
    public int getReturn() {
        return this.ret;
    }

    private class IntroPanel extends WizardPanelDescriptor {

        public IntroPanel(String property) {
            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            panel.add(new JLabel("A new unknown property '" + property + "' has arrived at the framework core."));
            panel.add(new JLabel("Use this wizard to specify the metadata for this property."));
            this.setPanelComponent(panel);
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

    }

    private class MetadataPanel extends WizardPanelDescriptor {

        private JTextField defField = new JTextField();
        private JTextField uomField = new JTextField();
        private JComboBox typeBox = new JComboBox(new String[] {"double", "text", "boolean"});
        private JCheckBox outputBox = new JCheckBox();
        private JCheckBox mandatoryBox = new JCheckBox();

        // FIXME remove unsused parameter
        public MetadataPanel(String property) {
            JPanel newPanel = new JPanel();
            GridLayout gl = new GridLayout(0, 2);
            gl.setHgap(5);
            gl.setVgap(5);
            newPanel.setLayout(gl);
            newPanel.setBorder(new EmptyBorder(2, 2, 2, 2));

            newPanel.add(new JLabel("definition"));
            newPanel.add(this.defField);

            newPanel.add(new JLabel("uom"));
            newPanel.add(this.uomField);

            newPanel.add(new JLabel("datatype"));
            newPanel.add(this.typeBox);

            newPanel.add(new JLabel("should generate output?"));
            newPanel.add(this.outputBox);

            newPanel.add(new JLabel("mandatory for output?"));
            newPanel.add(this.mandatoryBox);

            this.setPanelComponent(newPanel);
            this.setPanelDescriptorIdentifier(SECOND_DESCRIPTOR);
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
            return FINISH;
        }

        /**
         * @return the defField
         */
        public JTextField getDefField() {
            return this.defField;
        }

        /**
         * @return the uomField
         */
        public JTextField getUomField() {
            return this.uomField;
        }

        /**
         * @return the typeBox
         */
        public JComboBox getTypeBox() {
            return this.typeBox;
        }

        /**
         * @return the outputBox
         */
        public JCheckBox getOutputBox() {
            return this.outputBox;
        }

        /**
         * @return the mandatoryBox
         */
        public JCheckBox getMandatoryBox() {
            return this.mandatoryBox;
        }

    }

}
