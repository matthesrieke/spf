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

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.XmlRpcRequest;
import org.apache.xmlrpc.server.RequestProcessorFactoryFactory;
import org.n52.ifgicopter.spf.xmlrpc.XMLRPCInputPlugin.RPCListener;


/**
 * This class is a Factory for incoming RPC requests.
 * It returns the registered Object instead of creating
 * a new one. This enables state-based handling.
 * 
 * @author Matthes Rieke <m.rieke@uni-muenster.de>
 *
 */
public class RPCListenerProcessorFactoryFactory implements RequestProcessorFactoryFactory {

	RPCListener listener;

	/**
	 * @param rpcListener new {@link RPCListener} object
	 */
	public RPCListenerProcessorFactoryFactory(RPCListener rpcListener) {
		this.listener = rpcListener;
	}

	@Override
	public RequestProcessorFactory getRequestProcessorFactory(@SuppressWarnings("rawtypes") Class pClass)	throws XmlRpcException {
		return this.new RPCListenerProcessorFactory();
	}


	/**
	 * On a request the {@link RPCListenerProcessorFactory#getRequestProcessor(XmlRpcRequest)}
	 * method is called and will return the registered object.
	 * 
	 * @author Matthes Rieke <m.rieke@uni-muenster.de>
	 *
	 */
	private class RPCListenerProcessorFactory implements RequestProcessorFactory {
		public RPCListenerProcessorFactory() {
		    //
		}

		@Override
		public Object getRequestProcessor(XmlRpcRequest xmlRpcRequest) throws XmlRpcException {
			return RPCListenerProcessorFactoryFactory.this.listener;
		}
	}

}
