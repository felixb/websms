/*
 * Copyright (C) 2009 Felix Bechstein
 * 
 * This file is part of AndGMXsms.
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
import android.app.NotificationManager;
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

	/** HTTP Response 503. */
	static final int HTTP_SERVICE_UNAVAILABLE = 503;

	/** Connector type: GMX. */
	static final short GMX = 0;
	/** Connector type: O2. */
	static final short O2 = 1;
	/** Connector type: Sipgate. */
	static final short SIPGATE = 2;

	/** ID of Param-ID. This is to distinguish between different calls. */
	static final int ID_ID = 0;
	/** ID of text in array. */
	static final int ID_TEXT = 1;
	/** ID of recipient in array. */
	static final int ID_TO = 2;

	/** ID of mail in array. */
	static final int ID_MAIL = 1;
	/** ID of password in array. */
	static final int ID_PW = 2;

	/** Number of IDs in array for sms send. */
	static final int IDS_SEND = 3;

	/** ID_ID for sending a message. */
	static final String ID_SEND = "0";
	/** ID_ID for updating message count. */
	static final String ID_UPDATE = "1";
	/** ID_ID for bootstrapping. */
	public static final String ID_BOOSTR = "2";

	/** Parameters for updating message count. */
	static final String[] PARAMS_UPDATE = { ID_UPDATE };

	/** Standard buffer size. */
	public static final int BUFSIZE = 1024;

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

	/** User. */
	protected String user;
	/** Password. */
	protected String password;
	/** Default prefix. */
	private String defPrefix;
	/** Sender. */
	protected String sender;

	/** Connector is bootstrapping. */
	static boolean inBootstrap = false;

	/** Type of IO Op. */
	protected String type;

	/** ID of my notification. */
	private int notificationID = 0;
	/** Next notification ID. */
	private static int nextNotificationID = 0;

	/** Message to log to the user. */
	protected String failedMessage = null;

	/** Context IO is running from. */
	protected Context context;

	/** Concurrent updates running. */
	private static int countUpdates = 0;

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
		final Connector c = getConnector(con, connector);
		if (c != null) {
			c.execute(params);
		}
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
	 */
	public static final void send(final Context con, final short connector,
			final String recipients, final String text) {
		String[] params = new String[IDS_SEND];
		params[ID_ID] = ID_SEND;
		params[ID_TEXT] = text;
		params[ID_TO] = recipients;
		Connector.send(con, connector, params);
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
		case GMX:
			c = new ConnectorGMX();
			c.user = WebSMS.prefsUserGMX;
			c.password = WebSMS.prefsPasswordGMX;
			break;
		case O2:
			c = new ConnectorO2();
			c.user = WebSMS.prefsSender;
			c.password = WebSMS.prefsPasswordO2;
			break;
		case SIPGATE:
			c = new ConnectorSipgate();
			c.user = WebSMS.prefsUserSipgate;
			c.password = WebSMS.prefsPasswordSipgate;
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
	protected static final String stream2String(final InputStream is)
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
			((HttpPost) request).setEntity(new UrlEncodedFormEntity(postData));
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
		for (int i = 0; i < reciepients.length; i++) {
			if (reciepients[i] == null || reciepients[i].length() == 0) {
				continue; // skip empty recipients
			}
			// save sms to content://sms/sent
			ContentValues values = new ContentValues();
			values.put(ConnectorGMX.ADDRESS, reciepients[i]);
			values.put(ConnectorGMX.READ, 1);
			values.put(ConnectorGMX.TYPE, ConnectorGMX.MESSAGE_TYPE_SENT);
			values.put(ConnectorGMX.BODY, msgText);
			this.context.getContentResolver().insert(
					Uri.parse("content://sms/sent"), values);
		}
	}

	/**
	 * Get free sms count.
	 * 
	 * @return ok?
	 */
	protected abstract boolean updateMessages();

	/**
	 * Bootstrap: Get preferences. This default implementation odes nothing!
	 * 
	 * @param params
	 *            Parameters
	 * @return ok?
	 */
	protected boolean doBootstrap(final String[] params) {
		return false;
	}

	/**
	 * Send sms.
	 * 
	 * @return ok?
	 */
	protected abstract boolean sendMessage();

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
		boolean ret = false;
		String t;
		if (params == null || params[ID_ID] == null) {
			t = ID_UPDATE;
		} else {
			t = params[ID_ID];
		}
		this.type = t;
		if (t == ID_UPDATE) {
			this.publishProgress(false);
			ret = this.updateMessages();
		} else if (t == ID_BOOSTR) {
			this.publishProgress(false);
			ret = this.doBootstrap(params);
		} else if (t == ID_SEND) {
			this.notificationID = getNotificationID();
			this.text = params[ID_TEXT];
			this.tos = params[ID_TO];
			// this.to = getReceivers(params);
			this.prepareSend();
			this.publishProgress(false);
			ret = this.sendMessage();
		}
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
		if (t == ID_UPDATE) {
			((WebSMS) c).setProgressBarIndeterminateVisibility(true);
		} else if (t == ID_BOOSTR) {
			if (WebSMS.dialog != null) {
				try {
					WebSMS.dialog.dismiss();
				} catch (Exception e) {
					// do nothing
				}
			}
			WebSMS.dialogString = c.getString(R.string.bootstrap_);
			WebSMS.dialog = ProgressDialog.show(c, null,
					WebSMS.dialogString, true);
		} else if (t == ID_SEND) {
			this.displayNotification(false);
		}
	}

	/**
	 * Display a notification for this mesage.
	 * 
	 * @param failed
	 *            send failed?
	 */
	private void displayNotification(final boolean failed) {
		final Context c = this.context;
		NotificationManager mNotificationMgr = (NotificationManager) c
				.getSystemService(Context.NOTIFICATION_SERVICE);
		Notification notification = null;
		String rcvs = this.tos.trim();
		if (rcvs.endsWith(",")) {
			rcvs = rcvs.substring(0, rcvs.length() - 1);
		}
		if (failed) {
			notification = new Notification(R.drawable.stat_notify_sms_fail, c
					.getString(R.string.notify_failed_), System
					.currentTimeMillis());
			final Intent i = new Intent(c, WebSMS.class);
			if (this.failedMessage == null) {
				this.failedMessage = c.getString(R.string.notify_failed_);
			}
			i.setData(Uri.parse("sms://" + Uri.encode(this.tos) + "/"
					+ Uri.encode(this.text + "/" + this.failedMessage)));
			final PendingIntent contentIntent = PendingIntent.getActivity(c, 0,
					i, 0);
			notification.setLatestEventInfo(c, c
					.getString(R.string.notify_failed)
					+ this.failedMessage, rcvs + ": " + this.text,
					contentIntent);
			notification.flags |= Notification.FLAG_AUTO_CANCEL;
		} else {
			notification = new Notification(R.drawable.stat_notify_sms_out, "",
					System.currentTimeMillis());
			final PendingIntent contentIntent = PendingIntent.getActivity(c, 0,
					new Intent(c, WebSMS.class), 0);
			notification.setLatestEventInfo(c, c
					.getString(R.string.notify_sending)
					+ rcvs, this.text, contentIntent);
		}
		mNotificationMgr.notify(this.notificationID, notification);
	}

	/**
	 * Push data back to GUI. Close progress dialog.
	 * 
	 * @param result
	 *            result
	 */
	@Override
	protected final void onPostExecute(final Boolean result) {
		final String t = this.type;
		--countUpdates;
		if (t == ID_UPDATE && countUpdates == 0) {
			((WebSMS) this.context)
					.setProgressBarIndeterminateVisibility(false);
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
		if (t == ID_SEND) {
			if (!result) {
				this.displayNotification(true);
			} else {
				NotificationManager mNotificationMgr = (NotificationManager) this.context
						.getSystemService(Context.NOTIFICATION_SERVICE);
				mNotificationMgr.cancel(this.notificationID);
			}
		}
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
			int j = s.lastIndexOf('(');
			if (j >= 0) {
				int h = s.indexOf(')', j);
				if (h > 0) {
					s = s.substring(j + 1, h);
				}
			}
			ret[i] = s;
			// get name only
			s = ret0[i];
			j = s.lastIndexOf('(');
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
				.replace("(", "").replace(")", "").trim();
	}

	/**
	 * Get a fresh and unique ID for a new notification.
	 * 
	 * @return return the ID
	 */
	private static synchronized int getNotificationID() {
		++nextNotificationID;
		return nextNotificationID;
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
		final Context c = this.context;
		final String s = c.getString(msg);
		if (c instanceof WebSMS) {
			WebSMS.pushMessage(msgType, s);
		} else if (c instanceof IOService) {
			Log.d(TAG, s);
			if (msgType == WebSMS.MESSAGE_LOG
					&& (msg != R.string.log_error || this.failedMessage == null)) {
				this.failedMessage = s;
			}
		}
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
		final Context c = this.context;
		final String s = c.getString(msgFront);
		if (c instanceof WebSMS) {
			WebSMS.pushMessage(msgType, s + msgTail);
		} else if (c instanceof IOService) {
			Log.d(TAG, s + msgTail);
			if (msgType == WebSMS.MESSAGE_LOG) {
				this.failedMessage = s + msgTail;
			}
		}
	}
}
