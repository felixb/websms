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
package de.ub0r.android.websms.connector.o2;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Calendar;

import org.apache.http.HttpResponse;
import org.apache.http.cookie.Cookie;
import org.apache.http.cookie.MalformedCookieException;
import org.apache.http.message.BasicNameValuePair;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.BitmapDrawable;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
import de.ub0r.android.websms.connector.common.Connector;
import de.ub0r.android.websms.connector.common.ConnectorCommand;
import de.ub0r.android.websms.connector.common.ConnectorSpec;
import de.ub0r.android.websms.connector.common.Log;
import de.ub0r.android.websms.connector.common.Utils;
import de.ub0r.android.websms.connector.common.WebSMSException;
import de.ub0r.android.websms.connector.common.ConnectorSpec.SubConnectorSpec;

/**
 * AsyncTask to manage IO to O2 API.
 * 
 * @author flx
 */
public class ConnectorO2 extends Connector {
	/** Tag for output. */
	private static final String TAG = "o2";

	/** Custom Dateformater. */
	private static final String DATEFORMAT = "yyyy,MM,dd,kk,mm,00";

	/** URL before login. */
	private static final String URL_PRELOGIN = "https://login.o2online.de"
			+ "/loginRegistration/loginAction.do"
			+ "?_flowId=login&o2_type=asp&o2_label=login/"
			+ "comcenter-login&scheme=http&port=80&server=email"
			+ ".o2online.de&url=%2Fssomanager.osp%3FAPIID%3D"
			+ "AUTH-WEBSSO%26TargetApp%3D%2Fsmscenter_new.osp"
			+ "%253f%26o2_type%3Durl%26o2_label%3Dweb2sms-o2online";
	/** URL for login. */
	private static final String URL_LOGIN = "https://login.o2online.de"
			+ "/loginRegistration/loginAction.do";
	/** URL of captcha. */
	private static final String URL_CAPTCHA = "https://login.o2online.de"
			+ "/loginRegistration/jcaptchaReg";
	/** URL for solving captcha. */
	private static final String URL_SOLVECAPTCHA = URL_LOGIN;
	/** URL for sms center. */
	private static final String URL_SMSCENTER = "http://email.o2online.de:80"
			+ "/ssomanager.osp?APIID=AUTH-WEBSSO&TargetApp=/smscenter_new.osp"
			+ "?&o2_type=url&o2_label=web2sms-o2online";
	/** URL before sending. */
	private static final String URL_PRESEND = "https://email.o2online.de"
			+ "/smscenter_new.osp?Autocompletion=1&MsgContentID=-1";
	/** URL for sending. */
	private static final String URL_SEND = "https://email.o2online.de"
			+ "/smscenter_send.osp";
	/** URL for sending later. */
	private static final String URL_SCHEDULE = "https://email.o2online.de"
			+ "/smscenter_schedule.osp";

	/** Check for free sms. */
	private static final String CHECK_FREESMS = "Frei-SMS: ";
	/** Check for web2sms. */
	private static final String CHECK_WEB2SMS = "Web2SMS";
	/** Check if message was sent. */
	private static final String CHECK_SENT = // .
	"Ihre SMS wurde erfolgreich versendet.";
	// private static final String CHECK_SENT = "/app_pic/ico_mail_send_ok.gif";
	/** Check if message was scheduled. */
	private static final String CHECK_SCHED = "Ihre Web2SMS ist geplant";
	/** Check if captcha was solved wrong. */
	private static final String CHECK_WRONGCAPTCHA = // .
	"Sie haben einen falschen Code eingegeben.";
	/** Check for _flowExecutionKey. */
	private static final String CHECK_FLOW = // .
	"name=\"_flowExecutionKey\" value=\"";

	/** Stip bytes from stream: prelogin. */
	private static final int STRIP_PRELOGIN_START = 8000;
	/** Stip bytes from stream: prelogin. */
	private static final int STRIP_PRELOGIN_END = 11000;
	/** Stip bytes from stream: presend. */
	private static final int STRIP_PRESEND_START = 56000;
	/** Stip bytes from stream: presend. */
	private static final int STRIP_PRESEND_END = 62000;
	/** Stip bytes from stream: send. */
	private static final int STRIP_SEND_START = 2000;

	/** HTTP Useragent. */
	private static final String TARGET_AGENT = "Mozilla/5.0 (Windows; U;"
			+ " Windows NT 5.1; de; rv:1.9.0.9) Gecko/2009040821"
			+ " Firefox/3.0.9 (.NET CLR 3.5.30729)";

	/** Solved Captcha. */
	private static String captchaSolve = null;
	/** Object to sync with. */
	private static final Object CAPTCHA_SYNC = new Object();
	/** Timeout for entering the captcha. */
	private static final long CAPTCHA_TIMEOUT = 60000;

	/** Static cookies. */
	private static ArrayList<Cookie> staticCookies = new ArrayList<Cookie>();

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final ConnectorSpec initSpec(final Context context) {
		final String name = context.getString(R.string.connector_o2_name);
		ConnectorSpec c = new ConnectorSpec(name);
		c.setAuthor(context.getString(R.string.connector_o2_author));
		c.setBalance(null);
		c.setCapabilities(ConnectorSpec.CAPABILITIES_UPDATE
				| ConnectorSpec.CAPABILITIES_SEND
				| ConnectorSpec.CAPABILITIES_PREFS);
		c.addSubConnector("o2", c.getName(),
				SubConnectorSpec.FEATURE_CUSTOMSENDER
						| SubConnectorSpec.FEATURE_SENDLATER
						| SubConnectorSpec.FEATURE_SENDLATER_QUARTERS
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
	 * Extract _flowExecutionKey from HTML output.
	 * 
	 * @param html
	 *            input
	 * @return _flowExecutionKey
	 */
	private static String getFlowExecutionkey(final String html) {
		String ret = "";
		int i = html.indexOf(CHECK_FLOW);
		if (i > 0) {
			int j = html.indexOf("\"", i + 35);
			if (j >= 0) {
				ret = html.substring(i + 32, j);
			}
		}
		return ret;
	}

	/**
	 * Load captcha and wait for user input to solve it.
	 * 
	 * @param context
	 *            {@link Context}
	 * @param cookies
	 *            {@link Cookie}s
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
	private boolean solveCaptcha(final Context context,
			final ArrayList<Cookie> cookies, final String flow)
			throws IOException, MalformedCookieException, URISyntaxException,
			WebSMSException {
		HttpResponse response = Utils.getHttpClient(URL_CAPTCHA, cookies, null,
				TARGET_AGENT, URL_LOGIN);
		int resp = response.getStatusLine().getStatusCode();
		if (resp != HttpURLConnection.HTTP_OK) {
			throw new WebSMSException(context, R.string.error_http, "" + resp);
		}
		Utils.updateCookies(cookies, response.getAllHeaders(), URL_CAPTCHA);
		BitmapDrawable captcha = new BitmapDrawable(response.getEntity()
				.getContent());
		final Intent intent = new Intent(Connector.ACTION_CAPTCHA_REQUEST);
		intent.putExtra(Connector.EXTRA_CAPTCHA_DRAWABLE, captcha.getBitmap());
		captcha = null;
		this.getSpec(context).setToIntent(intent);
		context.sendBroadcast(intent);
		try {
			synchronized (CAPTCHA_SYNC) {
				CAPTCHA_SYNC.wait(CAPTCHA_TIMEOUT);
			}
		} catch (InterruptedException e) {
			Log.e(TAG, null, e);
			return false;
		}
		if (captchaSolve == null) {
			return false;
		}
		// got user response, try to solve captcha
		Log.d(TAG, "got solved captcha: " + captchaSolve);
		final ArrayList<BasicNameValuePair> postData = // .
		new ArrayList<BasicNameValuePair>(3);
		postData.add(new BasicNameValuePair("_flowExecutionKey", flow));
		postData.add(new BasicNameValuePair("_eventId", "submit"));
		postData.add(new BasicNameValuePair("riddleValue", captchaSolve));
		response = Utils.getHttpClient(URL_SOLVECAPTCHA, cookies, postData,
				TARGET_AGENT, URL_LOGIN);
		Log.d(TAG, cookies.toString());
		Log.d(TAG, postData.toString());
		resp = response.getStatusLine().getStatusCode();
		if (resp != HttpURLConnection.HTTP_OK) {
			throw new WebSMSException(context, R.string.error_http, "" + resp);
		}
		Utils.updateCookies(cookies, response.getAllHeaders(), URL_CAPTCHA);
		final String mHtmlText = Utils.stream2str(response.getEntity()
				.getContent());
		if (mHtmlText.indexOf(CHECK_WRONGCAPTCHA) > 0) {
			throw new WebSMSException(context, R.string.error_wrongcaptcha);
		}
		return true;
	}

	/**
	 * Login to O2.
	 * 
	 * @param context
	 *            Context
	 * @param command
	 *            ConnectorCommand
	 * @param cookies
	 *            {@link Cookie}s
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
	private boolean login(final Context context,
			final ConnectorCommand command, final ArrayList<Cookie> cookies,
			final String flow) throws IOException, MalformedCookieException,
			URISyntaxException, WebSMSException {
		// post data
		final ArrayList<BasicNameValuePair> postData = // .
		new ArrayList<BasicNameValuePair>(4);
		postData.add(new BasicNameValuePair("_flowExecutionKey", flow));
		postData.add(new BasicNameValuePair("loginName", Utils
				.international2national(command.getDefPrefix(), Utils
						.getSender(context, command.getDefSender()))));
		final SharedPreferences p = PreferenceManager
				.getDefaultSharedPreferences(context);
		postData.add(new BasicNameValuePair("password", p.getString(
				Preferences.PREFS_PASSWORD, "")));
		postData.add(new BasicNameValuePair("_eventId", "login"));
		HttpResponse response = Utils.getHttpClient(URL_LOGIN, cookies,
				postData, TARGET_AGENT, URL_PRELOGIN);
		int resp = response.getStatusLine().getStatusCode();
		if (resp != HttpURLConnection.HTTP_OK) {
			throw new WebSMSException(context, R.string.error_http, "" + resp);
		}
		resp = cookies.size();
		Utils.updateCookies(cookies, response.getAllHeaders(), URL_LOGIN);
		if (resp == cookies.size()) {
			String htmlText = null;
			if (PreferenceManager.getDefaultSharedPreferences(context)
					.getBoolean(Preferences.PREFS_TWEAK, false)) {
				htmlText = Utils.stream2str(response.getEntity().getContent(),
						STRIP_PRELOGIN_START, STRIP_PRELOGIN_END);
			} else {
				htmlText = Utils.stream2str(response.getEntity().getContent());
			}
			response = null;
			if (htmlText != null && htmlText.indexOf("captcha") > 0) {
				final String newFlow = getFlowExecutionkey(htmlText);
				htmlText = null;
				if (!this.solveCaptcha(context, cookies, newFlow)) {
					throw new WebSMSException(context,
							R.string.error_wrongcaptcha);
				}
			} else {
				throw new WebSMSException(context, R.string.error_pw);
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
	 * @param context
	 *            {@link Context}
	 * @param command
	 *            {@link ConnectorCommand}
	 * @param cookies
	 *            {@link Cookie}s
	 * @param htmlText
	 *            html source of previous site
	 * @throws IOException
	 *             IOException
	 * @throws MalformedCookieException
	 *             MalformedCookieException
	 * @throws URISyntaxException
	 *             URISyntaxException
	 * @throws WebSMSException
	 *             WebSMSException
	 */
	private void sendToO2(final Context context,
			final ConnectorCommand command, final ArrayList<Cookie> cookies,
			final String htmlText) throws IOException,
			MalformedCookieException, URISyntaxException, WebSMSException {
		ArrayList<BasicNameValuePair> postData = // .
		new ArrayList<BasicNameValuePair>();
		postData.add(new BasicNameValuePair("SMSTo", Utils
				.national2international(command.getDefPrefix(), Utils
						.getRecipientsNumber(command.getRecipients()[0]))));
		postData.add(new BasicNameValuePair("SMSText", command.getText()));
		final String customSender = command.getCustomSender();
		if (customSender != null) {
			postData.add(new BasicNameValuePair("SMSFrom", customSender));
			if (customSender.length() == 0) {
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
		if (command.getFlashSMS()) {
			postData.add(new BasicNameValuePair("FlagFlash", "1"));
		} else {
			postData.add(new BasicNameValuePair("FlagFlash", "0"));
		}
		String url = URL_SEND;
		final long sendLater = command.getSendLater();
		if (sendLater > 0) {
			url = URL_SCHEDULE;
			final Calendar cal = Calendar.getInstance();
			cal.setTimeInMillis(sendLater);
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
		String[] st = htmlText.split("<input type=\"Hidden\" ");
		for (String s : st) {
			if (s.startsWith("name=")) {
				String[] subst = s.split("\"", 5);
				if (subst.length >= 4) {
					if (sendLater > 0 && subst[1].startsWith("Repeat")) {
						continue;
					}
					postData.add(new BasicNameValuePair(subst[1], subst[3]));
				}
			}
		}
		st = null;

		HttpResponse response = Utils.getHttpClient(url, cookies, postData,
				TARGET_AGENT, URL_PRESEND);
		postData = null;
		int resp = response.getStatusLine().getStatusCode();
		if (resp != HttpURLConnection.HTTP_OK) {
			throw new WebSMSException(context, R.string.error_http, "" + resp);
		}
		String check = CHECK_SENT;
		if (sendLater > 0) {
			check = CHECK_SCHED;
		}
		String htmlText1 = null;
		if (sendLater <= 0
				&& PreferenceManager.getDefaultSharedPreferences(context)
						.getBoolean(Preferences.PREFS_TWEAK, false)) {
			htmlText1 = Utils.stream2str(response.getEntity().getContent(),
					STRIP_SEND_START, Utils.ONLY_MATCHING_LINE, check);
		} else {
			htmlText1 = Utils.stream2str(response.getEntity().getContent(), 0,
					Utils.ONLY_MATCHING_LINE, check);
		}
		if (htmlText1 == null) {
			throw new WebSMSException("error parsing website");
		} else if (htmlText1.indexOf(check) < 0) {
			// check output html for success message
			Log.w(TAG, htmlText1);
			throw new WebSMSException("error parsing website");
		}
	}

	/**
	 * Send data.
	 * 
	 * @param context
	 *            {@link Context}
	 * @param command
	 *            {@link ConnectorCommand}
	 * @param reuseSession
	 *            try to reuse existing session
	 * @throws WebSMSException
	 *             WebSMSException
	 */
	@SuppressWarnings("unchecked")
	private void sendData(final Context context,
			final ConnectorCommand command, final boolean reuseSession)
			throws WebSMSException {
		Log.d(TAG, "sendData(" + reuseSession + ")");
		ArrayList<Cookie> cookies;
		if (staticCookies == null) {
			cookies = new ArrayList<Cookie>();
		} else {
			cookies = (ArrayList<Cookie>) staticCookies.clone();
		}
		// do IO
		try {
			// get Connection
			HttpResponse response;
			int resp;
			if (!reuseSession || cookies.size() == 0) {
				// clear session data
				cookies.clear();
				Log.d(TAG, "init session");
				// pre-login
				response = Utils.getHttpClient(URL_PRELOGIN, cookies, null,
						TARGET_AGENT, null);
				resp = response.getStatusLine().getStatusCode();
				if (resp != HttpURLConnection.HTTP_OK) {
					throw new WebSMSException(context, R.string.error_http, ""
							+ resp);
				}
				Utils.updateCookies(cookies, response.getAllHeaders(),
						URL_PRELOGIN);
				String htmlText = Utils.stream2str(response.getEntity()
						.getContent(), 0, Utils.ONLY_MATCHING_LINE, CHECK_FLOW);
				final String flowExecutionKey = ConnectorO2
						.getFlowExecutionkey(htmlText);
				htmlText = null;

				// login
				if (!this.login(context, command, cookies, flowExecutionKey)) {
					throw new WebSMSException(context, R.string.error);
				}

				// sms-center
				response = Utils.getHttpClient(URL_SMSCENTER, cookies, null,
						TARGET_AGENT, URL_LOGIN);
				resp = response.getStatusLine().getStatusCode();
				if (resp != HttpURLConnection.HTTP_OK) {
					if (reuseSession) {
						// try again with clear session
						this.sendData(context, command, false);
						return;
					}
					throw new WebSMSException(context, R.string.error_http, ""
							+ resp);
				}
				Utils.updateCookies(cookies, response.getAllHeaders(),
						URL_SMSCENTER);
			}

			// pre-send
			response = Utils.getHttpClient(URL_PRESEND, cookies, null,
					TARGET_AGENT, URL_SMSCENTER);
			resp = response.getStatusLine().getStatusCode();
			if (resp != HttpURLConnection.HTTP_OK) {
				if (reuseSession) {
					// try again with clear session
					this.sendData(context, command, false);
					return;
				}
				throw new WebSMSException(context, R.string.error_http, ""
						+ resp);
			}
			Utils.updateCookies(cookies, response.getAllHeaders(), URL_PRESEND);
			String htmlText = null;
			if (PreferenceManager.getDefaultSharedPreferences(context)
					.getBoolean(Preferences.PREFS_TWEAK, false)) {
				htmlText = Utils.stream2str(response.getEntity().getContent(),
						STRIP_PRESEND_START, STRIP_PRESEND_END, CHECK_FREESMS);
			} else {
				htmlText = Utils.stream2str(response.getEntity().getContent(),
						0, -1, CHECK_FREESMS);
			}
			if (htmlText == null) {
				if (reuseSession) {
					this.sendData(context, command, false);
					return;
				} else {
					throw new WebSMSException(context, R.string.missing_freesms);
				}
			}
			int i = htmlText.indexOf(CHECK_FREESMS);
			if (i > 0) {
				int j = htmlText.indexOf(CHECK_WEB2SMS, i);
				if (j > 0) {
					ConnectorSpec c = this.getSpec(context);
					c.setBalance(htmlText.substring(i + 9, j).trim().split(" ",
							2)[0]);
					Log.d(TAG, "balance: " + c.getBalance());
				} else if (reuseSession) {
					// try again with clear session
					this.sendData(context, command, false);
					return;
				} else {
					Log.d(TAG, htmlText);
					throw new WebSMSException(context, R.string.missing_freesms);
				}
			} else {
				Log.d(TAG, htmlText);
				throw new WebSMSException(context, R.string.missing_freesms);
			}

			// send
			final String text = command.getText();
			if (text != null && text.length() > 0) {
				this.sendToO2(context, command, cookies, htmlText);
			}
			htmlText = null;
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
		staticCookies = cookies;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final void doUpdate(final Context context, final Intent intent)
			throws WebSMSException {
		this.sendData(context, new ConnectorCommand(intent), true);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final void doSend(final Context context, final Intent intent)
			throws WebSMSException {
		this.sendData(context, new ConnectorCommand(intent), true);
	}

	/**
	 * {@inheritDoc}
	 */
	protected final void gotSolvedCaptcha(final Context context,
			final String solvedCaptcha) {
		captchaSolve = solvedCaptcha;
		synchronized (CAPTCHA_SYNC) {
			CAPTCHA_SYNC.notify();
		}
	}
}
