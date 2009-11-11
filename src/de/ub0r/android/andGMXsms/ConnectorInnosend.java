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

	/** Innosend connector. */
	private final short connector;

	/**
	 * Public Connector Constructor.
	 * 
	 * @param con
	 *            connector type
	 */
	public ConnectorInnosend(final short con) {
		switch (con) {
		case INNOSEND_FREE:
			this.connector = 0;
			break; // FIXME
		case INNOSEND_WO_SENDER:
			this.connector = 2;
			break;
		case INNOSEND_W_SENDER:
			this.connector = 4;
			break;
		default:
			this.connector = 2;
			break;
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
				url.append("sms.php?");
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
				WebSMS.BALANCE_INNOSEND = htmlText.substring(0, i + 3);
				this.pushMessage(WebSMS.MESSAGE_FREECOUNT, null);
			} else {
				int ret = Integer.parseInt(htmlText);
				Log.d(TAG, url.toString());
				return this.checkReturnCode(ret);
			}
		} catch (IOException e) {
			Log.e(TAG, null, e);
			this.pushMessage(WebSMS.MESSAGE_LOG, e.toString());
			return false;
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
			this.pushMessage(WebSMS.MESSAGE_LOG, R.string.log_error);
			return false;
		} else {
			// result: ok
			return true;
		}
	}
}
