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

import java.util.ArrayList;

import android.telephony.gsm.SmsManager;
import android.util.Log;

/**
 * AsyncTask to manage IO by standard sms methods.
 * 
 * @author flx
 */
@SuppressWarnings("deprecation")
public class ConnectorSMS extends Connector {
	/** Tag for debug output. */
	private static final String TAG = "WebSMS.sms";

	/**
	 * Create a SMS Connector.
	 */
	public ConnectorSMS() {
		super(null, null, SMS);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final boolean updateMessages() {
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final boolean sendMessage() throws WebSMSException {
		try {
			SmsManager sm = SmsManager.getDefault();
			for (String t : this.to) {
				ArrayList<String> messages = sm.divideMessage(this.text);
				sm.sendMultipartTextMessage(t, null, messages, null, null);
				for (String m : messages) {
					Log.d(TAG, "send sms: " + t + " text: " + m);
				}
			}
			return true;
		} catch (Exception e) {
			Log.e(TAG, "inner exception", e);
			throw new WebSMSException(e.toString());
		}
	}
}
