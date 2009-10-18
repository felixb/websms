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

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.util.ArrayList;

import org.apache.http.HttpResponse;
import org.apache.http.cookie.Cookie;
import org.apache.http.cookie.MalformedCookieException;
import org.apache.http.message.BasicNameValuePair;

import android.util.Log;

/**
 * AsyncTask to manage IO to O2 API.
 * 
 * @author flx
 */
public class ConnectorO2 extends Connector {
	/** Tag for output. */
	private static final String TAG = "WebSMS.o2";

	/** Index in some arrays for o2online.de. */
	private static final short O2_DE = 0;
	/** Index in some arrays for o2Online.ie. */
	private static final short O2_IE = 1;

	/**
	 * URLs for this Connector. First dimension: DE/IE/..? Second dimension:
	 * urls.
	 */
	private static final String[][] URLS = { { // .de
					"https://login.o2online.de/loginRegistration"
							+ "/loginAction.do"
							+ "?_flowId=login&o2_type=asp&o2_label=login/"
							+ "comcenter-login&scheme=http&port=80&server=email"
							+ ".o2online.de&url=%2Fssomanager.osp%3FAPIID%3D"
							+ "AUTH-WEBSSO%26TargetApp%3D%2Fsmscenter_new.osp"
							+ "%253f%26o2_type"
							+ "%3Durl%26o2_label%3Dweb2sms-o2online",
					"https://login.o2online.de/loginRegistration/loginAction.do",
					"http://email.o2online.de:80/ssomanager.osp"
							+ "?APIID=AUTH-WEBSSO&TargetApp=/smscenter_new.osp"
							+ "?&o2_type=url&o2_label=web2sms-o2online",
					"https://email.o2online.de/smscenter_new.osp"
							+ "?Autocompletion=1&MsgContentID=-1",
					"https://email.o2online.de/smscenter_send.osp" }, // end .de
			{ // ie
			"???1", "???2", "???3", "???4", "???5" } };

	/**
	 * Strings for this Connector. First dimension: DE/IE/..? Second dimension:
	 * string.
	 */
	private static final String[][] STRINGS = { { // .de
			"Frei-SMS: ", // free sms
					"Web2SMS", // web2sms
					"SMS wurde erfolgreich versendet" // successful send
			}, // end .de
			{ // .ie
			"Number of free text messages remaining this month: ", "Web2SMS",
					"???" } };

	/** HTTP Useragent. */
	private static final String TARGET_AGENT = "Mozilla/5.0 (Windows; U;"
			+ " Windows NT 5.1; de; rv:1.9.0.9) Gecko/2009040821"
			+ " Firefox/3.0.9 (.NET CLR 3.5.30729)";

	/**
	 * Extract _flowExecutionKey from HTML output.
	 * 
	 * @param html
	 *            input
	 * @return _flowExecutionKey
	 */
	private static String getFlowExecutionkey(final String html) {
		String ret = "";
		int i = html.indexOf("name=\"_flowExecutionKey\" value=\"");
		if (i > 0) {
			int j = html.indexOf("\"", i + 35);
			if (j >= 0) {
				ret = html.substring(i + 32, j);
			}
		}
		return ret;
	}

	/**
	 * Send data.
	 * 
	 * @return successful?
	 */
	private boolean sendData() {
		// Operator of user. Selected by countrycode.
		short operator;
		// switch operator
		if (AndGMXsms.prefsSender.startsWith("+49")) {
			operator = O2_DE;
		} else if (AndGMXsms.prefsSender.startsWith("+353")) {
			operator = O2_IE;
		} else {
			AndGMXsms.sendMessage(AndGMXsms.MESSAGE_LOG,
					R.string.log_error_prefix);
			return false;
		}

		// do IO
		try { // get Connection
			ArrayList<Cookie> cookies = new ArrayList<Cookie>();
			HttpResponse response = getHttpClient(URLS[operator][0], cookies,
					null, TARGET_AGENT, null);
			int resp = response.getStatusLine().getStatusCode();
			if (resp != HttpURLConnection.HTTP_OK) {
				AndGMXsms.sendMessage(AndGMXsms.MESSAGE_LOG, AndGMXsms.me
						.getResources().getString(R.string.log_error_http)
						+ resp);
				return false;
			}
			updateCookies(cookies, response.getAllHeaders(), URLS[operator][0]);
			String htmlText = stream2String(response.getEntity().getContent());
			String flowExecutionKey = ConnectorO2.getFlowExecutionkey(htmlText);
			htmlText = null;

			// post data
			ArrayList<BasicNameValuePair> postData = new ArrayList<BasicNameValuePair>(
					4);
			postData.add(new BasicNameValuePair("_flowExecutionKey",
					flowExecutionKey));
			postData.add(new BasicNameValuePair("loginName", "0"
					+ AndGMXsms.prefsSender.substring(3)));
			postData.add(new BasicNameValuePair("password",
					AndGMXsms.prefsPasswordO2));
			postData.add(new BasicNameValuePair("_eventId", "login"));
			response = getHttpClient(URLS[operator][1], cookies, postData,
					TARGET_AGENT, URLS[operator][0]);
			postData = null;
			resp = response.getStatusLine().getStatusCode();
			if (resp != HttpURLConnection.HTTP_OK) {
				AndGMXsms.sendMessage(AndGMXsms.MESSAGE_LOG, AndGMXsms.me
						.getResources().getString(R.string.log_error_http)
						+ resp);
				return false;
			}
			resp = cookies.size();
			updateCookies(cookies, response.getAllHeaders(), URLS[operator][1]);
			if (resp == cookies.size()) {
				AndGMXsms.sendMessage(AndGMXsms.MESSAGE_LOG,
						R.string.log_error_pw);
				htmlText = stream2String(response.getEntity().getContent());
				return false;
			}
			response = getHttpClient(URLS[operator][2], cookies, null,
					TARGET_AGENT, URLS[operator][1]);
			resp = response.getStatusLine().getStatusCode();
			if (resp != HttpURLConnection.HTTP_OK) {
				AndGMXsms.sendMessage(AndGMXsms.MESSAGE_LOG, AndGMXsms.me
						.getResources().getString(R.string.log_error_http)
						+ resp);
				return false;
			}
			updateCookies(cookies, response.getAllHeaders(), URLS[operator][2]);

			response = getHttpClient(URLS[operator][3], cookies, null,
					TARGET_AGENT, URLS[operator][2]);
			resp = response.getStatusLine().getStatusCode();
			if (resp != HttpURLConnection.HTTP_OK) {
				AndGMXsms.sendMessage(AndGMXsms.MESSAGE_LOG, AndGMXsms.me
						.getResources().getString(R.string.log_error_http)
						+ resp);
				return false;
			}
			updateCookies(cookies, response.getAllHeaders(), URLS[operator][3]);
			htmlText = stream2String(response.getEntity().getContent());
			int i = htmlText.indexOf(STRINGS[operator][0]);
			if (i > 0) {
				int j = htmlText.indexOf(STRINGS[operator][1], i);
				if (j > 0) {
					AndGMXsms.SMS_FREE[O2][AndGMXsms.SMS_FREE_COUNT] = Integer
							.parseInt(htmlText.substring(i + 9, j).trim());
					AndGMXsms.sendMessage(AndGMXsms.MESSAGE_FREECOUNT, null);
				}
			}

			if (this.text != null && this.tos != null) {
				postData = new ArrayList<BasicNameValuePair>(15);
				postData.add(new BasicNameValuePair("SMSTo", this.tos));
				postData.add(new BasicNameValuePair("SMSText", this.text));
				postData.add(new BasicNameValuePair("SMSFrom", ""));
				postData.add(new BasicNameValuePair("Frequency", "5"));

				String[] st = htmlText.split("<input type=\"Hidden\" ");
				htmlText = null;
				for (String s : st) {
					if (s.startsWith("name=")) {
						String[] subst = s.split("\"", 5);
						if (subst.length >= 4) {
							postData.add(new BasicNameValuePair(subst[1],
									subst[3]));
						}
					}
				}
				st = null;

				response = getHttpClient(URLS[operator][4], cookies, postData,
						TARGET_AGENT, URLS[operator][3]);
				postData = null;
				resp = response.getStatusLine().getStatusCode();
				if (resp != HttpURLConnection.HTTP_OK) {
					AndGMXsms.sendMessage(AndGMXsms.MESSAGE_LOG, AndGMXsms.me
							.getResources().getString(R.string.log_error_http)
							+ resp);
					return false;
				}
				htmlText = stream2String(response.getEntity().getContent());
				if (htmlText.indexOf(STRINGS[operator][2]) < 0) {
					// check output html for success message
					return false;
				}
			}
		} catch (IOException e) {
			Log.e(TAG, null, e);
			AndGMXsms.sendMessage(AndGMXsms.MESSAGE_LOG, e.toString());
			return false;
		} catch (URISyntaxException e) {
			Log.e(TAG, null, e);
			AndGMXsms.sendMessage(AndGMXsms.MESSAGE_LOG, e.toString());
			return false;
		} catch (MalformedCookieException e) {
			Log.e(TAG, null, e);
			AndGMXsms.sendMessage(AndGMXsms.MESSAGE_LOG, e.toString());
			return false;
		}
		return true;
	}

	/**
	 * Get free sms count.
	 * 
	 * @return ok?
	 */
	@Override
	protected final boolean updateMessages() {
		return this.sendData();
	}

	/**
	 * Send sms.
	 * 
	 * @return ok?
	 */
	@Override
	protected final boolean sendMessage() {
		AndGMXsms.sendMessage(AndGMXsms.MESSAGE_DISPLAY_ADS, null);
		int j = 0;
		for (int i = 0; i < this.to.length; i++) {
			if (this.to[i] != null && this.to[i].length() > 1) {
				if (j > 1) {
					this.tos += ", ";
				}
				this.tos += this.to[i];
			}
		}
		this.publishProgress((Boolean) null);
		if (!this.sendData()) {
			// failed!
			AndGMXsms.sendMessage(AndGMXsms.MESSAGE_LOG, AndGMXsms.me
					.getResources().getString(R.string.log_error));
			return false;
		} else {
			// result: ok
			AndGMXsms.sendMessage(AndGMXsms.MESSAGE_RESET, null);
			saveMessage(this.to, this.text);
			return true;
		}
	}
}
