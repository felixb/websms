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

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.widget.Toast;

/**
 * Preferences.
 * 
 * @author flx
 */
public class Preferences extends PreferenceActivity implements
		SharedPreferences.OnSharedPreferenceChangeListener {
	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		WebSMS.doPreferences = true;
		this.addPreferencesFromResource(R.xml.prefs);
		final SharedPreferences p = PreferenceManager
				.getDefaultSharedPreferences(this);
		p.registerOnSharedPreferenceChangeListener(this);
		PreferenceCategory pc = (PreferenceCategory) this
				.findPreference("settings_connectors");
		for (ConnectorSpecs cs : Connector.getConnectorSpecs(this, false)) {
			final Intent i = cs.getPreferencesIntent();
			if (i == null) {
				continue;
			}
			// FIXME: create for intent?!?
			pc.getPreferenceManager().createPreferenceScreen(this);
			// pc.addPreference(i);
		}
	}

	/**
	 *{@inheritDoc}
	 */
	public final void onSharedPreferenceChanged(final SharedPreferences prefs,
			final String key) {
		// FIXME_ move to connector
		// if (key.equals(PREFS_ENABLE_GMX) || key.equals(PREFS_SENDER)
		// || key.equals(PREFS_PASSWORD_GMX)
		// || key.equals(PREFS_MAIL_GMX)) {
		// this.changed[Connector.GMX] = true;
		// }

		if (key.equals(WebSMS.PREFS_SENDER)) {
			// check for wrong sender format. people can't read..
			final String p = prefs.getString(WebSMS.PREFS_SENDER, "");
			if (!p.startsWith("+")) {
				Toast.makeText(this, R.string.log_error_sender,
						Toast.LENGTH_LONG).show();
			}
		}
		if (key.equals(WebSMS.PREFS_DEFPREFIX)) {
			final String p = prefs.getString(WebSMS.PREFS_DEFPREFIX, "");
			if (!p.startsWith("+")) {
				Toast.makeText(this, R.string.log_error_defprefix,
						Toast.LENGTH_LONG).show();
			}
		}
	}
}
