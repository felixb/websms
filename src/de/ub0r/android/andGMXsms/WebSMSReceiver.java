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
import android.content.Context;
import android.content.Intent;
import android.util.Log;
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
			WebSMS.addConnector(specs);
		}
	}
}
