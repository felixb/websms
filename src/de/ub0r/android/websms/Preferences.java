/*
 * Copyright (C) 2010-2011 Felix Bechstein
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
package de.ub0r.android.websms;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.widget.Toast;
import de.ub0r.android.lib.Log;
import de.ub0r.android.lib.Market;
import de.ub0r.android.lib.Utils;
import de.ub0r.android.websms.connector.common.Connector;
import de.ub0r.android.websms.connector.common.ConnectorSpec;

/**
 * Preferences.
 * 
 * @author flx
 */
public class Preferences extends PreferenceActivity implements
		SharedPreferences.OnSharedPreferenceChangeListener {
	/** Tag for output. */
	public static final String TAG = "pref";

	/** Preference's name: theme. */
	private static final String PREFS_THEME = "theme";
	/** Theme: black. */
	private static final String THEME_BLACK = "black";
	/** Theme: light. */
	private static final String THEME_LIGHT = "light";
	/** Preference's name: text size. */
	private static final String PREFS_TEXTSIZE = "textsizen";

	/** Preference's name: set standard connector. */
	private static final String PREFS_STANDARD_CONNECTOR_SET = // .
	"set_std_connector";
	/** Preference's name: clear standard connector. */
	private static final String PREFS_STANDARD_CONNECTOR_CLEAR = // .
	"clear_std_connector";

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.addPreferencesFromResource(R.xml.prefs);
		Market.setOnPreferenceClickListener(this, this
				.findPreference("more_connectors"), null, "websms+connector",
				"http://code.google.com/p/websmsdroid/downloads"
						+ "/list?can=2&q=label%3AConnector");
		Market.setOnPreferenceClickListener(this, this
				.findPreference("more_apps"), null, "Felix+Bechstein",
				"http://code.google.com/u/felix.bechstein/");
		Preference p = this.findPreference("send_logs");
		if (p != null) {
			p.setOnPreferenceClickListener(// .
					new Preference.OnPreferenceClickListener() {
						public boolean onPreferenceClick(
								final Preference preference) {
							Log.collectAndSendLog(Preferences.this);
							return true;
						}
					});
		}
		p = this.findPreference(Preferences.PREFS_STANDARD_CONNECTOR_SET);
		if (p != null) {
			p.setOnPreferenceClickListener(// .
					new Preference.OnPreferenceClickListener() {
						public boolean onPreferenceClick(
								final Preference preference) {
							final SharedPreferences p = PreferenceManager
									.getDefaultSharedPreferences(// .
									Preferences.this);
							final String c = p.getString(
									WebSMS.PREFS_CONNECTOR_ID, "");
							final String sc = p.getString(
									WebSMS.PREFS_SUBCONNECTOR_ID, "");
							Log.i(TAG, "set std connector: " + c + "/" + sc);
							final Editor e = p.edit();
							e.putString(WebSMS.PREFS_STANDARD_CONNECTOR, c);
							e.putString(WebSMS.PREFS_STANDARD_SUBCONNECTOR, sc);
							e.commit();
							return true;
						}
					});
		}
		p = this.findPreference(Preferences.PREFS_STANDARD_CONNECTOR_CLEAR);
		if (p != null) {
			p.setOnPreferenceClickListener(// .
					new Preference.OnPreferenceClickListener() {
						public boolean onPreferenceClick(
								final Preference preference) {
							Log.i(TAG, "clear std connector");
							final SharedPreferences p = PreferenceManager
									.getDefaultSharedPreferences(// .
									Preferences.this);
							final Editor e = p.edit();
							e.remove(WebSMS.PREFS_STANDARD_CONNECTOR);
							e.remove(WebSMS.PREFS_STANDARD_SUBCONNECTOR);
							e.commit();
							return true;
						}
					});
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void onResume() {
		super.onResume();
		WebSMS.doPreferences = true;
		final SharedPreferences p = PreferenceManager
				.getDefaultSharedPreferences(this);
		p.registerOnSharedPreferenceChangeListener(this);
		PreferenceCategory pc = (PreferenceCategory) this
				.findPreference("settings_connectors");
		final ConnectorSpec[] css = WebSMS.getConnectors(
				ConnectorSpec.CAPABILITIES_PREFS, // .
				ConnectorSpec.STATUS_INACTIVE);
		String pkg;
		Preference cp;
		String action;
		for (ConnectorSpec cs : css) {
			if (cs.getPackage() == null) {
				continue;
			}
			pkg = cs.getPackage();
			cp = pc.findPreference(pkg);
			if (cp == null) {
				cp = new Preference(this);
				cp.setKey(pkg);
				cp.setTitle(this.getString(R.string.settings) + " - "
						+ cs.getName());
				action = cs.getPackage() + Connector.ACTION_PREFS;
				cp.setIntent(new Intent(action));
				pc.addPreference(cp);
				Log.d("WebSMS.prefs", "added: " + action);
			}
			if (cs.isReady()) {
				cp.setSummary(R.string.status_ready);
			} else if (cs.hasStatus(ConnectorSpec.STATUS_ENABLED)) {
				cp.setSummary(R.string.status_enabled);
			} else {
				cp.setSummary(R.string.status_disabled);
			}
		}
	}

	/**
	 *{@inheritDoc}
	 */
	public final void onSharedPreferenceChanged(final SharedPreferences prefs,
			final String key) {
		if (key.equals(WebSMS.PREFS_SENDER)) {
			// check for wrong sender format. people can't read..
			final String p = prefs.getString(WebSMS.PREFS_SENDER, "");
			if (!p.startsWith("+")) {
				Toast.makeText(this, R.string.log_wrong_sender,
						Toast.LENGTH_LONG).show();
			}
		}
		if (key.equals(WebSMS.PREFS_DEFPREFIX)) {
			final String p = prefs.getString(WebSMS.PREFS_DEFPREFIX, "");
			if (!p.startsWith("+")) {
				Toast.makeText(this, R.string.log_wrong_defprefix,
						Toast.LENGTH_LONG).show();
			}
		}
	}

	/**
	 * Get Theme from Preferences.
	 * 
	 * @param context
	 *            {@link Context}
	 * @return theme
	 */
	static final int getTheme(final Context context) {
		final SharedPreferences p = PreferenceManager
				.getDefaultSharedPreferences(context);
		final String s = p.getString(PREFS_THEME, THEME_BLACK);
		if (s != null && THEME_LIGHT.equals(s)) {
			return android.R.style.Theme_Light;
		}
		return android.R.style.Theme_Black;
	}

	/**
	 * Get text's size from Preferences.
	 * 
	 * @param context
	 *            {@link Context}
	 * @return theme
	 */
	static final int getTextsize(final Context context) {
		final SharedPreferences p = PreferenceManager
				.getDefaultSharedPreferences(context);
		final String s = p.getString(PREFS_TEXTSIZE, null);
		Log.d(TAG, "text size: " + s);
		return Utils.parseInt(s, 0);
	}
}
