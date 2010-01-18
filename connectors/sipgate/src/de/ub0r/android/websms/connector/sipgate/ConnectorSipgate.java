/*
 * Copyright (C) 2010 Mirko Weber, Felix Bechstein
 * 
 * This file is part of WebSMS.
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
package de.ub0r.android.websms.connector.sipgate;

import java.io.Serializable;
import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;

import org.xmlrpc.android.XMLRPCClient;
import org.xmlrpc.android.XMLRPCException;
import org.xmlrpc.android.XMLRPCFault;

import android.content.Context;
import android.util.Log;
import de.ub0r.android.websms.connector.common.Connector;
import de.ub0r.android.websms.connector.common.WebSMSException;

/**
 * AsyncTask to manage XMLRPC-Calls to sipgate.de remote-API.
 * 
 * @author Mirko Weber
 */
public class ConnectorSipgate extends Connector {
	/** Tag for output. */
	private static final String TAG = "WebSMS.Sipg";

	/** Sipgate.de API URL. */
	private static final String SIPGATE_URL = "https://"
			+ "samurai.sipgate.net/RPC2";
	/** Sipfate.de TEAM-API URL. */
	private static final String SIPGATE_TEAM_URL = "https://"
			+ "api.sipgate.net/RPC2";

	/**
	 * Create a Sipgate Connector.
	 * 
	 * @param u
	 *            username
	 * @param p
	 *            password
	 */
	protected ConnectorSipgate(final String u, final String p) {
		super(null); // FIXME:
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final boolean sendMessage() throws WebSMSException {
		Log.d(TAG, "sendMessage()");
		Object back;
		try {
			XMLRPCClient client = this.init();
			Vector<String> remoteUris = new Vector<String>();
			for (int i = 0; i < this.to.length; i++) {
				if (this.to[i] != null && this.to[i].length() > 1) {
					remoteUris.add("sip:" + this.to[i].replaceAll("\\+", "")
							+ "@sipgate.net");
					Log.d(TAG, "Telefonnummer:" + remoteUris.get(i));
				}
			}
			Hashtable<String, Serializable> params = new Hashtable<String, Serializable>();
			// FIXME: if (WebSMS.prefsSender.length() > 6) {
			// String localUri = "sip:"
			// + WebSMS.prefsSender.replaceAll("\\+", "")
			// + "@sipgate.net";
			// params.put("LocalUri", localUri);
			// }
			params.put("RemoteUri", remoteUris);
			params.put("TOS", "text");
			params.put("Content", this.text);
			back = client.call("samurai.SessionInitiateMulti", params);
			Log.d(TAG, back.toString());
		} catch (XMLRPCFault e) {
			Log.e(TAG, null, e);
			if (e.getFaultCode() == HTTP_SERVICE_UNAUTHORIZED) {
				throw new WebSMSException(this.context, R.string.log_error_pw);
			}
			throw new WebSMSException(e.toString());
		} catch (XMLRPCException e) {
			Log.e(TAG, null, e);
			throw new WebSMSException(e.toString());
		}
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected final boolean updateMessages() throws WebSMSException {
		Log.d(TAG, "updateMessage()");
		Map<String, Object> back = null;
		try {
			XMLRPCClient client = this.init();
			back = (Map<String, Object>) client.call("samurai.BalanceGet");
			Log.d(TAG, back.toString());
			if (back.get("StatusCode").equals(new Integer(HTTP_SERVICE_OK))) {
				// FIXME WebSMS.SMS_BALANCE[SIPGATE] =
				// String.format("%.2f \u20AC",
				// ((Double) ((Map<String, Object>) back
				// .get("CurrentBalance")).get("TotalIncludingVat")));
			}
			this.pushMessage(WebSMS.MESSAGE_FREECOUNT, null);
		} catch (XMLRPCFault e) {
			Log.e(TAG, null, e);
			if (e.getFaultCode() == HTTP_SERVICE_UNAUTHORIZED) {
				throw new WebSMSException(this.context, R.string.log_error_pw);
			}
			throw new WebSMSException(e.toString());
		} catch (XMLRPCException e) {
			Log.e(TAG, null, e);
			throw new WebSMSException(e.toString());
		}

		return true;
	}

	/**
	 * Sets up and instance of XMLRPCClient.
	 * 
	 * @return the initialized XMLRPCClient
	 * @throws XMLRPCException
	 *             XMLRPCException
	 */
	private XMLRPCClient init() throws XMLRPCException {
		Log.d(TAG, "updateMessage()");
		Context c = this.context;
		final String version = c.getString(R.string.app_version);
		final String vendor = c.getString(R.string.author1);
		XMLRPCClient client = null; // FIXME
		// FIXME: if (WebSMS.prefsEnableSipgateTeam) {
		// client = new XMLRPCClient(SIPGATE_TEAM_URL);
		// } else {
		// client = new XMLRPCClient(SIPGATE_URL);
		// }

		// FIXME: client.setBasicAuthentication(this.user, this.password);
		Object back;
		try {
			Hashtable<String, String> ident = new Hashtable<String, String>();
			ident.put("ClientName", TAG);
			ident.put("ClientVersion", version);
			ident.put("ClientVendor", vendor);
			back = client.call("samurai.ClientIdentify", ident);
			Log.d(TAG, back.toString());
			return client;
		} catch (XMLRPCException e) {
			Log.e(TAG, "XMLRPCExceptin in init()", e);
			throw e;
		}
	}
}
