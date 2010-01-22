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
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import de.ub0r.android.websms.connector.common.Connector;
import de.ub0r.android.websms.connector.common.ConnectorCommand;
import de.ub0r.android.websms.connector.common.ConnectorSpec;
import de.ub0r.android.websms.connector.common.Utils;
import de.ub0r.android.websms.connector.common.WebSMSException;
import de.ub0r.android.websms.connector.common.ConnectorSpec.SubConnectorSpec;

/**
 * AsyncTask to manage XMLRPC-Calls to sipgate.de remote-API.
 * 
 * @author Mirko Weber
 */
public class ConnectorSipgate extends Connector {
	/** Tag for output. */
	private static final String TAG = "WebSMS.Sipg";

	/** Preferences intent action. */
	private static final String PREFS_INTENT_ACTION = "de.ub0r.android."
			+ "websms.connectors.sipgate.PREFS";

	/** Sipgate.de API URL. */
	private static final String SIPGATE_URL = "https://"
			+ "samurai.sipgate.net/RPC2";
	/** Sipfate.de TEAM-API URL. */
	private static final String SIPGATE_TEAM_URL = "https://"
			+ "api.sipgate.net/RPC2";

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final ConnectorSpec initSpec(final Context context) {
		final String name = context.getString(R.string.connector_sipgate_name);
		ConnectorSpec c = new ConnectorSpec(TAG, name);
		c.setAuthor(// .
				context.getString(R.string.connector_sipgate_author));
		c.setBalance(null);
		c.setPrefsIntent(PREFS_INTENT_ACTION);
		c.setPrefsTitle(context
				.getString(R.string.connector_sipgate_preferences));
		c.setCapabilities(ConnectorSpec.CAPABILITIES_UPDATE
				| ConnectorSpec.CAPABILITIES_SEND);
		c.addSubConnector(TAG, c.getName(),
				SubConnectorSpec.FEATURE_MULTIRECIPIENTS);
		return c;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final ConnectorSpec updateSpec(final Context context,
			final ConnectorSpec connectorSpec) {
		final SharedPreferences p = PreferenceManager
				.getDefaultSharedPreferences(context);
		if (p.getBoolean(Preferences.PREFS_ENABLE_SIPGATE, false)) {
			if (p.getString(Preferences.PREFS_USER_SIPGATE, "").length() > 0
					&& p.getString(Preferences.PREFS_PASSWORD_SIPGATE, "") // .
							.length() > 0) {
				connectorSpec.setReady();
			} else {
				connectorSpec.setStatus(ConnectorSpec.STATUS_ENABLED);
			}
		} else {
			connectorSpec.setStatus(ConnectorSpec.STATUS_INACTIVE);
		}
		return connectorSpec;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final void doSend(final Context context, final Intent intent)
			throws WebSMSException {
		Log.d(TAG, "doSend()");
		Object back;
		try {
			XMLRPCClient client = this.init(context);
			Vector<String> remoteUris = new Vector<String>();
			ConnectorCommand command = new ConnectorCommand(intent);

			for (String t : command.getRecipients()) {
				if (t != null && t.length() > 1) {
					// FIXME: force international number
					final String u = "sip:"
							+ Utils.getRecipientsNumber(t)
									.replaceAll("\\+", "") + "@sipgate.net";
					remoteUris.add(u);
					Log.d(TAG, "Mobile number: " + u);
				}
			}
			Hashtable<String, Serializable> params = // .
			new Hashtable<String, Serializable>();
			if (command.getDefSender().length() > 6) {
				String localUri = "sip:"
						+ command.getDefSender().replaceAll("\\+", "")
						+ "@sipgate.net";
				params.put("LocalUri", localUri);
			}
			params.put("RemoteUri", remoteUris);
			params.put("TOS", "text");
			params.put("Content", command.getText());
			back = client.call("samurai.SessionInitiateMulti", params);
			Log.d(TAG, back.toString());
		} catch (XMLRPCFault e) {
			Log.e(TAG, null, e);
			if (e.getFaultCode() == Utils.HTTP_SERVICE_UNAUTHORIZED) {
				throw new WebSMSException(context, R.string.error_pw);
			}
			throw new WebSMSException(e);
		} catch (XMLRPCException e) {
			Log.e(TAG, null, e);
			throw new WebSMSException(e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected final void doUpdate(final Context context, final Intent intent)
			throws WebSMSException {
		Log.d(TAG, "doUpdate()");
		Map<String, Object> back = null;
		try {
			XMLRPCClient client = this.init(context);
			back = (Map<String, Object>) client.call("samurai.BalanceGet");
			Log.d(TAG, back.toString());
			if (back.get("StatusCode").equals(
					new Integer(Utils.HTTP_SERVICE_OK))) {
				final String b = String.format("%.2f \u20AC",
						((Double) ((Map<String, Object>) back
								.get("CurrentBalance"))
								.get("TotalIncludingVat")));
				this.getSpec(context).setBalance(b);
			}
		} catch (XMLRPCFault e) {
			Log.e(TAG, null, e);
			if (e.getFaultCode() == Utils.HTTP_SERVICE_UNAUTHORIZED) {
				throw new WebSMSException(context, R.string.error_pw);
			}
			throw new WebSMSException(e);
		} catch (XMLRPCException e) {
			Log.e(TAG, null, e);
			throw new WebSMSException(e);
		}
	}

	/**
	 * Sets up and instance of {@link XMLRPCClient}.
	 * 
	 * @param context
	 *            {@link Context}
	 * @return the initialized {@link XMLRPCClient}
	 * @throws XMLRPCException
	 *             XMLRPCException
	 */
	private XMLRPCClient init(final Context context) throws XMLRPCException {
		Log.d(TAG, "init()");
		final String version = context.getString(R.string.app_version);
		final String vendor = context
				.getString(R.string.connector_sipgate_author);
		XMLRPCClient client = null;
		final SharedPreferences p = PreferenceManager
				.getDefaultSharedPreferences(context);
		if (p.getBoolean(Preferences.PREFS_ENABLE_SIPGATE_TEAM, false)) {
			client = new XMLRPCClient(SIPGATE_TEAM_URL);
		} else {
			client = new XMLRPCClient(SIPGATE_URL);
		}

		client.setBasicAuthentication(p.getString(
				Preferences.PREFS_USER_SIPGATE, ""), p.getString(
				Preferences.PREFS_PASSWORD_SIPGATE, ""));
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
