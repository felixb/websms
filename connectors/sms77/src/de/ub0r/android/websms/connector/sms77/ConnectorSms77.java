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
package de.ub0r.android.websms.connector.sms77;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;

import org.apache.http.HttpResponse;
import org.apache.http.message.BasicNameValuePair;

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
 * AsyncTask to manage IO to Sms77.de API.
 * 
 * @author flx
 */
public class ConnectorSms77 extends Connector {
	/** Tag for output. */
	private static final String TAG = "sms77";

	/** Gateway URL. */
	private static final String URL = "http://gateway.sms77.de/";
	/** Gateway URL for sending. */
	private static final String URL_SEND = URL;
	/** Gateway URL for balance update. */
	private static final String URL_BALACNCE = URL + "balance.php";
	/** Username. */
	private static final String PARAM_USERNAME = "u";
	/** Password. */
	private static final String PARAM_PASSWORD = "p";
	/** {@link SubConnectorSpec}. */
	private static final String PARAM_SUBCONNECTOR = "type";
	/** Reciepient. */
	private static final String PARAM_TO = "to";
	/** Text. */
	private static final String PARAM_TEXT = "text";
	/** Sender. */
	private static final String PARAM_SENDER = "from";
	/** Send later. */
	private static final String PARAM_SENDLATER = "delay";

	/** {@link SubConnectorSpec} ID: without sender. */
	private static final String ID_WO_SENDER = "basicplus";
	/** {@link SubConnectorSpec} ID: standard. */
	private static final String ID_STANDARD = "standard";
	/** {@link SubConnectorSpec} ID: quality. */
	private static final String ID_QUALITY = "quality";

	/** Max. length of custom sender. */
	private static final int MAX_CUSTOM_SENDER_LENGTH = 16;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final ConnectorSpec initSpec(final Context context) {
		final String name = context.getString(R.string.connector_sms77_name);
		ConnectorSpec c = new ConnectorSpec(name);
		c.setAuthor(context.getString(R.string.connector_sms77_author));
		c.setBalance(null);
		c.setLimitLength(MAX_CUSTOM_SENDER_LENGTH);
		c.setCapabilities(ConnectorSpec.CAPABILITIES_UPDATE
				| ConnectorSpec.CAPABILITIES_SEND
				| ConnectorSpec.CAPABILITIES_PREFS);
		c.addSubConnector(ID_WO_SENDER, context.getString(R.string.wo_sender),
				SubConnectorSpec.FEATURE_SENDLATER);
		c.addSubConnector(ID_STANDARD, context.getString(R.string.standard),
				SubConnectorSpec.FEATURE_CUSTOMSENDER
						| SubConnectorSpec.FEATURE_SENDLATER
						| SubConnectorSpec.FEATURE_FLASHSMS);
		c.addSubConnector(ID_QUALITY, context.getString(R.string.quality),
				SubConnectorSpec.FEATURE_CUSTOMSENDER
						| SubConnectorSpec.FEATURE_SENDLATER
						| SubConnectorSpec.FEATURE_FLASHSMS);
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
	 * Check return code from sms77.de.
	 * 
	 * @param context
	 *            Context
	 * @param ret
	 *            return code
	 * @return true if no error code
	 * @throws WebSMSException
	 *             WebSMSException
	 */
	private static boolean checkReturnCode(final Context context, final int ret)
			throws WebSMSException {
		switch (ret) {
		case 100:
			return true;
		case 101:
			throw new WebSMSException(context, R.string.error_sms77_101);
		case 202:
			throw new WebSMSException(context, R.string.error_sms77_202);
		case 300:
		case 900:
			throw new WebSMSException(context, R.string.error_pw);
		case 306:
			throw new WebSMSException(context, R.string.error_sms77_306);
		case 401:
			throw new WebSMSException(context, R.string.error_sms77_401);
		case 402:
			throw new WebSMSException(context, R.string.error_sms77_402);
		case 500:
			throw new WebSMSException(context, R.string.error_sms77_500);
		case 600:
			throw new WebSMSException(context, R.string.error_sms77_600);
		default:
			throw new WebSMSException(context, R.string.error, " code: " + ret);
		}
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
			final ConnectorSpec cs = this.getSpec(context);
			final SharedPreferences p = PreferenceManager
					.getDefaultSharedPreferences(context);
			String url;
			final ArrayList<BasicNameValuePair> d = // .
			new ArrayList<BasicNameValuePair>();
			final String text = command.getText();
			if (text != null && text.length() > 0) {
				url = URL_SEND;
				final String subCon = command.getSelectedSubConnector();
				d.add(new BasicNameValuePair(PARAM_TEXT, text));

				d.add(new BasicNameValuePair(PARAM_TO, Utils
						.joinRecipientsNumbers(command.getRecipients(), ",",
								true)));
				if (command.getFlashSMS()) {
					d.add(new BasicNameValuePair(PARAM_SUBCONNECTOR, "flash"));
				} else {
					d.add(new BasicNameValuePair(PARAM_SUBCONNECTOR, subCon));
				}

				final String customSender = command.getCustomSender();
				if (customSender == null) {
					d.add(new BasicNameValuePair(PARAM_SENDER, Utils
							.national2international(command.getDefPrefix(),
									Utils.getSender(context, command
											.getDefSender()))));
				} else {
					d.add(new BasicNameValuePair(PARAM_SENDER, customSender));
				}
				long sendLater = command.getSendLater();
				if (sendLater > 0) {
					d.add(new BasicNameValuePair(PARAM_SENDLATER, String
							.valueOf(sendLater / 1000)));
				}
			} else {
				url = URL_BALACNCE;
			}
			d.add(new BasicNameValuePair(PARAM_USERNAME, p.getString(
					Preferences.PREFS_USER, "")));
			d.add(new BasicNameValuePair(PARAM_PASSWORD, Utils.md5(p.getString(
					Preferences.PREFS_PASSWORD, ""))));
			HttpResponse response = Utils.getHttpClient(url, null, d, null,
					null);
			int resp = response.getStatusLine().getStatusCode();
			if (resp != HttpURLConnection.HTTP_OK) {
				throw new WebSMSException(context, R.string.error_http, " "
						+ resp);
			}
			String htmlText = Utils.stream2str(
					response.getEntity().getContent()).trim();
			int i = htmlText.indexOf('.');
			if (i > 0) {
				cs.setBalance(htmlText.replace('.', ',') + "\u20AC");
			} else {
				int ret;
				try {
					ret = Integer.parseInt(htmlText.trim());
				} catch (NumberFormatException e) {
					Log.e(TAG, "could not parse text to int: " + htmlText);
					ret = 700;
				}
				checkReturnCode(context, ret);
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
