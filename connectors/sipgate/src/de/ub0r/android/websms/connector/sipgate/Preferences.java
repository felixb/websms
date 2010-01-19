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
package de.ub0r.android.websms.connector.sipgate;

import android.os.Bundle;
import android.preference.PreferenceActivity;

/**
 * Preferences.
 * 
 * @author flx
 */
public final class Preferences extends PreferenceActivity {
	/** Preference's name: enable sipgate. */
	static final String PREFS_ENABLE_SIPGATE = "enable_sipgate";
	/** Preference's name: enable sipgate team accounts. */
	static final String PREFS_ENABLE_SIPGATE_TEAM = "enable_sipgate_team";
	/** Preference's name: sipgate username. */
	static final String PREFS_USER_SIPGATE = "user_sipgate";
	/** Preference's name: user's password - sipgate. */
	static final String PREFS_PASSWORD_SIPGATE = "password_sipgate";

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.addPreferencesFromResource(R.xml.connector_sipgate_prefs);
	}
}
