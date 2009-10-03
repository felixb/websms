/*
 * Copyright (C) 2009 Felix Bechstein
 * 
 * This file is part of AndGMXsms.
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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.MultiAutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import com.admob.android.ads.AdView;

/**
 * Main Activity.
 * 
 * @author flx
 */
public class AndGMXsms extends Activity implements OnClickListener {
	/** Tag for output. */
	private static final String TAG = "WebSMS";

	/** Static reference to running Activity. */
	static AndGMXsms me;
	/** Preference's name: mail. */
	private static final String PREFS_MAIL = "mail";
	/** Preference's name: username. */
	private static final String PREFS_USER = "user";
	/** Preference's name: user's password - gmx. */
	private static final String PREFS_PASSWORD_GMX = "password";
	/** Preference's name: user's password - o2. */
	private static final String PREFS_PASSWORD_O2 = "password_o2";
	/** Preference's name: user's phonenumber. */
	private static final String PREFS_SENDER = "sender";
	/** Preference's name: default prefix. */
	private static final String PREFS_DEFPREFIX = "defprefix";
	/** Preference's name: touch keyboard. */
	private static final String PREFS_SOFTKEYS = "softkeyboard";
	/** Preference's name: enable gmx. */
	private static final String PREFS_ENABLE_GMX = "enable_gmx";
	/** Preference's name: enable o2. */
	private static final String PREFS_ENABLE_O2 = "enable_o2";
	/** Preference's name: to. */
	private static final String PREFS_TO = "to";
	/** Preference's name: text. */
	private static final String PREFS_TEXT = "text";
	/** Preferences: mail. */
	static String prefsMail;
	/** Preferences: username. */
	static String prefsUser;
	/** Preferences: user's password - gmx. */
	static String prefsPasswordGMX;
	/** Preferences: user's password - o2. */
	static String prefsPasswordO2;
	/** Preferences: user's phonenumber. */
	static String prefsSender;
	/** Preferences: default prefix. */
	static String prefsDefPrefix;
	/** Preferences: ready for gmx? */
	static boolean prefsReadyGMX = false;
	/** Preferences: ready for o2? */
	static boolean prefsReadyO2 = false;
	/** Remaining free sms. */
	static String remFree = null;
	/** Preferences: use softkeys. */
	static boolean prefsSoftKeys = false;
	/** Preferences: enable gmx. */
	static boolean prefsEnableGMX = false;
	/** Preferences: enable o2. */
	static boolean prefsEnableO2 = false;
	/** Preferences: hide ads. */
	static boolean prefsNoAds = false;

	/** Array of md5(prefsSender) for which no ads should be displayed. */
	private static final String[] NO_AD_HASHS = {
			"2986b6d93053a53ff13008b3015a77ff", // me
			"f6b3b72300e918436b4c4c9fdf909e8c", // jörg s.
			"4c18f7549b643045f0ff69f61e8f7e72", // frank j.
			"7684154558d19383552388d9bc92d446", // henning k.
			"64c7414288e9a9b57a33e034f384ed30" // dominik l.
	};

	/** Public Dialog ref. */
	static Dialog dialog = null;
	/** Dialog String. */
	static String dialogString = null;

	/** true if preferences got opened. */
	private static boolean doPreferences = false;

	/** Dialog: about. */
	private static final int DIALOG_ABOUT = 0;
	/** Dialog: help. */
	private static final int DIALOG_HELP = 1;

	/** Message for logging. **/
	static final int MESSAGE_LOG = 0;
	/** Message for update free sms count. **/
	static final int MESSAGE_FREECOUNT = 1;
	/** Message to open settings. */
	static final int MESSAGE_SETTINGS = 4;
	/** Message to reset data. */
	static final int MESSAGE_RESET = 5;
	/** Message check prefsReady. */
	static final int MESSAGE_PREFSREADY = 6;
	/** Message display ads. */
	static final int MESSAGE_DISPLAY_ADS = 7;

	/** Menu: send via GMX. */
	private static final int MENU_SEND_GMX = Connector.GMX + 1;
	/** Menu: send via O2. */
	private static final int MENU_SEND_O2 = Connector.O2 + 1;
	/** Menu: cancel. */
	private static final int MENU_CANCEL = 3;

	/** Persistent Message store. */
	private static String lastMsg = null;
	/** Persistent Recipient store. */
	private static String lastTo = null;

	/**
	 * Remaining free sms. First dimension is the Connector, second is the
	 * free/limit.
	 */
	static final int[][] SMS_FREE = { { 0, 0 }, { 0, 0 } };
	/** ID of sms free count in array SMS_FREE. */
	static final int SMS_FREE_COUNT = 0;
	/** ID of sms limit in array SMS_FREE. */
	static final int SMS_FREE_LIMIT = 1;

	/** Text's label. */
	private TextView textLabel;

	/** Resource @@string/text__. */
	private String textLabelRef;

	/** Shared Preferences. */
	private SharedPreferences preferences;

	/** MessageHandler. */
	private Handler messageHandler;

	/**
	 * Called when the activity is first created.
	 * 
	 * @param savedInstanceState
	 *            default param
	 */
	@Override
	public final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// save ref to me.
		me = this;
		// inflate XML
		this.setContentView(R.layout.main);
		// register MessageHandler
		this.messageHandler = new AndGMXsms.MessageHandler();

		// Restore preferences
		this.preferences = PreferenceManager.getDefaultSharedPreferences(this);
		this.preferences
				.registerOnSharedPreferenceChangeListener(this.prefsOnChangeListener);
		this.reloadPrefs();

		lastTo = this.preferences.getString(PREFS_TO, "");
		lastMsg = this.preferences.getString(PREFS_TEXT, "");

		// register Listener
		((Button) this.findViewById(R.id.send_gmx)).setOnClickListener(this);
		((Button) this.findViewById(R.id.send_o2)).setOnClickListener(this);
		((Button) this.findViewById(R.id.cancel)).setOnClickListener(this);

		this.textLabelRef = this.getResources().getString(R.string.text__);
		this.textLabel = (TextView) this.findViewById(R.id.text_);
		((EditText) this.findViewById(R.id.text))
				.addTextChangedListener(this.textWatcher);

		((TextView) this.findViewById(R.id.freecount)).setOnClickListener(this);

		MultiAutoCompleteTextView to = (MultiAutoCompleteTextView) this
				.findViewById(R.id.to);
		to.setAdapter(new MobilePhoneAdapter(this));
		to.setTokenizer(new MultiAutoCompleteTextView.CommaTokenizer());

		Intent intend = this.getIntent();
		String action = intend.getAction();
		if (action != null && action.equals(Intent.ACTION_SENDTO)) {
			// launched by clicking a sms: link, target number is in URI.
			Uri uri = intend.getData();
			if (uri != null && uri.getScheme().equalsIgnoreCase("sms")) {
				String recipient = uri.getSchemeSpecificPart();
				if (recipient != null) {
					recipient = AndGMXsms.cleanRecipient(recipient);
					((EditText) this.findViewById(R.id.to)).setText(recipient);
					lastTo = recipient;
				}
			}
		}
	}

	/** Called on activity resume. */
	@Override
	protected final void onResume() {
		super.onResume();
		// set free sms count
		if (remFree != null) {
			TextView tw = (TextView) this.findViewById(R.id.freecount);
			tw.setText(this.getResources().getString(R.string.free_)
					+ " "
					+ remFree
					+ " "
					+ AndGMXsms.this.getResources().getString(
							R.string.click_for_update));
		} else {
			TextView tw = (TextView) this.findViewById(R.id.freecount);
			tw.setText(this.getResources().getString(R.string.free_)
					+ " "
					+ AndGMXsms.this.getResources().getString(
							R.string.click_for_update));
		}

		// restart dialog
		if (dialogString != null) {
			if (dialog != null) {
				try {
					dialog.dismiss();
				} catch (Exception e) {
					// nothing to do
				}
			}
			dialog = ProgressDialog.show(this, null, dialogString, true);
		}

		if (doPreferences) {
			this.reloadPrefs();
			this.checkPrefs();
			doPreferences = false;
			if (prefsEnableGMX
					&& this.prefsOnChangeListener.wasChanged(Connector.GMX)) {
				String[] params = new String[ConnectorGMX.IDS_BOOTSTR];
				params[Connector.ID_ID] = Connector.ID_BOOSTR;
				params[ConnectorGMX.ID_MAIL] = prefsMail;
				params[ConnectorGMX.ID_PW] = prefsPasswordGMX;
				Connector.bootstrap(Connector.GMX, params);
			}
		} else {
			this.checkPrefs();
		}

		this.setButtons();

		// reload text/recipient from local store
		EditText et = (EditText) this.findViewById(R.id.text);
		if (lastMsg != null) {
			et.setText(lastMsg);
		} else {
			et.setText("");
		}
		et = (EditText) this.findViewById(R.id.to);
		if (lastTo != null) {
			et.setText(lastTo);
		} else {
			et.setText("");
		}
	}

	/** Called on activity pause. */
	@Override
	public final void onPause() {
		super.onPause();
		// store input data to persitent stores
		lastMsg = ((EditText) this.findViewById(R.id.text)).getText()
				.toString();
		lastTo = ((EditText) this.findViewById(R.id.to)).getText().toString();

		// store input data to preferences
		SharedPreferences.Editor editor = this.preferences.edit();
		// common
		editor.putString(PREFS_TO, lastTo);
		editor.putString(PREFS_TEXT, lastMsg);
		// commit changes
		editor.commit();
	}

	/**
	 * Read static vars holding preferences.
	 */
	private void reloadPrefs() {
		prefsSender = this.preferences.getString(PREFS_SENDER, "");
		prefsDefPrefix = this.preferences.getString(PREFS_DEFPREFIX, "+49");
		prefsSoftKeys = this.preferences.getBoolean(PREFS_SOFTKEYS, false);

		prefsEnableGMX = this.preferences.getBoolean(PREFS_ENABLE_GMX, false);
		prefsMail = this.preferences.getString(PREFS_MAIL, "");
		prefsUser = this.preferences.getString(PREFS_USER, "");
		prefsPasswordGMX = this.preferences.getString(PREFS_PASSWORD_GMX, "");

		prefsEnableO2 = this.preferences.getBoolean(PREFS_ENABLE_O2, false);
		prefsPasswordO2 = this.preferences.getString(PREFS_PASSWORD_O2, "");

		prefsNoAds = false;
		String hash = md5(prefsSender);
		for (String h : NO_AD_HASHS) {
			if (hash.equals(h)) {
				prefsNoAds = true;
				break;
			}
		}
	}

	/**
	 * Show/hide, enable/disable send buttons.
	 */
	private void setButtons() {
		Button btn = (Button) this.findViewById(R.id.send_gmx);
		// show/hide buttons
		if (prefsEnableGMX && !prefsSoftKeys) {
			btn.setEnabled(prefsReadyGMX);
			btn.setVisibility(View.VISIBLE);
			if (prefsEnableO2) {
				btn.setText(this.getResources().getString(R.string.send_gmx));
			} else {
				btn.setText(this.getResources().getString(R.string.send_));
			}
		} else {
			btn.setVisibility(View.GONE);
		}
		btn = (Button) this.findViewById(R.id.send_o2);
		if (prefsEnableO2 && !prefsSoftKeys) {
			btn.setEnabled(prefsReadyO2);
			btn.setVisibility(View.VISIBLE);
			if (prefsEnableGMX) {
				btn.setText(this.getResources().getString(R.string.send_o2));
			} else {
				btn.setText(this.getResources().getString(R.string.send_));
			}
		} else {
			btn.setVisibility(View.GONE);
		}
		btn = (Button) this.findViewById(R.id.cancel);
		if (prefsSoftKeys) {
			btn.setVisibility(View.GONE);
		} else {
			btn.setVisibility(View.VISIBLE);
		}
	}

	/**
	 * Check if prefs are set.
	 */
	private void checkPrefs() {
		// check prefs
		if (prefsEnableGMX && prefsMail.length() != 0
				&& prefsUser.length() != 0 && prefsPasswordGMX.length() != 0
				&& prefsSender.length() != 0) {
			prefsReadyGMX = true;
		} else {
			if (prefsEnableGMX && !ConnectorGMX.inBootstrap) {
				this.log(this.getResources().getString(
						R.string.log_empty_settings));
			}
			prefsReadyGMX = false;
		}
		if (prefsEnableO2 && prefsSender.length() != 0
				&& prefsPasswordO2.length() != 0) {
			prefsReadyO2 = true;
		} else {
			if (prefsEnableO2) {
				this.log(this.getResources().getString(
						R.string.log_empty_settings));
			}
			prefsReadyO2 = false;
		}

		this.setButtons();
	}

	/**
	 * Resets persistent store.
	 */
	private void reset() {
		((EditText) this.findViewById(R.id.text)).setText("");
		((EditText) this.findViewById(R.id.to)).setText("");
		lastMsg = null;
		lastTo = null;
		// save user preferences
		SharedPreferences.Editor editor = this.preferences.edit();
		editor.putString(PREFS_TO, "");
		editor.putString(PREFS_TEXT, "");
		// commit changes
		editor.commit();
	}

	/** Save prefs. */
	final void savePreferences() {
		// save user preferences
		SharedPreferences.Editor editor = this.preferences.edit();
		// common
		editor.putString(PREFS_SENDER, prefsSender);
		editor.putString(PREFS_DEFPREFIX, prefsDefPrefix);
		// gmx
		editor.putString(PREFS_MAIL, prefsMail);
		editor.putString(PREFS_USER, prefsUser);
		editor.putString(PREFS_PASSWORD_GMX, prefsPasswordGMX);
		// commit changes
		editor.commit();
	}

	/**
	 * OnClick.
	 * 
	 * @param v
	 *            View
	 */
	public final void onClick(final View v) {
		switch (v.getId()) {
		case R.id.freecount:
			if (prefsEnableGMX) {
				Connector.update(Connector.GMX);
			}
			if (prefsEnableO2) {
				Connector.update(Connector.O2);
			}
			break;
		case R.id.btn_donate:
			Uri uri = Uri.parse(this.getString(R.string.donate_url));
			this.startActivity(new Intent(Intent.ACTION_VIEW, uri));
			break;
		case R.id.send_gmx:
			this.send(Connector.GMX);
			break;
		case R.id.send_o2:
			this.send(Connector.O2);
			break;
		case R.id.cancel:
			this.reset();
			break;
		default:
			break;
		}
	}

	/**
	 * Open menu.
	 * 
	 * @param menu
	 *            menu to inflate
	 * @return ok/fail?
	 */
	@Override
	public final boolean onPrepareOptionsMenu(final Menu menu) {
		if (prefsSoftKeys) {
			if (menu.findItem(MENU_CANCEL) == null) {
				menu.add(0, MENU_CANCEL, 1,
						this.getResources().getString(android.R.string.cancel))
						.setIcon(android.R.drawable.ic_menu_close_clear_cancel);
			}
			if (prefsEnableGMX) {
				if (menu.findItem(MENU_SEND_GMX) == null) {
					// add menu to send text
					MenuItem m;
					if (prefsEnableO2) {
						m = menu.add(0, MENU_SEND_GMX, 0, this.getResources()
								.getString(R.string.send_gmx));
					} else {
						m = menu.add(0, MENU_SEND_GMX, 0, this.getResources()
								.getString(R.string.send_));
					}
					m.setIcon(android.R.drawable.ic_menu_send);
				}
			} else {
				menu.removeItem(MENU_SEND_GMX);
			}
			if (prefsEnableO2) {
				if (menu.findItem(MENU_SEND_O2) == null) {
					// add menu to send text
					MenuItem m;
					if (prefsEnableGMX) {
						m = menu.add(0, MENU_SEND_O2, 0, this.getResources()
								.getString(R.string.send_o2));
					} else {
						m = menu.add(0, MENU_SEND_O2, 0, this.getResources()
								.getString(R.string.send_));
					}
					m.setIcon(android.R.drawable.ic_menu_send);
				}
			} else {
				menu.removeItem(MENU_SEND_O2);
			}
		} else {
			menu.removeItem(MENU_SEND_GMX);
			menu.removeItem(MENU_SEND_O2);
			menu.removeItem(MENU_CANCEL);
		}
		return true;
	}

	/**
	 * Create menu.
	 * 
	 * @param menu
	 *            menu to inflate
	 * @return ok/fail?
	 */
	@Override
	public final boolean onCreateOptionsMenu(final Menu menu) {
		MenuInflater inflater = this.getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		return true;
	}

	/**
	 * Handles item selections.
	 * 
	 * @param item
	 *            menu item
	 * @return done?
	 */
	public final boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
		case R.id.item_about: // start about dialog
			this.showDialog(DIALOG_ABOUT);
			return true;
		case R.id.item_settings: // start settings activity
			this.startActivity(new Intent(this, Preferences.class));
			return true;
		case R.id.item_help: // start help dialog
			this.showDialog(DIALOG_HELP);
			return true;
		case MENU_SEND_GMX:
			this.send(Connector.GMX);
			return true;
		case MENU_SEND_O2:
			this.send(Connector.O2);
			return true;
		case MENU_CANCEL:
			this.reset();
			return true;
		default:
			return false;
		}
	}

	/**
	 * Called to create dialog.
	 * 
	 * @param id
	 *            Dialog id
	 * @return dialog
	 */
	@Override
	protected final Dialog onCreateDialog(final int id) {
		Dialog myDialog;
		switch (id) {
		case DIALOG_ABOUT:
			myDialog = new Dialog(this);
			myDialog.setContentView(R.layout.about);
			myDialog.setTitle(this.getResources().getString(R.string.about_)
					+ " v"
					+ this.getResources().getString(R.string.app_version));
			((Button) myDialog.findViewById(R.id.btn_donate))
					.setOnClickListener(this);
			break;
		case DIALOG_HELP:
			myDialog = new Dialog(this);
			myDialog.setContentView(R.layout.help);
			myDialog.setTitle(this.getResources().getString(R.string.help_));
			break;
		default:
			myDialog = null;
		}
		return myDialog;
	}

	/**
	 * Log text.
	 * 
	 * @param text
	 *            text
	 */
	public final void log(final String text) {
		Toast.makeText(this.getApplicationContext(), text, Toast.LENGTH_LONG)
				.show();
	}

	/**
	 * Main Preferences as PreferencesActivity.
	 * 
	 * @author Felix Bechstein
	 */
	public static class Preferences extends PreferenceActivity {
		/**
		 * Called on Create.
		 * 
		 * @param savedInstanceState
		 *            saved Instance
		 */
		public final void onCreate(final Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			AndGMXsms.doPreferences = true;
			this.addPreferencesFromResource(R.xml.prefs);
		}
	}

	/**
	 * AndGMXsms's Handler to fetch MEssages from other Threads..
	 * 
	 * @author Felix Bechstein
	 */
	private class MessageHandler extends Handler {
		/**
		 * Handles incoming messages.
		 * 
		 * @param msg
		 *            message
		 */
		@Override
		public final void handleMessage(final Message msg) {
			switch (msg.what) {
			case MESSAGE_LOG:
				String l = (String) msg.obj;
				AndGMXsms.this.log(l);
				return;
			case MESSAGE_FREECOUNT:
				AndGMXsms.remFree = "";
				if (AndGMXsms.prefsEnableGMX) {
					AndGMXsms.remFree = "GMX: "
							+ AndGMXsms.SMS_FREE[Connector.GMX][AndGMXsms.SMS_FREE_COUNT]
							+ " / "
							+ AndGMXsms.SMS_FREE[Connector.GMX][AndGMXsms.SMS_FREE_LIMIT];
				}
				if (AndGMXsms.prefsEnableO2) {
					if (AndGMXsms.remFree.length() > 0) {
						AndGMXsms.remFree += " - ";
					}
					AndGMXsms.remFree += "O2: "
							+ AndGMXsms.SMS_FREE[Connector.O2][AndGMXsms.SMS_FREE_COUNT];
					if (AndGMXsms.SMS_FREE[Connector.O2][AndGMXsms.SMS_FREE_LIMIT] > 0) {
						AndGMXsms.remFree += " / "
								+ AndGMXsms.SMS_FREE[Connector.O2][AndGMXsms.SMS_FREE_LIMIT];
					}
				}
				if (AndGMXsms.remFree.length() == 0) {
					AndGMXsms.remFree = "---";
				}
				TextView tw = (TextView) AndGMXsms.this
						.findViewById(R.id.freecount);
				tw.setText(AndGMXsms.this.getResources().getString(
						R.string.free_)
						+ " "
						+ AndGMXsms.remFree
						+ " "
						+ AndGMXsms.this.getResources().getString(
								R.string.click_for_update));
				return;
			case MESSAGE_SETTINGS:
				AndGMXsms.this.startActivity(new Intent(AndGMXsms.this,
						Preferences.class));
			case MESSAGE_RESET:
				AndGMXsms.this.reset();
				return;
			case MESSAGE_PREFSREADY:
				AndGMXsms.this.checkPrefs();
				return;
			case MESSAGE_DISPLAY_ADS:
				if (prefsNoAds) {
					return; // do not display any ads
				}
				// display ads
				((AdView) AndGMXsms.this.findViewById(R.id.ad))
						.setVisibility(View.VISIBLE);
				return;
			default:
				return;
			}
		}
	}

	/** TextWatcher updating char count on writing. */
	private TextWatcher textWatcher = new TextWatcher() {
		/**
		 * Called after Text is changed.
		 * 
		 * @param s
		 *            text
		 */
		@Override
		public void afterTextChanged(final Editable s) {
			AndGMXsms.this.textLabel.setText(AndGMXsms.this.textLabelRef + " ("
					+ s.length() + "):");
		}

		/** Needed dummy. */
		@Override
		public void beforeTextChanged(final CharSequence s, final int start,
				final int count, final int after) {
		}

		/** Needed dummy. */
		@Override
		public void onTextChanged(final CharSequence s, final int start,
				final int before, final int count) {
		}
	};

	/** Preferences onChangeListener. */
	private MyPreferencesOnChangeListener prefsOnChangeListener = new MyPreferencesOnChangeListener();

	/**
	 * PreferencesOnChangeListener.
	 * 
	 * @author Felix Bechstein
	 */
	private class MyPreferencesOnChangeListener implements
			SharedPreferences.OnSharedPreferenceChangeListener {
		/** Changed? */
		private boolean[] changed = { false, false };

		/**
		 * Called when prefs are changed, added or removed.
		 * 
		 * @param prefs
		 *            Preferences
		 * @param key
		 *            key
		 */
		@Override
		public void onSharedPreferenceChanged(final SharedPreferences prefs,
				final String key) {
			if (key.equals(PREFS_ENABLE_GMX) || key.equals(PREFS_SENDER)
					|| key.equals(PREFS_PASSWORD_GMX) || key.equals(PREFS_MAIL)) {
				this.changed[Connector.GMX] = true;
			}
		}

		/**
		 * Were preferences changed since last call for connector?
		 * 
		 * @param connector
		 *            Connector
		 * @return was changed?
		 */
		public boolean wasChanged(final short connector) {
			boolean ret = this.changed[connector];
			this.changed[connector] = false;
			return ret;
		}
	}

	/**
	 * Parse a String of "name (number), name (number), number, ..." to
	 * addresses.
	 * 
	 * @param reciepients
	 *            reciepients
	 * @return array of reciepients
	 */
	private static String[] parseReciepients(final String reciepients) {
		int i = 0;
		int p = reciepients.indexOf(',');
		while (p >= 0) {
			++i;
			if (p == reciepients.length()) {
				p = -1;
			} else {
				p = reciepients.indexOf(',', p + 1);
			}
		}
		if (i < 2) {
			i = 2;
		}
		String[] ret = new String[i + 1];
		p = 0;
		int p2 = reciepients.indexOf(',', p + 1);
		i = 0;
		while (p >= 0) {
			if (p == 0) {
				p--;
			}
			if (p2 > 0) {
				ret[i] = reciepients.substring(p + 1, p2).trim();
			} else {
				ret[i] = reciepients.substring(p + 1).trim();
			}
			if (p == -1) {
				p++;
			}

			if (p == reciepients.length()) {
				p = -1;
			} else {
				p = reciepients.indexOf(',', p + 1);
				if (p == reciepients.length()) {
					p2 = -1;
				} else {
					p2 = reciepients.indexOf(',', p + 1);
				}
				++i;
			}
		}

		for (i = 0; i < ret.length; i++) {
			if (ret[i] == null) {
				continue;
			}
			p = ret[i].lastIndexOf('(');
			if (p >= 0) {
				p2 = ret[i].indexOf(')', p);
				if (p2 < 0) {
					ret[i] = null;
				} else {
					ret[i] = ret[i].substring(p + 1, p2);
				}
			}
		}

		return ret;
	}

	/**
	 * Send Text.
	 * 
	 * @param connector
	 *            which connector should be used.
	 */
	private void send(final short connector) {
		// fetch text/recipient
		String to = ((EditText) this.findViewById(R.id.to)).getText()
				.toString();
		String text = ((EditText) this.findViewById(R.id.text)).getText()
				.toString();
		if (to.length() == 0 || text.length() == 0) {
			return;
		}
		String[] numbers = AndGMXsms.parseReciepients(to);
		// fix number prefix
		for (int i = 0; i < numbers.length; i++) {
			String t = numbers[i];
			if (t != null) {
				if (!t.startsWith("+")) {
					if (t.startsWith("00")) {
						t = "+" + t.substring(2);
					} else if (t.startsWith("0")) {
						t = AndGMXsms.prefsDefPrefix + t.substring(1);
					}
				}
			}
			numbers[i] = AndGMXsms.cleanRecipient(t);
		}
		// start a Connector Thread
		Connector.send(connector, numbers, text);
	}

	/**
	 * Clean recipient's phone number from [ -.()].
	 * 
	 * @param recipient
	 *            recipient's mobile number
	 * @return clean number
	 */
	public static final String cleanRecipient(final String recipient) {
		if (recipient == null) {
			return "";
		}
		return recipient.replace(" ", "").replace("-", "").replace(".", "")
				.replace("(", "").replace(")", "").trim();
	}

	/**
	 * Send AndGMXsms a Message.
	 * 
	 * @param messageType
	 *            type
	 * @param data
	 *            data
	 */
	public static final void sendMessage(final int messageType,
			final Object data) {
		Message.obtain(AndGMXsms.me.messageHandler, messageType, data)
				.sendToTarget();
	}

	/**
	 * Calc MD5 Hash from String.
	 * 
	 * @param s
	 *            input
	 * @return hash
	 */
	private static String md5(final String s) {
		try {
			// Create MD5 Hash
			MessageDigest digest = java.security.MessageDigest
					.getInstance("MD5");
			digest.update(s.getBytes());
			byte[] messageDigest = digest.digest();
			// Create Hex String
			StringBuilder hexString = new StringBuilder(32);
			int b;
			for (int i = 0; i < messageDigest.length; i++) {
				b = 0xFF & messageDigest[i];
				if (b < 0x10) {
					hexString.append('0' + Integer.toHexString(b));
				} else {
					hexString.append(Integer.toHexString(b));
				}
			}
			return hexString.toString();
		} catch (NoSuchAlgorithmException e) {
			Log.e(TAG, null, e);
		}
		return "";
	}
}
