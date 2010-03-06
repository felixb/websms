/*
 * Copyright (C) 2010 Felix Bechstein
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
package de.ub0r.android.websms.connector.freenet;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;

import org.apache.http.HttpResponse;
import org.apache.http.message.BasicNameValuePair;

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
 * AsyncTask to manage IO to Freenet.de API.
 * 
 * @author flx
 */
public class ConnectorFreenet extends Connector {
	/** Tag for output. */
	private static final String TAG = "WebSMS.freenet";

	/** Freenet.de Gateway URL for login. */
	private static final String URL_LOGIN = "http://"
			+ "e-tools.freenet.de//freenetLogin.php3";
	/** Freenet.de Gateway URL for send. */
	private static final String URL_SEND = "http://"
			+ "e-tools.freenet.de/sms.php3";

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final ConnectorSpec initSpec(final Context context) {
		final String name = context.getString(R.string.connector_freenet_name);
		ConnectorSpec c = new ConnectorSpec(TAG, name);
		c.setAuthor(context.getString(R.string.connector_freenet_author));
		c.setBalance(null);
		c.setPrefsTitle(context
				.getString(R.string.connector_freenet_preferences));
		c.setCapabilities(ConnectorSpec.CAPABILITIES_UPDATE
				| ConnectorSpec.CAPABILITIES_SEND
				| ConnectorSpec.CAPABILITIES_PREFS);
		c
				.addSubConnector(c.getID(), c.getName(),
						SubConnectorSpec.FEATURE_NONE);
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
		if (p.getBoolean(Preferences.PREFS_ENABLED, false)) {
			if (p.getString(Preferences.PREFS_USER, "").length() > 0
					&& p.getString(Preferences.PREFS_PASSWORD, "")// .
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
	 * Login to freenet.de.
	 * 
	 * @param context
	 *            Context
	 * @param command
	 *            ConnectorCommand
	 * @return Session ID
	 * @throws WebSMSException
	 *             WebSMSException
	 * @throws IOException
	 *             IOException
	 */
	private String login(final Context context, final ConnectorCommand command)
			throws WebSMSException, IOException {
		final SharedPreferences p = PreferenceManager
				.getDefaultSharedPreferences(context);
		final StringBuilder url = new StringBuilder(URL_LOGIN);
		final ArrayList<BasicNameValuePair> d = // .
		new ArrayList<BasicNameValuePair>();
		d.add(new BasicNameValuePair("callback",
				"http://etools.freenet.de/etools.php3&password="
						+ p.getString(Preferences.PREFS_PASSWORD, "")
						+ "&username="
						+ p.getString(Preferences.PREFS_USER, "")));
		HttpResponse response = Utils.getHttpClient(url.toString(), null, d,
				null, null);
		int resp = response.getStatusLine().getStatusCode();
		if (resp != HttpURLConnection.HTTP_OK) {
			throw new WebSMSException(context, R.string.error_http, " " + resp);
		}
		String htmlText = Utils.stream2str(response.getEntity().getContent())
				.trim();
		// TODO: get session id
		return null;
	}

	/**
	 * Send data.
	 * 
	 * @param context
	 *            Context
	 * @param command
	 *            ConnectorCommand
	 * @throws WebSMSException
	 *             WebSMSException
	 */
	private void sendData(final Context context, final ConnectorCommand command)
			throws WebSMSException {
		// do IO
		try { // get Connection
			final String sessionID = this.login(context, command);
			final String text = command.getText();
			if (text == null || text.length() == 0) {
				return;
			}

			final ConnectorSpec cs = this.getSpec(context);
			final SharedPreferences p = PreferenceManager
					.getDefaultSharedPreferences(context);
			final ArrayList<BasicNameValuePair> d = // .
			new ArrayList<BasicNameValuePair>();
			d.add(new BasicNameValuePair("SIS_param", sessionID));
			d.add(new BasicNameValuePair("nachname", ""));
			d.add(new BasicNameValuePair("vorname", ""));
			d.add(new BasicNameValuePair("vorwahl", "???")); // FIXME
			d.add(new BasicNameValuePair("vorwahl", Utils
					.getRecipientsNumber(command.getRecipients()[0]))); // FIXME
			d.add(new BasicNameValuePair("smstext", text));
			d.add(new BasicNameValuePair("aktion", "Senden"));

			HttpResponse response = Utils.getHttpClient(URL_SEND
					+ "?SIS_param=" + sessionID
					+ "&aktion=sms_write&zielrufnummer=&anrede=Herr&nachname=",
					null, d, null, null);
			int resp = response.getStatusLine().getStatusCode();
			if (resp != HttpURLConnection.HTTP_OK) {
				throw new WebSMSException(context, R.string.error_http, " "
						+ resp);
			}
			String htmlText = Utils.stream2str(
					response.getEntity().getContent()).trim();
			// TODO: parse html
		} catch (IOException e) {
			Log.e(TAG, null, e);
			throw new WebSMSException(e.getMessage());
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final void doUpdate(final Context context, final Intent intent)
			throws WebSMSException {
		this.sendData(context, new ConnectorCommand(intent));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final void doSend(final Context context, final Intent intent)
			throws WebSMSException {
		this.sendData(context, new ConnectorCommand(intent));
	}
}
