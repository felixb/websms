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

/**
 * IOService handles all IO as a service. Call it with RPC!
 * 
 * @author flx
 */
public class IOService extends Service {
	/** Notification for failed message. */
	private static final int ID_FAILED = 0;

	/** The IBinder RPC Interface. */
	private final IIOOp.Stub mBinder = new IIOOp.Stub() {
		public void sendMessage(final int connector, final String[] params) {
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
		return this.mBinder;
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
	public final void onStart(final Intent intent, final int startId) {
		super.onStart(intent, startId);
		// Don't kill me!
		this.setForeground(true);
	}

	/**
	 * Called on Service destroy.
	 */
	@Override
	public final void onDestroy() {
		super.onDestroy();
		// TODO fill me
	}
}
