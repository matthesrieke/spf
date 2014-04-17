package org.n52.ifgicopter.spf.input.mk.gui.tools;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JTextField;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.n52.ifgicopter.javamk.CommandBasic;
import org.n52.ifgicopter.javamk.CommandReceive;
import org.n52.ifgicopter.javamk.MKConstants;
import org.n52.ifgicopter.javamk.outgoing.SendWaypointCommand;

/**
 * Extending {@link JDialog} having the capability
 * of decoding a raw command to an int-array.
 * 
 * @author Matthes Rieke <m.rieke@uni-muenster.de>
 *
 */
public class DecodeCommandDialog extends JDialog {

	private static final Log log = LogFactory.getLog(MKConstants.LOGGER_ID);

	private static final long serialVersionUID = 1L;
	private JTextField input;
	private JTextField result;


	/**
	 * @param parent the parent component
	 */
	public DecodeCommandDialog(Frame parent) {
		super(parent);
		this.setModal(true);

		input = new JTextField();
		input.setPreferredSize(new Dimension(400, 25));

		JButton decode = new JButton("Decode");
		decode.addActionListener(new DecodeAction());

		result = new JTextField();
		result.setEditable(false);
		result.setPreferredSize(new Dimension(400, 25));

		this.setLayout(new BorderLayout());

		this.add(input, BorderLayout.NORTH);
		this.add(decode, BorderLayout.CENTER);
		this.add(result, BorderLayout.SOUTH);

		this.setResizable(false);

		this.pack();
		this.setLocationRelativeTo(parent);
		this.setVisible(true);
	}


	private class DecodeAction implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			byte[] bytes = input.getText().getBytes();
			if (bytes.length < 2) {
				return;
			}
			CommandReceive cmd = new CommandReceive(bytes);
			int[] decoded = cmd.decode(bytes);

			StringBuilder sb = new StringBuilder();

			boolean checksum = CommandBasic.verifyChecksum(bytes);
			if (!checksum) {
				sb.append("Checksum fail!! ");
			}
			else {

				sb.append("Address: ");
				sb.append(MKConstants.getNameForAddress((char) (cmd.getAddress() - 'a')));
				sb.append(", id=");
				sb.append(cmd.getCommandID());
				sb.append(", decodedData=[");

				for (int i = 0; i < decoded.length - 1; i++) {
					sb.append(decoded[i]);
					sb.append(", ");
				}
				sb.append(decoded[decoded.length - 1]);
				sb.append("]");
			}

			if (!checksum) {
				sb.append("Checksum fail!! ");
			}

			result.setText(sb.toString());

			if (!checksum) {
				return;
			}
			
			switch (cmd.getCommandID()) {
			case 'w':
				SendWaypointCommand sendWP = new SendWaypointCommand(decoded);
				log.info(sendWP.getWaypoint().toString());
				break;

			default:
				break;
			}
		}

	}
}
