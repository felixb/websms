/*
 * Copyright (C) 2009 Mirko Weber, Felix Bechstein
 * 
 * This file is part of AndGMXsms.
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; If not, see <http://www.gnu.org/licenses/>.
 */
package de.ub0r.android.andGMXsms;

import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;

import org.xmlrpc.android.XMLRPCClient;
import org.xmlrpc.android.XMLRPCException;
import org.xmlrpc.android.XMLRPCFault;

import android.util.Log;

/**
 * AsyncTask to manage XMLRPC-Calls to sipgate.de remote-API
 * 
 * @author mirweb
 */
public class ConnectorSipgate extends Connector { 
	/** Tag for output. */
	private static final String TAG = "WebSMS.Sipgate";

	/**
	 * sipgate.de API URL
	 */
	private static final String SIPGATE_URL = "https://samurai.sipgate.net/RPC2";

	/**
	 * Send sms
	 * 
	 * @return ok?
	 * @see de.ub0r.android.andGMXsms.Connector#sendMessage()
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected boolean sendMessage() {
		Log.d(TAG, "sendMessage()");
		AndGMXsms.sendMessage(AndGMXsms.MESSAGE_DISPLAY_ADS, null);
		Object back;
		try {
			XMLRPCClient client = this.init();
			this.publishProgress((Boolean) null);
			Vector<String> remoteUris = new Vector<String>();
			for (int i = 0; i < this.to.length; i++) {
				if (this.to[i] != null && this.to[i].length() > 1) {
					remoteUris.add("sip:" + this.to[i].replaceAll("\\+", "")
							+ "@sipgate.net");
					Log.d(TAG, "Telefonnummer:" + remoteUris.get(i));
				}
			}
			Hashtable params = new Hashtable();
			params.put("RemoteUri", remoteUris);
			params.put("TOS", "text");
			params.put("Content", this.text);
			back = client.call("samurai.SessionInitiateMulti", params);
			Log.d(TAG, back.toString());
			AndGMXsms.sendMessage(AndGMXsms.MESSAGE_RESET, null);
			saveMessage(this.to, this.text);

		} catch (XMLRPCFault e) {
			Log.e(TAG, null, e);
			if(e.getFaultCode() == 401){
				AndGMXsms.sendMessage(AndGMXsms.MESSAGE_LOG,
						R.string.log_error_pw);
				return false;
			}
			AndGMXsms.sendMessage(AndGMXsms.MESSAGE_LOG, e.toString());
			return false;
		} catch (XMLRPCException e) {
			Log.e(TAG, null, e);
			AndGMXsms.sendMessage(AndGMXsms.MESSAGE_LOG, e.toString());
			return false;
		}
		return true;
	}

	/**
	 * get balance of account in euro
	 * 
	 * @return ok?
	 * @see de.ub0r.android.andGMXsms.Connector#updateMessages()
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected boolean updateMessages() {
		Log.d(TAG, "updateMessage()");
		this.publishProgress((Boolean) null);
		Map back = null;
		try {
			XMLRPCClient client = this.init();
			back = (Map) client.call("samurai.BalanceGet");
			Log.d(TAG, back.toString());
			if (back.get("StatusCode").equals(new Integer(200))) {
				AndGMXsms.BALANCE_SIPGATE = ((Double) ((Map) back
						.get("CurrentBalance")).get("TotalIncludingVat"));
			}
			AndGMXsms.sendMessage(AndGMXsms.MESSAGE_FREECOUNT, null);
		} catch (XMLRPCFault e) {
			Log.e(TAG, null, e);
			if(e.getFaultCode() == 401){
				AndGMXsms.sendMessage(AndGMXsms.MESSAGE_LOG,
						R.string.log_error_pw);
				return false;
			}
			AndGMXsms.sendMessage(AndGMXsms.MESSAGE_LOG, e.toString());
			return false;
		} catch (XMLRPCException e) {
			Log.e(TAG, null, e);
			AndGMXsms.sendMessage(AndGMXsms.MESSAGE_LOG, e.toString());
			return false;
		}

		return true;
	}

	/**
	 * sets up and instance of XMLRPCClient
	 * 
	 * @return the initialized XMLRPCClient
	 * @throws XMLRPCException
	 */
	private XMLRPCClient init() throws XMLRPCException {
		Log.d(TAG, "updateMessage()");
		String VERSION = AndGMXsms.me.getResources().getString(
				R.string.app_version);
		String VENDOR = AndGMXsms.me.getResources().getString(R.string.author1);
		this.publishProgress((Boolean) null);

		XMLRPCClient client = new XMLRPCClient(SIPGATE_URL);
		client.setBasicAuthentication(AndGMXsms.prefsUserSipgate,
				AndGMXsms.prefsPasswordSipgate);
		Object back;
		try {
			this.publishProgress((Boolean) null);
			Hashtable<String, String> ident = new Hashtable<String, String>();
			ident.put("ClientName", TAG);
			ident.put("ClientVersion", VERSION);
			ident.put("ClientVendor", VENDOR);
			back = client.call("samurai.ClientIdentify", ident);
			Log.d(TAG, back.toString());
			return client;
		} catch (XMLRPCException e) {
			Log.e(TAG, "XMLRPCExceptin in init()", e);
			throw e;
		}
	}

}
