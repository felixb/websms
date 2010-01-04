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
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Calendar;

import org.apache.http.HttpResponse;
import org.apache.http.cookie.Cookie;
import org.apache.http.cookie.MalformedCookieException;
import org.apache.http.message.BasicNameValuePair;

import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.format.DateFormat;
import android.util.Log;

/**
 * AsyncTask to manage IO to O2 API.
 * 
 * @author flx
 */
public class ConnectorO2 extends Connector {
	/** Tag for output. */
	private static final String TAG = "WebSMS.o2";

	/** Custom Dateformater. */
	private static final String DATEFORMAT = "yyyy,MM,dd,kk,mm,00";

	/** Index in some arrays for o2online.de. */
	private static final short O2_DE = 0;
	/** Index in some arrays for o2Online.ie. */
	private static final short O2_IE = 1;

	/** URL before login. */
	private static final int URL_PRELOGIN = 0;
	/** URL for login. */
	private static final int URL_LOGIN = 1;
	/** URL of captcha. */
	private static final int URL_CAPTCHA = 2;
	/** URL for solving captcha. */
	private static final int URL_SOLVECAPTCHA = 3;
	/** URL for sms center. */
	private static final int URL_SMSCENTER = 4;
	/** URL before sending. */
	private static final int URL_PRESEND = 5;
	/** URL for sending. */
	private static final int URL_SEND = 6;
	/** URL for sending later. */
	private static final int URL_SCHEDULE = 7;

	/** Check for free sms. */
	private static final int CHECK_FREESMS = 0;
	/** Check for web2sms. */
	private static final int CHECK_WEB2SMS = 1;
	/** Check if message was sent. */
	private static final int CHECK_SENT = 2;
	/** Check if captcha was solved wrong. */
	private static final int CHECK_WRONGCAPTCHA = 3;

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
					"https://login.o2online.de/loginRegistration"
							+ "/loginAction.do",
					"https://login.o2online.de/loginRegistration/jcaptcha",
					"https://login.o2online.de/loginRegistration"
							+ "/loginAction.do",
					"http://email.o2online.de:80/ssomanager.osp"
							+ "?APIID=AUTH-WEBSSO&TargetApp=/smscenter_new.osp"
							+ "?&o2_type=url&o2_label=web2sms-o2online",
					"https://email.o2online.de/smscenter_new.osp"
							+ "?Autocompletion=1&MsgContentID=-1",
					"https://email.o2online.de/smscenter_send.osp",
					"https://email.o2online.de/smscenter_schedule.osp" },
			// end .de
			{ // ie
			"???1", "???2", "???3", "???4", "???5" } };

	/**
	 * Strings for this Connector. First dimension: DE/IE/..? Second dimension:
	 * string.
	 */
	private static final String[][] STRINGS = { { // .de
			"Frei-SMS: ", // free sms
					"Web2SMS", // web2sms
					"/app_pic/ico_mail_send_ok.gif", // successful send
					"Sie haben einen falschen Code eingegeben." // wrong code
			}, // end .de
			{ // .ie
			"Number of free text messages remaining this month: ", "Web2SMS",
					"???" } };

	/** HTTP Useragent. */
	private static final String TARGET_AGENT = "Mozilla/5.0 (Windows; U;"
			+ " Windows NT 5.1; de; rv:1.9.0.9) Gecko/2009040821"
			+ " Firefox/3.0.9 (.NET CLR 3.5.30729)";

	/** Current Captcha to solve. */
	static Drawable captcha = null;
	/** Solved Captcha. */
	static String captchaSolve = null;
	/** Object to sync with. */
	static final Object CAPTCHA_SYNC = new Object();

	/** Global html response. */
	private String htmlText;

	/** Cookies. */
	private static final ArrayList<Cookie> COOKIES = new ArrayList<Cookie>();

	/**
	 * Create a o2 Connector.
	 * 
	 * @param u
	 *            username
	 * @param p
	 *            password
	 */
	protected ConnectorO2(final String u, final String p) {
		super(u, p, O2);
	}

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
	 * Get Operator Code.
	 * 
	 * @return operator
	 * @throws WebSMSException
	 *             WebSMSException
	 */
	private short getOperator() throws WebSMSException {
		short operator;
		// switch operator
		final String sndr = this.sender;
		if (sndr.startsWith("+49")) {
			operator = O2_DE;
		} else if (sndr.startsWith("+353")) {
			operator = O2_IE;
		} else {
			throw new WebSMSException(this.context, R.string.log_error_prefix);
		}
		return operator;
	}

	/**
	 * Load Captcha and wait for user input to solve it.
	 * 
	 * @param operator
	 *            operator to use.
	 * @param flow
	 *            _flowExecutionKey
	 * @return true if captcha was solved
	 * @throws IOException
	 *             IOException
	 * @throws MalformedCookieException
	 *             MalformedCookieException
	 * @throws URISyntaxException
	 *             URISyntaxException
	 * @throws WebSMSException
	 *             WebSMSException
	 */
	private boolean solveCaptcha(final short operator, final String flow)
			throws IOException, MalformedCookieException, URISyntaxException,
			WebSMSException {
		HttpResponse response = getHttpClient(URLS[operator][URL_CAPTCHA],
				COOKIES, null, TARGET_AGENT, URLS[operator][URL_LOGIN]);
		int resp = response.getStatusLine().getStatusCode();
		if (resp != HttpURLConnection.HTTP_OK) {
			throw new WebSMSException(this.context, R.string.log_error_http, ""
					+ resp);
		}
		updateCookies(COOKIES, response.getAllHeaders(),
				URLS[operator][URL_CAPTCHA]);
		captcha = new BitmapDrawable(response.getEntity().getContent());
		this.pushMessage(WebSMS.MESSAGE_ANTICAPTCHA, null);
		try {
			synchronized (CAPTCHA_SYNC) {
				CAPTCHA_SYNC.wait();
			}
		} catch (InterruptedException e) {
			Log.e(TAG, null, e);
			return false;
		}
		// got user response, try to solve captcha
		captcha = null;
		final ArrayList<BasicNameValuePair> postData = new ArrayList<BasicNameValuePair>(
				3);
		postData.add(new BasicNameValuePair("_flowExecutionKey", flow));
		postData.add(new BasicNameValuePair("_eventId", "submit"));
		postData.add(new BasicNameValuePair("riddleValue", captchaSolve));
		response = getHttpClient(URLS[operator][URL_SOLVECAPTCHA], COOKIES,
				postData, TARGET_AGENT, URLS[operator][URL_LOGIN]);
		resp = response.getStatusLine().getStatusCode();
		if (resp != HttpURLConnection.HTTP_OK) {
			throw new WebSMSException(this.context, R.string.log_error_http, ""
					+ resp);
		}
		updateCookies(COOKIES, response.getAllHeaders(),
				URLS[operator][URL_CAPTCHA]);
		final String htmlText = stream2str(response.getEntity().getContent());
		if (htmlText.indexOf(STRINGS[operator][CHECK_WRONGCAPTCHA]) > 0) {
			throw new WebSMSException(this.context,
					R.string.log_error_wrongcaptcha);
		}
		return true;
	}

	/**
	 * Login to O2.
	 * 
	 * @param operator
	 *            operator to use.
	 * @param flow
	 *            _flowExecutionKey
	 * @return true if logged in
	 * @throws IOException
	 *             IOException
	 * @throws MalformedCookieException
	 *             MalformedCookieException
	 * @throws URISyntaxException
	 *             URISyntaxException
	 * @throws WebSMSException
	 *             WebSMSException
	 */
	private boolean login(final short operator, final String flow)
			throws IOException, MalformedCookieException, URISyntaxException,
			WebSMSException {
		// post data
		final ArrayList<BasicNameValuePair> postData = new ArrayList<BasicNameValuePair>(
				4);
		postData.add(new BasicNameValuePair("_flowExecutionKey", flow));
		postData.add(new BasicNameValuePair("loginName", this.user));
		postData.add(new BasicNameValuePair("password", this.password));
		postData.add(new BasicNameValuePair("_eventId", "login"));
		HttpResponse response = getHttpClient(URLS[operator][URL_LOGIN],
				COOKIES, postData, TARGET_AGENT, URLS[operator][URL_PRELOGIN]);
		int resp = response.getStatusLine().getStatusCode();
		if (resp != HttpURLConnection.HTTP_OK) {
			throw new WebSMSException(this.context, R.string.log_error_http, ""
					+ resp);
		}
		resp = COOKIES.size();
		updateCookies(COOKIES, response.getAllHeaders(),
				URLS[operator][URL_LOGIN]);
		if (resp == COOKIES.size()) {
			this.htmlText = stream2str(response.getEntity().getContent());
			response = null;
			if (this.htmlText.indexOf("captcha") > 0) {
				final String newFlow = getFlowExecutionkey(this.htmlText);
				this.htmlText = null;
				if (!(this.context instanceof WebSMS)
						|| !this.solveCaptcha(operator, newFlow)) {
					throw new WebSMSException(this.context,
							R.string.log_error_captcha);
				}
			} else {
				throw new WebSMSException(this.context, R.string.log_error_pw);
			}
		}
		return true;
	}

	/**
	 * Format values from calendar to minimum 2 digits.
	 * 
	 * @param cal
	 *            calendar
	 * @param f
	 *            field
	 * @return value as string
	 */
	private static String getTwoDigitsFromCal(final Calendar cal, final int f) {
		int r = cal.get(f);
		if (f == Calendar.MONTH) {
			++r;
		}
		if (r < 10) {
			return "0" + r;
		} else {
			return "" + r;
		}
	}

	/**
	 * Send SMS.
	 * 
	 * @param operator
	 *            operator to use.
	 * @return true if send in
	 * @throws IOException
	 *             IOException
	 * @throws MalformedCookieException
	 *             MalformedCookieException
	 * @throws URISyntaxException
	 *             URISyntaxException
	 * @throws WebSMSException
	 *             WebSMSException
	 */
	private boolean sendToO2(final short operator) throws IOException,
			MalformedCookieException, URISyntaxException, WebSMSException {
		ArrayList<BasicNameValuePair> postData = new ArrayList<BasicNameValuePair>();
		String[] recvs = this.to;
		final int e = recvs.length;
		StringBuilder toBuf = new StringBuilder(recvs[0]);
		for (int j = 1; j < e; j++) {
			toBuf.append(", ");
			toBuf.append(recvs[j]);
		}
		postData.add(new BasicNameValuePair("SMSTo", toBuf.toString()));
		toBuf = null;
		recvs = null;
		postData.add(new BasicNameValuePair("SMSText", this.text));
		if (this.customSender != null) {
			postData.add(new BasicNameValuePair("SMSFrom", this.customSender));
			if (this.customSender.length() == 0) {
				postData.add(new BasicNameValuePair("FlagAnonymous", "1"));
			} else {
				postData.add(new BasicNameValuePair("FlagAnonymous", "0"));
				postData.add(new BasicNameValuePair("FlagDefSender", "1"));
			}
			postData.add(new BasicNameValuePair("FlagDefSender", "0"));
		} else {
			postData.add(new BasicNameValuePair("SMSFrom", ""));
			postData.add(new BasicNameValuePair("FlagDefSender", "1"));
		}
		postData.add(new BasicNameValuePair("Frequency", "5"));
		if (this.flashSMS) {
			postData.add(new BasicNameValuePair("FlagFlash", "1"));
		} else {
			postData.add(new BasicNameValuePair("FlagFlash", "0"));
		}
		String url = URLS[operator][URL_SEND];
		if (this.sendLater > 0) {
			url = URLS[operator][URL_SCHEDULE];
			final Calendar cal = Calendar.getInstance();
			cal.setTimeInMillis(this.sendLater);
			postData.add(new BasicNameValuePair("StartDateDay",
					getTwoDigitsFromCal(cal, Calendar.DAY_OF_MONTH)));
			postData.add(new BasicNameValuePair("StartDateMonth",
					getTwoDigitsFromCal(cal, Calendar.MONTH)));
			postData.add(new BasicNameValuePair("StartDateYear",
					getTwoDigitsFromCal(cal, Calendar.YEAR)));
			postData.add(new BasicNameValuePair("StartDateHour",
					getTwoDigitsFromCal(cal, Calendar.HOUR_OF_DAY)));
			postData.add(new BasicNameValuePair("StartDateMin",
					getTwoDigitsFromCal(cal, Calendar.MINUTE)));
			postData.add(new BasicNameValuePair("EndDateDay",
					getTwoDigitsFromCal(cal, Calendar.DAY_OF_MONTH)));
			postData.add(new BasicNameValuePair("EndDateMonth",
					getTwoDigitsFromCal(cal, Calendar.MONTH)));
			postData.add(new BasicNameValuePair("EndDateYear",
					getTwoDigitsFromCal(cal, Calendar.YEAR)));
			postData.add(new BasicNameValuePair("EndDateHour",
					getTwoDigitsFromCal(cal, Calendar.HOUR_OF_DAY)));
			postData.add(new BasicNameValuePair("EndDateMin",
					getTwoDigitsFromCal(cal, Calendar.MINUTE)));
			final String s = DateFormat.format(DATEFORMAT, cal).toString();
			postData.add(new BasicNameValuePair("RepeatStartDate", s));
			postData.add(new BasicNameValuePair("RepeatEndDate", s));
			postData.add(new BasicNameValuePair("RepeatType", "5"));
			postData.add(new BasicNameValuePair("RepeatEndType", "0"));
		}
		String[] st = this.htmlText.split("<input type=\"Hidden\" ");
		this.htmlText = null;
		for (String s : st) {
			if (s.startsWith("name=")) {
				String[] subst = s.split("\"", 5);
				if (subst.length >= 4) {
					if (this.sendLater > 0 && subst[1].startsWith("Repeat")) {
						continue;
					}
					postData.add(new BasicNameValuePair(subst[1], subst[3]));
				}
			}
		}
		st = null;

		HttpResponse response = getHttpClient(url, COOKIES, postData,
				TARGET_AGENT, URLS[operator][URL_PRESEND]);
		postData = null;
		int resp = response.getStatusLine().getStatusCode();
		if (resp != HttpURLConnection.HTTP_OK) {
			throw new WebSMSException(this.context, R.string.log_error_http, ""
					+ resp);
		}
		this.htmlText = stream2str(response.getEntity().getContent());
		if (this.htmlText.indexOf(STRINGS[operator][CHECK_SENT]) < 0) {
			// check output html for success message
			this.htmlText = null;
			return false;
		}
		this.htmlText = null;
		return true;
	}

	/**
	 * Send data.
	 * 
	 * @param reuseSession
	 *            try to reuse existing session
	 * @return successful?
	 * @throws WebSMSException
	 *             WebSMSException
	 */
	private boolean sendData(final boolean reuseSession) throws WebSMSException {
		// Operator of user. Selected by countrycode.
		final short operator = this.getOperator();
		if (operator < 0) {
			return false;
		}
		Log.d(TAG, "sendData(" + reuseSession + ")");
		// do IO
		try {
			// get Connection
			HttpResponse response;
			int resp;
			if (!reuseSession || COOKIES.size() == 0) {
				// clear session data
				COOKIES.clear();
				Log.d(TAG, "init session");
				// pre-login
				response = getHttpClient(URLS[operator][URL_PRELOGIN], COOKIES,
						null, TARGET_AGENT, null);
				resp = response.getStatusLine().getStatusCode();
				if (resp != HttpURLConnection.HTTP_OK) {
					throw new WebSMSException(this.context,
							R.string.log_error_http, "" + resp);
				}
				updateCookies(COOKIES, response.getAllHeaders(),
						URLS[operator][URL_PRELOGIN]);
				this.htmlText = stream2str(response.getEntity().getContent());
				final String flowExecutionKey = ConnectorO2
						.getFlowExecutionkey(this.htmlText);
				this.htmlText = null;

				// login
				if (!this.login(operator, flowExecutionKey)) {
					return false;
				}

				// sms-center
				response = getHttpClient(URLS[operator][URL_SMSCENTER],
						COOKIES, null, TARGET_AGENT, URLS[operator][URL_LOGIN]);
				resp = response.getStatusLine().getStatusCode();
				if (resp != HttpURLConnection.HTTP_OK) {
					if (reuseSession) {
						// try again with clear session
						return this.sendData(false);
					}
					throw new WebSMSException(this.context,
							R.string.log_error_http, "" + resp);
				}
				updateCookies(COOKIES, response.getAllHeaders(),
						URLS[operator][URL_SMSCENTER]);
			}

			// pre-send
			response = getHttpClient(URLS[operator][URL_PRESEND], COOKIES,
					null, TARGET_AGENT, URLS[operator][URL_SMSCENTER]);
			resp = response.getStatusLine().getStatusCode();
			if (resp != HttpURLConnection.HTTP_OK) {
				if (reuseSession) {
					// try again with clear session
					return this.sendData(false);
				}
				throw new WebSMSException(this.context,
						R.string.log_error_http, "" + resp);
			}
			updateCookies(COOKIES, response.getAllHeaders(),
					URLS[operator][URL_PRESEND]);
			this.htmlText = stream2str(response.getEntity().getContent());
			int i = this.htmlText.indexOf(STRINGS[operator][CHECK_FREESMS]);
			if (i > 0) {
				int j = this.htmlText.indexOf(STRINGS[operator][CHECK_WEB2SMS],
						i);
				if (j > 0) {
					WebSMS.SMS_BALANCE[O2] = this.htmlText.substring(i + 9, j)
							.trim();
					this.pushMessage(WebSMS.MESSAGE_FREECOUNT, null);
				} else if (reuseSession) {
					// try again with clear session
					return this.sendData(false);
				}
			}

			// send
			if (this.text != null && this.to != null && this.to.length > 0) {
				this.sendToO2(operator);
			}
			this.htmlText = null;
		} catch (IOException e) {
			Log.e(TAG, null, e);
			throw new WebSMSException(e.toString());
		} catch (URISyntaxException e) {
			Log.e(TAG, null, e);
			throw new WebSMSException(e.toString());
		} catch (MalformedCookieException e) {
			Log.e(TAG, null, e);
			throw new WebSMSException(e.toString());
		}
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final boolean updateMessages() throws WebSMSException {
		return this.sendData(true);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final boolean sendMessage() throws WebSMSException {
		if (!this.sendData(true)) {
			// failed!
			throw new WebSMSException(this.context, R.string.log_error);
		} else {
			// result: ok
			return true;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final boolean supportFlashsms() {
		return true;
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
