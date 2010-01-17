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
package de.ub0r.android.websms.connector.test;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;
import de.ub0r.android.websms.connector.common.ConnectorCommand;
import de.ub0r.android.websms.connector.common.ConnectorSpec;
import de.ub0r.android.websms.connector.common.Constants;
import de.ub0r.android.websms.connector.common.WebSMSException;
import de.ub0r.android.websms.connector.common.ConnectorSpec.SubConnectorSpec;

/**
 * Receives commands coming as broadcast from WebSMS.
 * 
 * @author flx
 */
public class CommandReceiverTest extends BroadcastReceiver {
	/** Tag for debug output. */
	private static final String TAG = "WebSMS.test";

	/** Preferences intent action. */
	private static final String PREFS_INTENT_ACTION = "de.ub0r.android."
			+ "websms.connectors.test.PREFS";

	/** Internal {@link ConnectorSpec}. */
	private static ConnectorSpec conector = null;

	/**
	 * Init ConnectorSpec.
	 * 
	 * @param context
	 *            context
	 * @return ConnectorSpec
	 */
	private static synchronized ConnectorSpec getSpecs(final Context context) {
		if (conector == null) {
			conector = new ConnectorSpec(TAG, context
					.getString(R.string.connector_test_name));
			conector.setAuthor(// .
					context.getString(R.string.connector_test_author));
			conector.setBalance(null);
			conector.setPrefsIntent(PREFS_INTENT_ACTION);
			conector.setPrefsTitle(context
					.getString(R.string.connector_test_preferences));
			conector.setCapabilities(ConnectorSpec.CAPABILITIES_BOOSTRAP
					| ConnectorSpec.CAPABILITIES_UPDATE
					| ConnectorSpec.CAPABILITIES_SEND);
			conector.addSubConnector(TAG, conector.getName(),
					SubConnectorSpec.FEATURE_MULTIRECIPIENTS
							| SubConnectorSpec.FEATURE_CUSTOMSENDER
							| SubConnectorSpec.FEATURE_SENDLATER);
		}
		final SharedPreferences p = PreferenceManager
				.getDefaultSharedPreferences(context);
		if (p.getBoolean(Preferences.PREFS_ENABLED, false)) {
			conector.setReady();

		} else {
			conector.setStatus(ConnectorSpec.STATUS_INACTIVE);
		}
		return conector;
	}

	/**
	 * Send INFO Broadcast back to WebSMS.
	 * 
	 * @param context
	 *            context
	 * @param specs
	 *            {@link ConnectorSpec}; if null, getSpecs() is called to get
	 *            them
	 * @param command
	 *            send back the {@link ConnectorCommand} which was done
	 */
	private void sendInfo(final Context context, final ConnectorSpec specs,
			final ConnectorCommand command) {
		ConnectorSpec c = specs;
		if (c == null) {
			c = getSpecs(context);
		}
		final Intent i = new Intent(Constants.ACTION_CONNECTOR_INFO);
		c.setToIntent(i);
		if (command != null) {
			command.setToIntent(i);
		}
		Log.d(TAG, "send broadcast: " + i.getAction());
		context.sendBroadcast(i);
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
		if (Constants.ACTION_CONNECTOR_UPDATE.equals(action)) {
			this.sendInfo(context, null, null);
		} else if (Constants.ACTION_CONNECTOR_RUN_SEND.equals(action)) {
			final ConnectorCommand command = new ConnectorCommand(intent);
			if (command.getType() == ConnectorCommand.TYPE_SEND) {
				final ConnectorSpec origSpecs = new ConnectorSpec(intent);
				final ConnectorSpec specs = CommandReceiverTest
						.getSpecs(context);
				if (specs.getID().equals(origSpecs.getID())
						&& specs.hasStatus(ConnectorSpec.STATUS_READY)) {
					// check internal status
					try {
						// FIXME: this.send(command);
						throw new WebSMSException("fixme");
					} catch (WebSMSException e) {
						Log.e(TAG, null, e);
						Toast.makeText(context,
								specs.getName() + ": " + e.getMessage(),
								Toast.LENGTH_LONG).show();
						specs.setErrorMessage(e.getMessage());
						this.sendInfo(context, specs, command);
					}
					// if nothing went wrong, info was send from inside.
				}
			}
		} else if (Constants.ACTION_CONNECTOR_RUN_BOOSTRAP.equals(action)) {
			final ConnectorSpec specs = CommandReceiverTest.getSpecs(context);
			this.sendInfo(context, specs, null);
		} else if (Constants.ACTION_CONNECTOR_RUN_UPDATE.equals(action)) {
			final ConnectorSpec specs = CommandReceiverTest.getSpecs(context);
			specs.setBalance("13,37\u20AC");
			this.sendInfo(context, specs, null);
		}
	}
}
