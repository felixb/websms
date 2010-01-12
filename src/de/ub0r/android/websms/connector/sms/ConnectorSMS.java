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

import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.telephony.gsm.SmsManager;
import android.util.Log;
import de.ub0r.android.andGMXsms.Connector;
import de.ub0r.android.andGMXsms.ConnectorSpecs;
import de.ub0r.android.andGMXsms.Connector.WebSMSException;
import de.ub0r.android.websms.connector.ConnectorIO;
import de.ub0r.android.websms.connector.Constants;

/**
 * {@link ConnectorIO} running sms IO.
 * 
 * @author flx
 */
@SuppressWarnings("deprecation")
public class ConnectorSMS extends ConnectorIO {
	/** Tag for debug output. */
	private static final String TAG = "WebSMS.sms";

	/**
	 * Connectors' specs.
	 */
	private static final ConnectorSpecs SPECS = new ConnectorSpecs() {
		/** Context to use. */
		private Context context = null;
		/** Connector's prefs prefix. */
		private static final String PREFS_PREFIX = "sms";

		/**
		 * {@inheritDoc}
		 */
		@Override
		public String getAuthor() {
			return this.context.getString(R.string.connector_sms_author);
		}

		/**
		 * {@inheritDoc}
		 */
		public void setBalance(final String b) {
			return;
		}

		/**
		 * {@inheritDoc}
		 */
		public String getBalance() {
			return null;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public Connector getConnector(final Context c) {
			Connector connector = new ConnectorSMS();
			connector.context = c;
			return connector;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public String getName(final boolean shortName) {
			return this.context.getString(R.string.connector_sms_name);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public Intent getPreferencesIntent() {
			return null;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public String getPreferencesTitle() {
			return null;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public String getPrefsPrefix() {
			return PREFS_PREFIX;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void init(final Context c) {
			this.context = c;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean isEnabled() {
			return PreferenceManager.getDefaultSharedPreferences(this.context)
					.getBoolean(PREFS_ENABLED + this.getPrefsPrefix(), false);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public short getFeatures() {
			return FEATURE_MULTIRECIPIENTS;
		}
	};

	static {
		Connector.registerConnectorSpecs(SPECS);
	};

	/**
	 * Create a SMS Connector.
	 * 
	 * @param c
	 *            context
	 * @param i
	 *            intent
	 */
	public ConnectorSMS(final Context c, final Intent i) {
		super(c, i);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final void doSend() throws WebSMSException {
		try {
			SmsManager sm = SmsManager.getDefault();
			for (String t : this.command
					.getStringArray(Constants.COMMAND_RECIPIENTS)) {
				ArrayList<String> messages = sm.divideMessage(this.command
						.getString(Constants.COMMAND_TEXT));
				sm.sendMultipartTextMessage(t, null, messages, null, null);
				for (String m : messages) {
					Log.d(TAG, "send sms: " + t + " text: " + m);
				}
			}
		} catch (Exception e) {
			throw new WebSMSException(e.toString());
		}
	}
}
