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
package de.ub0r.android.websms;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;
import de.ub0r.android.websms.connector.common.Connector;
import de.ub0r.android.websms.connector.common.ConnectorSpec;

/**
 * Preferences.
 * 
 * @author flx
 */
public class Preferences extends PreferenceActivity implements
		SharedPreferences.OnSharedPreferenceChangeListener {
	/** Packagename of SendLog. */
	public static final String SENDLOG_PACKAGE_NAME = "org.l6n.sendlog";
	/** Classname of SendLog. */
	public static final String SENDLOG_CLASS_NAME = ".SendLog";

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.addPreferencesFromResource(R.xml.prefs);
		Preference p = this.findPreference("send_logs");
		if (p != null) {
			p.setOnPreferenceClickListener(// .
					new Preference.OnPreferenceClickListener() {
						public boolean onPreferenceClick(
								final Preference preference) {
							Preferences.this.collectAndSendLog();
							return true;
						}
					});
		}
	}

	/**
	 * Fire a given {@link Intent}.
	 * 
	 * @author flx
	 */
	private static class FireIntent implements DialogInterface.OnClickListener {
		/** {@link Activity}. */
		private final Activity a;
		/** {@link Intent}. */
		private final Intent i;

		/**
		 * Default Constructor.
		 * 
		 * @param activity
		 *            {@link Activity}
		 * @param intent
		 *            {@link Intent}
		 */
		public FireIntent(final Activity activity, final Intent intent) {
			this.a = activity;
			this.i = intent;
		}

		/**
		 * {@inheritDoc}
		 */
		public void onClick(final DialogInterface dialog, // .
				final int whichButton) {
			this.a.startActivity(this.i);
		}
	}

	/**
	 * Collect and send Log.
	 */
	final void collectAndSendLog() {
		final PackageManager packageManager = this.getPackageManager();
		Intent intent = packageManager
				.getLaunchIntentForPackage(SENDLOG_PACKAGE_NAME);
		String message;
		if (intent == null) {
			intent = new Intent(Intent.ACTION_VIEW, Uri
					.parse("market://search?q=pname:" + SENDLOG_PACKAGE_NAME));
			message = "Install the free SendLog application to "
					+ "collect the device log and send "
					+ "it to the developer.";
		} else {
			intent.setType("0||flx.yoo@gmail.com");
			message = "Run SendLog application.\nIt will collect the "
					+ "device log and send it to the developer." + "\n"
					+ "You will have an opportunity to review "
					+ "and modify the data being sent.";
		}
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		new AlertDialog.Builder(this).setTitle(
				this.getString(R.string.app_name)).setIcon(
				android.R.drawable.ic_dialog_info).setMessage(message)
				.setPositiveButton(android.R.string.ok,
						new FireIntent(this, intent)).setNegativeButton(
						android.R.string.cancel, null).show();
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
}
