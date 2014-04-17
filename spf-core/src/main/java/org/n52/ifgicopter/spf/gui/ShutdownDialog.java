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
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;

/**
 * The shutdown dialog.
 * 
 * @author Matthes Rieke <m.rieke@uni-muenster.de>
 * 
 */
public class ShutdownDialog extends JDialog implements Runnable {

    private static final long serialVersionUID = 1L;

    public ShutdownDialog(SPFMainFrame spfMainFrame) {
        super(spfMainFrame);
    }

    @Override
    public void run() {
        this.setResizable(false);

        this.setTitle("SPF shutdown");

        JProgressBar progress = new JProgressBar(SwingConstants.HORIZONTAL);
        progress.setPreferredSize(new Dimension(200, 15));

        progress.setIndeterminate(true);

        JPanel panel = new JPanel();
        panel.add(new JLabel("shutting down system..."));

        JPanel panel2 = new JPanel();
        panel2.add(progress);
        panel2.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));

        this.getContentPane().add(panel, BorderLayout.CENTER);
        this.getContentPane().add(panel2, BorderLayout.SOUTH);

        this.pack();
        this.setLocationRelativeTo(getParent());

        this.setVisible(true);
        this.repaint();

        /*
         * wait for kill
         */
        while (true) {
            // just wait...
        }
    }

}
