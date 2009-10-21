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
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.MultiAutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import com.admob.android.ads.AdView;

/**
 * Main Activity.
 * 
 * @author flx
 */
public class WebSMS extends Activity implements OnClickListener,
		ServiceConnection {
	/** Tag for output. */
	private static final String TAG = "WebSMS";

	/** Static reference to running Activity. */
	private static WebSMS me;
	/** Preference's name: last version run. */
	private static final String PREFS_LAST_RUN = "lastrun";
	/** Preference's name: mail. */
	private static final String PREFS_MAIL = "mail";
	/** Preference's name: username. */
	private static final String PREFS_USER = "user";
	/** Preference's name: user's password - gmx. */
	private static final String PREFS_PASSWORD_GMX = "password";
	/** Preference's name: user's password - o2. */
	private static final String PREFS_PASSWORD_O2 = "password_o2";
	/** Preference's name: sipgate username. */
	private static final String PREFS_USER_SIPGATE = "user_sipgate";
	/** Preference's name: user's password - sipgate. */
	private static final String PREFS_PASSWORD_SIPGATE = "password_sipgate";
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
	/** Preference's name: enable sipgate. */
	private static final String PREFS_ENABLE_SIPGATE = "enable_sipgate";
	/** Preference's name: gmx hostname id. */
	private static final String PREFS_GMX_HOST = "gmx_host";
	/** Preference's name: to. */
	private static final String PREFS_TO = "to";
	/** Preference's name: text. */
	private static final String PREFS_TEXT = "text";
	/** Preferences: mail. */
	static String prefsMail;
	/** Preferences: username - gmx. */
	static String prefsUserGMX;
	/** Preferences: user's password - gmx. */
	static String prefsPasswordGMX;
	/** Preferences: user's password - o2. */
	/** Preferences: username sipgate. */
	static String prefsUserSipgate;
	/** Preferences: user's password - sipgate. */
	static String prefsPasswordSipgate;
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
	/** Preferences: ready for sipgate? */
	static boolean prefsReadySipgate = false;
	/** Remaining free sms. */
	static String remFree = null;
	/** Preferences: use softkeys. */
	static boolean prefsSoftKeys = false;
	/** Preferences: enable gmx. */
	static boolean prefsEnableGMX = false;
	/** Preferences: enable o2. */
	static boolean prefsEnableO2 = false;
	/** Preferences: enable o2. */
	static boolean prefsEnableSipgate = false;
	/** Preferences: hide ads. */
	static boolean prefsNoAds = false;
	/** Preferences: gmx hostname id. */
	static int prefsGMXhostname = 0;

	/** Array of md5(prefsSender) for which no ads should be displayed. */
	private static final String[] NO_AD_HASHS = {
			"2986b6d93053a53ff13008b3015a77ff", // me
			"f6b3b72300e918436b4c4c9fdf909e8c", // joerg s.
			"4c18f7549b643045f0ff69f61e8f7e72", // frank j.
			"7684154558d19383552388d9bc92d446", // henning k.
			"64c7414288e9a9b57a33e034f384ed30", // dominik l.
			"c479a2e701291c751f0f91426bcaabf3", // bernhard g.
			"ae7dfedf549f98a349ad8c2068473c6b" // dominik k.-v.
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
	/** Dialog: updates. */
	private static final int DIALOG_UPDATE = 2;
	/** Dialog: captcha. */
	private static final int DIALOG_CAPTCHA = 3;

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
	/** Message show cpatcha. */
	static final int MESSAGE_ANTICAPTCHA = 7;

	/** Menu: send via GMX. */
	private static final int MENU_SEND_GMX = Connector.GMX + 1;
	/** Menu: send via O2. */
	private static final int MENU_SEND_O2 = Connector.O2 + 1;
	/** Menu: send via Sipgate. */
	private static final int MENU_SEND_SIPGATE = Connector.SIPGATE + 1;
	/** Menu: cancel. */
	private static final int MENU_CANCEL = 4;

	/** Persistent Message store. */
	private static String lastMsg = null;
	/** Persistent Recipient store. */
	private static String lastTo = null;

	/**
	 * Remaining free sms. First dimension is the Connector, second is the
	 * free/limit.
	 */
	static final int[][] SMS_FREE = { { 0, 0 }, { 0, 0 }, { 0, 0 } };
	/** ID of sms free count in array SMS_FREE. */
	static final int SMS_FREE_COUNT = 0;
	/** ID of sms limit in array SMS_FREE. */
	static final int SMS_FREE_LIMIT = 1;
	// balance of sipgate.de
	static String BALANCE_SIPGATE = "0.00";

	/** Text's label. */
	private TextView textLabel;

	/** Resource @@string/text__. */
	private String textLabelRef;

	/** Shared Preferences. */
	private SharedPreferences preferences;

	/** MessageHandler. */
	private Handler messageHandler;

	/** Bound service. */
	private IIOOp mIOOp;

	/**
	 * Called when the activity is first created.
	 * 
	 * @param savedInstanceState
	 *            default param
	 */
	@Override
	public final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		// save ref to me.
		me = this;
		// inflate XML
		this.setContentView(R.layout.main);
		// register MessageHandler
		this.messageHandler = new WebSMS.MessageHandler();

		// Restore preferences
		this.preferences = PreferenceManager.getDefaultSharedPreferences(this);
		// display changelog?
		String v0 = this.preferences.getString(PREFS_LAST_RUN, "");
		String v1 = this.getResources().getString(R.string.app_version);
		if (!v0.equals(v1)) {
			SharedPreferences.Editor editor = this.preferences.edit();
			editor.putString(PREFS_LAST_RUN, v1);
			editor.commit();
			this.showDialog(DIALOG_UPDATE);
		}
		// listen on changes to prefs
		this.preferences
				.registerOnSharedPreferenceChangeListener(this.prefsOnChgListener);
		this.reloadPrefs();

		lastTo = this.preferences.getString(PREFS_TO, "");
		lastMsg = this.preferences.getString(PREFS_TEXT, "");

		// register Listener
		((Button) this.findViewById(R.id.send_gmx)).setOnClickListener(this);
		((Button) this.findViewById(R.id.send_o2)).setOnClickListener(this);
		((Button) this.findViewById(R.id.send_sipgate))
				.setOnClickListener(this);
		((Button) this.findViewById(R.id.cancel)).setOnClickListener(this);

		this.textLabelRef = this.getResources().getString(R.string.text__);
		this.textLabel = (TextView) this.findViewById(R.id.text_);
		((EditText) this.findViewById(R.id.text))
				.addTextChangedListener(this.textWatcher);

		((TextView) this.findViewById(R.id.freecount)).setOnClickListener(this);

		final MultiAutoCompleteTextView to = (MultiAutoCompleteTextView) this
				.findViewById(R.id.to);
		to.setAdapter(new MobilePhoneAdapter(this));
		to.setTokenizer(new MultiAutoCompleteTextView.CommaTokenizer());

		final Intent intent = this.getIntent();
		final String action = intent.getAction();
		if (action != null) { // && action.equals(Intent.ACTION_SENDTO)) {
			// launched by clicking a sms: link, target number is in URI.
			final Uri uri = intent.getData();
			if (uri != null && uri.getScheme().equalsIgnoreCase("sms")) {
				String recipient = uri.getSchemeSpecificPart();
				if (recipient != null) {
					// recipient = AndGMXsms.cleanRecipient(recipient);
					((EditText) this.findViewById(R.id.to)).setText(recipient);
					lastTo = recipient;
				}
			}
		}

		// reload sms from notification
		final Uri data = intent.getData();
		if (data != null) {
			final String recipient = data.getHost();
			String text = data.getPath();
			String error = null;
			final int i = text.lastIndexOf('/');
			if (i > 0) {
				error = text.substring(i + 1);
				text = text.substring(0, i);
			}
			if (recipient != null) {
				((EditText) this.findViewById(R.id.to)).setText(recipient);
				lastTo = recipient;
			}
			if (text != null && text.length() > 0) {
				text = text.substring(1);
				((EditText) this.findViewById(R.id.to)).setText(text);
				lastMsg = text;
			}
			if (error != null) {
				Toast.makeText(this, error, Toast.LENGTH_LONG).show();
			}

			if (!prefsNoAds) {
				// do not display any ads for donators
				// display ads
				((AdView) WebSMS.this.findViewById(R.id.ad))
						.setVisibility(View.VISIBLE);
			}
		}

		this.bindService(new Intent(this, IOService.class), this,
				Context.BIND_AUTO_CREATE);

		// check default prefix
		if (!prefsDefPrefix.startsWith("+")) {
			WebSMS.this.log(R.string.log_error_defprefix);
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
					+ WebSMS.this.getResources().getString(
							R.string.click_for_update));
		} else {
			TextView tw = (TextView) this.findViewById(R.id.freecount);
			tw.setText(this.getResources().getString(R.string.free_)
					+ " "
					+ WebSMS.this.getResources().getString(
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
					&& this.prefsOnChgListener.wasChanged(Connector.GMX)) {
				String[] params = new String[ConnectorGMX.IDS_BOOTSTR];
				params[Connector.ID_ID] = Connector.ID_BOOSTR;
				params[ConnectorGMX.ID_MAIL] = prefsMail;
				params[ConnectorGMX.ID_PW] = prefsPasswordGMX;
				Connector.bootstrap(this, Connector.GMX, params);
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
	 * Called on Activity destroy.
	 */
	@Override
	public final void onDestroy() {
		super.onDestroy();
		this.unbindService(this);
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
		prefsUserGMX = this.preferences.getString(PREFS_USER, "");
		prefsPasswordGMX = this.preferences.getString(PREFS_PASSWORD_GMX, "");

		prefsEnableO2 = this.preferences.getBoolean(PREFS_ENABLE_O2, false);
		prefsPasswordO2 = this.preferences.getString(PREFS_PASSWORD_O2, "");

		prefsEnableSipgate = this.preferences.getBoolean(PREFS_ENABLE_SIPGATE,
				false);
		prefsUserSipgate = this.preferences.getString(PREFS_USER_SIPGATE, "");
		prefsPasswordSipgate = this.preferences.getString(
				PREFS_PASSWORD_SIPGATE, "");

		prefsNoAds = false;
		String hash = md5(prefsSender);
		for (String h : NO_AD_HASHS) {
			if (hash.equals(h)) {
				prefsNoAds = true;
				break;
			}
		}

		prefsGMXhostname = this.preferences.getInt(PREFS_GMX_HOST,
				prefsGMXhostname);
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
			if (prefsEnableO2 || prefsEnableSipgate) {
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
			if (prefsEnableGMX || prefsEnableSipgate) {
				btn.setText(this.getResources().getString(R.string.send_o2));
			} else {
				btn.setText(this.getResources().getString(R.string.send_));
			}
		} else {
			btn.setVisibility(View.GONE);
		}
		btn = (Button) this.findViewById(R.id.send_sipgate);
		if (prefsEnableSipgate && !prefsSoftKeys) {
			btn.setEnabled(prefsReadySipgate);
			btn.setVisibility(View.VISIBLE);
			if (prefsEnableGMX || prefsEnableO2) {
				btn.setText(this.getResources()
						.getString(R.string.send_sipgate));
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
				&& prefsUserGMX.length() != 0 && prefsPasswordGMX.length() != 0
				&& prefsSender.length() != 0) {
			prefsReadyGMX = true;
		} else {
			if (prefsEnableGMX && !Connector.inBootstrap) {
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
		if (prefsEnableSipgate && prefsUserSipgate.length() != 0
				&& prefsPasswordSipgate.length() != 0) {
			prefsReadySipgate = true;
		} else {
			if (prefsEnableSipgate) {
				this.log(this.getResources().getString(
						R.string.log_empty_settings));
			}
			prefsReadySipgate = false;
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
		editor.putString(PREFS_USER, prefsUserGMX);
		editor.putString(PREFS_PASSWORD_GMX, prefsPasswordGMX);
		editor.putInt(PREFS_GMX_HOST, prefsGMXhostname);
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
				Connector.update(this, Connector.GMX);
			}
			if (prefsEnableO2) {
				Connector.update(this, Connector.O2);
			}
			if (prefsEnableSipgate) {
				Connector.update(this, Connector.SIPGATE);
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
		case R.id.send_sipgate:
			this.send(Connector.SIPGATE);
			break;
		case R.id.cancel:
			this.reset();
			break;
		case R.id.captcha_btn:
			ConnectorO2.anticaptcha = ((EditText) v.getRootView().findViewById(
					R.id.captcha_edt)).getText().toString();
			this.dismissDialog(DIALOG_CAPTCHA);
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
					if (prefsEnableO2 || prefsEnableSipgate) {
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
					if (prefsEnableGMX || prefsEnableSipgate) {
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
			if (prefsEnableSipgate) {
				if (menu.findItem(MENU_SEND_SIPGATE) == null) {
					// add menu to send text
					MenuItem m;
					if (prefsEnableO2 || prefsEnableGMX) {
						m = menu.add(0, MENU_SEND_SIPGATE, 0, this
								.getResources()
								.getString(R.string.send_sipgate));
					} else {
						m = menu.add(0, MENU_SEND_SIPGATE, 0, this
								.getResources().getString(R.string.send_));
					}
					m.setIcon(android.R.drawable.ic_menu_send);
				}
			} else {
				menu.removeItem(MENU_SEND_SIPGATE);
			}
		} else {
			menu.removeItem(MENU_SEND_GMX);
			menu.removeItem(MENU_SEND_O2);
			menu.removeItem(MENU_SEND_SIPGATE);
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
		case MENU_SEND_SIPGATE:
			this.send(Connector.SIPGATE);
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
			myDialog.setTitle(this.getString(R.string.about_) + " v"
					+ this.getString(R.string.app_version));
			((Button) myDialog.findViewById(R.id.btn_donate))
					.setOnClickListener(this);
			break;
		case DIALOG_HELP:
			myDialog = new Dialog(this);
			myDialog.setContentView(R.layout.help);
			myDialog.setTitle(R.string.help_);
			break;
		case DIALOG_UPDATE:
			myDialog = new Dialog(this);
			myDialog.setContentView(R.layout.update);
			myDialog.setTitle(R.string.changelog_);
			LinearLayout layout = (LinearLayout) myDialog
					.findViewById(R.id.base_view);
			TextView tw;
			String[] changes = this.getResources().getStringArray(
					R.array.updates);
			for (String c : changes) {
				tw = new TextView(this);
				tw.setText(c);
				layout.addView(tw);
			}
			break;
		case DIALOG_CAPTCHA:
			myDialog = new Dialog(this);
			myDialog.setContentView(R.layout.captcha);
			((Button) myDialog.findViewById(R.id.captcha_btn))
					.setOnClickListener(this);
			break;
		default:
			myDialog = null;
		}
		return myDialog;
	}

	/**
	 * Called to create dialog.
	 * 
	 * @param id
	 *            Dialog id
	 * @param dialog
	 *            Dialog
	 */
	@Override
	protected final void onPrepareDialog(final int id, final Dialog dialog) {
		switch (id) {
		case DIALOG_CAPTCHA:
			if (ConnectorO2.captcha != null) {
				((ImageView) dialog.findViewById(R.id.captcha_img))
						.setImageDrawable(ConnectorO2.captcha);
				ConnectorO2.captcha = null;
			}
			break;
		default:
			break;
		}
	}

	/**
	 * Log text.
	 * 
	 * @param text
	 *            text as resID
	 */
	public final void log(final int text) {
		this.log(this.getString(text));
	}

	/**
	 * Called when a connection to the Service has been established, with the
	 * IBinder of the communication channel to the Service.
	 * 
	 * @param name
	 *            The concrete component name of the service that has been
	 *            connected.
	 * @param service
	 *            The IBinder of the Service's communication channel, which you
	 *            can now make calls on.
	 */
	public final void onServiceConnected(final ComponentName name,
			final IBinder service) {
		this.mIOOp = IIOOp.Stub.asInterface(service);
	}

	/**
	 * Called when a connection to the Service has been lost.
	 * 
	 * @param name
	 *            The concrete component name of the service that has been
	 *            connected.
	 */
	public final void onServiceDisconnected(final ComponentName name) {
		this.mIOOp = null;
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
			WebSMS.doPreferences = true;
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
			case MESSAGE_LOG: // msg is String or Resource StringID
				if (msg.obj instanceof String) {
					WebSMS.this.log((String) msg.obj);
				} else if (msg.obj instanceof Integer) {
					WebSMS.this.log(WebSMS.this.getResources().getString(
							((Integer) msg.obj).intValue()));
				} else {
					WebSMS.this.log(msg.obj.toString());
				}
				return;
			case MESSAGE_FREECOUNT:
				WebSMS.remFree = "";
				if (WebSMS.prefsEnableGMX) {
					WebSMS.remFree = "GMX: "
							+ WebSMS.SMS_FREE[Connector.GMX][WebSMS.SMS_FREE_COUNT]
							+ " / "
							+ WebSMS.SMS_FREE[Connector.GMX][WebSMS.SMS_FREE_LIMIT];
				}
				if (WebSMS.prefsEnableO2) {
					if (WebSMS.remFree.length() > 0) {
						WebSMS.remFree += " - ";
					}
					WebSMS.remFree += "O2: "
							+ WebSMS.SMS_FREE[Connector.O2][WebSMS.SMS_FREE_COUNT];
					if (WebSMS.SMS_FREE[Connector.O2][WebSMS.SMS_FREE_LIMIT] > 0) {
						WebSMS.remFree += " / "
								+ WebSMS.SMS_FREE[Connector.O2][WebSMS.SMS_FREE_LIMIT];
					}
				}
				if (WebSMS.prefsEnableSipgate) {
					if (WebSMS.remFree.length() > 0) {
						WebSMS.remFree += " - ";
					}
					WebSMS.remFree += "Sipgate: ";
					WebSMS.remFree += BALANCE_SIPGATE + " \u20AC";
				}
				if (WebSMS.remFree.length() == 0) {
					WebSMS.remFree = "---";
				}
				TextView tw = (TextView) WebSMS.this
						.findViewById(R.id.freecount);
				tw.setText(WebSMS.this.getResources().getString(R.string.free_)
						+ " "
						+ WebSMS.remFree
						+ " "
						+ WebSMS.this.getResources().getString(
								R.string.click_for_update));
				return;
			case MESSAGE_SETTINGS:
				WebSMS.this.startActivity(new Intent(WebSMS.this,
						Preferences.class));
			case MESSAGE_RESET:
				WebSMS.this.reset();
				return;
			case MESSAGE_PREFSREADY:
				WebSMS.this.checkPrefs();
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
		public void afterTextChanged(final Editable s) {
			WebSMS.this.textLabel.setText(WebSMS.this.textLabelRef + " ("
					+ s.length() + "):");
		}

		/** Needed dummy. */
		public void beforeTextChanged(final CharSequence s, final int start,
				final int count, final int after) {
		}

		/** Needed dummy. */
		public void onTextChanged(final CharSequence s, final int start,
				final int before, final int count) {
		}
	};

	/** Preferences onChangeListener. */
	private MyPrefsOnChgListener prefsOnChgListener = new MyPrefsOnChgListener();

	/**
	 * PreferencesOnChangeListener.
	 * 
	 * @author Felix Bechstein
	 */
	private class MyPrefsOnChgListener implements
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
		public void onSharedPreferenceChanged(final SharedPreferences prefs,
				final String key) {
			if (key.equals(PREFS_ENABLE_GMX) || key.equals(PREFS_SENDER)
					|| key.equals(PREFS_PASSWORD_GMX) || key.equals(PREFS_MAIL)) {
				this.changed[Connector.GMX] = true;
			}
			if (key.equals(PREFS_SENDER)) {
				// check for wrong sender format. people can't read..
				final String p = prefs.getString(PREFS_SENDER, "");
				if (!p.startsWith("+")) {
					WebSMS.this.log(R.string.log_error_sender);
				}
			}
			if (key.equals(PREFS_DEFPREFIX)) {
				final String p = prefs.getString(PREFS_DEFPREFIX, "");
				if (!p.startsWith("+")) {
					WebSMS.this.log(R.string.log_error_defprefix);
				}
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
	 * Send Text.
	 * 
	 * @param connector
	 *            which connector should be used.
	 */
	private void send(final short connector) {
		// fetch text/recipient
		final String to = ((EditText) this.findViewById(R.id.to)).getText()
				.toString();
		final String text = ((EditText) this.findViewById(R.id.text)).getText()
				.toString();
		if (to.length() == 0 || text.length() == 0) {
			return;
		}

		if (this.mIOOp == null) {
			Log.e(TAG, "mIOOp == null");
			return;
		}

		if (!prefsNoAds) {
			// do not display any ads for donators
			// display ads
			((AdView) WebSMS.this.findViewById(R.id.ad))
					.setVisibility(View.VISIBLE);
		}
		// start a Connector Thread
		// Connector.send(connector, to, text);
		String[] params = new String[Connector.IDS_SEND];
		params[Connector.ID_ID] = Connector.ID_SEND;
		params[Connector.ID_TO] = to;
		params[Connector.ID_TEXT] = text;
		try {
			this.mIOOp.sendMessage(connector, params);
		} catch (RemoteException e) {
			Log.e(TAG, null, e);
		} finally {
			this.reset();
		}
	}

	/**
	 * Send AndGMXsms a Message.
	 * 
	 * @param messageType
	 *            type
	 * @param data
	 *            data
	 */
	public static final void pushMessage(final int messageType,
			final Object data) {
		Message.obtain(WebSMS.me.messageHandler, messageType, data)
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
