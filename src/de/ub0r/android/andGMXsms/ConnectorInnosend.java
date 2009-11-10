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
				url.append("&type=2");
				url.append("&empfaenger=");
				String[] recvs = this.to;
				final int e = recvs.length;
				StringBuilder toBuf = new StringBuilder("00"
						+ recvs[0].substring(1));
				for (int j = 1; j < e; j++) {
					toBuf.append(";00");
					toBuf.append(recvs[j].substring(1));
				}
				url.append(toBuf.toString());
				toBuf = null;
				if (e > 1) {
					url.append("&massen=1");
				}
				url.append("&absender=");
				url.append("0" + WebSMS.prefsSender.substring(3));
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
				switch (ret) {
				case 100:
					return true;
				case 112:
					throw new WebSMSException(this.context,
							R.string.log_error_pw);
				default:
					throw new WebSMSException(this.context, R.string.log_error,
							" code: " + ret);
				}
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
