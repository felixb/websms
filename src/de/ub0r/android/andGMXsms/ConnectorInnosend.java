/*
 * Copyright (C) 2009 Felix Bechstein
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
import java.net.URLEncoder;

import org.apache.http.HttpResponse;

import android.util.Log;

/**
 * AsyncTask to manage IO to Innosend.de API.
 * 
 * @author flx
 */
public class ConnectorInnosend extends Connector {
	/** Tag for output. */
	private static final String TAG = "WebSMS.inno";

	/** Innosend Gateway URL. */
	private static final String URL = "https://www.innosend.de/gateway/";

	/** Try to send free sms. */
	private final boolean free;

	/** Innosend connector. */
	private final short connector;

	/**
	 * Create an Innosend.de.
	 * 
	 * @param u
	 *            user
	 * @param p
	 *            password
	 * @param con
	 *            connector type
	 */
	public ConnectorInnosend(final String u, final String p, final short con) {
		super(u, p, con);
		switch (con) {
		case INNOSEND_FREE:
			this.connector = 2;
			this.free = true;
			break;
		case INNOSEND_WO_SENDER:
			this.connector = 2;
			this.free = false;
			break;
		case INNOSEND_W_SENDER:
			this.free = false;
			this.connector = 4;
			break;
		default:
			this.connector = 2;
			this.free = false;
			break;
		}
	}

	/**
	 * Check return code from innosend.de.
	 * 
	 * @param ret
	 *            return code
	 * @param more
	 *            more text
	 * @return true if no error code
	 * @throws WebSMSException
	 *             WebSMSException
	 */
	private boolean checkReturnCode(final int ret, final String more)
			throws WebSMSException {
		switch (ret) {
		case 100:
		case 101:
			// this.pushMessage(WebSMS.MESSAGE_LOG, more + " "
			// + this.context.getString(R.string.log_remain_free));
			return true;
		case 161:
			throw new WebSMSException(this.context,
					R.string.log_error_innosend_161, " " + more);
		default:
			throw new WebSMSException(this.context, R.string.log_error,
					" code: " + ret + " " + more);
		}
	}

	/**
	 * Check return code from innosend.de.
	 * 
	 * @param ret
	 *            return code
	 * @return true if no error code
	 * @throws WebSMSException
	 *             WebSMSException
	 */
	private boolean checkReturnCode(final int ret) throws WebSMSException {
		switch (ret) {
		case 100:
		case 101:
			return true;
		case 111:
			throw new WebSMSException(this.context,
					R.string.log_error_innosend_111);
		case 112:
			throw new WebSMSException(this.context, R.string.log_error_pw);
		case 120:
			throw new WebSMSException(this.context,
					R.string.log_error_innosend_111);
		case 121:
			throw new WebSMSException(this.context,
					R.string.log_error_innosend_121);
		case 122:
			throw new WebSMSException(this.context,
					R.string.log_error_innosend_122);
		case 123:
			throw new WebSMSException(this.context,
					R.string.log_error_innosend_123);
		case 129:
			throw new WebSMSException(this.context,
					R.string.log_error_innosend_129);
		case 130:
			throw new WebSMSException(this.context,
					R.string.log_error_innosend_130);
		case 140:
			throw new WebSMSException(this.context,
					R.string.log_error_innosend_140);
		case 150:
			throw new WebSMSException(this.context,
					R.string.log_error_innosend_150);
		case 170:
			throw new WebSMSException(this.context,
					R.string.log_error_innosend_170);
		case 171:
			throw new WebSMSException(this.context,
					R.string.log_error_innosend_171);
		case 172:
			throw new WebSMSException(this.context,
					R.string.log_error_innosend_172);
		case 173:
			throw new WebSMSException(this.context,
					R.string.log_error_innosend_173);
		default:
			throw new WebSMSException(this.context, R.string.log_error,
					" code: " + ret);
		}
	}

	/**
	 * Send data.
	 * 
	 * @return successful?
	 * @throws WebSMSException
	 *             WebSMSException
	 */
	private boolean sendData() throws WebSMSException {
		// do IO
		try { // get Connection
			final StringBuilder url = new StringBuilder(URL);
			if (this.text != null && this.to != null && this.to.length > 0) {
				if (this.free) {
					url.append("free.php?app=1&was=iphone&");
				} else {
					url.append("sms.php?");
				}
				url.append("text=");
				url.append(URLEncoder.encode(this.text));
				url.append("&type=");
				url.append(this.connector);
				url.append("&empfaenger=");
				String[] recvs = this.to;
				final int e = recvs.length;
				StringBuilder toBuf = new StringBuilder(
						international2oldformat(recvs[0]));
				for (int j = 1; j < e; j++) {
					toBuf.append(";");
					toBuf.append(international2oldformat(recvs[j]));
				}
				url.append(toBuf.toString());
				toBuf = null;
				if (e > 1) {
					url.append("&massen=1");
				}
				url.append("&absender=");
				url.append(international2national(WebSMS.prefsSender));
				url.append('&');
			} else {
				url.append("konto.php?");
			}
			url.append("id=");
			url.append(this.user);
			url.append("&pw=");
			url.append(this.password);
			HttpResponse response = getHttpClient(url.toString(), null, null,
					null, null);
			int resp = response.getStatusLine().getStatusCode();
			if (resp != HttpURLConnection.HTTP_OK) {
				throw new WebSMSException(this.context,
						R.string.log_error_http, "" + resp);
			}
			String htmlText = stream2str(response.getEntity().getContent())
					.trim();
			int i = htmlText.indexOf(',');
			if (i > 0) {
				WebSMS.SMS_BALANCE[INNOSEND] = htmlText.substring(0, i + 3)
						+ "\u20AC";
				this.pushMessage(WebSMS.MESSAGE_FREECOUNT, null);
			} else {
				i = htmlText.indexOf("<br>");
				int ret;
				Log.d(TAG, url.toString());
				if (i < 0) {
					ret = Integer.parseInt(htmlText);
					return this.checkReturnCode(ret);
				} else {
					ret = Integer.parseInt(htmlText.substring(0, i));
					return this.checkReturnCode(ret, htmlText.substring(i + 4)
							.trim());
				}
			}
		} catch (IOException e) {
			Log.e(TAG, null, e);
			throw new WebSMSException(e.getMessage());
		}
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final boolean updateMessages() throws WebSMSException {
		return this.sendData();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final boolean sendMessage() throws WebSMSException {
		if (!this.sendData()) {
			// failed!
			throw new WebSMSException(this.context, R.string.log_error);
		} else {
			// result: ok
			return true;
		}
	}
}
