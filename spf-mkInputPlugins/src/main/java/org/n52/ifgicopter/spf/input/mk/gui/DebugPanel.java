package org.n52.ifgicopter.spf.input.mk.gui;

import java.awt.Color;

import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;

import org.n52.ifgicopter.javamk.incoming.AnalogValue;

/**
 * @author Matthes Rieke <m.rieke@uni-muenster.de>
 *
 */
public class DebugPanel extends JPanel {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private DefaultTableModel analogModel;
	
	
	/**
	 * default constructor
	 */
	public DebugPanel() {
		String[][] analogLabelsAndData = new String[32][2];
		for (int i = 0; i < analogLabelsAndData.length; i++) {
			analogLabelsAndData[i][0] = "AnalogValue"+ i;
		}
		
		JTable analogTable = new JTable();
		this.analogModel = new DefaultTableModel(analogLabelsAndData, new String[] {"Label", "Value"}) {
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isCellEditable(int row, int column) {
				return false;
			}
		};
		analogTable.setModel(this.analogModel);
		analogTable.getColumnModel().getColumn(0).setPreferredWidth(120);
		analogTable.getColumnModel().getColumn(1).setPreferredWidth(50);
		analogTable.setBorder(new LineBorder(Color.gray));
		
		this.add(analogTable);
	}
	
	
	/**
	 * @param values new decoded analog value
	 */
	public void processDebugData(int[] values) {
		for (int i = 0; i < values.length; i++) {
			this.analogModel.setValueAt(values[i], i, 1);
		}
		this.analogModel.fireTableDataChanged();
	}

	/**
	 * @param value new property label
	 */
	public void processAnalogLabel(AnalogValue value) {
		int pos = value.getPosition();
		
		if (pos < 32 && pos >= 0) {
			this.analogModel.setValueAt(value.getName(), pos, 0);
			this.analogModel.fireTableCellUpdated(pos, 0);
		}
	}
	
}
