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
package de.ub0r.android.websms.connector.sms;

import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.telephony.gsm.SmsManager;
import android.util.Log;
import de.ub0r.android.andGMXsms.Connector.WebSMSException;
import de.ub0r.android.websms.connector.ConnectorIO;

/**
 * {@link ConnectorIO} running sms IO.
 * 
 * @author flx
 */
@SuppressWarnings("deprecation")
public class ConnectorSMS extends ConnectorIO {
	/** Tag for debug output. */
	private static final String TAG = "WebSMS.sms";

	/**
	 * Create a SMS Connector.
	 * 
	 * @param c
	 *            context
	 * @param i
	 *            intent
	 */
	public ConnectorSMS(final Context c, final Intent i) {
		super(c, i);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final void doSend() throws WebSMSException {
		try {
			SmsManager sm = SmsManager.getDefault();
			for (String t : this.command.getRecipients()) {
				ArrayList<String> messages = sm.divideMessage(this.command
						.getText());
				sm.sendMultipartTextMessage(t, null, messages, null, null);
				for (String m : messages) {
					Log.d(TAG, "send sms: " + t + " text: " + m);
				}
			}
		} catch (Exception e) {
			throw new WebSMSException(e.toString());
		}
	}
}
