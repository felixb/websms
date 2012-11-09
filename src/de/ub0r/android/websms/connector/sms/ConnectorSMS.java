/*
 * Copyright (C) 2010-2011 Felix Bechstein
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

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.widget.Toast;
import de.ub0r.android.lib.Log;
import de.ub0r.android.websms.R;
import de.ub0r.android.websms.connector.common.Connector;
import de.ub0r.android.websms.connector.common.ConnectorCommand;
import de.ub0r.android.websms.connector.common.ConnectorService;
import de.ub0r.android.websms.connector.common.ConnectorSpec;
import de.ub0r.android.websms.connector.common.ConnectorSpec.SubConnectorSpec;
import de.ub0r.android.websms.connector.common.Utils;
import de.ub0r.android.websms.connector.common.WebSMSException;

/**
 * Receives commands coming as broadcast from WebSMS.
 * 
 * @author flx
 */
public class ConnectorSMS extends Connector {
	/** Tag for debug output. */
	private static final String TAG = "sms";

	/** Preference key: enabled. */
	private static final String PREFS_ENABLED = "enable_sms";

	/** Message set action. */
	public static final String MESSAGE_SENT_ACTION = "com.android.mms.transaction.MESSAGE_SENT";

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final ConnectorSpec initSpec(final Context context) {
		final String name = context.getString(R.string.connector_sms_name);
		final ConnectorSpec c = new ConnectorSpec(name);
		c.setAuthor(context.getString(R.string.connector_sms_author));
		c.setBalance(null);
		c.setCapabilities(ConnectorSpec.CAPABILITIES_SEND);
		c.addSubConnector(TAG, name, SubConnectorSpec.FEATURE_MULTIRECIPIENTS);
		return c;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final ConnectorSpec updateSpec(final Context context,
			final ConnectorSpec connectorSpec) {
		final SharedPreferences p = PreferenceManager
				.getDefaultSharedPreferences(context);
		if (p.getBoolean(PREFS_ENABLED, false)) {
			connectorSpec.setReady();
		} else {
			connectorSpec.setStatus(ConnectorSpec.STATUS_INACTIVE);
		}
		return connectorSpec;
	}

	/**
	 * Send a message.
	 * 
	 * @param context
	 *            {@link Context}
	 * @param specs
	 *            {@link ConnectorSpec}s
	 * @param command
	 *            command coming from intent
	 */
	private void send(final Context context, final ConnectorSpec specs,
			final ConnectorCommand command) {
		try {
			final String[] r = command.getRecipients();
			final String text = command.getText();
			final long msgId = command.getMsgId();
			Log.d(TAG, "text: " + text);
			int[] l = SmsMessage.calculateLength(text, false);
			Log.i(TAG, "text7: " + text.length() + ", " + l[0] + " " + l[1]
					+ " " + l[2] + " " + l[3]);
			l = SmsMessage.calculateLength(text, true);
			Log.i(TAG, "text8: " + text.length() + ", " + l[0] + " " + l[1]
					+ " " + l[2] + " " + l[3]);
			SmsManager smsmgr = SmsManager.getDefault();
			for (String t : r) {
				Log.d(TAG, "send messages to: " + t);
				final ArrayList<String> messages = smsmgr.divideMessage(text);
				final int c = messages.size();
				final ArrayList<PendingIntent> sentIntents = new ArrayList<PendingIntent>(
						c);
				for (int i = 0; i < c; i++) {
					final String m = messages.get(i);
					Log.d(TAG, "devided messages: " + m);

					final Intent sent = new Intent(MESSAGE_SENT_ACTION, null,
							context, ConnectorSMS.class);
					command.setToIntent(sent);
					specs.setToIntent(sent);
					sentIntents.add(PendingIntent.getBroadcast(context,
							(int) msgId, sent,
							PendingIntent.FLAG_UPDATE_CURRENT));
				}
				final NotificationManager nm = (NotificationManager) context
						.getSystemService(Context.NOTIFICATION_SERVICE);
				nm.notify(ConnectorService.NOTIFICATION_PENDING,
						ConnectorService.getNotification(context, command));
				smsmgr.sendMultipartTextMessage(Utils.getRecipientsNumber(t),
						null, messages, sentIntents, null);
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
		Log.d(TAG, "action: " + action);
		if (action == null) {
			return;
		}
		if (MESSAGE_SENT_ACTION.equals(action)) {
			final int resultCode = this.getResultCode();
			final Uri uri = intent.getData();
			Log.d(TAG, "sent message: " + uri + ", rc: " + resultCode);

			final ConnectorSpec specs = new ConnectorSpec(intent);
			final ConnectorCommand command = new ConnectorCommand(intent);

			final NotificationManager nm = (NotificationManager) context
					.getSystemService(Context.NOTIFICATION_SERVICE);
			nm.cancel(ConnectorService.NOTIFICATION_PENDING);

			if (resultCode != Activity.RESULT_OK) {
				specs.setErrorMessage(context.getString(R.string.log_error_sms)
						+ resultCode);
			}
			ConnectorSMS.this.sendInfo(context, specs, command);
		} else if (ACTION_CONNECTOR_UPDATE.equals(action)) {
			this.sendInfo(context, null, null);
		} else if (action.endsWith(ACTION_RUN_SEND)) {
			final ConnectorCommand command = new ConnectorCommand(intent);
			if (command.getType() == ConnectorCommand.TYPE_SEND) {
				final ConnectorSpec origSpecs = new ConnectorSpec(intent);
				final ConnectorSpec specs = this.getSpec(context);
				if (specs.equals(origSpecs)
						&& specs.hasStatus(ConnectorSpec.STATUS_READY)) {
					// check internal status
					try {
						this.send(context, specs, command);
					} catch (WebSMSException e) {
						Log.e(TAG, null, e);
						Toast.makeText(context,
								specs.getName() + ": " + e.getMessage(),
								Toast.LENGTH_LONG).show();
						specs.setErrorMessage(context, e);
					}
					try {
						this.setResultCode(Activity.RESULT_OK);
					} catch (Exception e) {
						Log.w(TAG, "not an ordered boradcast", e);
					}
				}
			}
		}
	}
}
