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

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

/**
 * IOService handles all IO as a service. Call it with RPC!
 * 
 * @author flx
 */
public class IOService extends Service {
	/** Tag for output. */
	private static final String TAG = "WebSMS.IO";

	/** Intent's action for sending a message. */
	static final String INTENT_ACTION = "de.ub0r.andGMXsms.send";
	/** Intent's extra for params. */
	static final String INTENT_PARAMS = "prams";
	/** Intent's extra for connector. */
	static final String INTENT_CONNECTOR = "connector";

	/** Ref to single instance. */
	private static IOService me = null;

	/** Number of jobs running. */
	private static int currentIOOps = 0;

	/** Notification ID of this Service. */
	private static final int NOTIFICATION_PENDING = 0;

	/** A list of notifications to display on destroy. */
	private static ArrayList<Notification> notifications = new ArrayList<Notification>();

	/** Next notification ID. */
	private static int nextNotificationID = 1;

	/** Wrapper for API5 commands. */
	private HelperAPI5Service helperAPI5s = null;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final IBinder onBind(final Intent intent) {
		Log.d(TAG, "onBind()");
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void onCreate() {
		super.onCreate();
		Log.d(TAG, "onCreate()");
		me = this;

		try {
			this.helperAPI5s = new HelperAPI5Service();
			if (!this.helperAPI5s.isAvailable()) {
				this.helperAPI5s = null;
			}
		} catch (VerifyError e) {
			this.helperAPI5s = null;
			Log.d(TAG, "no api5 running", e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void onStart(final Intent intent, final int startId) {
		if (intent != null) {
			final String a = intent.getAction();
			if (a != null && a.equals(INTENT_ACTION)) {
				final Bundle b = intent.getExtras();
				final String[] params = b.getStringArray(INTENT_PARAMS);
				final ConnectorSpecs connector = Connector.getConnectorSpecs(
						this, b.getString(INTENT_CONNECTOR));
				Connector.send(IOService.this, connector, params);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final int onStartCommand(final Intent intent, final int flags,
			final int startId) {
		this.onStart(intent, startId);
		return START_NOT_STICKY;
	}

	/**
	 * Display Notification for failed message.
	 * 
	 * @param n
	 *            Notification
	 */
	private void displayFailedNotification(final Notification n) {
		n.flags |= Notification.FLAG_SHOW_LIGHTS;
		n.ledARGB = 0xffff0000;
		n.ledOnMS = 500;
		n.ledOffMS = 2000;

		if (WebSMS.prefsVibrateOnFail) {
			n.flags |= Notification.DEFAULT_VIBRATE;
		}
		n.sound = WebSMS.prefsSoundOnFail;

		NotificationManager mNotificationMgr = (NotificationManager) this
				.getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationMgr.notify(getNotificationID(), n);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void onDestroy() {
		super.onDestroy();
		Log.d(TAG, "onDestroy()");
		Log.d(TAG, "currentIOOps=" + currentIOOps);
		final int s = IOService.notifications.size();
		for (int i = 0; i < s; i++) {
			this.displayFailedNotification(IOService.notifications.get(i));
		}
		this.displayNotification(0);
	}

	/**
	 * Register a IO task.
	 * 
	 * @param n
	 *            Notification for pending message
	 */
	public static final synchronized void register(final Notification n) {
		Log.d(TAG, "register(" + n + ")");
		Log.d(TAG, "currentIOOps=" + currentIOOps);
		notifications.add(n);
		++currentIOOps;
		me.displayNotification(currentIOOps);
		Log.d(TAG, "currentIOOps=" + currentIOOps);
	}

	/**
	 * Unregister a IO task.
	 * 
	 * @param n
	 *            Notification for pending message
	 * @param failed
	 *            IO failed?
	 */
	public static final synchronized void unregister(final Notification n,
			final boolean failed) {
		Log.d(TAG, "unregister(" + n + ", " + failed + ")");
		Log.d(TAG, "currentIOOps=" + currentIOOps);
		if (failed) {
			me.displayFailedNotification(n);
		}
		notifications.remove(n);
		--currentIOOps;
		me.displayNotification(currentIOOps);
		if (currentIOOps <= 0) {
			me.stopSelf();
		}
		Log.d(TAG, "currentIOOps=" + currentIOOps);
	}

	/**
	 * Display a notification for pending send.
	 * 
	 * @param count
	 *            number of pending messages?
	 */
	private void displayNotification(final int count) {
		Log.d(TAG, "displayNotification(" + count + ")");
		NotificationManager mNotificationMgr = (NotificationManager) this
				.getSystemService(Context.NOTIFICATION_SERVICE);
		if (count <= 0) {
			// set background
			if (this.helperAPI5s == null) {
				this.setForeground(false);
			} else {
				this.helperAPI5s.stopForeground(this, true);
			}
			mNotificationMgr.cancel(NOTIFICATION_PENDING);
		} else {
			// set foreground, don't let kill while IO
			final Notification notification = new Notification(
					R.drawable.stat_notify_sms_pending, this
							.getString(R.string.notify_sending), System
							.currentTimeMillis());
			final PendingIntent contentIntent = PendingIntent.getActivity(this,
					0, new Intent(this, WebSMS.class), 0);
			notification.setLatestEventInfo(this, this
					.getString(R.string.notify_sending), "", contentIntent);
			notification.defaults |= Notification.FLAG_NO_CLEAR;
			mNotificationMgr.notify(NOTIFICATION_PENDING, notification);
			if (this.helperAPI5s == null) {
				this.setForeground(true);
			} else {
				this.helperAPI5s.startForeground(this, NOTIFICATION_PENDING,
						notification);
			}
		}
		Log.d(TAG, "displayNotification(" + count + ") return");
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
}
