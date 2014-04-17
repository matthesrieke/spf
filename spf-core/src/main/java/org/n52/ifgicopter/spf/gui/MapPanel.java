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
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JPanel;

import org.n52.ifgicopter.spf.common.IPositionListener;
import org.n52.ifgicopter.spf.gui.map.ImageMapMarker;
import org.n52.ifgicopter.spf.gui.map.JMKOMap;
import org.n52.ifgicopter.spf.gui.map.JMapWithOverlay;
import org.n52.ifgicopter.spf.gui.map.MapOverlay;
import org.n52.ifgicopter.spf.xml.Plugin;
import org.openstreetmap.gui.jmapviewer.DefaultMapController;
import org.openstreetmap.gui.jmapviewer.MemoryTileCache;

/**
 * Class holding a OSM presentation with tracked platform positions.
 * 
 * @author Matthes Rieke <m.rieke@uni-muenster.de>
 * 
 */
public class MapPanel extends JPanel implements IPositionListener {

    /**
	 * 
	 */
    private static final long serialVersionUID = 1L;
    JMKOMap map;
    private Map<Plugin, ImageMapMarker> markers = new HashMap<Plugin, ImageMapMarker>();

    /**
     * default constructor creating an OSM map.
     */
    public MapPanel() {
        this.map = new JMapWithOverlay(new MemoryTileCache(), 4);

        /*
         * enable dragging
         */
        @SuppressWarnings("unused")
        DefaultMapController controller = new DefaultMapController((JMapWithOverlay) this.map);

        /*
         * handle JMapWithOverlay
         */
        @SuppressWarnings("unused")
        DefaultMapController controller2 = new DefaultMapController((JMapWithOverlay) this.map) {

            @Override
            public void mouseMoved(MouseEvent e) {
                MapPanel.this.map.setDisplayLatLon(this.map.getPosition(e.getPoint()));
                super.mouseMoved(e);
            }

        };

        this.setLayout(new BorderLayout());

        this.add((JMapWithOverlay) this.map);
    }

    @Override
    public void positionUpdate(Plugin plugin, Map<String, Object> newPos) {

        Object first = newPos.get(plugin.getLocation().getFirstCoordinateName());
        Object second = newPos.get(plugin.getLocation().getSecondCoordinateName());

        ImageMapMarker tmp = this.markers.get(plugin);
        if (tmp == null) {
            /*
             * create a new map marker and list entry
             */
            int icon = 0;
            switch (this.markers.size()) {
            case 0:
                icon = ImageMapMarker.YELLOW_ICON;
                break;
            case 1:
                icon = ImageMapMarker.GREEN_ICON;
                break;
            case 2:
                icon = ImageMapMarker.RED_ICON;
                break;
            case 3:
                icon = ImageMapMarker.BLUE_ICON;
                break;
            default:
                icon = ImageMapMarker.YELLOW_ICON;
                break;
            }
            tmp = new ImageMapMarker( ((Double) first).doubleValue(), ((Double) second).doubleValue(), icon);
            this.markers.put(plugin, tmp);
            this.map.addMapMarker(tmp);

            if (this.map instanceof JMapWithOverlay) {
                ((JMapWithOverlay) this.map).setDisplayPositionByLatLon(tmp.getLat(), tmp.getLon(), 16);
                ((JMapWithOverlay) this.map).addOverlay(new ItemOverlay(MapOverlay.TOP_RIGHT,
                                                                        this.map,
                                                                        plugin.getName(),
                                                                        tmp.getImage()));
            }
        }
        else {
            /*
             * update the old and repaint
             */
            tmp.setLat(((Double) first).doubleValue());
            tmp.setLon(((Double) second).doubleValue());
            this.repaint();
        }

    }

    private class ItemOverlay extends MapOverlay {

        /**
		 * 
		 */
        private static final long serialVersionUID = 1L;
        private Graphics2D graphics;
        private final Color trans = new Color(0, 0, 0, 140);
        private Map<String, BufferedImage> entries = new HashMap<String, BufferedImage>();
        private Font itemfontItalic;
        private Font itemfont;

        public ItemOverlay(int i, JMKOMap observ, String firstPluginName, BufferedImage icon) {
            super(i, observ);

            String newString = ": " + firstPluginName;
            this.itemfontItalic = new Font(Font.SANS_SERIF, Font.ITALIC, 16);
            this.itemfont = new Font(Font.SANS_SERIF, Font.PLAIN, 16);

            this.overlay = new BufferedImage(200, 18, BufferedImage.TYPE_INT_ARGB);

            this.graphics = this.overlay.createGraphics();
            this.graphics.setFont(this.itemfontItalic);

            addNewItem(newString + " - 34.2m", icon);
        }

        public void addNewItem(String firstPluginName, BufferedImage icon) {
            this.entries.put(firstPluginName, icon);

            FontMetrics metrics = this.graphics.getFontMetrics();

            /*
             * get the widest
             */
            int max = 0;
            int tmp;
            for (String str : this.entries.keySet()) {
                tmp = metrics.stringWidth(str);
                if (tmp > max)
                    max = tmp;
            }

            this.width = 4 + icon.getWidth() + max;

            /*
             * number of lines
             */
            this.height = metrics.getHeight() * this.entries.size() + 4;

            /*
             * workaround - needed twice because FontMetrics are not available until image is created.
             */
            this.overlay = new BufferedImage(this.width, this.height, BufferedImage.TYPE_INT_ARGB);

            this.graphics = this.overlay.createGraphics();
            this.graphics.setFont(this.itemfontItalic);
            this.graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // /*
            // * remove the old
            // */
            // this.graphics.setComposite(AlphaComposite.Clear);
            // this.graphics.fillRect(0,0,this.width, this.height);
            // this.graphics.setComposite(AlphaComposite.SrcOver);

            /*
             * rounded rect half transparent
             */
            this.graphics.setColor(this.trans);
            this.graphics.fillRoundRect(0, 0, this.width, this.height, 5, 5);
            this.graphics.setColor(Color.white);

            /*
             * draw the new
             */
            int offset = metrics.getHeight() - 2;

            /*
             * iterate over the entries. draw multiple lines using an incremented offset
             */
            BufferedImage img;
            for (String str : this.entries.keySet()) {
                img = this.entries.get(str);
                if (img != null) {
                    this.graphics.drawImage(img, null, 2, offset - img.getHeight());
                    this.graphics.setFont(this.itemfontItalic);
                }
                else {
                    this.graphics.setFont(this.itemfont);
                }

                this.graphics.drawString(str, icon.getWidth() + 2, offset);
                offset += metrics.getHeight();
            }

        }

    }

}
