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

package org.n52.ifgicopter.spf.xmlrpc;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.apache.xmlrpc.client.XmlRpcCommonsTransportFactory;
import org.joda.time.DateTime;

/**
 * @author Matthes Rieke <m.rieke@uni-muenster.de>
 *
 */
public class Client {
    /**
     * @param args not used
     * @throws Exception if fails
     */
    public static void main(String[] args) throws Exception {
    	System.out.println(new DateTime(Long.parseLong("1286628019171")));
        // create configuration
        XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
        config.setServerURL(new URL("http://127.0.0.1:8081/xmlrpc"));
        config.setEnabledForExtensions(true);  
        config.setConnectionTimeout(60 * 1000);
        config.setReplyTimeout(60 * 1000);

        XmlRpcClient client = new XmlRpcClient();
      
        // use Commons HttpClient as transport
        client.setTransportFactory(
            new XmlRpcCommonsTransportFactory(client));
        // set configuration
        client.setConfig(config);

        // make the a regular call
        Object[] params = new Object[]
            { Integer.valueOf(1), "alles kaputt..." };
        System.out.println(client.execute("RPCListener.setStatus", params));
        
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("time", Long.valueOf(System.currentTimeMillis()));
        map.put("humidity", Double.valueOf(70.2));
        params = new Object[] {map};
        System.out.println(client.execute("RPCListener.populateData", params));
    }
}
