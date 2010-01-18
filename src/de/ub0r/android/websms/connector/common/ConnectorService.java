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

import java.util.ArrayList;

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
final class ConnectorService extends Service {
	/** Tag for output. */
	private static final String TAG = "WebSMS.IO";

	/** Wrapper for API5 commands. */
	private HelperAPI5Service helperAPI5s = null;

	/** Pending tasks. */
	private final ArrayList<Intent> pendingIOOps = new ArrayList<Intent>();

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
			Log.d(TAG, "no api5 currentIOOps", e);
		}
	}

	/**
	 * Register a IO task.
	 * 
	 * @param intent
	 *            intent holding IO operation
	 */
	public void register(final Intent intent) {
		Log.d(TAG, "register(" + intent.getAction() + ")");
		synchronized (this.pendingIOOps) {
			Log.d(TAG, "currentIOOps=" + this.pendingIOOps.size());
			this.pendingIOOps.add(intent);
			Log.d(TAG, "currentIOOps=" + this.pendingIOOps.size());
		}
	}

	/**
	 * Unregister a IO task.
	 * 
	 * @param intent
	 *            intent holding IO operation
	 */
	public void unregister(final Intent intent) {
		Log.d(TAG, "unregister(" + intent.getAction() + ")");
		synchronized (this.pendingIOOps) {
			Log.d(TAG, "currentIOOps=" + this.pendingIOOps.size());
			this.pendingIOOps.remove(intent);
			Log.d(TAG, "currentIOOps=" + this.pendingIOOps.size());
			if (this.pendingIOOps.size() == 0) {
				this.stopSelf();
			}
		}
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
	public void onDestroy() {
		super.onDestroy();
		Log.d(TAG, "onDestroy()");
		Log.d(TAG, "currentIOOps=" + this.pendingIOOps.size());
		final int s = this.pendingIOOps.size();
		for (int i = 0; i < s; i++) {
			// TODO: send error message for intent pendingIOOps.get(i)
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
