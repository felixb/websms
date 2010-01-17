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
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

/**
 * {@link Service} run by the connectors BroadcastReceiver.
 * 
 * @author flx
 */
public abstract class ConnectorService extends Service {
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
	public final IBinder onBind(final Intent intent) {
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void onCreate() {
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
	public final synchronized void register() {
		Log.d(TAG, "register()");
		Log.d(TAG, "currentIOOps=" + this.running);
		++this.running;
		Log.d(TAG, "currentIOOps=" + this.running);
	}

	/**
	 * Unregister a IO task.
	 */
	public final synchronized void unregister() {
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
	public final void onStart(final Intent intent, final int startId) {
		if (intent != null) {
			final String a = intent.getAction();
			if (a != null && // .
					(a.equals(CommandReceiver.ACTION_CONNECTOR_RUN_BOOSTRAP)
							|| a.equals(// .
									CommandReceiver.ACTION_CONNECTOR_RUN_UPDATE) // .
					|| a.equals(CommandReceiver.ACTION_CONNECTOR_RUN_SEND))) {
				// TODO: setForeground / startForeground
				this.startConnectorTask(this, intent);
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
	 * Start a connectorTask.
	 * 
	 * @param context
	 *            context
	 * @param intent
	 *            intent
	 */
	protected abstract void startConnectorTask(final Context context,
			final Intent intent);
}
