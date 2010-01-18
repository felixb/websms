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
package de.ub0r.android.websms.connector.sloono;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URLEncoder;

import org.apache.http.HttpResponse;

import android.content.Intent;
import android.util.Log;
import de.ub0r.android.websms.connector.common.CommandReceiver;
import de.ub0r.android.websms.connector.common.WebSMSException;

/**
 * AsyncTask to manage IO to sloono.de API.
 * 
 * @author flx
 */
public class ConnectorSloono extends CommandReceiver {
	/** Tag for output. */
	private static final String TAG = "WebSMS.sloono";

	/** Sloono Gateway URL. */
	private static final String URL_SEND = "http://www.sloono.de/API/httpsms.php";
	/** Sloono Gateway URL. */
	private static final String URL_BALANCE = "http://www.sloono.de/API/httpkonto.php";

	/** Sloono connector. */
	private final short connector;

	/**
	 * Check return code from cherry-sms.com.
	 * 
	 * @param ret
	 *            return code
	 * @return true if no error code
	 * @throws WebSMSException
	 *             WebSMSException
	 */
	private boolean checkReturnCode(final int ret) throws WebSMSException {
		Log.d(TAG, "ret=" + ret);
		if (ret < 200) {
			return true;
		} else if (ret < 300) {
			throw new WebSMSException(this.context, R.string.log_error_input);
		} else {
			if (ret == 401) {
				throw new WebSMSException(this.context, R.string.log_error_pw);
			}
			throw new WebSMSException(this.context, R.string.log_error_server,
					"" + ret);
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
			final boolean checkOnly = this.text == null || this.to == null
					|| this.to.length == 0;
			final StringBuilder url = new StringBuilder();
			if (checkOnly) {
				url.append(URL_BALANCE);
			} else {
				url.append(URL_SEND);
			}
			url.append("?user=");
			// FIXME: url.append(this.user);
			url.append("&password=");
			// FIXME: url.append(this.password);

			if (!checkOnly) {
				url.append("&typ=");
				if (this.flashSMS) {
					url.append(3);
				} else {
					url.append(this.connector - SLOONO_DISCOUNT);
				}
				if (this.sendLater > 0) {
					url.append("&timestamp=");
					url.append(this.sendLater / 1000);
				}
				url.append("&text=");
				url.append(URLEncoder.encode(this.text));
				url.append("&to=");
				String[] recvs = this.to;
				final int e = recvs.length;
				StringBuilder toBuf = new StringBuilder(
						international2oldformat(recvs[0]));
				for (int j = 1; j < e; j++) {
					toBuf.append(",");
					toBuf.append(international2oldformat(recvs[j]));
				}
				url.append(toBuf.toString());
				toBuf = null;
			}
			// send data
			HttpResponse response = getHttpClient(url.toString(), null, null,
					null, null);
			int resp = response.getStatusLine().getStatusCode();
			if (resp != HttpURLConnection.HTTP_OK) {
				this.checkReturnCode(resp);
				throw new WebSMSException(this.context,
						R.string.log_error_http, "" + resp);
			}
			String htmlText = stream2str(response.getEntity().getContent())
					.trim();
			String[] lines = htmlText.split("\n");
			Log.d(TAG, htmlText);
			htmlText = null;
			for (String s : lines) {
				if (s.startsWith("Kontostand: ")) {
					// FIXME: WebSMS.SMS_BALANCE[SLOONO] =
					// s.split(" ")[1].trim() + "\u20AC";
					this.pushMessage(WebSMS.MESSAGE_FREECOUNT, null);
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
	protected final void doUpdate(final Intent intent) throws WebSMSException {
		this.sendData();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final void doSend(final Intent intent) throws WebSMSException {
		if (!this.sendData()) {
			// failed!
			throw new WebSMSException(this.context, R.string.log_error);
		}
	}
}
