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
package de.ub0r.android.websms.connector.sloono;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URLEncoder;

import org.apache.http.HttpResponse;

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
 * AsyncTask to manage IO to sloono.de API.
 * 
 * @author flx
 */
public class ConnectorSloono extends Connector {
	/** Tag for output. */
	private static final String TAG = "WebSMS.sloono";
	/** {@link SubConnectorSpec} ID: basic. */
	private static final String ID_BASIC = "1";
	/** {@link SubConnectorSpec} ID: discount. */
	private static final String ID_DISCOUNT = "0";
	/** {@link SubConnectorSpec} ID: pro. */
	private static final String ID_PRO = "2";
	/** {@link SubConnectorSpec} ID: flash. */
	private static final String ID_FLASH = "3";

	/** Sloono Gateway URL. */
	private static final String URL_SEND = // .
	"http://www.sloono.de/API/httpsms.php";
	/** Sloono Gateway URL. */
	private static final String URL_BALANCE = // .
	"http://www.sloono.de/API/httpkonto.php";

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final ConnectorSpec initSpec(final Context context) {
		final String name = context.getString(R.string.connector_sloono_name);
		ConnectorSpec c = new ConnectorSpec(TAG, name);
		c.setAuthor(// .
				context.getString(R.string.connector_sloono_author));
		c.setBalance(null);
		c.setPrefsTitle(context.getString(R.string.settings_sloono));
		c.setCapabilities(ConnectorSpec.CAPABILITIES_UPDATE
				| ConnectorSpec.CAPABILITIES_SEND
				| ConnectorSpec.CAPABILITIES_PREFS);
		final short f = (short) (SubConnectorSpec.FEATURE_MULTIRECIPIENTS
				| SubConnectorSpec.FEATURE_FLASHSMS | // .
		SubConnectorSpec.FEATURE_SENDLATER);
		c.addSubConnector(ID_BASIC, context.getString(R.string.basic), f);
		c.addSubConnector(ID_DISCOUNT, context.getString(R.string.discount), f);
		c.addSubConnector(ID_PRO, context.getString(R.string.pro), f);
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
	 * Check return code from sloono.de.
	 * 
	 * @param context
	 *            {@link Context}
	 * @param ret
	 *            return code
	 * @return true if no error code
	 * @throws WebSMSException
	 *             WebSMSException
	 */
	private boolean checkReturnCode(final Context context, final int ret)
			throws WebSMSException {
		Log.d(TAG, "ret=" + ret);
		if (ret < 200) {
			return true;
		} else if (ret < 300) {
			throw new WebSMSException(context, R.string.error_input);
		} else {
			if (ret == 401) {
				throw new WebSMSException(context, R.string.error_pw);
			}
			throw new WebSMSException(context, R.string.error_server, // .
					" " + ret);
		}
	}

	/**
	 * Send data.
	 * 
	 * @param context
	 *            {@link Context}
	 * @param command
	 *            {@link ConnectorCommand}
	 * @throws WebSMSException
	 *             WebSMSException
	 */
	private void sendData(final Context context, final ConnectorCommand command)
			throws WebSMSException {
		// do IO
		try { // get Connection
			final String text = command.getText();
			final boolean checkOnly = (text == null || text.length() == 0);
			final StringBuilder url = new StringBuilder();
			final ConnectorSpec cs = this.getSpec(context);
			final SharedPreferences p = PreferenceManager
					.getDefaultSharedPreferences(context);
			if (checkOnly) {
				url.append(URL_BALANCE);
			} else {
				url.append(URL_SEND);
			}
			url.append("?user=");
			url.append(p.getString(Preferences.PREFS_USER, ""));
			url.append("&password=");
			url.append(Utils.md5(p.getString(Preferences.PREFS_PASSWORD, "")));

			if (!checkOnly) {
				url.append("&typ=");
				if (command.getFlashSMS()) {
					url.append(ID_FLASH);
				} else {
					url.append(command.getSelectedSubConnector());
				}
				final long sendLater = command.getSendLater();
				if (sendLater > 0) {
					url.append("&timestamp=");
					url.append(sendLater / 1000);
				}
				url.append("&text=");
				url.append(URLEncoder.encode(text));
				url.append("&to=");
				url.append(Utils.joinRecipientsNumbers(command.getRecipients(),
						",", true));
			}
			// send data
			HttpResponse response = Utils.getHttpClient(url.toString(), null,
					null, null, null);
			int resp = response.getStatusLine().getStatusCode();
			if (resp != HttpURLConnection.HTTP_OK) {
				this.checkReturnCode(context, resp);
				throw new WebSMSException(context, R.string.error_http, " "
						+ resp);
			}
			String htmlText = Utils.stream2str(
					response.getEntity().getContent()).trim();
			String[] lines = htmlText.split("\n");
			Log.d(TAG, "--HTTP RESPONSE--");
			Log.d(TAG, htmlText);
			Log.d(TAG, "--HTTP RESPONSE--");
			htmlText = null;
			for (String s : lines) {
				if (s.startsWith("Kontostand: ")) {
					cs.setBalance(s.split(" ")[1].trim() + "\u20AC");
				}
			}
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
