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

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import de.ub0r.android.websms.connector.ConnectorCommand;
import de.ub0r.android.websms.connector.ConnectorSpec;
import de.ub0r.android.websms.connector.Constants;

/**
 * Fetch all incomming Broadcasts and forward them to WebSMS.
 * 
 * @author flx
 */
public final class WebSMSReceiver extends BroadcastReceiver {
	/** Tag for debug output. */
	private static final String TAG = "WebSMS.bcr";

	/** SMS DB: address. */
	static final String ADDRESS = "address";
	/** SMS DB: person. */
	// private static final String PERSON = "person";
	/** SMS DB: date. */
	private static final String DATE = "date";
	/** SMS DB: read. */
	static final String READ = "read";
	/** SMS DB: status. */
	// private static final String STATUS = "status";
	/** SMS DB: type. */
	static final String TYPE = "type";
	/** SMS DB: body. */
	static final String BODY = "body";
	/** SMS DB: type - sent. */
	static final int MESSAGE_TYPE_SENT = 2;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onReceive(final Context context, final Intent intent) {
		final String action = intent.getAction();
		Log.d(TAG, "action: " + action);
		if (action == null) {
			return;
		}
		if (Constants.ACTION_CONNECTOR_INFO.equals(action)) {
			final ConnectorSpec specs = new ConnectorSpec(intent);
			final ConnectorCommand command = new ConnectorCommand(intent);
			WebSMS.addConnector(specs);
			// save send messages
			if (command != null
					&& command.getType() == ConnectorCommand.TYPE_SEND
					&& !specs.hasStatus(ConnectorSpec.STATUS_ERROR)) {
				this.saveMessage(context, command);
			}
			// TODO: DISPLAY notification if sending failed
		}
	}

	/**
	 * Save Message to internal database.
	 * 
	 * @param context
	 *            {@link Context}
	 * @param command
	 *            {@link ConnectorCommand}
	 */
	private void saveMessage(final Context context,
			final ConnectorCommand command) {
		if (command.getType() != ConnectorCommand.TYPE_SEND) {
			return;
		}
		final String[] recipients = command.getRecipients();
		for (int i = 0; i < recipients.length; i++) {
			if (recipients[i] == null || recipients[i].length() == 0) {
				continue; // skip empty recipients
			}
			// save sms to content://sms/sent
			ContentValues values = new ContentValues();
			values.put(ADDRESS, recipients[i]);
			values.put(READ, 1);
			values.put(TYPE, MESSAGE_TYPE_SENT);
			values.put(BODY, command.getText());
			if (command.getSendLater() > 0) {
				values.put(DATE, command.getSendLater());
			}
			context.getContentResolver().insert(
					Uri.parse("content://sms/sent"), values);
		}
	}

}
