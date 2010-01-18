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
package de.ub0r.android.websms.connector.innosend;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;

import org.apache.http.HttpResponse;
import org.apache.http.message.BasicNameValuePair;

import android.R;
import android.text.format.DateFormat;
import android.util.Log;
import de.ub0r.android.websms.connector.common.Connector;
import de.ub0r.android.websms.connector.common.WebSMSException;

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

	/** Custom Dateformater. */
	private static final String DATEFORMAT = "dd.MM.yyyy-kk:mm";

	/** Try to send free sms. */
	private final boolean free;

	/** Innosend connector. */
	private final short connector;

	/** Innosend balance. */
	private String balance = "";

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
		super(null); // FIXME:
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
	 * @param failOnError
	 *            fail if return code is not 10*
	 * @return true if no error code
	 * @throws WebSMSException
	 *             WebSMSException
	 */
	private boolean checkReturnCode(final int ret, final String more,
			final boolean failOnError) throws WebSMSException {
		switch (ret) {
		case 100:
		case 101:
			// this.pushMessage(WebSMS.MESSAGE_LOG, more + " "
			// + this.context.getString(R.string.log_remain_free));
			return true;
		case 161:
			if (!failOnError) {
				if (this.balance.length() > 0) {
					this.balance += "/";
				}
				this.balance += this.context
						.getString(R.string.innosend_next_free)
						+ " " + more;
				// FIXME: WebSMS.SMS_BALANCE[INNOSEND] = this.balance;
				this.pushMessage(WebSMS.MESSAGE_FREECOUNT, null);
				return true;
			}
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
	 * @param updateFree
	 *            update free sms
	 * @return successful?
	 * @throws WebSMSException
	 *             WebSMSException
	 */
	private boolean sendData(final boolean updateFree) throws WebSMSException {
		// do IO
		try { // get Connection
			final StringBuilder url = new StringBuilder(URL);
			ArrayList<BasicNameValuePair> d = new ArrayList<BasicNameValuePair>();
			if (this.text != null && this.to != null && this.to.length > 0) {
				if (this.free) {
					url.append("free.php");
					d.add(new BasicNameValuePair("app", "1"));
					d.add(new BasicNameValuePair("was", "iphone"));
				} else {
					url.append("sms.php");
				}
				d.add(new BasicNameValuePair("text", this.text));
				d.add(new BasicNameValuePair("type", this.connector + ""));
				d.add(new BasicNameValuePair("empfaenger",
						international2oldformat(this.to[0])));

				if (this.customSender == null) {
					// FIXME: d.add(new BasicNameValuePair("absender",
					// international2national(WebSMS.prefsSender)));
				} else {
					d
							.add(new BasicNameValuePair("absender",
									this.customSender));
				}

				if (this.flashSMS) {
					d.add(new BasicNameValuePair("flash", "1"));
				}
				if (this.sendLater > 0) {
					if (this.sendLater <= 0) {
						this.sendLater = System.currentTimeMillis();
					}
					d.add(new BasicNameValuePair("termin", DateFormat.format(
							DATEFORMAT, this.sendLater).toString()));
				}
			} else {
				if (updateFree) {
					url.append("free.php");
					d.add(new BasicNameValuePair("app", "1"));
					d.add(new BasicNameValuePair("was", "iphone"));
				} else {
					url.append("konto.php");
				}
			}
			// FIXME: d.add(new BasicNameValuePair("id", this.user));
			// FIXME: d.add(new BasicNameValuePair("pw", this.password));
			HttpResponse response = getHttpClient(url.toString(), null, d,
					null, null);
			int resp = response.getStatusLine().getStatusCode();
			if (resp != HttpURLConnection.HTTP_OK) {
				throw new WebSMSException(this.context,
						R.string.log_error_http, "" + resp);
			}
			String htmlText = stream2str(response.getEntity().getContent())
					.trim();
			int i = htmlText.indexOf(',');
			if (i > 0 && !updateFree) {
				this.balance = htmlText.substring(0, i + 3) + "\u20AC";
				// FIXME: WebSMS.SMS_BALANCE[INNOSEND] = this.balance;
				this.pushMessage(WebSMS.MESSAGE_FREECOUNT, null);
			} else {
				i = htmlText.indexOf("<br>");
				int ret;
				Log.d(TAG, url.toString());
				if (i < 0) {
					ret = Integer.parseInt(htmlText);
					return updateFree || this.checkReturnCode(ret);
				} else {
					ret = Integer.parseInt(htmlText.substring(0, i));
					return this.checkReturnCode(ret, htmlText.substring(i + 4)
							.trim(), !updateFree);
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
		return this.sendData(false) && this.sendData(true);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final boolean sendMessage() throws WebSMSException {
		if (!this.sendData(false)) {
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
		return (this.connector == 2 && !this.free);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final boolean supportCustomsender() {
		return (this.connector != 2 && !this.free);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final boolean supportSendLater() {
		return !this.free;
	}
}
