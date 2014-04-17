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

package org.n52.ifgicopter.spf.util;

import java.util.ArrayList;
import java.util.Comparator;


/**
 * <p>This class implements a sorted list. It is constructed with a comparator
 * that can compare two objects and sort objects accordingly. When you add an
 * object to the list, it is inserted in the correct place. Object that are
 * equal according to the comparator, will be in the list in the order that
 * they were added to this list. Add only objects that the comparator can
 * compare.</p>
 * @param <E> The datatype
 */
public class ConcurrentSortedList<E> extends ArrayList<E> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private Comparator<E> comparator;

	/**
	 * <p>Constructs a new sorted list. The objects in the list will be sorted
	 * according to the specified comparator.</p>
	 *
	 * @param c a comparator
	 */
	public ConcurrentSortedList(Comparator<E> c) {
		this.comparator = c;
	}

	/**
	 * <p>This method has no effect. It is not allowed to specify an index to
	 * insert an element as this might violate the sorting order of the objects
	 * in the list.</p>
	 */
	@Override
    public void add(int index, E element) {
		return;
	}

	/**
	 * <p>Adds an object to the list. The object will be inserted in the correct
	 * place so that the objects in the list are sorted. When the list already
	 * contains objects that are equal according to the comparator, the new
	 * object will be inserted immediately after these other objects.</p>
	 *
	 * @param o the object to be added
	 */
	@Override
    public boolean add(E o) {
		synchronized (this) {
			int i = 0;
			boolean found = false;
			while (!found && (i < size())) {
				found = this.comparator.compare(o, get(i)) < 0;
				if (!found) i++;
			}
			super.add(i, o);
			return true;
		}
	}

	@Override
	public E remove(int index) {
		synchronized (this) {
			return super.remove(index);
		}
	}

	@Override
	public boolean remove(Object o) {
		synchronized (this) {
			return super.remove(o);
		}
	}

	@Override
	public void clear() {
		synchronized (this) {
			super.clear();
		}
	}
	
	


}