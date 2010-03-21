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
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import de.ub0r.android.websms.connector.common.Connector;
import de.ub0r.android.websms.connector.common.ConnectorSpec;
import de.ub0r.android.websms.connector.common.Utils;
import de.ub0r.android.websms.connector.common.WebSMSException;

/**
 * Connector for arcor.de free sms / payed sms.
 * 
 * @author lado
 */
public class ConnectorArcor extends Connector {

	/** Tag for output. */
	private static final String TAG = "WebSMS.arcor";

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
	private static final String MATCH_UNLIMITTED_SMS = // .
	"<b>unbegrenzt viele</b>";

	/** HTTP Header User-Agent. */
	// TODO share this. Make it Configurable global and local
	private static final String FAKE_USER_AGENT = "Mozilla/5.0 (Windows; U;"
			+ " Windows NT 5.1; de; rv:1.9.0.9) Gecko/2009040821"
			+ " Firefox/3.0.9 (.NET CLR 3.5.30729)";

	/** where to find sms count. */
	private static final int SMS_COUNT_AREA_START = 16100;

	/** where to find sms count. */
	private static final int SMS_COUNT_AREA_END = SMS_COUNT_AREA_START + 8000;

	/** where to find logout link. */
	private static final int LOGOUT_LINK_AREA_START = 4000;

	/** where to find logout link. */
	private static final int LOGOUT_LINK_AREAD_END = LOGOUT_LINK_AREA_START + 8000;
	/** Login URL, to send Login (POST). */
	private static final String LOGIN_URL = "https://www.arcor.de"
			+ "/login/login.jsp";
	/** Send SMS URL(POST) / Free SMS Count URL(GET). */
	private static final String SMS_URL = "https://www.arcor.de"
			+ "/ums/ums_neu_sms.jsp";

	/** Encoding to use. */
	private static final String ARCOR_ENCODING = "ISO-8859-15";

	@Override
	public final ConnectorSpec initSpec(final Context context) {
		final String name = context.getString(R.string.connector_name);
		ConnectorSpec c = new ConnectorSpec(TAG, name);
		c.setAuthor(context.getString(R.string.connector_author));
		c.setBalance(null);
		c.setPrefsTitle(context.getString(R.string.settings));
		c.setCapabilities(ConnectorSpec.CAPABILITIES_UPDATE
				| ConnectorSpec.CAPABILITIES_SEND
				| ConnectorSpec.CAPABILITIES_PREFS);
		c.addSubConnector("dymmy", "dummy", 0);
		return c;
	}

	/**
	 * Login to arcor.
	 * 
	 * @param ctx
	 *            {@link ConnectorContext}
	 * @return true if successfullu logged in, false otherwise.
	 * @throws WebSMSException
	 *             if any Exception occures.
	 */
	private boolean login(final ConnectorContext ctx) throws WebSMSException {
		try {

			SharedPreferences p = ctx.getPreferences();
			final HttpPost request = createPOST(LOGIN_URL, getLoginPost(p
					.getString(Preferences.USER, ""), p.getString(
					Preferences.PASSWORD, "")));
			final HttpResponse response = ctx.getClient().execute(request);
			final String cutContent = cutLoginInfoFromContent(response
					.getEntity().getContent());
			int idx = cutContent.indexOf(MATCH_LOGIN_SUCCESS);
			if (cutContent.indexOf(MATCH_LOGIN_SUCCESS) == -1) {
				throw new WebSMSException(ctx.getContext(), R.string.error_pw);
			}
		} catch (final Exception e) {
			throw new WebSMSException(e.getMessage());
		}
		return true;
	}

	/**
	 * Updates balance andl pushes it to WebSMS.
	 * 
	 * @param ctx
	 *            {@link ConnectorContext}
	 * @throws WebSMSException
	 *             on an error
	 */
	private void updateBalance(final ConnectorContext ctx)
			throws WebSMSException {
		try {
			final HttpResponse response = ctx.getClient().execute(
					new HttpGet(SMS_URL));
			this.notifyFreeCount(ctx, cutFreeCountFromContent(response
					.getEntity().getContent()));

		} catch (final Exception ex) {
			throw new WebSMSException(ex.getMessage());
		}
	}

	/**
	 * Push SMS Free Count to WebSMS.
	 * 
	 * @param ctx
	 *            {@link ConnectorContext}
	 * @param content
	 *            conten to investigate.
	 */
	private void notifyFreeCount(final ConnectorContext ctx,
			final String content) {
		final Matcher m = BALANCE_MATCH_PATTERN.matcher(content);
		String term = null;
		if (m.find()) {
			term = m.group(1) + "+" + m.group(2);
		} else if (content.contains(MATCH_NO_SMS)) {
			term = "0+0";
		} else if (content.contains(MATCH_UNLIMITTED_SMS)) {
			term = "\u221E";
		} else {
			Log.w(TAG, content);
			term = "?";
		}
		this.getSpec(ctx.getContext()).setBalance(term);
	}

	/**
	 * Sends an sms via HTTP POST.
	 * 
	 * @param ctx
	 *            {@link ConnectorContext}
	 * @return successfull?
	 * @throws WebSMSException
	 *             on an error
	 */
	private boolean sendSms(final ConnectorContext ctx) throws WebSMSException {
		try {
			final HttpResponse response = ctx.getClient().execute(
					createPOST(SMS_URL, this.getSmsPost(ctx)));
			return this.afterSmsSent(ctx, response);
		} catch (final Exception ex) {
			throw new WebSMSException(ex.getMessage());
		}
	}

	/**
	 * Handles content after sms sending.
	 * 
	 * @param ctx
	 *            {@link ConnectorContext}
	 * @param response
	 *            HTTP Response
	 * @return true if arcor returns success
	 * @throws Exception
	 *             if an Error occures
	 */
	private boolean afterSmsSent(final ConnectorContext ctx,
			final HttpResponse response) throws Exception {

		final String body = cutFreeCountFromContent(response.getEntity()
				.getContent());

		final Matcher m = SEND_CHECK_STATUS_PATTERN.matcher(body);
		final boolean found = m.find();
		if (!found || m.groupCount() != 2) {
			// should not happen
			Log.w("WebSMS.ConnectorArcor", body);
			throw new Exception(ctx.getContext().getString(
					R.string.log_unknow_status_after_send));
		}

		final String status = m.group(1);
		// ok, message sent!
		if (status.equals("hint")) {
			this.notifyFreeCount(ctx, body);
			return true;
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
		sb.append(URLEncoder.encode(username, ARCOR_ENCODING));
		sb.append("&password=");
		sb.append(URLEncoder.encode(password, ARCOR_ENCODING));
		sb
				.append("&login=Login&protocol="
						+ "https&info=Online-Passwort&goto=");
		return sb.toString();
	}

	/**
	 * These post data is needed for sending a sms.
	 * 
	 * @param ctx
	 *            {@link ConnectorContext}
	 * @return array of params
	 * @throws Exception
	 *             if an error occures.
	 */
	private String getSmsPost(final ConnectorContext ctx) throws Exception {
		final StringBuilder sb = new StringBuilder();
		String[] to = ctx.getCommand().getRecipients();
		for (final String r : to) {
			sb.append(r).append(",");
		}
		final StringBuilder sb1 = new StringBuilder();
		sb1.append("empfaengerAn=");
		sb1.append(URLEncoder.encode(sb.toString(), ARCOR_ENCODING));
		sb1.append("&nachricht=");
		sb1.append(URLEncoder
				.encode(ctx.getCommand().getText(), ARCOR_ENCODING));
		sb1
				.append("&firstVisitOfPage=foo&part=0&senden=Senden&ordnername=Posteingang");

		if (ctx.getPreferences().getBoolean(Preferences.COPY_SENT_SMS,
				Boolean.TRUE)) {
			sb1.append("&gesendetkopiesms=on");
		}
		if (ctx.getPreferences().getBoolean(
				Preferences.ENABLE_VALIDATED_NUMBER, Boolean.FALSE)) {
			sb1.append("&useOwnMobile=on");
		} else {
			sb1.append("&emailAdressen=");
			String email = ctx.getPreferences().getString(Preferences.USER, "")
					+ "@arcor.de";
			email = URLEncoder.encode(email, ARCOR_ENCODING);
			sb1.append(email);
		}
		Log.w(TAG, sb1.toString());
		System.out.println(sb1.toString());
		System.err.println(sb1.toString());
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
		post.setHeader("Referer", "https://www.arcor.de/ums/ums_neu_sms.jsp");
		post.setHeader(new BasicHeader(HTTP.CONTENT_TYPE,
				URLEncodedUtils.CONTENT_TYPE));
		post.setEntity(new StringEntity(urlencodedparams));
		return post;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final void doSend(final Context context, final Intent intent)
			throws WebSMSException {
		final ConnectorContext ctx = ConnectorContext.create(context, intent);
		if (this.login(ctx)) {
			this.sendSms(ctx);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final void doUpdate(final Context context, final Intent intent)
			throws WebSMSException {
		final ConnectorContext ctx = ConnectorContext.create(context, intent);
		if (this.login(ctx)) {
			this.updateBalance(ctx);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final ConnectorSpec updateSpec(final Context context,
			final ConnectorSpec connectorSpec) {
		final SharedPreferences p = PreferenceManager
				.getDefaultSharedPreferences(context);
		if (p.getBoolean(Preferences.ENABLED, false)) {
			if (p.getString(Preferences.PASSWORD, "").length() > 0) {
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
		// return Utils.stream2str(is);
		return Utils.stream2str(is, LOGOUT_LINK_AREA_START,
				+LOGOUT_LINK_AREAD_END);
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
		// return Utils.stream2str(is);
		return Utils.stream2str(is, SMS_COUNT_AREA_START, SMS_COUNT_AREA_END);
	}
}
