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

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.BitmapDrawable;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;
import de.ub0r.android.websms.connector.common.Connector;
import de.ub0r.android.websms.connector.common.ConnectorSpec;
import de.ub0r.android.websms.connector.common.WebSMSException;
import de.ub0r.android.websms.connector.common.ConnectorSpec.SubConnectorSpec;

/**
 * Receives commands coming as broadcast from WebSMS.
 * 
 * @author flx
 */
public class ConnectorTest extends Connector {
	/** Tag for debug output. */
	private static final String TAG = "WebSMS.test";

	/** Preferences intent action. */
	private static final String PREFS_INTENT_ACTION = "de.ub0r.android."
			+ "websms.connectors.test.PREFS";

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final ConnectorSpec initSpec(// .
			final Context context) {
		final String name = context.getString(R.string.connector_test_name);
		final ConnectorSpec c = new ConnectorSpec(TAG, name);

		c.setAuthor(context.getString(R.string.connector_test_author));
		c.setBalance(null);
		c.setPrefsIntent(PREFS_INTENT_ACTION);
		c.setPrefsTitle(context.getString(R.string.connector_test_preferences));
		c.setCapabilities(ConnectorSpec.CAPABILITIES_BOOTSTRAP
				| ConnectorSpec.CAPABILITIES_UPDATE
				| ConnectorSpec.CAPABILITIES_SEND);
		c.addSubConnector(TAG, name, SubConnectorSpec.FEATURE_MULTIRECIPIENTS
				| SubConnectorSpec.FEATURE_CUSTOMSENDER
				| SubConnectorSpec.FEATURE_SENDLATER
				| SubConnectorSpec.FEATURE_FLASHSMS);
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
		if (p.getBoolean(Preferences.PREFS_ENABLED, false)) {
			connectorSpec.setReady();

		} else {
			connectorSpec.setStatus(ConnectorSpec.STATUS_INACTIVE);
		}
		return connectorSpec;
	}

	/**
	 * Do nothing but fail if needed.
	 * 
	 * @param context
	 *            {@link Context}
	 * @throws WebSMSException
	 *             WebSMSException
	 */
	private void doStuff(final Context context) throws WebSMSException {
		if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
				"fail", false)) {
			throw new WebSMSException("fail");
		}
		if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
				"need_captcha", false)) {
			final Intent intent = new Intent(Connector.ACTION_CAPTCHA_REQUEST);
			BitmapDrawable d = (BitmapDrawable) context.getResources()
					.getDrawable(R.drawable.icon);
			intent.putExtra(Connector.EXTRA_CAPTCHA_DRAWABLE, d.getBitmap());
			// intent.putExtra(Connector.EXTRA_CAPTCHA_MESSAGE, "solv it!");
			this.getSpec(context).setToIntent(intent);
			Log.d(TAG, "send broadcast: " + intent.getAction());
			context.sendBroadcast(intent);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final void doBootstrap(final Context context, final Intent intent)
			throws WebSMSException {
		this.doStuff(context);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final void doUpdate(final Context context, final Intent intent)
			throws WebSMSException {
		this.doStuff(context);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final void doSend(final Context context, final Intent intent)
			throws WebSMSException {
		this.doStuff(context);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final void gotSolvedCaptcha(final Context context,
			final String solvedCaptcha) {
		Toast.makeText(context, "solved: " + solvedCaptcha, Toast.LENGTH_LONG)
				.show();
	}
}
