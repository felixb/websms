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
package de.ub0r.android.websms.connector.gmx;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.util.Log;
import de.ub0r.android.websms.connector.common.Connector;
import de.ub0r.android.websms.connector.common.ConnectorCommand;
import de.ub0r.android.websms.connector.common.ConnectorSpec;
import de.ub0r.android.websms.connector.common.Utils;
import de.ub0r.android.websms.connector.common.WebSMSException;
import de.ub0r.android.websms.connector.common.ConnectorSpec.SubConnectorSpec;

/**
 * AsyncTask to manage IO to GMX API.
 * 
 * @author flx
 */
public class ConnectorGMX extends Connector {
	/** Tag for output. */
	private static final String TAG = "WebSMS.GMX";

	/** Custom {@link DateFormat}. */
	private static final String DATEFORMAT = "yyyy-MM-dd kk-mm-00";

	/** Target host. */
	private static final String[] TARGET_HOST = { "app0.wr-gmbh.de",
			"app5.wr-gmbh.de" };
	/** Target path on host. */
	private static final String TARGET_PATH = "/WRServer/WRServer.dll/WR";
	/** Target mime encoding. */
	private static final String TARGET_ENCODING = "wr-cs";
	/** Target mime type. */
	private static final String TARGET_CONTENT = "text/plain";
	/** HTTP Useragent. */
	private static final String TARGET_AGENT = "Mozilla/3.0 (compatible)";
	/** Target version of protocol. */
	private static final String TARGET_PROTOVERSION = "1.13.03";

	/** Result: ok. */
	private static final int RSLT_OK = 0;
	/** Result: wrong customerid/password. */
	private static final int RSLT_WRONG_CUSTOMER = 11;
	/** Result: wrong mail/password. */
	private static final int RSLT_WRONG_MAIL = 25;
	/** Result: wrong sender. */
	private static final int RSLT_WRONG_SENDER = 8;
	/** Result: sender is unregistered by gmx. */
	private static final int RSLT_UNREGISTERED_SENDER = 71;

	/** Max custom sender length. */
	private static final int CUSTOM_SENDER_LEN = 10;

	/** Check whether this connector is bootstrapping. */
	private static boolean inBootstrap = false;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final ConnectorSpec initSpec(final Context context) {
		final String name = context.getString(R.string.connector_gmx_name);
		ConnectorSpec c = new ConnectorSpec(name);
		c.setAuthor(context.getString(R.string.connector_gmx_author));
		c.setBalance(null);
		c.setCapabilities(ConnectorSpec.CAPABILITIES_BOOTSTRAP
				| ConnectorSpec.CAPABILITIES_UPDATE
				| ConnectorSpec.CAPABILITIES_SEND
				| ConnectorSpec.CAPABILITIES_PREFS);
		c.addSubConnector("gmx", c.getName(),
				SubConnectorSpec.FEATURE_MULTIRECIPIENTS
						| SubConnectorSpec.FEATURE_CUSTOMSENDER
						| SubConnectorSpec.FEATURE_SENDLATER);
		c.setLimitLength(CUSTOM_SENDER_LEN);
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
			if (p.getString(Preferences.PREFS_MAIL, "").length() > 0
					&& p.getString(Preferences.PREFS_PASSWORD, "") // .
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
	protected final void doBootstrap(final Context context, final Intent intent)
			throws WebSMSException {
		Log.d(TAG, "bootstrap");
		if (inBootstrap) {
			Log.d(TAG, "already in bootstrap: skip bootstrap");
			return;
		}
		if (!Preferences.needBootstrap(context)) {
			Log.d(TAG, "skip bootstrap");
			return;
		}
		inBootstrap = true;
		StringBuilder packetData = openBuffer(context, "GET_CUSTOMER", "1.10",
				false);
		final SharedPreferences p = PreferenceManager
				.getDefaultSharedPreferences(context);
		writePair(packetData, "email_address", p.getString(
				Preferences.PREFS_MAIL, ""));
		writePair(packetData, "password", p.getString(
				Preferences.PREFS_PASSWORD, ""));
		writePair(packetData, "gmx", "1");
		this.sendData(context, closeBuffer(packetData));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final void doUpdate(final Context context, final Intent intent)
			throws WebSMSException {
		Log.d(TAG, "update");
		this.doBootstrap(context, intent);

		this.sendData(context, closeBuffer(openBuffer(context,
				"GET_SMS_CREDITS", "1.00", true)));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final void doSend(final Context context, final Intent intent)
			throws WebSMSException {
		Log.d(TAG, "send");
		this.doBootstrap(context, intent);

		ConnectorCommand command = new ConnectorCommand(intent);
		StringBuilder packetData = openBuffer(context, // .
				"SEND_SMS", "1.01", true);
		// fill buffer
		writePair(packetData, "sms_text", command.getText());
		StringBuilder recipients = new StringBuilder();
		// table: <id>, <name>, <number>
		int j = 0;
		String[] to = command.getRecipients();
		for (int i = 0; i < to.length; i++) {
			if (to[i] != null && to[i].length() > 1) {
				recipients.append(++j);
				recipients.append("\\;null\\;");
				recipients.append(Utils.national2international(command
						.getDefPrefix(), Utils.getRecipientsNumber(to[i])));
				recipients.append("\\;");
			}
		}
		recipients.append("</TBL>");
		String recipientsString = "<TBL ROWS=\"" + j + "\" COLS=\"3\">"
				+ "receiver_id\\;receiver_name\\;receiver_number\\;"
				+ recipients.toString();
		recipients = null;
		writePair(packetData, "receivers", recipientsString);
		writePair(packetData, "send_option", "sms");
		final String customSender = command.getCustomSender();
		if (customSender != null && customSender.length() > 0) {
			writePair(packetData, "sms_sender", customSender);
		} else {
			writePair(packetData, "sms_sender", Utils.getSender(context,
					command.getDefSender()));
		}
		final long sendLater = command.getSendLater();
		if (sendLater > 0) {
			writePair(packetData, "send_date", DateFormat.format(DATEFORMAT,
					sendLater).toString());
		}
		// push data
		this.sendData(context, closeBuffer(packetData));
	}

	/**
	 * Write key,value to StringBuilder.
	 * 
	 * @param buffer
	 *            buffer
	 * @param key
	 *            key
	 * @param value
	 *            value
	 */
	private static void writePair(final StringBuilder buffer, final String key,
			final String value) {
		buffer.append(key);
		buffer.append('=');
		buffer.append(value.replace("\\", "\\\\").replace(">", "\\>").replace(
				"<", "\\<"));
		buffer.append("\\p");
	}

	/**
	 * Create default data hashtable.
	 * 
	 * @param context
	 *            {@link Context}
	 * @param packetName
	 *            packetName
	 * @param packetVersion
	 *            packetVersion
	 * @param addCustomer
	 *            add customer id/password
	 * @return Hashtable filled with customer_id and password.
	 */
	private static StringBuilder openBuffer(final Context context,
			final String packetName, final String packetVersion,
			final boolean addCustomer) {
		StringBuilder ret = new StringBuilder();
		ret.append("<WR TYPE=\"RQST\" NAME=\"");
		ret.append(packetName);
		ret.append("\" VER=\"");
		ret.append(packetVersion);
		ret.append("\" PROGVER=\"");
		ret.append(TARGET_PROTOVERSION);
		ret.append("\">");
		if (addCustomer) {
			SharedPreferences p = PreferenceManager
					.getDefaultSharedPreferences(context);
			writePair(ret, "customer_id", p.getString(Preferences.PREFS_USER,
					""));
			writePair(ret, "password", p.getString(Preferences.PREFS_PASSWORD,
					""));
		}
		return ret;
	}

	/**
	 * Close Buffer.
	 * 
	 * @param buffer
	 *            buffer
	 * @return buffer
	 */
	private static StringBuilder closeBuffer(final StringBuilder buffer) {
		buffer.append("</WR>");
		return buffer;
	}

	/**
	 * Parse returned packet. Search for name=(.*)\n and return (.*)
	 * 
	 * @param packet
	 *            packet
	 * @param name
	 *            parma's name
	 * @return param's value
	 */
	private static String getParam(final String packet, final String name) {
		int i = packet.indexOf(name + '=');
		if (i < 0) {
			return null;
		}
		int j = packet.indexOf("\n", i);
		if (j < 0) {
			return packet.substring(i + name.length() + 1);
		} else {
			return packet.substring(i + name.length() + 1, j);
		}
	}

	/**
	 * Send data.
	 * 
	 * @param context
	 *            {@link Context}
	 * @param packetData
	 *            packetData
	 * @throws WebSMSException
	 *             WebSMSException
	 */
	private void sendData(final Context context, final StringBuilder packetData)
			throws WebSMSException {
		try {
			// check connection:
			// get cluster side
			SharedPreferences sp = PreferenceManager
					.getDefaultSharedPreferences(context);
			int gmxHost = sp.getInt(Preferences.PREFS_GMX_HOST, 0);
			HttpURLConnection c = (HttpURLConnection) (new URL("http://"
					+ TARGET_HOST[gmxHost] + TARGET_PATH)).openConnection();
			// set prefs
			c.setRequestProperty("User-Agent", TARGET_AGENT);
			c.setRequestProperty("Content-Encoding", TARGET_ENCODING);
			c.setRequestProperty("Content-Type", TARGET_CONTENT);
			int resp = c.getResponseCode();
			if (resp == Utils.HTTP_SERVICE_UNAVAILABLE) {
				// switch hostname
				gmxHost = (gmxHost + 1) % 2;
				sp.edit().putInt(Preferences.PREFS_GMX_HOST, gmxHost).commit();
			}

			// get Connection
			c = (HttpURLConnection) (new URL("http://" + TARGET_HOST[gmxHost]
					+ TARGET_PATH)).openConnection();
			// set prefs
			c.setRequestProperty("User-Agent", TARGET_AGENT);
			c.setRequestProperty("Content-Encoding", TARGET_ENCODING);
			c.setRequestProperty("Content-Type", TARGET_CONTENT);
			c.setRequestMethod("POST");
			c.setDoOutput(true);
			// push post data
			OutputStream os = c.getOutputStream();
			os.write(packetData.toString().getBytes("ISO-8859-15"));
			os.close();
			os = null;
			Log.d(TAG, "--HTTP POST--");
			Log.d(TAG, packetData.toString());
			Log.d(TAG, "--HTTP POST--");

			// send data
			resp = c.getResponseCode();
			if (resp != HttpURLConnection.HTTP_OK) {
				if (resp == Utils.HTTP_SERVICE_UNAVAILABLE
						|| resp == Utils.HTTP_SERVICE_500) {
					throw new WebSMSException(context, R.string.error_service,
							" HTTP: " + resp);
				} else {
					throw new WebSMSException(context, R.string.error_http, " "
							+ resp);
				}
			}
			// read received data
			int bufsize = c.getHeaderFieldInt("Content-Length", -1);
			if (bufsize > 0) {
				String resultString = Utils.stream2str(c.getInputStream());
				if (resultString.startsWith("The truth")) {
					// wrong data sent!
					throw new WebSMSException(context, R.string.error_server,
							"" + resultString);
				}
				Log.d(TAG, "--HTTP RESPONSE--");
				Log.d(TAG, resultString);
				Log.d(TAG, "--HTTP RESPONSE--");

				// strip packet
				int resultIndex = resultString.indexOf("rslt=");
				String outp = resultString.substring(resultIndex).replace(
						"\\p", "\n");
				outp = outp.replace("</WR>", "");

				// get result code
				String resultValue = getParam(outp, "rslt");
				int rslt;
				try {
					rslt = Integer.parseInt(resultValue);
				} catch (Exception e) {
					Log.e(TAG, null, e);
					throw new WebSMSException(e.toString());
				}
				switch (rslt) {
				case RSLT_OK: // ok
					// fetch additional info
					String p = getParam(outp, "free_rem_month");
					if (p != null) {
						String b = p;
						p = getParam(outp, "free_max_month");
						if (p != null) {
							b += "/" + p;
						}
						this.getSpec(context).setBalance(b);
					}
					p = getParam(outp, "customer_id");
					if (p != null) {
						Editor e = PreferenceManager
								.getDefaultSharedPreferences(context).edit();
						e.putString(Preferences.PREFS_USER, p);
						e.commit();
						inBootstrap = false;
					}
					return;
				case RSLT_WRONG_CUSTOMER: // wrong user/pw
					throw new WebSMSException(context, R.string.error_pw);
				case RSLT_WRONG_MAIL: // wrong mail/pw
					inBootstrap = false;
					throw new WebSMSException(context, R.string.error_mail);
				case RSLT_WRONG_SENDER: // wrong sender
					throw new WebSMSException(context, R.string.error_sender);
				case RSLT_UNREGISTERED_SENDER: // unregistered sender
					throw new WebSMSException(context,
							R.string.error_sender_unregistered);
				default:
					throw new WebSMSException(outp + " #" + rslt);
				}
			} else {
				throw new WebSMSException(context,
						R.string.error_http_header_missing);
			}
		} catch (IOException e) {
			Log.e(TAG, null, e);
			throw new WebSMSException(e);
		}
	}
}
