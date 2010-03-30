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
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Calendar;

import org.apache.http.cookie.MalformedCookieException;
import org.apache.http.message.BasicNameValuePair;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.BitmapDrawable;
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
 * AsyncTask to manage IO to O2 API.
 * 
 * @author flx
 */
public class ConnectorO2 extends Connector {
	/** Tag for output. */
	private static final String TAG = "WebSMS.o2";

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
	private static final String CHECK_SCHED = "Ihre Web2SMS ist geplant.";
	/** Check if captcha was solved wrong. */
	private static final String CHECK_WRONGCAPTCHA = // .
	"Sie haben einen falschen Code eingegeben.";

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

	/** Key to safe cookies. */
	private static final String PREFS_SAFECOOKIES = "cookie_store";

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final ConnectorSpec initSpec(final Context context) {
		final String name = context.getString(R.string.connector_o2_name);
		ConnectorSpec c = new ConnectorSpec(name);
		c.setAuthor(context.getString(R.string.connector_o2_author));
		c.setBalance(null);
		c.setPrefsTitle(context.getString(R.string.connector_o2_preferences));
		c.setCapabilities(ConnectorSpec.CAPABILITIES_UPDATE
				| ConnectorSpec.CAPABILITIES_SEND
				| ConnectorSpec.CAPABILITIES_PREFS);
		c.addSubConnector("sub", c.getName(),
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
	 * Load captcha and wait for user input to solve it.
	 * 
	 * @param context
	 *            {@link Context}
	 * @param netHandler
	 *            {@link ApacheNetworkHandler}
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
			final ApacheNetworkHandler netHandler) throws IOException,
			MalformedCookieException, URISyntaxException, WebSMSException {
		final String flow = netHandler.getFlowExecutionKey();
		netHandler.httpHandler(Constants.HTTP_GET, URL_CAPTCHA, null, false,
				URL_LOGIN, -1, -1);
		BitmapDrawable captcha = new BitmapDrawable(netHandler
				.getContentStream());
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
		netHandler.httpHandler(Constants.HTTP_POST, URL_SOLVECAPTCHA, postData,
				true, URL_LOGIN, 1, -1);
		final String htmlText = netHandler.getContent();
		if (htmlText.indexOf(CHECK_WRONGCAPTCHA) > 0) {
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
	 * @param netHandler
	 *            {@link ApacheNetworkHandler}
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
			final ConnectorCommand command,
			final ApacheNetworkHandler netHandler) throws IOException,
			MalformedCookieException, URISyntaxException, WebSMSException {
		// post data
		final ArrayList<BasicNameValuePair> postData = // .
		new ArrayList<BasicNameValuePair>(4);
		postData.add(new BasicNameValuePair("_flowExecutionKey", netHandler
				.getFlowExecutionKey()));
		postData.add(new BasicNameValuePair("loginName", Utils
				.international2national(command.getDefPrefix(), Utils
						.getSender(context, command.getDefSender()))));
		final SharedPreferences p = PreferenceManager
				.getDefaultSharedPreferences(context);
		postData.add(new BasicNameValuePair("password", p.getString(
				Preferences.PREFS_PASSWORD, "")));
		postData.add(new BasicNameValuePair("_eventId", "login"));
		String oldCookie = netHandler.getCookie();
		netHandler.httpHandler(Constants.HTTP_POST, URL_LOGIN, postData, true,
				URL_PRELOGIN, -1, -1);
		if (netHandler.getCookie().equals(oldCookie)) {
			String htmlText = netHandler.getContent();
			if (htmlText != null && htmlText.indexOf("captcha") > 0) {
				if (!this.solveCaptcha(context, netHandler)) {
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
	 * @param netHandler
	 *            {@link ApacheNetworkHandler}
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
			final ConnectorCommand command,
			final ApacheNetworkHandler netHandler) throws IOException,
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
		String[] st = netHandler.getContent().split("<input type=\"Hidden\" ");
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

		netHandler.httpHandler(Constants.HTTP_POST, url, postData, true,
				URL_PRESEND, -1, -1);
		String check = CHECK_SENT;
		if (sendLater > 0) {
			check = CHECK_SCHED;
		}
		String htmlText1 = netHandler.getContent();
		if (htmlText1 == null) {
			throw new WebSMSException("error parsing website");
		} else if (htmlText1.indexOf(check) < 0) {
			// check output html for success message
			Log.d(TAG, htmlText1);
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
	private void sendData(final Context context,
			final ConnectorCommand command, final boolean reuseSession)
			throws WebSMSException {
		Log.d(TAG, "sendData(" + reuseSession + ")");
		String cookies = null;
		if (reuseSession) {
			cookies = PreferenceManager.getDefaultSharedPreferences(context)
					.getString(PREFS_SAFECOOKIES, null);
			Log.d(TAG, "loaded cookies: " + cookies);
		}
		ApacheNetworkHandler netHandler = new ApacheNetworkHandler(cookies);
		cookies = null;
		// do IO
		try {
			// get Connection
			if (!reuseSession || netHandler.getCookie().length() == 0) {
				// clear session data
				netHandler = new ApacheNetworkHandler(null);
				Log.d(TAG, "init session");
				// pre-login
				netHandler.httpHandler(Constants.HTTP_GET, URL_PRELOGIN, null,
						true, null, -1, -1);
				// login
				if (!this.login(context, command, netHandler)) {
					throw new WebSMSException(context, R.string.error);
				}
				// sms-center
				netHandler.httpHandler(Constants.HTTP_GET, URL_SMSCENTER, null,
						true, URL_LOGIN, -1, -1);
			}
			// pre-send
			try {
				netHandler.httpHandler(Constants.HTTP_GET, URL_PRESEND, null,
						true, URL_SMSCENTER, -1, -1);
			} catch (WebSMSException e) {
				if (reuseSession) {
					// try again with clear session
					this.sendData(context, command, false);
					return;
				}
				throw e;
			}
			String htmlText = netHandler.getContent();
			if (htmlText == null) {
				if (reuseSession) {
					Log.d(TAG, "htmlText == null");
					this.sendData(context, command, false);
					return;
				} else {
					throw new WebSMSException("failed to locate freesms"
							+ " on site\nplease deactivate tweak.");
				}
			}
			try {
				ConnectorSpec c = this.getSpec(context);
				c.setBalance(netHandler.getFreeSMS());
				Log.d(TAG, "balance: " + c.getBalance());
			} catch (WebSMSException e) {
				if (reuseSession) {
					// try again with clear session
					this.sendData(context, command, false);
					return;
				} else {
					throw e;
				}
			}

			// send
			final String text = command.getText();
			if (text != null && text.length() > 0) {
				this.sendToO2(context, command, netHandler);
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
		// save cookies
		PreferenceManager.getDefaultSharedPreferences(context).edit()
				.putString(PREFS_SAFECOOKIES, netHandler.getCookie()).commit();
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
	@Override
	protected final void gotSolvedCaptcha(final Context context,
			final String solvedCaptcha) {
		captchaSolve = solvedCaptcha;
		synchronized (CAPTCHA_SYNC) {
			CAPTCHA_SYNC.notify();
		}
	}
}
