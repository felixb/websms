/*
 * Copyright (C) 2010 Lado Kumsiashvili, Felix Bechstein
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
package de.ub0r.android.websms.connector.arcor;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;

import android.content.Intent;
import android.util.Log;
import de.ub0r.android.websms.connector.common.CommandReceiver;
import de.ub0r.android.websms.connector.common.WebSMSException;

/**
 * Connector for arcor.de free sms / payed sms.
 * 
 * @author lado
 */
public class ConnectorArcor extends CommandReceiver {

	/** Preference's name: arcor username. */
	private static final String PREFS_USER_ARCOR = "user_arcor";
	/** Preference's name: user's password - arcor. */
	private static final String PREFS_PASSWORD_ARCOR = "password_arcor";
	/** Preference's name: user's password - arcor. */
	private static final String PREFS_COPY_SENT_SMS_ARCOR = // .
	"copy_sent_sms_arcor";
	/** Preference's name: ovrride default sender number? - arcor. */
	private static final String PREFS_ENABLE_VALIDATED_NUMBER_ARCOR = // .
	"enable_validated_number_arcor";
	/** Preference's name: enable o2. */
	private static final String PREFS_ENABLE_ARCOR = "enable_arcor";

	/**
	 * Pattern to extract free sms count from sms page. Looks like.
	 */
	private static final Pattern BALANCE_MATCH_PATTERN = Pattern.compile(
			"<td.+class=\"txtRed\".*?<b>" + "(\\d{1,2})</b>.+"
					+ "<td.+class=\"txtRed\".*?<b>(\\d{1,})</b>",
			Pattern.DOTALL);
	/**
	 * After Sending arcor sends, hint/error/warning as status. hint meand sent,
	 * warning and eror bad things happen :)
	 */
	private static final Pattern SEND_CHECK_STATUS_PATTERN = Pattern.compile(
			"<div class=\"contentArea\">.+?"
					+ "<div class=\"(hint|warning|error)\">(.+?)</div>",
			Pattern.DOTALL);
	/** This String will be matched if the user is logged in. */
	private static final String MATCH_LOGIN_SUCCESS = "logout.jsp";

	/**
	 * No more SMS?
	 */
	private static final String MATCH_NO_SMS = "Sie haben "
			+ "derzeit keine SMS zur Verf";

	/**
	 * No limits on SMS ? see issue
	 * http://code.google.com/p/websmsdroid/issues/detail?id=55
	 */
	private static final String MATCH_UNLIMITTED_SMS = "<b>unbegrenzt viele</b>";

	/** Cache this client over several calls. */
	private final HttpClient client = new DefaultHttpClient();
	/** HTTP Header User-Agent. */
	// TODO share this. Make it Configurable global and local
	private static final String FAKE_USER_AGENT = "Mozilla/5.0 (Windows; U;"
			+ " Windows NT 5.1; de; rv:1.9.0.9) Gecko/2009040821"
			+ " Firefox/3.0.9 (.NET CLR 3.5.30729)";

	/** where to find sms count. */
	private static final int APPROXIMATE_SMS_COUNT_POSITION = 18000;

	/** where to find sms count. */
	private static final int APPROXIMATE_SMS_COUNT_LENGTH = 3000;

	/** where to find logout link. */
	private static final int APPROXIMATE_LOGOUT_LINK_COUNT_POSITION = 4000;

	/** where to find logout link. */
	private static final int APPROXIMATE_LOGOUT_LENGTH = 2000;
	/** Login URL, to send Login (POST). */
	private static final String LOGIN_URL = "https://www.arcor.de"
			+ "/login/login.jsp";
	/** Send SMS URL(POST) / Free SMS Count URL(GET). */
	private static final String SMS_URL = "https://www.arcor.de"
			+ "/ums/ums_neu_sms.jsp";

	/** Encoding to use. */
	private static final String AROCR_ENCODING = "ISO-8859-15";

	/** Step to skip. */
	private static final int STEP = 256;

	/**
	 * Login to arcor.
	 * 
	 * @return true if successfullu logged in, false otherwise.
	 * @throws WebSMSException
	 *             if any Exception occures.
	 */
	private boolean login() throws WebSMSException {
		try {
			final HttpPost request = createPOST(LOGIN_URL, getLoginPost(
					this.user, this.password));
			final HttpResponse response = this.client.execute(request);
			final String cutContent = cutLoginInfoFromContent(response
					.getEntity().getContent());
			if (cutContent.indexOf(MATCH_LOGIN_SUCCESS) == -1) {
				throw new WebSMSException(this.context, R.string.log_error_pw);
			}
		} catch (final Exception e) {
			throw new WebSMSException(e.getMessage());
		}
		return true;
	}

	/**
	 * Updates balance andl pushes it to WebSMS.
	 * 
	 * @return successful?
	 * @throws WebSMSException
	 *             on an error
	 */
	private boolean updateBalance() throws WebSMSException {
		try {
			final HttpResponse response = this.client.execute(new HttpGet(
					SMS_URL));
			return this.pushFreeCount(cutFreeCountFromContent(response
					.getEntity().getContent()));
		} catch (final Exception ex) {
			throw new WebSMSException(ex.getMessage());
		}
	}

	/**
	 * Push SMS Free Count to WebSMS.
	 * 
	 * @param content
	 *            conten to investigate.
	 * @return push ok?
	 */
	private boolean pushFreeCount(final String content) {
		final Matcher m = BALANCE_MATCH_PATTERN.matcher(content);
		String term = null;
		if (m.find()) {
			term = m.group(1) + "+" + m.group(2);
		} else if (content.contains(MATCH_NO_SMS)) {
			term = "0+0";
		} else if (content.contains(MATCH_UNLIMITTED_SMS)) {
			term = "\u221E";
		} else {
			return false;
		}
		WebSMS.SMS_BALANCE[ARCOR] = term;
		this.pushMessage(WebSMS.MESSAGE_FREECOUNT, null);
		return true;
	}

	/**
	 * Sends an sms via HTTP POST.
	 * 
	 * @return successfull?
	 * @throws WebSMSException
	 *             on an error
	 */
	private boolean sendSms() throws WebSMSException {
		try {
			final HttpResponse response = this.client.execute(createPOST(
					SMS_URL, getSmsPost(this.to, this.text)));
			return this.afterSmsSent(response);
		} catch (final Exception ex) {
			throw new WebSMSException(ex.getMessage());
		}
	}

	/**
	 * Handles content after sms sending.
	 * 
	 * @param response
	 *            HTTP Response
	 * @return true if arcor returns success
	 * @throws Exception
	 *             if an Error occures
	 */
	private boolean afterSmsSent(final HttpResponse response) throws Exception {

		final String body = cutFreeCountFromContent(response.getEntity()
				.getContent());

		final Matcher m = SEND_CHECK_STATUS_PATTERN.matcher(body);
		final boolean found = m.find();
		if (!found || m.groupCount() != 2) {
			// should not happen
			Log.w("WebSMS.ConnectorArcor", body);
			throw new Exception(this.context
					.getString(R.string.log_unknow_status_after_send_arcor));
		}

		final String status = m.group(1);
		// ok, message sent!
		if (status.equals("hint")) {
			return this.pushFreeCount(body);
		}
		// warning or error
		throw new Exception(m.group(2));
	}

	/**
	 * This post data is needed for log in.
	 * 
	 * @param username
	 *            username
	 * @param password
	 *            password
	 * @return array of params
	 * @throws UnsupportedEncodingException
	 *             if the url can not be encoded
	 */
	private static String getLoginPost(final String username,
			final String password) throws UnsupportedEncodingException {
		final StringBuilder sb = new StringBuilder();
		sb.append("user_name=");
		sb.append(URLEncoder.encode(username, AROCR_ENCODING));
		sb.append("&password=");
		sb.append(URLEncoder.encode(password, AROCR_ENCODING));
		sb
				.append("&login=Login&protocol="
						+ "https&info=Online-Passwort&goto=");
		return sb.toString();
	}

	/**
	 * These post data is needed for sending a sms.
	 * 
	 * @param to
	 *            receiver array
	 * @param text
	 *            text to send
	 * @return array of params
	 * @throws Exception
	 *             if an error occures.
	 */
	private static String getSmsPost(final String[] to, final String text)
			throws Exception {
		final StringBuilder sb = new StringBuilder();
		for (final String r : to) {
			sb.append(r).append(",");
		}
		final StringBuilder sb1 = new StringBuilder();
		sb1.append("empfaengerAn=");
		sb1.append(URLEncoder.encode(sb.toString(), AROCR_ENCODING));
		sb1.append("&emailAdressen=");
		sb1.append(URLEncoder.encode(WebSMS.prefsSender, AROCR_ENCODING));
		sb1.append("&nachricht=");
		sb1.append(URLEncoder.encode(text, AROCR_ENCODING));
		sb1.append("&firstVisitOfPage=foo&part=0&senden=Senden");

		if (WebSMS.prefsCopySentSmsArcor) {
			sb1.append("&gesendetkopiesms=on");
		}
		if (WebSMS.prefsEnableValidatedNumberArcor) {
			sb1.append("&useOwnMobile=on");
		}

		return sb1.toString();
	}

	/**
	 * Create and Prepare a Post Request. Set also an User-Agent
	 * 
	 * @param url
	 *            http post url
	 * @param urlencodedparams
	 *            key=value pairs as url encoded string
	 * @return HttpPost
	 * @throws Exception
	 *             if an error occures
	 */
	private static HttpPost createPOST(final String url,
			final String urlencodedparams) throws Exception {
		final HttpPost post = new HttpPost(url);
		post.setHeader("User-Agent", FAKE_USER_AGENT);
		post.setHeader(new BasicHeader(HTTP.CONTENT_TYPE,
				URLEncodedUtils.CONTENT_TYPE));
		post.setEntity(new StringEntity(urlencodedparams));
		return post;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final void doSend(final Intent intent) throws WebSMSException {
		if (this.login()) {
			this.sendSms();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final void doUpdate(final Intent intent) throws WebSMSException {
		if (this.login()) {
			this.updateBalance();
		}
	}

	/**
	 * Cuts only for us interessting content part. Here, if user ist logged in.
	 * 
	 * @param is
	 *            response body stream
	 * @return cut content
	 * @throws IOException
	 *             on an I/O error
	 */
	private static String cutLoginInfoFromContent(final InputStream is)
			throws IOException {
		skip(is, APPROXIMATE_LOGOUT_LINK_COUNT_POSITION);
		final byte[] data = readBytes(APPROXIMATE_LOGOUT_LENGTH, is);
		return new String(data, AROCR_ENCODING);
	}

	/**
	 * skip 'bytes' size bytes from stream, consuming it in 'step' steps.
	 * 
	 * @param is
	 *            Content stream
	 * @param bytes
	 *            bytes to skip
	 * @throws IOException
	 *             on an I/O error
	 */
	private static void skip(final InputStream is, final long bytes)
			throws IOException {
		long alreadySkipped = 0;
		long skip = 0;
		while (true) {
			skip = is.skip(STEP);
			if (skip == 0) {
				break;
			}
			alreadySkipped += skip;
			if (alreadySkipped >= bytes) {
				break;
			}
		}
	}

	/**
	 * Reads a size portion of bytes from strem.
	 * 
	 * @param size
	 *            to read
	 * @param is
	 *            content stream
	 * @return read bytes
	 * @throws IOException
	 *             on an I/O error
	 */
	private static byte[] readBytes(final int size, final InputStream is)
			throws IOException {
		final byte[] data = new byte[size];
		int offset = 0;
		int read = 0;
		int length = size;
		while (true) {
			read = is.read(data, offset, length);
			if (read == -1) {
				break;
			}
			offset = offset + read;
			if (offset >= size) {
				break;
			}
			length = length - read;
		}
		return data;
	}

	/**
	 * Cuts only for us interessting content part. Here, to match free sms
	 * count.
	 * 
	 * @param is
	 *            HttpResponse strem.
	 * @return cut part of the response.
	 * @throws IOException
	 *             on I/O error.
	 */
	private static String cutFreeCountFromContent(final InputStream is)
			throws IOException {
		skip(is, APPROXIMATE_SMS_COUNT_POSITION);
		final byte[] data = readBytes(APPROXIMATE_SMS_COUNT_LENGTH, is);
		return new String(data, AROCR_ENCODING);
	}
}
