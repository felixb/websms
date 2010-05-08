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
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.RedirectHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HttpContext;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import de.ub0r.android.websms.connector.common.Connector;
import de.ub0r.android.websms.connector.common.ConnectorCommand;
import de.ub0r.android.websms.connector.common.ConnectorSpec;
import de.ub0r.android.websms.connector.common.Log;
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
	private static final String TAG = "freenet";

	/** Freenet.de Gateway URL for login. */
	private static final String URL_LOGIN = "https://"
			+ "e-tools.mobil.freenet.de/login.php3";
	// "https://secure.freenet.de/etools.freenet.de/login.php3";
	// "http://e-tools.freenet.de//freenetLogin.php3";
	/** Freenet.de Gateway URL for send. */
	private static final String URL_SEND = "http://"
			+ "e-tools.freenet.de/sms.php3";

	/** Static cookies. */
	private static ArrayList<Cookie> staticCookies = new ArrayList<Cookie>();

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final ConnectorSpec initSpec(final Context context) {
		final String name = context.getString(R.string.connector_freenet_name);
		ConnectorSpec c = new ConnectorSpec(name);
		c.setAuthor(context.getString(R.string.connector_freenet_author));
		c.setBalance(null);
		c.setCapabilities(ConnectorSpec.CAPABILITIES_UPDATE
				| ConnectorSpec.CAPABILITIES_SEND
				| ConnectorSpec.CAPABILITIES_PREFS);
		c.addSubConnector(TAG, c.getName(), SubConnectorSpec.FEATURE_NONE);
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
	 *            {@link Context}
	 * @param client
	 *            {@link DefaultHttpClient}
	 * @return Session ID
	 * @throws WebSMSException
	 *             WebSMSException
	 * @throws IOException
	 *             IOException
	 */
	private String login(final Context context, final DefaultHttpClient client)
			throws WebSMSException, IOException {
		HttpResponse response = client.execute(new HttpGet(
				"http://email.mobil.freenet.de/"));

		int resp = response.getStatusLine().getStatusCode();
		if (resp != HttpURLConnection.HTTP_OK) {
			throw new WebSMSException(context, R.string.error_http, " " + resp);
		}

		final SharedPreferences p = PreferenceManager
				.getDefaultSharedPreferences(context);
		final ArrayList<BasicNameValuePair> d = // .
		new ArrayList<BasicNameValuePair>();
		d.add(new BasicNameValuePair("callback",
				"http://email.mobil.freenet.de/login/index.html"));
		d.add(new BasicNameValuePair("returnto", ""));
		d.add(new BasicNameValuePair("tplversion", ""));
		d.add(new BasicNameValuePair("login", "action"));
		d.add(new BasicNameValuePair("Login", "Login"));
		d.add(new BasicNameValuePair("world", "bml_DE"));
		d.add(new BasicNameValuePair("username", p.getString(
				Preferences.PREFS_USER, "")));
		d.add(new BasicNameValuePair("password", p.getString(
				Preferences.PREFS_PASSWORD, "")));
		Log.d(TAG, "---HTTP POST---");
		Log.d(TAG, URL_LOGIN + " data: " + d);
		Log.d(TAG, "---HTTP POST---");

		HttpPost request = new HttpPost(URL_LOGIN);
		request.setEntity(new UrlEncodedFormEntity(d));
		request.setHeader("Referer", "http://email.mobil.freenet.de/");
		response = client.execute(request);
		List<Cookie> cookies = client.getCookieStore().getCookies();
		int l = cookies.size();
		Cookie c;
		String ret = null;
		for (int i = 0; i < l; i++) {
			c = cookies.get(i);
			if (c.getName().equals("SIS")) {
				ret = c.getValue();
				break;
			}
		}
		Log.d(TAG, "session id: " + ret);
		if (ret == null) {
			return null;
		}
		Log.d(TAG, "---HTTP GET---");
		Log.d(TAG, "https://email.mobil.freenet.de/Email/View/SmsEdit");
		Log.d(TAG, "---HTTP GET---");
		response = client.execute(new HttpGet(
				"https://email.mobil.freenet.de/Email/View/SmsEdit"));
		if (resp != HttpURLConnection.HTTP_OK) {
			throw new WebSMSException(context, R.string.error_http, " " + resp);
		}

		String htmlText = Utils.stream2str(response.getEntity().getContent());
		Log.d(TAG, "---HTTP RESPONSE---");
		for (Header h : response.getAllHeaders()) {
			Log.d(TAG, "HEADER: " + h.getName() + ": " + h.getValue());
		}
		Log.d(TAG, htmlText);
		Log.d(TAG, "---HTTP RESPONSE---");
		return ret;
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
			final DefaultHttpClient client = new DefaultHttpClient();
			client.setRedirectHandler(new RedirectHandler() {
				@Override
				public boolean isRedirectRequested(final HttpResponse response,
						final HttpContext context) {
					return false;
				}

				@Override
				public URI getLocationURI(final HttpResponse response,
						final HttpContext context) {
					return null;
				}
			});

			final String sessionID = this.login(context, client);
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
		} catch (Exception e) {
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
