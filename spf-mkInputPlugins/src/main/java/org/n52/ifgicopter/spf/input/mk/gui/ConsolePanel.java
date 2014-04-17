package org.n52.ifgicopter.spf.input.mk.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextPane;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.impl.Log4JLogger;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.spi.LoggingEvent;
import org.n52.ifgicopter.javamk.MKConstants;
import org.n52.ifgicopter.javamk.protocol.AbstractCommLayer;


/**
 * Class for displaying the command strings.
 * 
 * @author Matthes Rieke <m.rieke@uni-muenster.de>
 *
 */
public class ConsolePanel extends JPanel {

	private static final Log log = LogFactory.getLog(MKConstants.LOGGER_ID);

	private static final long serialVersionUID = 1L;

	private static final String CONSOLE_LOGGER = ConsolePanel.class.getName() + ".ConsoleLogger";
	private StyledDocument consoleDoc;
	private SimpleAttributeSet blackColor;
	private SimpleAttributeSet blueColor;
	private SimpleAttributeSet greenColoer;
	private JTextPane console;
	private DateFormat format = new SimpleDateFormat("HH:mm:ss.S ");

	private boolean displayDebug = true;
	private JCheckBox scrollLock;

	private Log commandLogger;

	private static Log defaultLogger = LogFactory.getLog(ConsolePanel.CONSOLE_LOGGER);


	/**
	 * Default constructor.
	 */
	public ConsolePanel() {

		JPanel checkboxPanel = new JPanel();
		checkboxPanel.setLayout(new BorderLayout());
		JSeparator separator = new JSeparator(SwingConstants.HORIZONTAL);
		checkboxPanel.add(separator, BorderLayout.NORTH);
		JCheckBox debug = new JCheckBox("debug");
		debug.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				ConsolePanel.this.displayDebug = ((JCheckBox) e.getSource()).isSelected();
			}
		});
		debug.setSelected(true);
		checkboxPanel.add(debug, BorderLayout.WEST);

		scrollLock = new JCheckBox("Scroll lock");
		scrollLock.setSelected(true);
		checkboxPanel.add(scrollLock, BorderLayout.EAST);

		/*
		 * black text color AttributeSet
		 */
		this.blackColor = new SimpleAttributeSet();
		this.blackColor.addAttribute(StyleConstants.Foreground, Color.black);

		/*
		 * blue text color AttributeSet
		 */
		this.blueColor = new SimpleAttributeSet();
		this.blueColor.addAttribute(StyleConstants.Foreground, Color.blue);

		/*
		 * red text color AttributeSet 
		 */
		this.greenColoer = new SimpleAttributeSet();
		this.greenColoer.addAttribute(StyleConstants.Foreground, Color.green);

		this.setLayout(new BorderLayout());
		this.console = new JTextPane() {
			private static final long serialVersionUID = 1L;

			@Override
			public boolean getScrollableTracksViewportWidth() {
				return false;
			}
		};
		this.consoleDoc = this.console.getStyledDocument();

		this.console.setBackground(this.getBackground());
		this.console.setFont(Font.getFont(Font.MONOSPACED));
		this.console.setEditable(false);

		JScrollPane jsp = new JScrollPane(this.console);

		jsp.setPreferredSize(new Dimension(850, 150));
		jsp.setBackground(Color.white);

		this.add(checkboxPanel, BorderLayout.NORTH);
		this.add(jsp);

		CommandLoggingHandler logHandler = new CommandLoggingHandler();
		this.commandLogger = LogFactory.getLog(AbstractCommLayer.COMMAND_LOGGER_IDENTIFIER);
		if (this.commandLogger instanceof Log4JLogger) {
			((Log4JLogger) this.commandLogger).getLogger().addAppender(logHandler);
		}
		
		/*
		 * the default logger appender
		 */
		if (defaultLogger instanceof Log4JLogger) {
			((Log4JLogger) defaultLogger).getLogger().addAppender(new ConsoleLoggingHandler());
		}
	}


	/**
	 * Append a new String[3] to the console.
	 * 
	 * @param strRec String[] of length 3. first element contains time and source method.
	 * 		second element holds the logging message. If the third element is != null
	 * 		the message is treated as a warning.
	 */
	private synchronized void appendString(String[] strRec) {
		if (!this.displayDebug && strRec[1].charAt(7) == 'D') {
			return;
		}
		if (strRec.length != 3) throw new IllegalArgumentException("Records must contain three items!");

		try {
			this.consoleDoc.insertString(this.consoleDoc.getLength(), strRec[0], this.blackColor);

			/*
			 * is this a warning? strRec[2] != null
			 */
			if (strRec[2] == null) {
				this.consoleDoc.insertString(this.consoleDoc.getLength(), strRec[1], this.blueColor);
			} else {
				this.consoleDoc.insertString(this.consoleDoc.getLength(), strRec[1], this.greenColoer);
			}

			this.consoleDoc.insertString(this.consoleDoc.getLength(), "\n", this.blackColor);
			/*
			 * autoscroll to end
			 */
			if (scrollLock.isSelected()) {
				this.console.setCaretPosition(this.consoleDoc.getLength());
			}

		} catch (BadLocationException e) {
			log.warn(e.getMessage(), e);
		} catch (IllegalArgumentException e) {
			log.warn(e.getMessage(), e);
		}
	}

	@Override
	public Color getBackground() {
		return Color.white;
	}


	private class CommandLoggingHandler extends AppenderSkeleton {

		@Override
		public void close() throws SecurityException {
		}



		@Override
		public boolean requiresLayout() {
			return false;
		}

		@Override
		protected void append(LoggingEvent record) {
			if (!record.getLevel().isGreaterOrEqual(Level.INFO)) {
				/*
				 * dont log, its too low
				 */
				return;
			}
			if (record.getMessage().toString().startsWith("[out]")) {
				appendString(new String[] {ConsolePanel.this.format.format(new Date(record.getTimeStamp())),
						record.getMessage().toString(), null});
			} else {
				appendString(new String[] {ConsolePanel.this.format.format(new Date(record.getTimeStamp())),
						record.getMessage().toString(), ""});
			}
		}

	}

	/**
	 * Appends any logs to the console
	 * 
	 * @author matthes rieke
	 *
	 */
	public class ConsoleLoggingHandler extends AppenderSkeleton {

		@Override
		public void close() throws SecurityException {
		}

		@Override
		public boolean requiresLayout() {
			return false;
		}

		@Override
		protected void append(LoggingEvent record) {
			if (!record.getLevel().isGreaterOrEqual(Level.INFO)) {
				/*
				 * dont log, its too low
				 */
				return;
			}
			boolean warn = record.getLevel().isGreaterOrEqual(Level.WARN);
			appendString(new String[] {ConsolePanel.this.format.format(new Date(record.getTimeStamp())),
					record.getMessage().toString(), warn ? "" : null});
		}

	}

	/**
	 * global console logger
	 * @return the logger
	 */
	public static Log getDefaultLogger() {
		return defaultLogger;
	}

}
