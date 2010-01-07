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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.cookie.Cookie;
import org.apache.http.cookie.CookieOrigin;
import org.apache.http.cookie.MalformedCookieException;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.cookie.BrowserCompatSpec;
import org.apache.http.impl.cookie.CookieSpecBase;
import org.apache.http.message.BasicNameValuePair;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

/**
 * Connector is the basic Connector. Implement other real Connectors as extend.
 * Connector will act as entry to them.
 * 
 * @author Felix Bechstein
 */
public abstract class Connector extends AsyncTask<String, Boolean, Boolean> {
	/** Tag for output. */
	private static final String TAG = "WebSMS.con";

	/** Intent's scheme to send sms. */
	public static final String INTENT_SCHEME_SMSTO = "smsto";

	/** HTTP Response 200. */
	static final int HTTP_SERVICE_OK = 200;
	/** HTTP Response 401. */
	static final int HTTP_SERVICE_UNAUTHORIZED = 401;
	/** HTTP Response 503. */
	static final int HTTP_SERVICE_UNAVAILABLE = 503;

	/** Connector type: SMS. */
	static final short SMS = 0;
	/** Connector type: GMX. */
	static final short GMX = 1;
	/** Connector type: O2. */
	static final short O2 = 2;
	/** Connector type: Sipgate. */
	static final short SIPGATE = 3;
	/** Connector type: Innosend. */
	static final short INNOSEND_FREE = 4;
	/** Connector type: Innosend w/o sender. */
	static final short INNOSEND_WO_SENDER = 5;
	/** Connector type: Innosend w/ sender. */
	static final short INNOSEND_W_SENDER = 6;
	/** Connector type: Innosend. */
	static final short INNOSEND = INNOSEND_WO_SENDER;
	/** Connector type: CherrySMS w/o sender. */
	static final short CHERRY_WO_SENDER = 7;
	/** Connector type: CherrySMS w/ sender. */
	static final short CHERRY_W_SENDER = 8;
	/** Connector type: CherrySMS. */
	static final short CHERRY = CHERRY_WO_SENDER;
	/** Connector type: Sloono discount. */
	static final short SLOONO_DISCOUNT = 9;
	/** Connector type: Sloono basic. */
	static final short SLOONO_BASIC = 10;
	/** Connector type: Sloono pro. */
	static final short SLOONO_PRO = 11;
	/** Connector type: Sloono. */
	static final short SLOONO = SLOONO_PRO;

	/** Number of connectors. */
	static final short CONNECTORS = SLOONO_PRO + 1;

	/** ID of Param-ID. This is to distinguish between different calls. */
	static final int ID_ID = 0;
	/** ID of text in array. */
	static final int ID_TEXT = 1;
	/** ID of recipient in array. */
	static final int ID_TO = 2;
	/** ID of falshsms. */
	static final int ID_FLASHSMS = 3;
	/** ID of custom sender. */
	static final int ID_CUSTOMSENDER = 4;
	/** ID of send later. */
	static final int ID_SENDLATER = 5;

	/** ID of mail in array. */
	static final int ID_MAIL = 1;
	/** ID of password in array. */
	static final int ID_PW = 2;

	/** Number of IDs in array for sms send. */
	static final int IDS_SEND = 6;

	/** ID_ID for sending a message. */
	static final String ID_SEND = "0";
	/** ID_ID for updating message count. */
	static final String ID_UPDATE = "1";
	/** ID_ID for bootstrapping. */
	public static final String ID_BOOSTR = "2";

	/** Parameters for updating message count. */
	static final String[] PARAMS_UPDATE = { ID_UPDATE };

	/** Standard buffer size. */
	public static final int BUFSIZE = 32768;

	/** SMS DB: address. */
	static final String ADDRESS = "address";
	/** SMS DB: person. */
	// private static final String PERSON = "person";
	/** SMS DB: date. */
	private static final String DATE = "date";
	/** SMS DB: read. */
	static final String READ = "read";
	/** SMS DB: status. */
	// private static final String STATUS = "status";
	/** SMS DB: type. */
	static final String TYPE = "type";
	/** SMS DB: body. */
	static final String BODY = "body";
	/** SMS DB: type - sent. */
	static final int MESSAGE_TYPE_SENT = 2;

	/** recipient, numbers only. */
	protected String[] to;
	/** recipient, names only. */
	protected String[] toNames;
	/** recipient, splitted but not stripped. */
	protected String[] toFull;
	/** recipients list. as it comes from the user. */
	protected String tos = "";

	/** Text. */
	protected String text;
	/** Send as flashSMS? */
	protected boolean flashSMS;
	/** Custom sender. */
	protected String customSender;
	/** Timestamp when to send sms. */
	protected long sendLater;

	/** User. */
	protected final String user;
	/** Password. */
	protected final String password;
	/** Default prefix. */
	private String defPrefix;
	/** Sender. */
	protected String sender;

	/** Connector is bootstrapping. */
	static boolean inBootstrap = false;

	/** Connector is in update. */
	private static final boolean[] IN_UPDATE = new boolean[CONNECTORS];

	static {
		for (int i = 0; i < CONNECTORS; i++) {
			IN_UPDATE[i] = false;
		}
	}

	/** Type of IO Op. */
	protected String type;

	/** Message to log to the user. */
	protected String failedMessage = null;

	/** Context IO is running from. */
	protected Context context;

	/** Concurrent updates running. */
	private static int countUpdates = 0;

	/** Notification showed in case of failure. */
	private Notification notification = null;

	/** Type of connector. */
	private short connector = 0;

	/**
	 * Exception while Connector IO.
	 * 
	 * @author flx
	 */
	static class WebSMSException extends Exception {

		/** The Constant serialVersionUID. */
		private static final long serialVersionUID = -6215729019426883487L;

		/**
		 * Create a new WebSMSException.
		 * 
		 * @param s
		 *            error message
		 */
		public WebSMSException(final String s) {
			super(s);
		}

		/**
		 * Create a new WebSMSException.
		 * 
		 * @param c
		 *            Context to resolve resource id
		 * @param rid
		 *            error message as resource id
		 */
		public WebSMSException(final Context c, final int rid) {
			super(c.getString(rid));
		}

		/**
		 * Create a new WebSMSException.
		 * 
		 * @param c
		 *            Context to resolve resource id
		 * @param rid
		 *            error message as resource id
		 * @param s
		 *            error message
		 */
		public WebSMSException(final Context c, final int rid, final String s) {
			super(c.getString(rid) + s);
		}
	}

	/**
	 * Default Constructor.
	 * 
	 * @param u
	 *            user
	 * @param p
	 *            password
	 * @param con
	 *            connector type
	 */
	protected Connector(final String u, final String p, final short con) {
		this.connector = con;
		this.user = u;
		this.password = p;
	}

	/**
	 * Send a message to one or more receivers. This is done in background!
	 * 
	 * @param con
	 *            Context
	 * @param connector
	 *            Connector which should be used.
	 * @param params
	 *            Sending parameters.
	 */
	public static final void send(final Context con, final short connector,
			final String[] params) {
		Connector c;
		switch (connector) {
		case O2:
		case INNOSEND_FREE:
		case INNOSEND_W_SENDER:
		case INNOSEND_WO_SENDER:
			// for a few senders we just split recipients for our self
			String r = params[ID_TO].trim();
			if (r.endsWith(",")) {
				r = r.substring(0, r.length() - 1).trim();
			}
			final String[] recipients = r.split(",");
			r = null;
			for (String rs : recipients) {
				final String[] p = params.clone();
				p[ID_TO] = rs;
				c = getConnector(con, connector);
				c.execute(p);
			}
			return;
		default:
			c = getConnector(con, connector);
			if (c != null) {
				c.execute(params);
			}
			return;
		}
	}

	/**
	 * Build a params[] array for sending a sms.
	 * 
	 * @param recipients
	 *            Receivers of the message.
	 * @param text
	 *            Text which should be sent.
	 * @param flashSMS
	 *            true if sms should be send as flashsms
	 * @param customSender
	 *            custom sender if wanted
	 * @param sendLater
	 *            timestamp for sending later
	 * @return params[] array
	 */
	public static final String[] buildSendParams(final String recipients,
			final String text, final boolean flashSMS,
			final String customSender, final long sendLater) {
		String[] params = new String[IDS_SEND];
		params[ID_ID] = ID_SEND;
		params[ID_TEXT] = text;
		params[ID_TO] = recipients;
		if (flashSMS) {
			params[ID_FLASHSMS] = "1";
		} else {
			params[ID_FLASHSMS] = null;
		}
		params[ID_CUSTOMSENDER] = customSender;
		params[ID_SENDLATER] = "" + sendLater;
		return params;
	}

	/**
	 * Send a message to one or more receivers. This is done in background!
	 * 
	 * @param con
	 *            Context
	 * @param connector
	 *            Connector which should be used.
	 * @param recipients
	 *            Receivers of the message.
	 * @param text
	 *            Text which should be sent.
	 * @param flashSMS
	 *            true if sms should be send as flashsms
	 * @param customSender
	 *            custom sender if wanted
	 * @param sendLater
	 *            timestamp for sending later
	 */
	public static final void send(final Context con, final short connector,
			final String recipients, final String text, final boolean flashSMS,
			final String customSender, final long sendLater) {
		Connector.send(con, connector, buildSendParams(recipients, text,
				flashSMS, customSender, sendLater));
	}

	/**
	 * Update (free) message count. This is done in background!
	 * 
	 * @param con
	 *            Context
	 * @param connector
	 *            Connector which should be used.
	 */
	public static synchronized void update(final Context con,
			final short connector) {
		++countUpdates;
		final Connector c = getConnector(con, connector);
		if (c != null) {
			c.execute(PARAMS_UPDATE);
		}
	}

	/**
	 * Bootstrap a Connector. Like checking settings etc. This is done in
	 * background!
	 * 
	 * @param con
	 *            Context
	 * @param connector
	 *            Connector which should be used.
	 * @param params
	 *            Parameters the Connector expects
	 */
	public static final void bootstrap(final Context con,
			final short connector, final String[] params) {
		final Connector c = getConnector(con, connector);
		if (c != null) {
			c.execute(params);
		}
	}

	/**
	 * Get a Connector of given type.
	 * 
	 * @param con
	 *            Context
	 * @param connector
	 *            Connector which should be used.
	 * @return a fresh Connector Object
	 */
	private static Connector getConnector(final Context con,
			final short connector) {
		Connector c;
		switch (connector) {
		case SMS:
			c = new ConnectorSMS();
			break;
		case GMX:
			c = new ConnectorGMX(WebSMS.prefsUserGMX, WebSMS.prefsPasswordGMX);
			break;
		case O2:
			c = new ConnectorO2(international2national(WebSMS.prefsSender),
					WebSMS.prefsPasswordO2);
			break;
		case SIPGATE:
			c = new ConnectorSipgate(WebSMS.prefsUserSipgate,
					WebSMS.prefsPasswordSipgate);
			break;
		case INNOSEND_FREE:
		case INNOSEND_WO_SENDER:
		case INNOSEND_W_SENDER:
			c = new ConnectorInnosend(WebSMS.prefsUserInnosend,
					WebSMS.prefsPasswordInnosend, connector);
			break;
		case CHERRY_WO_SENDER:
		case CHERRY_W_SENDER:
			c = new ConnectorCherrySMS(
					international2oldformat(WebSMS.prefsSender), WebSMS
							.md5(WebSMS.prefsPasswordCherrySMS), connector);
			break;
		case SLOONO_BASIC:
		case SLOONO_DISCOUNT:
		case SLOONO_PRO:
			c = new ConnectorSloono(WebSMS.prefsUserSloono, WebSMS
					.md5(WebSMS.prefsPasswordSloono), connector);
			break;
		default:
			Log.e(TAG, "missing Connector");
			return null;
		}
		c.context = con;
		c.defPrefix = WebSMS.prefsDefPrefix;
		c.sender = WebSMS.prefsSender;
		return c;
	}

	/**
	 * Get Connectors name.
	 * 
	 * @param con
	 *            Context to read the strings
	 * @param connector
	 *            connector
	 * @return name
	 */
	public static final String getConnectorName(final Context con,
			final short connector) {
		String[] ret = con.getResources().getStringArray(R.array.connectors);
		if (connector < ret.length) {
			return ret[connector];
		} else {
			return null;
		}
	}

	/**
	 * Get Connector ID.
	 * 
	 * @param con
	 *            Context to read the strings
	 * @param connector
	 *            connector
	 * @return id
	 */
	public static final short getConnectorID(final Context con,
			final String connector) {
		String[] connectors = con.getResources().getStringArray(
				R.array.connectors);
		for (int i = 0; i < connectors.length; i++) {
			if (connector.equals(connectors[i])) {
				return (short) i;
			}
		}
		return 0;
	}

	/**
	 * Extract receivers from a parameters list.
	 * 
	 * @param params
	 *            parameters
	 * @return receivers of a message
	 */
	protected static final String[] getReceivers(final String[] params) {
		String[] ret = new String[params.length - 2];
		for (int i = 2; i < params.length; i++) {
			ret[i - 2] = params[i];
		}
		return ret;
	}

	/**
	 * Read in data from Stream into String.
	 * 
	 * @param is
	 *            stream
	 * @return String
	 * @throws IOException
	 *             IOException
	 */
	protected static final String stream2str(final InputStream is)
			throws IOException {
		BufferedReader bufferedReader = new BufferedReader(
				new InputStreamReader(is), BUFSIZE);
		StringBuilder data = new StringBuilder();
		String line = null;
		while ((line = bufferedReader.readLine()) != null) {
			data.append(line + "\n");
		}
		bufferedReader.close();
		return data.toString();
	}

	/**
	 * Get a fresh HTTP-Connection.
	 * 
	 * @param url
	 *            URL to open
	 * @param cookies
	 *            cookies to transmit
	 * @param postData
	 *            post data
	 * @param userAgent
	 *            user agent
	 * @param referer
	 *            referer
	 * @return the connection
	 * @throws IOException
	 *             IOException
	 */
	protected static HttpResponse getHttpClient(final String url,
			final ArrayList<Cookie> cookies,
			final ArrayList<BasicNameValuePair> postData,
			final String userAgent, final String referer) throws IOException {
		HttpClient client = new DefaultHttpClient();
		HttpRequestBase request;
		if (postData == null) {
			request = new HttpGet(url);
		} else {
			request = new HttpPost(url);
			((HttpPost) request).setEntity(new UrlEncodedFormEntity(postData,
					"ISO-8859-15"));
		}
		if (referer != null) {
			request.setHeader("Referer", referer);
		}
		if (userAgent != null) {
			request.setHeader("User-Agent", userAgent);
		}

		if (cookies != null && cookies.size() > 0) {
			CookieSpecBase cookieSpecBase = new BrowserCompatSpec();
			for (Header cookieHeader : cookieSpecBase.formatCookies(cookies)) {
				// Setting the cookie
				request.setHeader(cookieHeader);
			}
		}
		return client.execute(request);
	}

	/**
	 * Update cookies from response.
	 * 
	 * @param cookies
	 *            old cookie list
	 * @param headers
	 *            headers from response
	 * @param url
	 *            requested url
	 * @throws URISyntaxException
	 *             malformed uri
	 * @throws MalformedCookieException
	 *             malformed cookie
	 */
	protected static void updateCookies(final ArrayList<Cookie> cookies,
			final Header[] headers, final String url)
			throws URISyntaxException, MalformedCookieException {
		final URI uri = new URI(url);
		int port = uri.getPort();
		if (port < 0) {
			if (url.startsWith("https")) {
				port = 443;
			} else {
				port = 80;
			}
		}
		CookieOrigin origin = new CookieOrigin(uri.getHost(), port, uri
				.getPath(), false);
		CookieSpecBase cookieSpecBase = new BrowserCompatSpec();
		for (Header header : headers) {
			for (Cookie cookie : cookieSpecBase.parse(header, origin)) {
				// THE cookie
				String name = cookie.getName();
				String value = cookie.getValue();
				if (value == null || value.equals("")) {
					continue;
				}
				for (Cookie c : cookies) {
					if (name.equals(c.getName())) {
						cookies.remove(c);
						cookies.add(cookie);
						name = null;
						break;
					}
				}
				if (name != null) {
					cookies.add(cookie);
				}
			}
		}
	}

	/**
	 * Save Message to internal database.
	 * 
	 * @param reciepients
	 *            reciepients. first entry is skipped!
	 * @param msgText
	 *            text of message.
	 */
	protected final void saveMessage(final String[] reciepients,
			final String msgText) {
		if (reciepients == null || msgText == null) {
			return;
		}
		for (int i = 0; i < reciepients.length; i++) {
			if (reciepients[i] == null || reciepients[i].length() == 0) {
				continue; // skip empty recipients
			}
			// save sms to content://sms/sent
			ContentValues values = new ContentValues();
			values.put(ADDRESS, reciepients[i]);
			values.put(READ, 1);
			values.put(TYPE, MESSAGE_TYPE_SENT);
			values.put(BODY, msgText);
			if (this.sendLater > 0) {
				values.put(DATE, this.sendLater);
			}
			this.context.getContentResolver().insert(
					Uri.parse("content://sms/sent"), values);
		}
	}

	/**
	 * Get free sms count.
	 * 
	 * @return ok?
	 * @throws WebSMSException
	 *             WebSMSException
	 */
	protected abstract boolean updateMessages() throws WebSMSException;

	/**
	 * Bootstrap: Get preferences. This default implementation odes nothing!
	 * 
	 * @param params
	 *            Parameters
	 * @return ok?
	 * @throws WebSMSException
	 *             WebSMSException
	 */
	protected boolean doBootstrap(final String[] params) throws WebSMSException {
		return false;
	}

	/**
	 * Send sms.
	 * 
	 * @return ok?
	 * @throws WebSMSException
	 *             WebSMSException
	 */
	protected abstract boolean sendMessage() throws WebSMSException;

	/**
	 * Check if update is possible.
	 * 
	 * @param startUpdate
	 *            true for starting, false for ending update
	 * @return true if update is possible
	 */
	private synchronized boolean checkUpdate(final boolean startUpdate) {
		if (startUpdate) {
			if (IN_UPDATE[this.connector]) {
				return false;
			} else {
				IN_UPDATE[this.connector] = true;
				return true;
			}
		} else {
			IN_UPDATE[this.connector] = false;
			return true;
		}
	}

	/**
	 * Prepare reciepients before sending.
	 */
	private void prepareSend() {
		// fetch text/recipient
		final String[] numbers = this.parseReciepients(this.tos);
		final String prefix = this.defPrefix;
		// fix number prefix
		for (int i = 0; i < numbers.length; i++) {
			String t = numbers[i];
			if (t != null) {
				if (!t.startsWith("+")) {
					if (t.startsWith("00")) {
						t = "+" + t.substring(2);
					} else if (t.startsWith("0")) {
						t = prefix + t.substring(1);
					}
				}
			}
			numbers[i] = Connector.cleanRecipient(t);
		}
		this.to = numbers;
	}

	/**
	 * Run IO in background.
	 * 
	 * @param params
	 *            (text,recipient)
	 * @return ok?
	 */
	@Override
	protected final Boolean doInBackground(final String... params) {
		Log.d(TAG, "doInBackground()");
		boolean ret = false;
		try {
			String t;
			if (params == null || params[ID_ID] == null) {
				t = ID_UPDATE;
			} else {
				t = params[ID_ID];
			}
			this.type = t;
			if (t.equals(ID_UPDATE)) {
				if (!this.checkUpdate(true)) {
					return false;
				}
				this.publishProgress(false);
				ret = this.updateMessages();
				this.checkUpdate(false);
			} else if (t.equals(ID_BOOSTR)) {
				this.publishProgress(false);
				ret = this.doBootstrap(params);
			} else if (t.equals(ID_SEND)) {
				this.text = params[ID_TEXT];
				this.tos = params[ID_TO];
				this.flashSMS = params[ID_FLASHSMS] != null;
				this.customSender = params[ID_CUSTOMSENDER];
				final String s = params[ID_SENDLATER];
				if (s == null) {
					this.sendLater = -1;
				} else {
					this.sendLater = Long.parseLong(s);
				}
				this.prepareSend();
				this.notification = this.updateNotification(null);
				IOService.register(this.notification);
				this.publishProgress(false);
				ret = this.sendMessage();
			}
		} catch (WebSMSException e) {
			this.pushMessage(WebSMS.MESSAGE_LOG, e.getMessage());
			ret = false;
		}
		Log.d(TAG, "doInBackground() return " + ret);
		return ret;
	}

	/**
	 * Update progress. Only ran once on startup to display progress dialog.
	 * 
	 * @param progress
	 *            finished?
	 */
	@Override
	protected final void onProgressUpdate(final Boolean... progress) {
		final Context c = this.context;
		final String t = this.type;
		if (t.equals(ID_UPDATE)) {
			((WebSMS) c).setProgressBarIndeterminateVisibility(true);
		} else if (t.equals(ID_BOOSTR)) {
			if (WebSMS.dialog != null) {
				try {
					WebSMS.dialog.dismiss();
				} catch (Exception e) {
					// do nothing
				}
			}
			WebSMS.dialogString = c.getString(R.string.bootstrap_);
			WebSMS.dialog = ProgressDialog.show(c, null, WebSMS.dialogString,
					true);
		}
	}

	/**
	 * Update or Create a Notification.
	 * 
	 * @param n
	 *            Notification for update
	 * @return created/updated Notification
	 */
	private Notification updateNotification(final Notification n) {
		Notification noti = n;
		final Context c = this.context;
		String rcvs = this.tos.trim();
		if (rcvs.endsWith(",")) {
			rcvs = rcvs.substring(0, rcvs.length() - 1);
		}
		if (noti == null) {
			noti = new Notification(R.drawable.stat_notify_sms_failed, c
					.getString(R.string.notify_failed_), System
					.currentTimeMillis());
		} else {
			noti.contentIntent.cancel();
		}
		final Intent i = new Intent(Intent.ACTION_SENDTO, Uri
				.parse(INTENT_SCHEME_SMSTO + ":" + Uri.encode(this.tos)), c,
				WebSMS.class);
		i.putExtra(Intent.EXTRA_TEXT, this.text);
		if (this.failedMessage == null) {
			this.failedMessage = c.getString(R.string.notify_failed_);
		}
		i.putExtra(WebSMS.EXTRA_ERRORMESSAGE, this.failedMessage);
		i.setFlags(i.getFlags() | Intent.FLAG_ACTIVITY_NEW_TASK);
		final PendingIntent cIntent = PendingIntent.getActivity(c, 0, i, 0);
		noti.setLatestEventInfo(c, c.getString(R.string.notify_failed) + " "
				+ this.failedMessage, rcvs + ": " + this.text, cIntent);
		noti.flags |= Notification.FLAG_AUTO_CANCEL;
		Log.d(TAG, "update notification " + this.failedMessage);
		return noti;
	}

	/**
	 * Push data back to GUI. Close progress dialog.
	 * 
	 * @param result
	 *            result
	 */
	@Override
	protected final void onPostExecute(final Boolean result) {
		Log.d(TAG, "onPostExecute(" + result + ")");
		final String t = this.type;
		if (t.equals(ID_UPDATE)) {
			--countUpdates;
			if (countUpdates == 0) {
				((WebSMS) this.context)
						.setProgressBarIndeterminateVisibility(false);
			}
		} else {
			WebSMS.dialogString = null;
			if (WebSMS.dialog != null) {
				try {
					WebSMS.dialog.dismiss();
					WebSMS.dialog = null;
				} catch (Exception e) {
					System.gc();
				}
			}
		}
		if (t.equals(ID_SEND)) {
			if (result) {
				this.saveMessage(this.to, this.text);
			} else {
				this.updateNotification(this.notification);
			}
		}
		if (this.context instanceof IOService) {
			IOService.unregister(this.notification, !result);
		}
		System.gc();
		Log.d(TAG, "onPostExecute(" + result + ") return");
	}

	/**
	 * Check whether this connector supports flashsms. Override to change.
	 * 
	 * @return true if connector supports flashsms
	 */
	protected boolean supportFlashsms() {
		return false;
	}

	/**
	 * Check given connector.
	 * 
	 * @param connector
	 *            connector to check
	 * @return true if given connector supports flashsms
	 */
	static final boolean supportFlashsms(final short connector) {
		return getConnector(null, connector).supportFlashsms();
	}

	/**
	 * Check whether this connector supports custom sender. Override to change.
	 * 
	 * @return true if connector supports custom sender
	 */
	protected boolean supportCustomsender() {
		return false;
	}

	/**
	 * Check given connector.
	 * 
	 * @param connector
	 *            connector to check
	 * @return true if given connector supports custom sender
	 */
	static final boolean supportCustomsender(final short connector) {
		return getConnector(null, connector).supportCustomsender();
	}

	/**
	 * Check whether this connector supports send later. Override to change.
	 * 
	 * @return true if connector supports custom sender
	 */
	protected boolean supportSendLater() {
		return false;
	}

	/**
	 * Check given connector.
	 * 
	 * @param connector
	 *            connector to check
	 * @return true if given connector supports send later
	 */
	static final boolean supportSendLater(final short connector) {
		return getConnector(null, connector).supportSendLater();
	}

	/**
	 * Parse a String of "name (number), name (number), number, ..." to an array
	 * of numbers. It will fill this.toFull and this.toNames too.
	 * 
	 * @param reciepients
	 *            reciepients
	 * @return array of reciepients
	 */
	private String[] parseReciepients(final String reciepients) {
		String s = reciepients.trim();
		if (s.endsWith(",")) {
			s = s.substring(0, s.length() - 1);
		}
		String[] ret0 = s.split(",");
		final int e = ret0.length;
		final String[] ret = new String[e];
		final String[] ret1 = new String[e];
		for (int i = 0; i < e; i++) {
			// get number only
			s = ret0[i];
			int j = s.lastIndexOf('<');
			if (j >= 0) {
				int h = s.indexOf('>', j);
				if (h > 0) {
					s = s.substring(j + 1, h);
				}
			}
			ret[i] = s;
			// get name only
			s = ret0[i];
			j = s.lastIndexOf('<');
			if (j >= 0) {
				ret1[i] = s.substring(0, j).trim();
			}
		}
		this.toFull = ret0;
		this.toNames = ret1;
		return ret;
	}

	/**
	 * Clean recipient's phone number from [ -.()].
	 * 
	 * @param recipient
	 *            recipient's mobile number
	 * @return clean number
	 */
	public static final String cleanRecipient(final String recipient) {
		if (recipient == null) {
			return "";
		}
		return recipient.replace(" ", "").replace("-", "").replace(".", "")
				.replace("(", "").replace(")", "").replace("<", "").replace(
						">", "").trim();
	}

	/**
	 * Convert international number to national.
	 * 
	 * @param number
	 *            international number
	 * @return national number
	 */
	static final String international2national(final String number) {
		if (number.startsWith(WebSMS.prefsDefPrefix)) {
			return '0' + number.substring(WebSMS.prefsDefPrefix.length());
		}
		return number;
	}

	/**
	 * Convert international number to old format. Eg. +49123 to 0049123
	 * 
	 * @param number
	 *            international number starting with +
	 * @return international number in old format starting with 00
	 */
	static final String international2oldformat(final String number) {
		if (number.startsWith("+")) {
			return "00" + number.substring(1);
		}
		return number;
	}

	/**
	 * Add sub-connectors to item list, return numer of connectors added.
	 * 
	 * @param connector
	 *            connector to add
	 * @param items
	 *            add names to this list
	 * @param allItems
	 *            list of all connctors
	 * @return number of connectors added
	 */
	static final short getSubConnectors(final short connector,
			final ArrayList<String> items, final String[] allItems) {
		switch (connector) {
		case INNOSEND:
			if (items != null && allItems != null) {
				items.add(allItems[Connector.INNOSEND_FREE]);
				items.add(allItems[Connector.INNOSEND_WO_SENDER]);
				items.add(allItems[Connector.INNOSEND_W_SENDER]);
			}
			return 3;
		case CHERRY:
			if (items != null && allItems != null) {
				items.add(allItems[Connector.CHERRY_WO_SENDER]);
				items.add(allItems[Connector.CHERRY_W_SENDER]);
			}
			return 2;
		case SLOONO: // do not change order here!
			if (items != null && allItems != null) {
				items.add(allItems[Connector.SLOONO_DISCOUNT]);
				items.add(allItems[Connector.SLOONO_BASIC]);
				items.add(allItems[Connector.SLOONO_PRO]);
			}
			return 3;
		default:
			if (items != null && allItems != null) {
				items.add(allItems[connector]);
			}
			return 1;
		}
	}

	/**
	 * Send a Message to Activity or Log.
	 * 
	 * @param msgType
	 *            message type
	 * @param msg
	 *            message
	 */
	protected final void pushMessage(final int msgType, final String msg) {
		final Context c = this.context;
		if (c instanceof WebSMS) {
			WebSMS.pushMessage(msgType, msg);
		} else if (c instanceof IOService) {
			if (msgType == WebSMS.MESSAGE_FREECOUNT) {
				WebSMS.pushMessage(msgType, msg);
			}
			if (msg == null) {
				Log.d(TAG, "null");
			} else {
				Log.d(TAG, msg);
				if (msgType == WebSMS.MESSAGE_LOG) {
					this.failedMessage = msg;
				}
			}
		}
	}

	/**
	 * Send a Message to Activity or Log.
	 * 
	 * @param msgType
	 *            message type
	 * @param msg
	 *            message
	 */
	protected final void pushMessage(final int msgType, final int msg) {
		this.pushMessage(msgType, this.context.getString(msg));
	}

	/**
	 * Send a Message to Activity or Log.
	 * 
	 * @param msgType
	 *            message type
	 * @param msgFront
	 *            message's first part as resID
	 * @param msgTail
	 *            message's last part
	 */
	protected final void pushMessage(final int msgType, final int msgFront,
			final String msgTail) {
		this.pushMessage(msgType, this.context.getString(msgFront) + msgTail);
	}
}
