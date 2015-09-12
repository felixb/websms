/*
 * Copyright (C) 2010-2012 Felix Bechstein
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

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.MenuItem;
import android.widget.Toast;

import de.ub0r.android.lib.IPreferenceContainer;
import de.ub0r.android.lib.Log;
import de.ub0r.android.lib.Utils;
import de.ub0r.android.websms.connector.common.Connector;
import de.ub0r.android.websms.connector.common.ConnectorSpec;

/**
 * Preferences.
 * 
 * @author flx
 */
@SuppressWarnings("deprecation")
public class PreferencesActivity extends PreferenceActivity implements
		IPreferenceContainer {
	/** Tag for output. */
	private static final String TAG = "pref";

	/** Preference's name: theme. */
	private static final String PREFS_THEME = "theme";
	/** Theme: black. */
	private static final String THEME_BLACK = "black";
	/** Preference's name: text size. */
	private static final String PREFS_TEXTSIZE = "textsizen";

	/** Preference's name: set standard connector. */
	private static final String PREFS_STANDARD_CONNECTOR_SET = "set_std_connector";
	/** Preference's name: clear standard connector. */
	private static final String PREFS_STANDARD_CONNECTOR_CLEAR = "clear_std_connector";
	/** Preference's name: promoted_connectors. */
	private static final String PREFS_PROMOTED_CONNECTORS = "settings_promoted_connectors";

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.addPreferencesFromResource(R.xml.prefs_common);
		this.addPreferencesFromResource(R.xml.prefs_appearance_behavior);
		this.addPreferencesFromResource(R.xml.prefs_connectors);
		this.addPreferencesFromResource(R.xml.prefs_about);
		if (BuildConfig.DEBUG_LOG) {
			this.addPreferencesFromResource(R.xml.prefs_debug);
		}
		this.setTitle(R.string.settings);
		registerPreferenceChecker(this);
		registerOnPreferenceChangeListener(this);
	}

	/**
	 * Register {@link OnPreferenceChangeListener}.
	 * 
	 * @param pc
	 *            {@link IPreferenceContainer}
	 */
	static void registerOnPreferenceChangeListener(final IPreferenceContainer pc) {
		Log.d(TAG, "registerOnSharedPreferenceChangeListener()");
		final SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(pc.getContext());

		final Preference ps = pc.findPreference(WebSMS.PREFS_SENDER);
		final Preference pp = pc.findPreference(WebSMS.PREFS_DEFPREFIX);
		if (ps != null) {
			Log.d(TAG, "found: " + WebSMS.PREFS_SENDER);
			ps.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(final Preference preference,
						final Object newValue) {
					// check for wrong sender format. people can't
					// read..
					final String n = (String) newValue;
					if (n == null || !n.startsWith("+")) {
						Toast.makeText(pc.getContext(),
								R.string.log_wrong_sender, Toast.LENGTH_LONG)
								.show();
						return false;
					} else if (TextUtils.isEmpty(prefs.getString(
							WebSMS.PREFS_DEFPREFIX, null))) {
						final String p = Utils.getPrefixFromTelephoneNumber(n);
						final Editor e = prefs.edit();
						if (pp != null) {
							EditTextPreference epp = (EditTextPreference) pp;
							epp.getEditText().setText(p);
							epp.setText(p);
						}
						e.putString(WebSMS.PREFS_DEFPREFIX, p).commit();
						Log.i(TAG, "set prefix=" + p);
					}
					return true;
				}
			});
		}

		if (pp != null) {
			Log.d(TAG, "found: " + WebSMS.PREFS_DEFPREFIX);
			pp.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(final Preference preference,
						final Object newValue) {
					final String p = (String) newValue;
					if (p == null || !p.startsWith("+") || p.length() < 2) {
						Toast.makeText(pc.getContext(),
								R.string.log_wrong_defprefix, Toast.LENGTH_LONG)
								.show();
						return false;
					}
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
		addConnectorPreferences(this);
	}

	/**
	 * Get Theme from Preferences.
	 * 
	 * @param context
	 *            {@link Context}
	 * @return theme
	 */
	static int getTheme(final Context context) {
		final SharedPreferences p = PreferenceManager
				.getDefaultSharedPreferences(context);
		final String s = p.getString(PREFS_THEME, null);
		if (s != null && THEME_BLACK.equals(s)) {
			return R.style.Theme_SherlockWebSMS;
		} else {
			return R.style.Theme_SherlockWebSMS_Light;
		}
	}

	/**
	 * Get text's size from Preferences.
	 * 
	 * @param context
	 *            {@link Context}
	 * @return theme
	 */
	static int getTextsize(final Context context) {
		final SharedPreferences p = PreferenceManager
				.getDefaultSharedPreferences(context);
		final String s = p.getString(PREFS_TEXTSIZE, null);
		Log.d(TAG, "text size: " + s);
		return Utils.parseInt(s, 0);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			// app icon in Action Bar clicked; go home
			Intent intent = new Intent(this, WebSMS.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			this.startActivity(intent);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final Context getContext() {
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final Activity getActivity() {
		return this;
	}

	private static OnPreferenceClickListener getPreferenceLauncher(final Context context, final Intent intent) {
		return new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(final Preference preference) {
				try {
					context.startActivity(intent);
				} catch (ActivityNotFoundException e) {
					Log.e(TAG, "error launching intent: " + intent, e);
					Toast.makeText(context, R.string.error, Toast.LENGTH_LONG).show();
				}
				return true;
			}
		};
	}

	private static void registerPreferenceLauncher(final Preference p, final Context context, final Intent intent) {
		if (p != null) {
			p.setOnPreferenceClickListener(getPreferenceLauncher(context, intent));
		}
	}

	/**
	 * Check all {@link SharedPreferences} and register
	 * {@link OnPreferenceChangeListener}.
	 * 
	 * @param pc
	 *            {@link IPreferenceContainer}
	 */
	static void registerPreferenceChecker(final IPreferenceContainer pc) {
		registerPreferenceLauncher(pc.findPreference("more_connectors"), pc.getContext(), new Intent(Intent.ACTION_VIEW,
				Uri.parse("https://play.google.com/store/search?q=websms+connector")));
		registerPreferenceLauncher(pc.findPreference("more_apps"), pc.getContext(), new Intent(Intent.ACTION_VIEW,
				Uri.parse("https://play.google.com/store/apps/developer?id=Felix+Bechstein")));

		Preference pr = pc.findPreference("send_logs");
		if (pr != null) {
			pr.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
				public boolean onPreferenceClick(final Preference preference) {
					Log.collectAndSendLog(pc.getActivity(), pc.getContext()
							.getString(R.string.sendlog_install_), pc
							.getContext().getString(R.string.sendlog_install),
							pc.getContext().getString(R.string.sendlog_run_),
							pc.getContext().getString(R.string.sendlog_run));
					return true;
				}
			});
		}
		pr = pc.findPreference(PreferencesActivity.PREFS_STANDARD_CONNECTOR_SET);
		if (pr != null) {
			pr.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
				public boolean onPreferenceClick(final Preference preference) {
					final SharedPreferences p = PreferenceManager
							.getDefaultSharedPreferences(pc.getContext());
					final String c = p.getString(WebSMS.PREFS_CONNECTOR_ID, "");
					final String sc = p.getString(WebSMS.PREFS_SUBCONNECTOR_ID,
							"");
					Log.i(TAG, "set std connector: " + c + "/" + sc);
					final Editor e = p.edit();
					e.putString(WebSMS.PREFS_STANDARD_CONNECTOR, c);
					e.putString(WebSMS.PREFS_STANDARD_SUBCONNECTOR, sc);
					e.commit();
					return true;
				}
			});
		}
		pr = pc.findPreference(PreferencesActivity.PREFS_STANDARD_CONNECTOR_CLEAR);
		if (pr != null) {
			pr.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
				public boolean onPreferenceClick(final Preference preference) {
					Log.i(TAG, "clear std connector");
					final SharedPreferences p = PreferenceManager
							.getDefaultSharedPreferences(pc.getContext());
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
	 * Add preferences of connectors.
	 * 
	 * @param pc
	 *            {@link IPreferenceContainer}
	 */
	static void addConnectorPreferences(final IPreferenceContainer pc) {
		Log.d(TAG, "addConnectorPreferences()");
		PreferenceCategory pcat = (PreferenceCategory) pc
				.findPreference("settings_connectors");
		if (pcat == null) {
			Log.d(TAG, "settings_connectors not found; exit");
			return;
		}
		PreferenceGroup container = (PreferenceGroup) pc
				.findPreference("container");
		Preference pr = pc
				.findPreference(PreferencesActivity.PREFS_PROMOTED_CONNECTORS);
		final ConnectorSpec[] css = WebSMS
				.getConnectors(ConnectorSpec.CAPABILITIES_PREFS,
                        ConnectorSpec.STATUS_INACTIVE,
                        false /*isIncludePseudoConnectors*/);
		if (css.length == 0) {
			Log.i(TAG, "css.length == 0");
			// hide standard connector preferences
			PreferenceGroup stdConnectors = (PreferenceGroup) pc
					.findPreference("std_connector");
			if (stdConnectors != null && container != null) {
				container.removePreference(stdConnectors);
			}
		}
		String pkg;
		Preference cp;
		for (ConnectorSpec cs : css) {
			pkg = cs.getPackage();
			if (pkg == null) {
				Log.w(TAG, "pkg == null");
				continue;
			}
			if (pr != null
					&& container != null
					&& pkg.equals("de.ub0r.android.websms.connector.smsflatratenet")) {
				// remove promoted connector category
				// if any of them is installed already
				container.removePreference(pr);
				pr = null;
			}
			cp = pcat.findPreference(pkg);
			if (cp != null) {
				Log.i(TAG, "pkg " + pkg + "already added..");
			} else {
				cp = new Preference(pc.getContext());
				cp.setKey(pkg);
				cp.setTitle(pc.getContext().getString(R.string.settings)
						+ " - " + cs.getName());
				final String action = cs.getPackage() + Connector.ACTION_PREFS;
				cp.setOnPreferenceClickListener(new OnPreferenceClickListener() {
					@Override
					public boolean onPreferenceClick(final Preference preference) {
						try {
							pc.getActivity().startActivity(new Intent(action));
							return true;
						} catch (ActivityNotFoundException e) {
							Toast.makeText(pc.getContext(),
									R.string.log_error_connector_not_found,
									Toast.LENGTH_LONG).show();
							return false;
						}
					}
				});
				pcat.addPreference(cp);
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
}
