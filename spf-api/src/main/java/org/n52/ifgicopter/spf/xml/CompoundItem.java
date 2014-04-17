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

package org.n52.ifgicopter.spf.xml;

import java.util.ArrayList;

/**
 * A CompoundItem is an item which holds one or
 * more other Items. other items can be {@link Item}
 * or {@link CompoundItem}, creating a recursive tree
 * structure.
 * For an Input plugin only the leafs of this structure
 * are used inside the framework as input properties.
 * 
 * @author Matthes Rieke <m.rieke@uni-muenster.de>
 *
 */
public class CompoundItem extends Item {

	private ArrayList<Item> compoundItems;

	/**
	 * @param prop the property name
	 */
	public CompoundItem(String prop) {
		super(prop);
		this.compoundItems = new ArrayList<Item>();
	}
	
	/**
	 * @param i the new compounded item
	 */
	public void addCompoundedItem(Item i) {
		this.compoundItems.add(i);
	}

	/**
	 * @return a list of the compounded items
	 */
	public ArrayList<Item> getCompoundItems() {
		return this.compoundItems;
	}

	
	
}
