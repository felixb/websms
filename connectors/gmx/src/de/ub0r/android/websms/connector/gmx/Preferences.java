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
package de.ub0r.android.websms.connector.gmx;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

/**
 * Preferences.
 * 
 * @author flx
 */
public final class Preferences extends PreferenceActivity {
	/** Preference's name: mail. */
	static final String PREFS_MAIL = "gmx_mail";
	/** Preference's name: username. */
	static final String PREFS_USER = "gmx_user";
	/** Preference's name: user's password. */
	static final String PREFS_PASSWORD = "gmx_password";
	/** Preference's name: gmx hostname id. */
	static final String PREFS_GMX_HOST = "gmx_host";
	/** Preference key: enabled. */
	static final String PREFS_ENABLED = "enable_gmx";

	/** mail. */
	private static String mail;
	/** password. */
	private static String pw;

	/** Need to bootstrap? */
	private static boolean needBootstrap = false;

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// FIXME: WebSMS.doPreferences = true;
		this.addPreferencesFromResource(R.xml.connector_gmx_prefs);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onPause() {
		super.onPause();
		// check if prefs changed
		SharedPreferences p = PreferenceManager
				.getDefaultSharedPreferences(this);
		needBootstrap |= mail != null
				&& !mail.equals(p.getString(PREFS_MAIL, ""));
		needBootstrap |= pw != null
				&& !pw.equals(p.getString(PREFS_PASSWORD, ""));
	}

	/**
	 * @return true if bootstrap is needed
	 */
	static boolean needBootstrap() {
		return needBootstrap;
	}
}
