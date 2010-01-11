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
package de.ub0r.android.andGMXsms;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.util.Log;

/**
 * AsyncTask to manage IO to GMX API.
 * 
 * @author flx
 */
public class ConnectorGMX extends Connector {
	/** Tag for output. */
	private static final String TAG = "WebSMS.GMX";

	/** Custom Dateformater. */
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

	/** ID of mail in array. */
	static final int ID_MAIL = 1;
	/** ID of password in array. */
	static final int ID_PW = 2;

	/** Number of IDs in array for bootstrap. */
	static final int IDS_BOOTSTR = 3;

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

	/** mail. */
	private String mail;
	/** password. */
	private String pw;

	/**
	 * Preferences.
	 * 
	 * @author flx
	 */
	public static class Preferences extends PreferenceActivity {
		/**
		 * {@inheritDoc}
		 */
		@Override
		public final void onCreate(final Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			WebSMS.doPreferences = true;
			this.addPreferencesFromResource(R.xml.connector_gmx_prefs);
		}
	}

	/**
	 * Connectors' specs.
	 */
	private static final ConnectorSpecs SPECS = new ConnectorSpecs() {
		/** Context to use. */
		private Context context = null;
		/** Connector's prefs prefix. */
		private static final String PREFS_PREFIX = "gmx";

		/** Prefs intent action. */
		private static final String PREFS_INTENT_ACTION = "de.ub0r.android.websms.connectors.gmx.PREFS";

		/** Preference's name: mail gmx. */
		// private static final String PREFS_MAIL_GMX = "mail";
		/** Preference's name: username gmx. */
		private static final String PREFS_USER_GMX = "user";
		/** Preference's name: user's password - gmx. */
		private static final String PREFS_PASSWORD_GMX = "password";

		/** Connector's balance. */
		private String balance = null;

		/**
		 * {@inheritDoc}
		 */
		@Override
		public String getAuthor() {
			return this.context.getString(R.string.connector_gmx_author);
		}

		/**
		 * {@inheritDoc}
		 */
		public void setBalance(final String b) {
			this.balance = b;
		}

		/**
		 * {@inheritDoc}
		 */
		public String getBalance() {
			return this.balance;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public Connector getConnector(final Context c) {
			final SharedPreferences p = PreferenceManager
					.getDefaultSharedPreferences(c);
			Connector connector = new ConnectorGMX(p.getString(PREFS_USER_GMX,
					""), p.getString(PREFS_PASSWORD_GMX, ""));
			connector.context = c;
			return connector;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public String getName(final boolean shortName) {
			return this.context.getString(R.string.connector_gmx_name);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public Intent getPreferencesIntent() {
			return new Intent(PREFS_INTENT_ACTION);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public String getPreferencesTitle() {
			return this.context.getString(R.string.settings_gmx);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public String getPrefsPrefix() {
			return PREFS_PREFIX;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void init(final Context c) {
			this.context = c;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean isEnabled() {
			return PreferenceManager.getDefaultSharedPreferences(this.context)
					.getBoolean(PREFS_ENABLED + this.getPrefsPrefix(), false);
		}

		/**
		 * {@inheritDoc}
		 */
		public boolean supportMultipleRecipients() {
			return true;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean supportCustomsender() {
			return true;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean supportFlashsms() {
			return false;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean supportSendLater() {
			return true;
		}

	};

	static {
		Connector.registerConnectorSpecs(SPECS);
	};

	/**
	 * Create a GMX Connector.
	 * 
	 * @param u
	 *            username
	 * @param p
	 *            password
	 */
	ConnectorGMX(final String u, final String p) {
		super(u, p, GMX);
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
	 * @param packetName
	 *            packetName
	 * @param packetVersion
	 *            packetVersion
	 * @param addCustomer
	 *            add customer id/password
	 * @return Hashtable filled with customer_id and password.
	 */
	private StringBuilder openBuffer(final String packetName,
			final String packetVersion, final boolean addCustomer) {
		StringBuilder ret = new StringBuilder();
		ret.append("<WR TYPE=\"RQST\" NAME=\"");
		ret.append(packetName);
		ret.append("\" VER=\"");
		ret.append(packetVersion);
		ret.append("\" PROGVER=\"");
		ret.append(TARGET_PROTOVERSION);
		ret.append("\">");
		if (addCustomer) {
			writePair(ret, "customer_id", this.user);
			writePair(ret, "password", this.password);
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
	private String getParam(final String packet, final String name) {
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
	 * @param packetData
	 *            packetData
	 * @return successful?
	 * @throws WebSMSException
	 *             WebSMSException
	 */
	private boolean sendData(final StringBuilder packetData)
			throws WebSMSException {
		try {
			// check connection:
			HttpURLConnection c = (HttpURLConnection) (new URL("http://"
					+ TARGET_HOST[WebSMS.prefsGMXhostname] + TARGET_PATH))
					.openConnection();
			// set prefs
			c.setRequestProperty("User-Agent", TARGET_AGENT);
			c.setRequestProperty("Content-Encoding", TARGET_ENCODING);
			c.setRequestProperty("Content-Type", TARGET_CONTENT);
			int resp = c.getResponseCode();
			if (resp == HTTP_SERVICE_UNAVAILABLE) {
				// switch hostname
				WebSMS.prefsGMXhostname = (WebSMS.prefsGMXhostname + 1) % 2;
			}

			// get Connection
			c = (HttpURLConnection) (new URL("http://"
					+ TARGET_HOST[WebSMS.prefsGMXhostname] + TARGET_PATH))
					.openConnection();
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

			// send data
			resp = c.getResponseCode();
			if (resp != HttpURLConnection.HTTP_OK) {
				if (resp == HTTP_SERVICE_UNAVAILABLE) {
					throw new WebSMSException(this.context,
							R.string.log_error_service, "" + resp);
				} else {
					throw new WebSMSException(this.context,
							R.string.log_error_http, "" + resp);
				}
			}
			// read received data
			int bufsize = c.getHeaderFieldInt("Content-Length", -1);
			if (bufsize > 0) {
				String resultString = stream2str(c.getInputStream());
				if (resultString.startsWith("The truth")) {
					// wrong data sent!
					throw new WebSMSException(this.context,
							R.string.log_error_server, "" + resultString);
				}

				// strip packet
				int resultIndex = resultString.indexOf("rslt=");
				String outp = resultString.substring(resultIndex).replace(
						"\\p", "\n");
				outp = outp.replace("</WR>", "");

				// get result code
				String resultValue = this.getParam(outp, "rslt");
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
					String p = this.getParam(outp, "free_rem_month");
					if (p != null) {
						String b = p;
						p = this.getParam(outp, "free_max_month");
						if (p != null) {
							b += "/" + p;
						}
						SPECS.setBalance(b);
						this.pushMessage(WebSMS.MESSAGE_FREECOUNT, null);
					}
					p = this.getParam(outp, "customer_id");
					if (p != null) {
						WebSMS.prefsUserGMX = p;
						if (this.pw != null) {
							WebSMS.prefsPasswordGMX = this.pw;
						}
						if (this.mail != null) {
							WebSMS.prefsMailGMX = this.mail;
						}
						((WebSMS) this.context).savePreferences();
						inBootstrap = false;
						this.pushMessage(WebSMS.MESSAGE_PREFSREADY, null);
					}
					return true;
				case RSLT_WRONG_CUSTOMER: // wrong user/pw
					throw new WebSMSException(this.context,
							R.string.log_error_pw);
				case RSLT_WRONG_MAIL: // wrong mail/pw
					inBootstrap = false;
					WebSMS.prefsPasswordGMX = "";
					this.pushMessage(WebSMS.MESSAGE_PREFSREADY, null);
					throw new WebSMSException(this.context,
							R.string.log_error_mail);
				case RSLT_WRONG_SENDER: // wrong sender
					throw new WebSMSException(this.context,
							R.string.log_error_sender);
				case RSLT_UNREGISTERED_SENDER: // unregistered sender
					throw new WebSMSException(this.context,
							R.string.log_error_sender_unregistered);
				default:
					throw new WebSMSException(outp + " #" + rslt);
				}
			} else {
				throw new WebSMSException(this.context,
						R.string.log_http_header_missing);
			}
		} catch (IOException e) {
			Log.e(TAG, null, e);
			throw new WebSMSException(e.toString());
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final boolean updateMessages() throws WebSMSException {
		return this.sendData(closeBuffer(this.openBuffer("GET_SMS_CREDITS",
				"1.00", true)));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final boolean sendMessage() throws WebSMSException {
		StringBuilder packetData = this.openBuffer("SEND_SMS", "1.01", true);
		// fill buffer
		writePair(packetData, "sms_text", this.text);
		StringBuilder recipients = new StringBuilder();
		// table: <id>, <name>, <number>
		int j = 0;
		for (int i = 0; i < this.to.length; i++) {
			if (this.to[i] != null && this.to[i].length() > 1) {
				recipients.append(++j);
				recipients.append("\\;null\\;");
				recipients.append(this.to[i]);
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
		if (this.customSender != null && this.customSender.length() > 0) {
			writePair(packetData, "sms_sender", this.customSender);
		} else {
			writePair(packetData, "sms_sender", this.sender);
		}
		if (this.sendLater > 0) {
			writePair(packetData, "send_date", DateFormat.format(DATEFORMAT,
					this.sendLater).toString());
		}
		// push data
		if (!this.sendData(closeBuffer(packetData))) {
			// failed!
			throw new WebSMSException(this.context, R.string.log_error);
		}
		// result: ok
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final boolean doBootstrap(final String[] params)
			throws WebSMSException {
		inBootstrap = true;
		StringBuilder packetData = this.openBuffer("GET_CUSTOMER", "1.10",
				false);
		writePair(packetData, "email_address", params[ID_MAIL]);
		writePair(packetData, "password", params[ID_PW]);
		writePair(packetData, "gmx", "1");
		return this.sendData(closeBuffer(packetData));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final boolean supportCustomsender() {
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final boolean supportSendLater() {
		return true;
	}
}
