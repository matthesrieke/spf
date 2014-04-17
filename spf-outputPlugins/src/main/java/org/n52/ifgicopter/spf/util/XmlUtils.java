/**
 * ﻿Copyright (C) 2012
 * by 52 North Initiative for Geospatial Open Source Software GmbH
 *
 * Contact: Andreas Wytzisk
 * 52 North Initiative for Geospatial Open Source Software GmbH
 * Martin-Luther-King-Weg 24
 * 48155 Muenster, Germany
 * info@52north.org
 *
 * This program is free software; you can redistribute and/or modify it under
 * the terms of the GNU General Public License serviceVersion 2 as published by the
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
import java.util.Iterator;

import org.apache.xmlbeans.XmlError;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;

public class XmlUtils {

    private static final String[] ILLEGAL_ID_CHARACTERS = new String[] {"\\", "/", ":", "*", "?", "\"", "<", ">", "|"};

    /**
     * http://xmlbeans.apache.org/docs/2.0.0/guide/conValidationWithXmlBeans.html
     * 
     * @param xml
     * @return
     */
    public static String validateAndIterateErrors(XmlObject xml) {
        ArrayList<XmlError> validationErrors = new ArrayList<XmlError>();
        XmlOptions validationOptions = new XmlOptions();
        validationOptions.setErrorListener(validationErrors);

        boolean isValid = xml.validate(validationOptions);

        StringBuilder sb = new StringBuilder();
        if ( !isValid) {
            sb.append("XmlObject of class <");
            sb.append(xml.getClass().getSimpleName());
            sb.append(" /> is NOT valid! The validation errors are:");

            Iterator<XmlError> iter = validationErrors.iterator();
            while (iter.hasNext()) {
                sb.append("\n\t");
                sb.append(iter.next().toString());
            }
        }
        else {
            sb.append("XmlObject ");
            sb.append(xml.getClass().getSimpleName());
            sb.append(" (");
            sb.append(xml.xmlText().substring(0, 40));
            sb.append("...) is valid!");
        }

        return sb.toString();
    }

    public static String cleanIdentifier(String id) {
        String cleanId = id;
        for (String s : ILLEGAL_ID_CHARACTERS) {
            if (cleanId.contains(s))
                cleanId = cleanId.replace(s, "");
        }
        return cleanId;
    }

}
