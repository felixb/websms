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
package de.ub0r.android.websms.connector.common;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

/**
 * {@link Service} run by the connectors BroadcastReceiver.
 * 
 * @author flx
 */
public final class ConnectorService extends Service {
	/** Tag for output. */
	private static final String TAG = "WebSMS.IO";

	/** Wrapper for API5 commands. */
	private HelperAPI5Service helperAPI5s = null;

	/** Number of running Tasks. */
	private int running = 0;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IBinder onBind(final Intent intent) {
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onCreate() {
		super.onCreate();
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
	 * Register a IO task.
	 */
	public synchronized void register() {
		Log.d(TAG, "register()");
		Log.d(TAG, "currentIOOps=" + this.running);
		++this.running;
		Log.d(TAG, "currentIOOps=" + this.running);
	}

	/**
	 * Unregister a IO task.
	 */
	public synchronized void unregister() {
		Log.d(TAG, "unregister()");
		Log.d(TAG, "currentIOOps=" + this.running);
		--this.running;
		if (this.running <= 0) {
			this.stopSelf();
		}
		Log.d(TAG, "currentIOOps=" + this.running);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onStart(final Intent intent, final int startId) {
		if (intent != null) {
			final String a = intent.getAction();
			Log.d("WebSMS.service", "action: " + a);
			if (a != null && // .
					(a.equals(CommandReceiver.ACTION_RUN_BOOSTRAP)
							|| a.equals(CommandReceiver.ACTION_RUN_UPDATE) || a
							.equals(CommandReceiver.ACTION_RUN_SEND))) {
				// TODO: setForeground / startForeground
				try {
					new ConnectorTask(intent, CommandReceiver.getInstance(),
							this).execute((Void[]) null);
				} catch (WebSMSException e) {
					Log.e(TAG, "error starting service", e);
					Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG)
							.show();
				}
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int onStartCommand(final Intent intent, final int flags,
			final int startId) {
		this.onStart(intent, startId);
		return START_NOT_STICKY;
	}
}
