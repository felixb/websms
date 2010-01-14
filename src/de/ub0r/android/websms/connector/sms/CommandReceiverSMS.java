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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.gsm.SmsManager;
import android.util.Log;
import de.ub0r.android.andGMXsms.Connector.WebSMSException;
import de.ub0r.android.websms.connector.ConnectorCommand;
import de.ub0r.android.websms.connector.ConnectorSpec;
import de.ub0r.android.websms.connector.Constants;

/**
 * Receives commands coming as broadcast from WebSMS.
 * 
 * @author flx
 */
public class CommandReceiverSMS extends BroadcastReceiver {
	/** Tag for debug output. */
	private static final String TAG = "WebSMS.sms";

	/**
	 * Init ConnectorSpec.
	 * 
	 * @return ConnectorSpec
	 */
	private ConnectorSpec initSpecs() {
		// TODO: fill me
		return null;
	}

	private void send(final ConnectorCommand command) throws WebSMSException {
		try {
			SmsManager sm = SmsManager.getDefault();
			for (String t : command.getRecipients()) {
				ArrayList<String> messages = sm
						.divideMessage(command.getText());
				sm.sendMultipartTextMessage(t, null, messages, null, null);
				for (String m : messages) {
					Log.d(TAG, "send sms: " + t + " text: " + m);
				}
			}
		} catch (Exception e) {
			throw new WebSMSException(e.toString());
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void onReceive(final Context context, final Intent intent) {
		final String action = intent.getAction();
		if (action == null) {
			return;
		}
		if (Constants.ACTION_UPDATE_CONNECTOR.equals(action)) {
			final ConnectorSpec specs = this.initSpecs();
			// TODO: return specs as broadcast to WebSMS
		} else if (Constants.ACTION_RUN_CONNECTOR.equals(action)) {
			final Bundle extras = intent.getExtras();
			if (extras != null) {
				final ConnectorCommand command = new ConnectorCommand(extras
						.getBundle(Constants.EXTRAS_COMMAND));
				if (command.getType() == ConnectorCommand.TYPE_SEND) {
					try {
						this.send(command);
					} catch (WebSMSException e) {
						// TODO Auto-generated catch block
						Log.e(TAG, null, e);
					} finally {
						// TODO: send back broadcast to WebSMS
					}
				}
			}
		}
	}
}
