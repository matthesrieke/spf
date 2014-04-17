package org.n52.ifgicopter.spf.input.mk.gui.map;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

/**
 * An overlay showing the signal strength of the MK.
 * 
 * @author Matthes Rieke <m.rieke@uni-muenster.de>
 *
 */
public class SignalOverlay extends MapOverlay {
	
	private static final long serialVersionUID = 1L;
	BufferedImage[] images = new BufferedImage[10];

	/**
	 * @param pos orientation
	 * @param observ the map
	 * @throws IOException if error occures.
	 */
	public SignalOverlay(int pos, JMKOMap observ) throws IOException {
		super(pos, observ);
		
		for (int i = 1; i <= 10; i++) {
			InputStream in = null;
			File f = new File("img/mk_signal_" +i*10 +".png");
			if (!f.exists()) {
				in = getClass().getResourceAsStream("/img/mk_signal_" +i*10 +".png");
			}else {
				in = new FileInputStream(f);
			}
			
			images[i-1] = ImageIO.read(in);
		}
		
		this.setOverlay(images[9]);
		
	}
	
	/**
	 * @param strength must be in range [1..10]
	 */
	public void setSignalStrength(int strength) {
		if (strength < 1 || strength > 10) {
			throw new IllegalArgumentException("strength must be in range [1..10]");
		}
		
		this.setOverlay(images[strength-1]);
	}

}
