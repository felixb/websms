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
package de.ub0r.android.websms.connector.fishtext;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.util.ArrayList;

import org.apache.http.HttpResponse;
import org.apache.http.cookie.Cookie;
import org.apache.http.cookie.MalformedCookieException;
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
 * AsyncTask to manage IO to fishtext API.
 * 
 * @author flx
 */
public class ConnectorFishtext extends Connector {
	/** Tag for output. */
	private static final String TAG = "WebSMS.fishtext";
	/** {@link SubConnectorSpec} ID: basic. */

	/** Preferences intent action. */
	private static final String PREFS_INTENT_ACTION = "de.ub0r.android."
			+ "websms.connectors.fishtext.PREFS";

	/** Fishtext URL: login. */
	private static final String URL_LOGIN = // .
	"https://www.fishtext.com/cgi-bin/account";
	/** Fishtext URL: send. */
	private static final String URL_SEND = // .
	"https://www.fishtext.com/SendSMS/SendSMS";

	/** Check for balance. */
	private static final String CHECK_BALANCE = "Current balance:";
	/** Check for sent. */
	private static final String CHECK_SENT = "Message sent";

	/** Stip bytes from stream: login. */
	private static final int STRIP_LOGIN_START = 4500;
	/** Stip bytes from stream: login. */
	private static final int STRIP_LOGIN_END = 6500;

	/** HTTP Useragent. */
	private static final String TARGET_AGENT = "Mozilla/5.0 (Windows; U;"
			+ " Windows NT 5.1; de; rv:1.9.0.9) Gecko/2009040821"
			+ " Firefox/3.0.9 (.NET CLR 3.5.30729)";

	/** Static cookies. */
	private static ArrayList<Cookie> staticCookies = new ArrayList<Cookie>();

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final ConnectorSpec initSpec(final Context context) {
		final String name = context.getString(R.string.connector_fishtext_name);
		ConnectorSpec c = new ConnectorSpec(TAG, name);
		c.setAuthor(// .
				context.getString(R.string.connector_fishtext_author));
		c.setBalance(null);
		c.setPrefsIntent(PREFS_INTENT_ACTION);
		c.setPrefsTitle(context.getString(R.string.settings_fishtext));
		c.setCapabilities(ConnectorSpec.CAPABILITIES_UPDATE
				| ConnectorSpec.CAPABILITIES_SEND);
		c.addSubConnector(// .
				c.getID(), c.getName(), SubConnectorSpec.FEATURE_NONE);
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
			if (p.getString(Preferences.PREFS_PASSWORD, "").length() > 0) {
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
	 * Login to fishtext.com.
	 * 
	 * @param context
	 *            {@link Context}
	 * @param command
	 *            {@link ConnectorCommand}
	 * @param connector
	 *            {@link ConnectorSpec}
	 * @param cookies
	 *            {@link Cookie}s
	 * @return successful login?
	 * @throws IOException
	 *             IOException
	 * @throws WebSMSException
	 *             WebSMSException
	 * @throws URISyntaxException
	 *             URISyntaxException
	 * @throws MalformedCookieException
	 *             MalformedCookieException
	 */
	private boolean doLogin(final Context context,
			final ConnectorCommand command, final ConnectorSpec connector,
			final ArrayList<Cookie> cookies) throws IOException,
			WebSMSException, MalformedCookieException, URISyntaxException {
		// TODO: use cached cookies
		final SharedPreferences p = PreferenceManager
				.getDefaultSharedPreferences(context);

		ArrayList<BasicNameValuePair> postData = // .
		new ArrayList<BasicNameValuePair>(4);
		postData.add(new BasicNameValuePair("action", "login"));
		postData.add(new BasicNameValuePair("mobile", Utils.getSender(context,
				command.getDefSender())));
		postData.add(new BasicNameValuePair("password", p.getString(
				Preferences.PREFS_PASSWORD, "")));
		postData.add(new BasicNameValuePair("rememberSession", "yes"));

		HttpResponse response = Utils.getHttpClient(URL_LOGIN, cookies,
				postData, TARGET_AGENT, null);
		postData = null;
		int resp = response.getStatusLine().getStatusCode();
		if (resp != HttpURLConnection.HTTP_OK) {
			throw new WebSMSException(context, R.string.error_http, "" + resp);
		}
		Utils.updateCookies(cookies, response.getAllHeaders(), URL_LOGIN);
		final String htmlText = Utils.stream2str(response.getEntity()
				.getContent(), STRIP_LOGIN_START, STRIP_LOGIN_END);
		Log.d(TAG, "----HTTP RESPONSE---");
		Log.d(TAG, htmlText);
		Log.d(TAG, "----HTTP RESPONSE---");

		final int i = htmlText.indexOf(CHECK_BALANCE);
		if (i < 0) {
			throw new WebSMSException(context, R.string.error_pw);
		}
		final int j = htmlText.indexOf("<p>", i);
		if (j > 0) {
			final int h = htmlText.indexOf("</p>", j);
			if (h > 0) {
				String b = htmlText.substring(j + 3, h);
				if (b.startsWith("&euro;")) {
					b = b.substring(6) + "\u20AC";
				}
				Log.d(TAG, "balance: " + b);
				this.getSpec(context).setBalance(b);
			}
		}

		return true;
	}

	/**
	 * Send data.
	 * 
	 * @param context
	 *            {@link Context}
	 * @param command
	 *            {@link ConnectorCommand}
	 * @param connector
	 *            {@link ConnectorSpec}
	 * @param cookies
	 *            {@link Cookie}s
	 * @throws WebSMSException
	 *             WebSMSException
	 * @throws IOException
	 *             IOException
	 * @throws URISyntaxException
	 *             URISyntaxException
	 * @throws MalformedCookieException
	 *             MalformedCookieException
	 */
	private static void sendText(final Context context,
			final ConnectorCommand command, final ConnectorSpec connector,
			final ArrayList<Cookie> cookies) throws WebSMSException,
			IOException, MalformedCookieException, URISyntaxException {
		ArrayList<BasicNameValuePair> postData = // .
		new ArrayList<BasicNameValuePair>(6);
		postData.add(new BasicNameValuePair("action", "Send"));
		postData.add(new BasicNameValuePair("SA", "0"));
		postData.add(new BasicNameValuePair("DR", "1"));
		postData.add(new BasicNameValuePair("ST", "1"));
		postData.add(new BasicNameValuePair("RN", Utils.national2international(
				command.getDefPrefix(),
				Utils.getRecipientsNumber(command.getRecipients()[0]))
				.substring(1)));
		postData.add(new BasicNameValuePair(
				"M38a734bb6bbcf1c2172fe91b052fc663", command.getText()));

		HttpResponse response = Utils.getHttpClient(URL_LOGIN, cookies,
				postData, TARGET_AGENT, URL_LOGIN);
		postData = null;
		int resp = response.getStatusLine().getStatusCode();
		if (resp != HttpURLConnection.HTTP_OK) {
			throw new WebSMSException(context, R.string.error_http, "" + resp);
		}
		Utils.updateCookies(cookies, response.getAllHeaders(), URL_SEND);
		final String htmlText = Utils.stream2str(response.getEntity()
				.getContent());
		Log.d(TAG, "----HTTP RESPONSE---");
		Log.d(TAG, htmlText);
		Log.d(TAG, "----HTTP RESPONSE---");

		final int i = htmlText.indexOf(CHECK_SENT);
		if (i < 0) {
			throw new WebSMSException(context, R.string.error_service);
		}

	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected final void doUpdate(final Context context, final Intent intent)
			throws WebSMSException {
		ArrayList<Cookie> cookies;
		if (staticCookies == null) {
			cookies = new ArrayList<Cookie>();
		} else {
			cookies = (ArrayList<Cookie>) staticCookies.clone();
		}
		try {
			this.doLogin(context, new ConnectorCommand(intent),
					new ConnectorSpec(intent), cookies);
			staticCookies = cookies;
		} catch (final Exception e) {
			throw new WebSMSException(e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected final void doSend(final Context context, final Intent intent)
			throws WebSMSException {
		ArrayList<Cookie> cookies;
		if (staticCookies == null) {
			cookies = new ArrayList<Cookie>();
		} else {
			cookies = (ArrayList<Cookie>) staticCookies.clone();
		}
		try {
			final ConnectorCommand command = new ConnectorCommand(intent);
			final ConnectorSpec connector = new ConnectorSpec(intent);
			if (this.doLogin(context, command, connector, cookies)) {
				sendText(context, command, connector, cookies);
			}
			staticCookies = cookies;
		} catch (final Exception e) {
			throw new WebSMSException(e);
		}
	}
}
