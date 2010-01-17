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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

/**
 * Receives commands coming as broadcast from WebSMS.
 * 
 * @author flx
 */
public abstract class CommandReceiver extends BroadcastReceiver {
	/** Tag for debug output. */
	private static final String TAG = "WebSMS.cbcr";

	/** Common Action prefix. */
	private static final String ACTION_PREFIX = "de.ub0r."
			+ "android.websms.connector.";

	/**
	 * Action to start a connector's {@link Service}. This should include a
	 * {@link ConnectorCommand}: boostrap.
	 */
	public static final String ACTION_CONNECTOR_RUN_BOOSTRAP = ACTION_PREFIX
			+ "RUN_BOOTSTRAP";

	/**
	 * Action to start a connector's {@link Service}. This should include a
	 * {@link ConnectorCommand}: update.
	 */
	public static final String ACTION_CONNECTOR_RUN_UPDATE = ACTION_PREFIX
			+ "RUN_UPDATE";

	/**
	 * Action to start a connector's {@link Service}. This should include a
	 * {@link ConnectorCommand}: send.
	 */
	public static final String ACTION_CONNECTOR_RUN_SEND = ACTION_PREFIX
			+ "RUN_SEND";

	/** Broadcast Action requesting update of {@link ConnectorSpec}'s status. */
	public static final String ACTION_CONNECTOR_UPDATE = ACTION_PREFIX
			+ "UPDATE";

	/**
	 * Broadcast Action sending updated {@link ConnectorSpec} informations back
	 * to WebSMS. This should include a {@link ConnectorSpec}.
	 */
	public static final String ACTION_CONNECTOR_INFO = ACTION_PREFIX + "INFO";

	/** Internal {@link ConnectorSpec}. */
	private static ConnectorSpec connector = null;

	/** Sync access to connector. */
	private static final Object syncUpdate = new Object();

	/**
	 * Init {@link ConnectorSpec}. This is only run once. Changing properties
	 * should be set in updateSpec(). Default implementation does nothing at
	 * all.
	 * 
	 * @param context
	 *            context
	 * @return updated {@link ConnectorSpec}
	 */
	public ConnectorSpec initSpec(final Context context) {
		return new ConnectorSpec(TAG, "noname");
	}

	/**
	 * Update {@link ConnectorSpec}. Default implementation does nothing at all.
	 * 
	 * @param context
	 *            context
	 * @param connectorSpec
	 *            {@link ConnectorSpec}
	 * @return updated {@link ConnectorSpec}
	 */
	public ConnectorSpec updateSpec(final Context context,
			final ConnectorSpec connectorSpec) {
		return connectorSpec;
	}

	/**
	 * Init {@link ConnectorSpec}.
	 * 
	 * @param context
	 *            context
	 * @return ConnectorSpec
	 */
	public final synchronized ConnectorSpec getSpecs(final Context context) {
		synchronized (syncUpdate) {
			if (connector == null) {
				connector = this.initSpec(context);
			}
			return this.updateSpec(context, connector);
		}
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
	public final void sendInfo(final Context context,
			final ConnectorSpec specs, final ConnectorCommand command) {
		ConnectorSpec c = specs;
		if (c == null) {
			c = this.getSpecs(context);
		}
		final Intent i = new Intent(CommandReceiver.ACTION_CONNECTOR_INFO);
		c.setToIntent(i);
		if (command != null) {
			command.setToIntent(i);
		}
		Log.d("WebSMS." + this.getSpecs(context), "-> broadcast: "
				+ i.getAction());
		context.sendBroadcast(i);
	}

	/**
	 * {@inheritDoc} //TODO: change me.
	 */
	@Override
	public void onReceive(final Context context, final Intent intent) {
		final String action = intent.getAction();
		Log.d(TAG, "action: " + action);
		if (action == null) {
			return;
		}
		if (CommandReceiver.ACTION_CONNECTOR_UPDATE.equals(action)) {
			this.sendInfo(context, null, null);
		} else if (CommandReceiver.ACTION_CONNECTOR_RUN_SEND.equals(action)) {
			final ConnectorCommand command = new ConnectorCommand(intent);
			if (command.getType() == ConnectorCommand.TYPE_SEND) {
				final ConnectorSpec origSpecs = new ConnectorSpec(intent);
				final ConnectorSpec specs = this.getSpecs(context);
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
		}
	}
}
