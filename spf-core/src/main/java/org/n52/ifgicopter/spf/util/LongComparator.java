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

import java.io.Serializable;
import java.util.Comparator;

/**
 * Simple {@link Comparator} for Long objects.
 * 
 * @author Matthes Rieke <m.rieke@uni-muenster.de>
 * 
 */
public class LongComparator implements Comparator<Long>, Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 4387042111749351252L;

    @Override
    public int compare(Long o1, Long o2) {
        if (o1.longValue() == o2.longValue()) {
            return 0;
        }

        return (o1.longValue() > o2.longValue() ? 1 : -1);
    }

}
