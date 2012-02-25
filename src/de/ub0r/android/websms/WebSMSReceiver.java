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
package de.ub0r.android.websms;

import java.util.ArrayList;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.widget.Toast;
import de.ub0r.android.websms.connector.common.Connector;
import de.ub0r.android.websms.connector.common.ConnectorCommand;
import de.ub0r.android.websms.connector.common.ConnectorSpec;
import de.ub0r.android.websms.connector.common.Log;
import de.ub0r.android.websms.connector.common.Utils;

/**
 * Fetch all incoming Broadcasts and forward them to WebSMS.
 * 
 * @author flx
 */
public final class WebSMSReceiver extends BroadcastReceiver {
	/** Tag for debug output. */
	private static final String TAG = "bcr";

	/** {@link Uri} for saving messages. */
	private static final Uri URI_SMS = Uri.parse("content://sms");
	/** {@link Uri} for saving sent messages. */
	private static final Uri URI_SENT = Uri.parse("content://sms/sent");
	/** Projection for getting the id. */
	private static final String[] PROJECTION_ID = new String[] { BaseColumns._ID };

	/** Intent's scheme to send sms. */
	private static final String INTENT_SCHEME_SMSTO = "smsto";

	/** ACTION for publishing information about sent websms. */
	private static final String ACTION_CM_WEBSMS = "de.ub0r.android.callmeter.SAVE_WEBSMS";
	/** Extra holding uri of sent sms. */
	private static final String EXTRA_WEBSMS_URI = "uri";
	/** Extra holding name of connector. */
	private static final String EXTRA_WEBSMS_CONNECTOR = "connector";

	/** Vibrate x seconds on send. */
	private static final long VIBRATOR_SEND = 100L;

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
	/** SMS DB: type - draft. */
	static final int MESSAGE_TYPE_DRAFT = 3;

	/** Next notification ID. */
	private static int nextNotificationID = 1;

	/** LED color for notification. */
	private static final int NOTIFICATION_LED_COLOR = 0xffff0000;
	/** LED blink on (ms) for notification. */
	private static final int NOTIFICATION_LED_ON = 500;
	/** LED blink off (ms) for notification. */
	private static final int NOTIFICATION_LED_OFF = 2000;

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
		if (Connector.ACTION_INFO.equals(action)) {
			WebSMSReceiver.handleInfoAction(context, intent);
		} else if (Connector.ACTION_CAPTCHA_REQUEST.equals(action)) {
			final Intent i = new Intent(context, CaptchaActivity.class);
			i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			i.putExtras(intent.getExtras());
			context.startActivity(i);
		}
	}

	/**
	 * Fetch INFO broadcast.
	 * 
	 * @param context
	 *            context
	 * @param intent
	 *            intent
	 */
	private static void handleInfoAction(final Context context,
			final Intent intent) {
		final ConnectorSpec specs = new ConnectorSpec(intent);
		final ConnectorCommand command = new ConnectorCommand(intent);

		if (specs == null) {
			// security check. some other apps may send faulty broadcasts
			return;
		}

		try {
			WebSMS.addConnector(specs);
		} catch (Exception e) {
			Log.e(TAG, "error while receiving broadcast", e);
		}
		// save send messages
		if (command != null && command.getType() == ConnectorCommand.TYPE_SEND) {
			handleSendCommand(specs, context, intent, command);
		}
	}

	/**
	 * Save sent message or display error notification if failed sending.
	 * 
	 * @param specs
	 *            {@link ConnectorSpec}
	 * @param context
	 *            context
	 * @param intent
	 *            intent
	 * @param command
	 *            {@link ConnectorCommand}
	 */
	static void handleSendCommand(final ConnectorSpec specs,
			final Context context, final Intent intent,
			final ConnectorCommand command) {

		if (!specs.hasStatus(ConnectorSpec.STATUS_ERROR)) {
			saveMessage(specs, context, command, MESSAGE_TYPE_SENT);
			final SharedPreferences p = PreferenceManager
					.getDefaultSharedPreferences(context);
			if (p.getBoolean(WebSMS.PREFS_SEND_VIBRATE, false)) {
				final Vibrator v = (Vibrator) context
						.getSystemService(Context.VIBRATOR_SERVICE);
				if (v != null) {
					v.vibrate(VIBRATOR_SEND);
					v.cancel();
				}
			}
			return;
		}
		// Display notification if sending failed
		final String[] r = command.getRecipients();
		final int l = r.length;
		StringBuilder buf = new StringBuilder(r[0]);
		for (int i = 1; i < l; i++) {
			buf.append(", ");
			buf.append(r[i]);
		}
		final String to = buf.toString();
		buf = null;

		Notification n = new Notification(R.drawable.stat_notify_sms_failed,
				context.getString(R.string.notify_failed_),
				System.currentTimeMillis());
		final Intent i = new Intent(Intent.ACTION_SENDTO,
				Uri.parse(INTENT_SCHEME_SMSTO + ":" + Uri.encode(to)), context,
				WebSMS.class);
		// add pending intent
		i.putExtra(Intent.EXTRA_TEXT, command.getText());
		i.putExtra(WebSMS.EXTRA_ERRORMESSAGE, specs.getErrorMessage());
		i.setFlags(i.getFlags() | Intent.FLAG_ACTIVITY_NEW_TASK);
		final PendingIntent cIntent = PendingIntent.getActivity(context, 0, i,
				PendingIntent.FLAG_CANCEL_CURRENT);
		n.setLatestEventInfo(context, context.getString(R.string.notify_failed)
				+ " " + specs.getErrorMessage(), to + ": " + command.getText(),
				cIntent);
		n.flags |= Notification.FLAG_AUTO_CANCEL;

		n.flags |= Notification.FLAG_SHOW_LIGHTS;
		n.ledARGB = NOTIFICATION_LED_COLOR;
		n.ledOnMS = NOTIFICATION_LED_ON;
		n.ledOffMS = NOTIFICATION_LED_OFF;

		final SharedPreferences p = PreferenceManager
				.getDefaultSharedPreferences(context);
		final boolean vibrateOnFail = p.getBoolean(WebSMS.PREFS_FAIL_VIBRATE,
				false);
		final String s = p.getString(WebSMS.PREFS_FAIL_SOUND, null);
		Uri soundOnFail;
		if (s == null || s.length() <= 0) {
			soundOnFail = null;
		} else {
			soundOnFail = Uri.parse(s);
		}

		if (vibrateOnFail) {
			n.defaults |= Notification.DEFAULT_VIBRATE;
		}
		n.sound = soundOnFail;

		NotificationManager mNotificationMgr = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationMgr.notify(getNotificationID(), n);
	}

	/**
	 * Get a fresh and unique ID for a new notification.
	 * 
	 * @return return the ID
	 */
	private static synchronized int getNotificationID() {
		++nextNotificationID;
		return nextNotificationID;
	}

	/**
	 * Save Message to internal database.
	 * 
	 * @param specs
	 *            {@link ConnectorSpec}
	 * @param context
	 *            {@link Context}
	 * @param command
	 *            {@link ConnectorCommand}
	 * @param msgType
	 *            sent or draft?
	 */
	static void saveMessage(final ConnectorSpec specs, final Context context,
			final ConnectorCommand command, final int msgType) {
		if (command.getType() != ConnectorCommand.TYPE_SEND) {
			return;
		}
		if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
				WebSMS.PREFS_DROP_SENT, false)) {
			Log.i(TAG, "drop sent messages");
			return;
		}
		final ContentResolver cr = context.getContentResolver();
		final ContentValues values = new ContentValues();
		values.put(TYPE, msgType);

		if (msgType == MESSAGE_TYPE_SENT) {
			final String[] uris = command.getMsgUris();
			if (uris != null && uris.length > 0) {
				for (String s : uris) {
					final Uri u = Uri.parse(s);
					try {
						final int updated = cr.update(u, values, null, null);
						Log.d(TAG, "updated: " + updated);
						if (updated > 0
								&& specs != null
								&& !specs.getPackage().equals(
										"de.ub0r.android.websms.connector."
												+ "sms")) {
							final Intent intent = new Intent(ACTION_CM_WEBSMS);
							intent.setFlags(intent.getFlags()
									| Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
							intent.putExtra(EXTRA_WEBSMS_URI, u.toString());
							intent.putExtra(EXTRA_WEBSMS_CONNECTOR, specs
									.getName().toLowerCase());
							context.sendBroadcast(intent);
						}
					} catch (SQLiteException e) {
						Log.e(TAG, "error updating sent message: " + u, e);
						Toast.makeText(context,
								R.string.log_error_saving_message,
								Toast.LENGTH_LONG).show();
					}
				}
				return; // skip legacy saving
			}
		}

		final String text = command.getText();

		Log.d(TAG, "save message(s):");
		Log.d(TAG, "type: " + msgType);
		Log.d(TAG, "TEXT: " + text);
		values.put(READ, 1);
		values.put(BODY, text);
		if (command.getSendLater() > 0) {
			values.put(DATE, command.getSendLater());
			Log.d(TAG, "DATE: " + command.getSendLater());
		}
		final String[] recipients = command.getRecipients();
		final ArrayList<String> inserted = new ArrayList<String>(
				recipients.length);
		for (int i = 0; i < recipients.length; i++) {
			if (recipients[i] == null || recipients[i].trim().length() == 0) {
				continue; // skip empty recipients

			}
			String address = Utils.getRecipientsNumber(recipients[i]);
			Log.d(TAG, "TO: " + address);
			try {
				final Cursor c = cr.query(URI_SMS, PROJECTION_ID,
						TYPE + " = " + MESSAGE_TYPE_DRAFT + " AND " + ADDRESS
								+ " = '" + address + "' AND " + BODY
								+ " like '" + text.replace("'", "_") + "'",
						null, DATE + " DESC");
				if (c != null && c.moveToFirst()) {
					final Uri u = URI_SENT.buildUpon()
							.appendPath(c.getString(0)).build();
					Log.d(TAG, "skip saving draft: " + u);
					inserted.add(u.toString());
				} else {
					final ContentValues cv = new ContentValues(values);
					cv.put(ADDRESS, address);
					// save sms to content://sms/sent
					inserted.add(cr.insert(URI_SENT, cv).toString());
				}
				if (c != null && !c.isClosed()) {
					c.close();
				}
			} catch (IllegalArgumentException e) {
				Log.e(TAG, "failed saving message", e);
				Toast.makeText(context, R.string.log_error_saving_message,
						Toast.LENGTH_LONG).show();
			}
		}
		if (msgType == MESSAGE_TYPE_DRAFT && inserted.size() > 0) {
			command.setMsgUris(inserted.toArray(new String[] {}));
		}
	}
}
