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

import java.util.ArrayList;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
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

	/** The IBinder RPC Interface. */
	private final IIOOp.Stub mBinder = new IIOOp.Stub() {
		public void sendMessage(final int connector, final String[] params) {
			Connector.send(IOService.this, (short) connector, params);
		}
	};

	/**
	 * {@inheritDoc}
	 */
	public final IBinder onBind(final Intent intent) {
		Log.d(TAG, "onBind()");
		return this.mBinder;
	}

	/**
	 * Called when all clients have disconnected from a particular interface
	 * published by the service.
	 * 
	 * @param intent
	 *            The Intent that was used to bind to this service, as given to
	 *            Context.bindService. Note that any extras that were included
	 *            with the Intent at that point will not be seen here.
	 * @return Return true if you would like to have the service's
	 *         onRebind(Intent) method later called when new clients bind to it.
	 */
	@Override
	public final boolean onUnbind(final Intent intent) {
		Log.d(TAG, "onUnbind()");
		Log.d(TAG, "currentIOOps=" + currentIOOps);
		if (currentIOOps <= 0) {
			this.stopSelf();
		}
		Log.d(TAG, "onUnbind() return true");
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void onCreate() {
		super.onCreate();
		Log.d(TAG, "onCreate()");
		me = this;
	}

	/**
	 * Display Notification for failed message.
	 * 
	 * @param n
	 *            Notification
	 */
	private void displayFailedNotification(final Notification n) {
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
		if (count == 0) {
			this.setForeground(false);
			// set background
			try {
				new HelperAPI5().stopForeground(this, true);
			} catch (VerifyError e) {
				Log.d(TAG, "no api5 running");
			}
			mNotificationMgr.cancel(NOTIFICATION_PENDING);
		} else {
			// set foreground, don't let kill while IO
			final Notification notification = new Notification(
					R.drawable.stat_notify_sms_pending, "", System
							.currentTimeMillis());
			final PendingIntent contentIntent = PendingIntent.getActivity(this,
					0, new Intent(this, WebSMS.class), 0);
			notification.setLatestEventInfo(this, this
					.getString(R.string.notify_sending), "", contentIntent);
			notification.defaults |= Notification.FLAG_NO_CLEAR;
			mNotificationMgr.notify(NOTIFICATION_PENDING, notification);
			this.setForeground(true);
			try {
				new HelperAPI5().startForeground(this, NOTIFICATION_PENDING,
						notification);
			} catch (VerifyError e) {
				Log.d(TAG, "no api5 running");
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
