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

import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.telephony.gsm.SmsManager;
import android.util.Log;

/**
 * AsyncTask to manage IO by standard sms methods.
 * 
 * @author flx
 */
@SuppressWarnings("deprecation")
public class ConnectorSMS extends Connector {
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
		@Override
		public Connector getConnector(final Context c) {
			return new ConnectorSMS();
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
		public boolean supportCustomsender() {
			return false;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean supportFlashsms() {
			return false;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean supportSendLater() {
			return false;
		}

	};

	static {
		Connector.registerConnectorSpecs(SPECS);
	};

	/**
	 * Create a SMS Connector.
	 */
	public ConnectorSMS() {
		super(null, null, SMS);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final boolean updateMessages() {
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final boolean sendMessage() throws WebSMSException {
		try {
			SmsManager sm = SmsManager.getDefault();
			for (String t : this.to) {
				ArrayList<String> messages = sm.divideMessage(this.text);
				sm.sendMultipartTextMessage(t, null, messages, null, null);
				for (String m : messages) {
					Log.d(TAG, "send sms: " + t + " text: " + m);
				}
			}
			return true;
		} catch (Exception e) {
			throw new WebSMSException(e.toString());
		}
	}
}
