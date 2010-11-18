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
package de.ub0r.android.websms.connector.sms;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;
import de.ub0r.android.websms.R;
import de.ub0r.android.websms.WebSMS;
import de.ub0r.android.websms.connector.common.Connector;
import de.ub0r.android.websms.connector.common.ConnectorCommand;
import de.ub0r.android.websms.connector.common.ConnectorSpec;
import de.ub0r.android.websms.connector.common.Utils;
import de.ub0r.android.websms.connector.common.WebSMSException;
import de.ub0r.android.websms.connector.common.ConnectorSpec.SubConnectorSpec;

/**
 * Receives commands coming as broadcast from WebSMS.
 * 
 * @author flx
 */
public class ConnectorSMS extends Connector {
	/** Tag for debug output. */
	private static final String TAG = "WebSMS.sms";

	/** Preference key: enabled. */
	private static final String PREFS_ENABLED = "enable_sms";

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final ConnectorSpec initSpec(final Context context) {
		final String name = context.getString(R.string.connector_sms_name);
		final ConnectorSpec c = new ConnectorSpec(name);
		c.setAuthor(context.getString(R.string.connector_sms_author));
		c.setBalance(null);
		c.setCapabilities(ConnectorSpec.CAPABILITIES_SEND);
		c.addSubConnector(TAG, name, SubConnectorSpec.FEATURE_MULTIRECIPIENTS);
		return c;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final ConnectorSpec updateSpec(final Context context,
			final ConnectorSpec connectorSpec) {
		final SharedPreferences p = PreferenceManager
				.getDefaultSharedPreferences(context);
		if (p.getBoolean(PREFS_ENABLED, false)) {
			connectorSpec.setReady();
		} else {
			connectorSpec.setStatus(ConnectorSpec.STATUS_INACTIVE);
		}
		return connectorSpec;
	}

	/**
	 * Send a message.
	 * 
	 * @param command
	 *            command coming from intent
	 * @throws WebSMSException
	 *             WebSMSException
	 */
	private void send(final ConnectorCommand command) throws WebSMSException {
		try {
			final String[] r = command.getRecipients();
			ArrayList<String> messages;
			for (String t : r) {
				messages = WebSMS.TWRAPPER.divideMessage(command.getText());
				WebSMS.TWRAPPER.sendMultipartTextMessage(Utils
						.getRecipientsNumber(t), null, messages, null, null);
				for (String m : messages) {
					Log.d(TAG, "send sms: " + t + ", text: " + m);
				}
			}
		} catch (Exception e) {
			throw new WebSMSException(e.toString());
		}
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
		if (ACTION_CONNECTOR_UPDATE.equals(action)) {
			this.sendInfo(context, null, null);
			try {
				this.setResultCode(Activity.RESULT_OK);
			} catch (Exception e) {
				Log.w(TAG, "not an ordered boradcast: " + e.toString());
			}
		} else if (action.endsWith(ACTION_RUN_SEND)) {
			final ConnectorCommand command = new ConnectorCommand(intent);
			if (command.getType() == ConnectorCommand.TYPE_SEND) {
				final ConnectorSpec origSpecs = new ConnectorSpec(intent);
				final ConnectorSpec specs = this.getSpec(context);
				if (specs.equals(origSpecs)
						&& specs.hasStatus(ConnectorSpec.STATUS_READY)) {
					// check internal status
					try {
						this.send(command);
					} catch (WebSMSException e) {
						Log.e(TAG, null, e);
						Toast.makeText(context,
								specs.getName() + ": " + e.getMessage(),
								Toast.LENGTH_LONG).show();
						specs.setErrorMessage(context, e);
					}
					this.sendInfo(context, specs, command);
					try {
						this.setResultCode(Activity.RESULT_OK);
					} catch (Exception e) {
						Log.w(TAG, "not an ordered boradcast", e);
					}
				}
			}
		}
	}
}
