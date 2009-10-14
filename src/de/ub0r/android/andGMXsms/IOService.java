/*
 * Copyright (C) 2009 Felix Bechstein
 * 
 * This file is part of AndGMXsms.
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

import android.app.Service;
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

	/** Notification for failed message. */
	private static final int ID_FAILED = 0;

	/** Number of jobs running. */
	static int currentIOOps = 0;

	/** Reference to running Service. */
	static IOService me;

	/**
	 * Is some client bound to this service? IO Tasks can kill this service, if
	 * no Client is bound and all IO is done.
	 */
	static boolean isBound = false;

	/** The IBinder RPC Interface. */
	private final IIOOp.Stub mBinder = new IIOOp.Stub() {
		public void sendMessage(final int connector, final String[] params) {
			Connector.send((short) connector, params);
		}

		public String getFailedMessage(final int id, final String[] params) {
			return null;
		}
	};

	/**
	 * Called on bind().
	 * 
	 * @param intent
	 *            intend called
	 * @return RPC callback
	 */
	@Override
	public final IBinder onBind(final Intent intent) {
		Log.d(TAG, "onBind()");
		isBound = true;
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
		isBound = false;
		if (currentIOOps <= 0) {
			this.stopSelf();
		}
		return true;
	}

	/**
	 * Called when new clients have connected to the service, after it had
	 * previously been notified that all had disconnected in its
	 * onUnbind(Intent). This will only be called if the implementation of
	 * onUnbind(Intent) was overridden to return true.
	 * 
	 * @param The
	 *            Intent that was used to bind to this service, as given to
	 *            Context.bindService. Note that any extras that were included
	 *            with the Intent at that point will not be seen here.
	 */
	@Override
	public final void onRebind(final Intent intent) {
		isBound = true;
	}

	/**
	 * Called on Service start.
	 * 
	 * @param intent
	 *            intent called
	 * @param startId
	 *            start id
	 */
	@Override
	public final void onCreate() {
		super.onCreate();
		// Don't kill me!
		this.setForeground(true);
		me = this;
	}

	/**
	 * Called on Service destroy.
	 */
	@Override
	public final void onDestroy() {
		super.onDestroy();
		me = null;
	}
}
