package org.n52.ifgicopter.spf.input.mk.gui.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

import org.openstreetmap.gui.jmapviewer.Coordinate;

/**
 * Class for handling JPEG COM tags.
 * Reads and writes comments to specification conform
 * JPEG/JFIF files.
 * 
 * @author Matthes Rieke <m.rieke@uni-muenster.de>
 *
 */
public class JPEGCommentIO {
	
	private static final char TAG_START = 0xFF;
	private static final char SOS_TAG = 0xDA;
	private static final char COM_TAG = 0xFE;
	private File file;
	private int[] originalBytes;
	private int sosPosition;
	private int comPosition;


	/**
	 * @param file the file holding the JPEG image
	 * @throws IOException if error occures
	 */
	public JPEGCommentIO(File file) throws IOException {
		this.file = file;
		this.originalBytes = readBytes();
	}

	/**
	 * @return the contents of the comment tag
	 */
	public String readComment() {
		if (this.comPosition == 0) {
			return null;
		}
		
		int length = (originalBytes[this.comPosition + 2] << 8) |
			originalBytes[this.comPosition + 3];
		StringBuilder sb = new StringBuilder();
		
		for (int i = this.comPosition + 4; i < this.comPosition + length + 2; i++) {
			sb.append((char) originalBytes[i]);
		}
		
		return sb.toString();
	}

	/**
	 * Appends a comment to the JPEG image.
	 * 
	 * @param string the comment
	 * @throws UnsupportedOperationException if there is already a COM tag
	 * @throws IOException if error occures
	 */
	public void appendComment(String string) throws UnsupportedOperationException, IOException {
		if (this.comPosition != 0) {
			throw new UnsupportedOperationException("Comment already exists - unsupported.");
		}
		
		char[] chars = string.toCharArray();
		int length = string.length() + 2;
		
		if (((length >> 16) & 0xFF) > 0) {
			/*
			 * comment is too large
			 */
			throw new IllegalArgumentException("Comment too large.");
		}
		
		int high = (length >> 8) & 0xFF;
		int low = length & 0xFF;
		
		FileOutputStream fos = new FileOutputStream(this.file);
		
		/*
		 * append all other header markers
		 */
		for (int i = 0; i < this.sosPosition; i++) {
			fos.write(originalBytes[i]);
		}
		
		/*
		 * add the new comment 
		 */
		fos.write(0xFF);
		fos.write(0xFE);
		fos.write(high);
		fos.write(low);
		
		for (char c : chars) {
			fos.write(c);
		}
		
		/*
		 * add the rest (real data)
		 */
		for (int i = this.sosPosition; i < this.originalBytes.length; i++) {
			fos.write(originalBytes[i]);
		}
		
		fos.flush();
		fos.close();
	}


	private int[] readBytes() throws IOException {
		boolean tagFound = false;
		InputStream is = new FileInputStream(this.file);
		long length = this.file.length();
		
		if (length > Integer.MAX_VALUE) {
			throw new IOException("File too large.");
		} 
		
		int[] bytes = new int[(int)length];
		
		int i = 0;
		while (is.available() > 0) {
			bytes[i] = is.read();
			if (tagFound) {
				if (bytes[i] == SOS_TAG) {
					this.sosPosition = i-1;
				}
				else if (bytes[i] == COM_TAG) {
					this.comPosition = i-1;
				}
				tagFound = false;
			}
			else if (bytes[i] == TAG_START) {
				tagFound = true;
			}
			i++;
		}

		is.close();
		return bytes;
	}

	/**
	 * Appends GEOInformation to a JPEG file.
	 * this is used for offline maps and can also be
	 * used with the MikrokopterTool.
	 * 
	 * @param topLeft topLeft coord of the image
	 * @param lowRight lowRight coord of the image
	 * @throws UnsupportedOperationException if there is already a COM tag
	 * @throws IOException if error occures
	 */
	public void appendGeoInfo(Coordinate topLeft, Coordinate lowRight) throws UnsupportedOperationException, IOException {
		DecimalFormat format = new DecimalFormat("#0.000000");
		DecimalFormatSymbols sym = format.getDecimalFormatSymbols();
		sym.setDecimalSeparator('.');
		format.setDecimalFormatSymbols(sym);
		StringBuilder sb = new StringBuilder();
		/*
		 * top right, top left, bottom right, bottom left
		 */
		//51.972840:7.600740,51.972840:7.598670,51.971220:7.600740,51.971220:7.598670,0.000000
		sb.append("GEO-Information,");

		sb.append(format.format(topLeft.getLat()));
		sb.append(":");
		sb.append(format.format(lowRight.getLon()));
		sb.append(",");

		sb.append(format.format(topLeft.getLat()));
		sb.append(":");
		sb.append(format.format(topLeft.getLon()));
		sb.append(",");

		sb.append(format.format(lowRight.getLat()));
		sb.append(":");
		sb.append(format.format(lowRight.getLon()));
		sb.append(",");

		sb.append(format.format(lowRight.getLat()));
		sb.append(":");
		sb.append(format.format(topLeft.getLon()));
		sb.append(",0.000000");

		this.appendComment(sb.toString());		
	}

}
