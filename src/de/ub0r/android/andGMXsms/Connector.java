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
import org.apache.http.client.ClientProtocolException;
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

	/** recipient, numbers only. */
	protected String[] to;
	/** recipient, names only. */
	protected String[] toNames;
	/** recipient, splitted but not stripped. */
	protected String[] toFull;
	/** recipients list. as it comes from the user. */
	protected String tos = "";

	/** text. */
	protected String text;

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

	/**
	 * Send a message to one or more receivers. This is done in background!
	 * 
	 * @param connector
	 *            Connector which should be used.
	 * @param params
	 *            Sending parameters.
	 */
	public static final void send(final short connector, final String[] params) {
		switch (connector) {
		case GMX:
			new ConnectorGMX().execute(params);
			break;
		case O2:
			new ConnectorO2().execute(params);
			break;
		case SIPGATE:
			new ConnectorSipgate().execute(params);
			break;
		default:
			break;
		}
	}

	/**
	 * Send a message to one or more receivers. This is done in background!
	 * 
	 * @param connector
	 *            Connector which should be used.
	 * @param recipients
	 *            Receivers of the message.
	 * @param text
	 *            Text which should be sent.
	 */
	public static final void send(final short connector,
			final String recipients, final String text) {
		String[] params = new String[IDS_SEND];
		params[ID_ID] = ID_SEND;
		params[ID_TEXT] = text;
		params[ID_TO] = recipients;
		Connector.send(connector, params);
	}

	/**
	 * Update (free) message count. This is done in background!
	 * 
	 * @param connector
	 *            Connector which should be used.
	 */
	public static void update(final short connector) {
		switch (connector) {
		case GMX:
			new ConnectorGMX().execute(PARAMS_UPDATE);
			break;
		case O2:
			new ConnectorO2().execute(PARAMS_UPDATE);
			break;
		case SIPGATE:
			new ConnectorSipgate().execute(PARAMS_UPDATE);
		default:
			break;
		}
	}

	/**
	 * Bootstrap a Connector. Like checking settings etc. This is done in
	 * background!
	 * 
	 * @param connector
	 *            Connector which should be used.
	 * @param params
	 *            Parameters the Connector expects
	 */
	public static final void bootstrap(final short connector,
			final String[] params) {
		switch (connector) {
		case GMX:
			new ConnectorGMX().execute(params);
			break;
		case SIPGATE:
			new ConnectorSipgate().execute(params);
		default:
			break;
		}
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
				new InputStreamReader(is));
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
	 * @return the connection
	 * @throws IOException
	 * @throws ClientProtocolException
	 */
	protected static HttpResponse getHttpClient(final String url,
			final ArrayList<Cookie> cookies,
			final ArrayList<BasicNameValuePair> postData, final String userAgent)
			throws ClientProtocolException, IOException {
		HttpClient client = new DefaultHttpClient();
		HttpRequestBase request;
		if (postData == null) {
			request = new HttpGet(url);
		} else {
			request = new HttpPost(url);
			((HttpPost) request).setEntity(new UrlEncodedFormEntity(postData));
		}
		request.setHeader("User-Agent", userAgent);

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
	 * @param text
	 *            text of message.
	 */
	protected static final void saveMessage(final String[] reciepients,
			final String text) {
		for (int i = 0; i < reciepients.length; i++) {
			if (reciepients[i] == null || reciepients[i].length() == 0) {
				continue; // skip empty recipients
			}
			// save sms to content://sms/sent
			ContentValues values = new ContentValues();
			values.put(ConnectorGMX.ADDRESS, reciepients[i]);
			values.put(ConnectorGMX.READ, 1);
			values.put(ConnectorGMX.TYPE, ConnectorGMX.MESSAGE_TYPE_SENT);
			values.put(ConnectorGMX.BODY, text);
			AndGMXsms.me.getContentResolver().insert(
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
		String to = this.tos;
		String[] numbers = Connector.parseReciepients(to);
		final String defPrefix = AndGMXsms.prefsDefPrefix;
		// fix number prefix
		for (int i = 0; i < numbers.length; i++) {
			String t = numbers[i];
			if (t != null) {
				if (!t.startsWith("+")) {
					if (t.startsWith("00")) {
						t = "+" + t.substring(2);
					} else if (t.startsWith("0")) {
						t = defPrefix + t.substring(1);
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
			this.publishProgress((Boolean) null);
			ret = this.updateMessages();
		} else if (t == ID_BOOSTR) {
			this.publishProgress((Boolean) null);
			ret = this.doBootstrap(params);
		} else if (t == ID_SEND) {
			this.notificationID = getNotificationID();
			this.text = params[ID_TEXT];
			this.tos = params[ID_TO];
			// this.to = getReceivers(params);
			this.prepareSend();
			this.publishProgress(true);
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
		boolean p;
		if (progress == null || progress.length == 0 || progress[0] == null) {
			p = false;
		} else {
			p = progress[0];
		}
		if (AndGMXsms.dialog != null) {
			try {
				AndGMXsms.dialog.dismiss();
			} catch (Exception e) {
				// do nothing
			}
		}
		final String t = this.type;
		if (t == ID_UPDATE) {
			AndGMXsms.me.setProgressBarIndeterminateVisibility(true);
		} else if (t == ID_BOOSTR) {
			AndGMXsms.dialogString = AndGMXsms.me.getResources().getString(
					R.string.bootstrap_);
			AndGMXsms.dialog = ProgressDialog.show(AndGMXsms.me, null,
					AndGMXsms.dialogString, true);
		} else if (t == ID_SEND) {
			this.displayNotification(!p);
		}
	}

	/**
	 * Display a notification for this mesage.
	 * 
	 * @param failed
	 *            send failed?
	 */
	private void displayNotification(final boolean failed) {
		NotificationManager mNotificationMgr = (NotificationManager) IOService.me
				.getSystemService(Context.NOTIFICATION_SERVICE);
		Notification notification = null;
		String rcvs = this.tos.trim();
		if (rcvs.endsWith(",")) {
			rcvs = rcvs.substring(0, rcvs.length() - 1);
		}
		if (failed) {
			notification = new Notification(
					android.R.drawable.stat_notify_error, "failed", System
							.currentTimeMillis());
			final Intent i = new Intent(IOService.me, AndGMXsms.class);
			i.setData(Uri.parse("sms://" + Uri.encode(this.tos) + "/"
					+ Uri.encode(this.text)));
			final PendingIntent contentIntent = PendingIntent.getActivity(
					IOService.me, 0, i, 0);
			if (this.failedMessage == null) {
				this.failedMessage = "";
			}
			notification.setLatestEventInfo(IOService.me, "failed send: "
					+ this.failedMessage, rcvs + "\n" + this.text,
					contentIntent);
			notification.flags |= Notification.FLAG_AUTO_CANCEL;
		} else {
			notification = new Notification(android.R.drawable.stat_sys_upload,
					"", System.currentTimeMillis());
			final PendingIntent contentIntent = PendingIntent.getActivity(
					IOService.me, 0, new Intent(IOService.me, AndGMXsms.class),
					0);
			notification.setLatestEventInfo(IOService.me, "sending: " + rcvs,
					this.text, contentIntent);
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
		if (t == ID_UPDATE) {
			AndGMXsms.me.setProgressBarIndeterminateVisibility(false);
		} else {
			AndGMXsms.dialogString = null;
			if (AndGMXsms.dialog != null) {
				try {
					AndGMXsms.dialog.dismiss();
					AndGMXsms.dialog = null;
				} catch (Exception e) {
					System.gc();
				}
			}
		}
		if (t == ID_SEND) {
			if (!result) {
				this.displayNotification(true);
			}
		}
	}

	/**
	 * Parse a String of "name (number), name (number), number, ..." to
	 * addresses.
	 * 
	 * @param reciepients
	 *            reciepients
	 * @return array of reciepients
	 */
	private static String[] parseReciepients(final String reciepients) {
		int i = 0;
		int p = reciepients.indexOf(',');
		while (p >= 0) {
			++i;
			if (p == reciepients.length()) {
				p = -1;
			} else {
				p = reciepients.indexOf(',', p + 1);
			}
		}
		if (i < 2) {
			i = 2;
		}
		String[] ret = new String[i + 1];
		p = 0;
		int p2 = reciepients.indexOf(',', p + 1);
		i = 0;
		while (p >= 0) {
			if (p == 0) {
				p--;
			}
			if (p2 > 0) {
				ret[i] = reciepients.substring(p + 1, p2).trim();
			} else {
				ret[i] = reciepients.substring(p + 1).trim();
			}
			if (p == -1) {
				p++;
			}

			if (p == reciepients.length()) {
				p = -1;
			} else {
				p = reciepients.indexOf(',', p + 1);
				if (p == reciepients.length()) {
					p2 = -1;
				} else {
					p2 = reciepients.indexOf(',', p + 1);
				}
				++i;
			}
		}

		for (i = 0; i < ret.length; i++) {
			if (ret[i] == null) {
				continue;
			}
			p = ret[i].lastIndexOf('(');
			if (p >= 0) {
				p2 = ret[i].indexOf(')', p);
				if (p2 < 0) {
					ret[i] = null;
				} else {
					ret[i] = ret[i].substring(p + 1, p2);
				}
			}
		}

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
		if (this.type == ID_SEND || AndGMXsms.me == null) {
			if (msg == null) {
				Log.d(TAG, "null");
			} else {
				Log.d(TAG, msg);
				if (msgType == AndGMXsms.MESSAGE_LOG) {
					this.failedMessage = msg;
				}
			}
		} else {
			AndGMXsms.pushMessage(msgType, msg);
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
		Context c = AndGMXsms.me;
		if (this.type == ID_SEND || c == null) {
			c = IOService.me;
		}
		final String s = c.getString(msg);
		if (this.type == ID_SEND) {
			Log.d(TAG, s);
			if (msgType == AndGMXsms.MESSAGE_LOG
					&& (msg != R.string.log_error || this.failedMessage == null)) {
				this.failedMessage = s;
			}
		} else {
			AndGMXsms.pushMessage(msgType, c);
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
		Context c = AndGMXsms.me;
		if (this.type == ID_SEND || c == null) {
			c = IOService.me;
		}
		final String s = c.getString(msgFront);
		if (this.type == ID_SEND) {
			Log.d(TAG, s + msgTail);
			if (msgType == AndGMXsms.MESSAGE_LOG) {
				this.failedMessage = s + msgTail;
			}
		} else {
			AndGMXsms.pushMessage(msgType, c + msgTail);
		}
	}
}
