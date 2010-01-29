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

import java.util.ArrayList;
import java.util.Calendar;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Contacts.PeopleColumns;
import android.provider.Contacts.Phones;
import android.provider.Contacts.PhonesColumns;
import android.telephony.TelephonyManager;
import android.telephony.gsm.SmsMessage;
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
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.MultiAutoCompleteTextView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.admob.android.ads.AdView;

import de.ub0r.android.websms.connector.common.Connector;
import de.ub0r.android.websms.connector.common.ConnectorCommand;
import de.ub0r.android.websms.connector.common.ConnectorSpec;
import de.ub0r.android.websms.connector.common.Utils;
import de.ub0r.android.websms.connector.common.ConnectorSpec.SubConnectorSpec;

/**
 * Main Activity.
 * 
 * @author flx
 */
@SuppressWarnings("deprecation")
public class WebSMS extends Activity implements OnClickListener,
		OnDateSetListener, OnTimeSetListener {
	/** Tag for output. */
	private static final String TAG = "WebSMS";

	/** Static reference to running Activity. */
	private static WebSMS me;
	/** Preference's name: last version run. */
	private static final String PREFS_LAST_RUN = "lastrun";
	/** Preference's name: user's phonenumber. */
	static final String PREFS_SENDER = "sender";
	/** Preference's name: default prefix. */
	static final String PREFS_DEFPREFIX = "defprefix";
	/** Preference's name: touch keyboard. */
	private static final String PREFS_SOFTKEYS = "softkeyboard";
	/** Preference's name: update balace on start. */
	private static final String PREFS_AUTOUPDATE = "autoupdate";
	/** Preference's name: exit after sending. */
	private static final String PREFS_AUTOEXIT = "autoexit";
	/** Preference's name: show mobile numbers only. */
	private static final String PREFS_MOBILES_ONLY = "mobiles_only";
	/** Preference's name: vibrate on failed sending. */
	static final String PREFS_FAIL_VIBRATE = "fail_vibrate";
	/** Preference's name: sound on failed sending. */
	static final String PREFS_FAIL_SOUND = "fail_sound";
	/** Preferemce's name: enable change connector button. */
	private static final String PREFS_CHANGE_CONNECTOR_BUTTON = // .
	"change_connector_button";
	/** Preferemce's name: hide cancel button. */
	private static final String PREFS_HIDE_CANCEL_BUTTON = "hide_cancel_button";

	/** Preference's name: to. */
	private static final String PREFS_TO = "to";
	/** Preference's name: text. */
	private static final String PREFS_TEXT = "text";
	/** Preference's name: selected {@link ConnectorSpec} ID. */
	private static final String PREFS_CONNECTOR_ID = "connector_id";
	/** Preference's name: selected {@link SubConnectorSpec} ID. */
	private static final String PREFS_SUBCONNECTOR_ID = "subconnector_id";

	/** Sleep before autoexit. */
	private static final int SLEEP_BEFORE_EXIT = 75;

	/** Preferences: hide ads. */
	private static boolean prefsNoAds = false;
	/** Hased IMEI. */
	private static String imeiHash = null;
	/** Preferences: selected {@link ConnectorSpec}. */
	private static ConnectorSpec prefsConnectorSpec = null;
	/** Preferences: selected {@link SubConnectorSpec}. */
	private static SubConnectorSpec prefsSubConnectorSpec = null;
	/** Save prefsConnectorSpec.getID() here. */
	private static String prefsConnectorID = null;

	/** List of available {@link ConnectorSpec}s. */
	private static final ArrayList<ConnectorSpec> CONNECTORS = // .
	new ArrayList<ConnectorSpec>();

	/** Array of md5(prefsSender) for which no ads should be displayed. */
	private static final String[] NO_AD_HASHS = {
			"43dcb861b9588fb733300326b61dbab9", // flx
			"57a3c7c19329fd84c2252a9b2866dd93", // mirweb
			"10b7a2712beee096acbc67416d7d71a1", // mo
			"f6b3b72300e918436b4c4c9fdf909e8c", // joerg s.
			"4c18f7549b643045f0ff69f61e8f7e72", // frank j.
			"7684154558d19383552388d9bc92d446", // henning k.
			"64c7414288e9a9b57a33e034f384ed30", // dominik l.
			"c479a2e701291c751f0f91426bcaabf3", // bernhard g.
			"ae7dfedf549f98a349ad8c2068473c6b", // dominik k.-v.
			"18bc29cd511613552861da6ef51766ce", // niels b.
			"2985011f56d0049b0f4f0caed3581123", // sven l.
			"64724033da297a915a89023b11ac2e47", // wilfried m.
			"cfd8d2efb3eac39705bd62c4dfe5e72d", // achim e.
			"ca56e7518fdbda832409ef07edd4c273", // michael s.
			"bed2f068ca8493da4179807d1afdbd83", // axel q.
			"4c35400c4fa3ffe2aefcf1f9131eb855", // gerhard s.
			"02158d2a80b1ef9c4d684a4ca808b93d", // camilo s.
			"1177c6e67f98cdfed6c84d99e85d30de", // daniel p.
			"3f082dd7e21d5c64f34a69942c474ce7", // andre j.
			"5383540b2f8c298532f874126b021e73", // marco a.
			"858ddfb8635d1539884086dca2726468", // lado
			"6e8bbb35091219a80e278ae61f31cce9", // mario s.
			"9f01eae4eaecd9158a2caddc04bad77e", // andreas p.
	};

	/** true if preferences got opened. */
	static boolean doPreferences = false;

	/** Dialog: about. */
	private static final int DIALOG_ABOUT = 0;
	/** Dialog: updates. */
	private static final int DIALOG_UPDATE = 2;
	/** Dialog: post donate. */
	private static final int DIALOG_POSTDONATE = 4;
	/** Dialog: custom sender. */
	private static final int DIALOG_CUSTOMSENDER = 5;
	/** Dialog: send later: date. */
	private static final int DIALOG_SENDLATER_DATE = 6;
	/** Dialog: send later: time. */
	private static final int DIALOG_SENDLATER_TIME = 7;
	/** Dialog: pre donate. */
	private static final int DIALOG_PREDONATE = 8;

	/** Intent's extra for error messages. */
	static final String EXTRA_ERRORMESSAGE = // .
	"de.ub0r.android.intent.extra.ERRORMESSAGE";

	/** Persistent Message store. */
	private static String lastMsg = null;
	/** Persistent Recipient store. */
	private static String lastTo = null;
	/** Backup for params: custom sender. */
	private static String lastCustomSender = null;
	/** Backup for params: send later. */
	private static long lastSendLater = -1;

	/** Helper for API 5. */
	static HelperAPI5Contacts helperAPI5c = null;

	/** Text's label. */
	private TextView textLabel;

	/** Show extras. */
	private boolean showExtras = false;

	/** TextWatcher updating char count on writing. */
	private TextWatcher textWatcher = new TextWatcher() {
		/**
		 * {@inheritDoc}
		 */
		public void afterTextChanged(final Editable s) {
			int[] l = SmsMessage.calculateLength(s, false);
			WebSMS.this.textLabel.setText(l[0] + "/" + l[2]);
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

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

		// save ref to me.
		me = this;
		try {
			WebSMS.helperAPI5c = new HelperAPI5Contacts();
			if (!helperAPI5c.isAvailable()) {
				WebSMS.helperAPI5c = null;
			}
		} catch (VerifyError e) {
			WebSMS.helperAPI5c = null;
			Log.d(TAG, "no api5 running", e);
		}
		// Restore preferences
		final SharedPreferences p = PreferenceManager
				.getDefaultSharedPreferences(this);
		// inflate XML
		if (p.getBoolean(PREFS_SOFTKEYS, false)) {
			this.setContentView(R.layout.main_touch);
		} else {
			this.setContentView(R.layout.main);
		}

		this.findViewById(R.id.to).requestFocus();

		// display changelog?
		String v0 = p.getString(PREFS_LAST_RUN, "");
		String v1 = this.getResources().getString(R.string.app_version);
		if (!v0.equals(v1)) {
			SharedPreferences.Editor editor = p.edit();
			editor.putString(PREFS_LAST_RUN, v1);
			editor.commit();
			this.showDialog(DIALOG_UPDATE);
		}

		this.reloadPrefs();

		lastTo = p.getString(PREFS_TO, "");
		lastMsg = p.getString(PREFS_TEXT, "");

		// register Listener
		this.findViewById(R.id.send_).setOnClickListener(this);
		this.findViewById(R.id.cancel).setOnClickListener(this);
		this.findViewById(R.id.change_connector).setOnClickListener(this);
		this.findViewById(R.id.extras).setOnClickListener(this);
		this.findViewById(R.id.custom_sender).setOnClickListener(this);
		this.findViewById(R.id.send_later).setOnClickListener(this);

		this.textLabel = (TextView) this.findViewById(R.id.text_);
		((EditText) this.findViewById(R.id.text))
				.addTextChangedListener(this.textWatcher);

		((TextView) this.findViewById(R.id.freecount)).setOnClickListener(this);

		final MultiAutoCompleteTextView to = (MultiAutoCompleteTextView) this
				.findViewById(R.id.to);
		to.setAdapter(new MobilePhoneAdapter(this));
		to.setTokenizer(new MultiAutoCompleteTextView.CommaTokenizer());

		Intent intent = this.getIntent();
		final String action = intent.getAction();
		if (action != null) {
			// launched by clicking a sms: link, target number is in URI.
			final Uri uri = intent.getData();
			if (uri != null) {
				final String scheme = uri.getScheme();
				if (scheme.equals("sms") || scheme.equals("smsto")) {
					String s = uri.getSchemeSpecificPart();
					if (s != null) {
						s = s.trim();
						if (s.endsWith(",")) {
							s = s.substring(0, s.length() - 1).trim();
						}
						// recipient = WebSMS.cleanRecipient(recipient);
						if (s.indexOf('<') < 0) {
							// try to fetch recipient's name from phonebook
							String n = null;
							if (helperAPI5c != null) {
								try {
									n = helperAPI5c.getNameForNumber(this, s);
								} catch (NoClassDefFoundError e) {
									helperAPI5c = null;
								}
							}
							if (helperAPI5c == null) {
								Cursor c = this
										.managedQuery(Phones.CONTENT_URI,
												new String[] {
														PhonesColumns.NUMBER,
														PeopleColumns.// .
														DISPLAY_NAME },
												PhonesColumns.NUMBER + " = '"
														+ s + "'", null, null);
								if (c.moveToFirst()) {
									n = c.getString(c.getColumnIndex(// .
											PeopleColumns.DISPLAY_NAME));
								}
							}
							if (n != null) {
								s = n + " <" + s + ">, ";
							}
						}
						((EditText) this.findViewById(R.id.to)).setText(s);
						lastTo = s;
					}
					final Bundle extras = intent.getExtras();
					if (extras != null) {
						s = extras.getCharSequence(Intent.EXTRA_TEXT)
								.toString();
						if (s != null) {
							((EditText) this.findViewById(R.id.text))
									.setText(s);
							lastMsg = s;
						}
						s = extras.getString(EXTRA_ERRORMESSAGE);
						if (s != null) {
							Toast.makeText(this, s, Toast.LENGTH_LONG).show();
						}
					}
					if (!prefsNoAds) {
						// do not display any ads for donators
						// display ads
						((AdView) WebSMS.this.findViewById(R.id.ad))
								.setVisibility(View.VISIBLE);
					}
				}
			}
		}

		// check default prefix
		if (!p.getString(PREFS_DEFPREFIX, "").startsWith("+")) {
			WebSMS.this.log(R.string.log_error_defprefix);
		}
		if (p.getBoolean(PREFS_AUTOUPDATE, false)) {
			this.updateFreecount(false);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final void onResume() {
		super.onResume();
		// set free sms count
		this.updateBalance();

		// if coming from prefs..
		if (doPreferences) {
			this.reloadPrefs();
			doPreferences = false;
			final SharedPreferences p = PreferenceManager
					.getDefaultSharedPreferences(this);
			final String defPrefix = p.getString(PREFS_DEFPREFIX, "+49");
			final String defSender = p.getString(PREFS_SENDER, "");
			final ConnectorSpec[] css = getConnectors(
					ConnectorSpec.CAPABILITIES_BOOTSTRAP, // .
					ConnectorSpec.STATUS_ENABLED);
			for (ConnectorSpec cs : css) {
				final Intent intent = new Intent(cs.getPackage()
						+ Connector.ACTION_RUN_BOOTSTRAP);
				ConnectorCommand.bootstrap(defPrefix, defSender).setToIntent(
						intent);
				Log.d(TAG, "send broadcast: " + intent.getAction());
				this.sendBroadcast(intent);
			}
		}

		this.setButtons();

		// reload text/recipient from local store
		final EditText et0 = (EditText) this.findViewById(R.id.text);
		if (lastMsg != null) {
			et0.setText(lastMsg);
		} else {
			et0.setText("");
		}
		final EditText et1 = (EditText) this.findViewById(R.id.to);
		if (lastTo != null) {
			et1.setText(lastTo);
		} else {
			et1.setText("");
		}

		if (lastTo != null && lastTo.length() > 0) {
			et0.requestFocus();
		} else {
			et1.requestFocus();
		}

		// query for connectors
		final Intent i = new Intent(Connector.ACTION_CONNECTOR_UPDATE);
		Log.d(TAG, "send broadcast: " + i.getAction());
		this.sendBroadcast(i);
	}

	/**
	 * Update balance.
	 */
	private void updateBalance() {
		final StringBuilder buf = new StringBuilder();
		final ConnectorSpec[] css = getConnectors(
				ConnectorSpec.CAPABILITIES_UPDATE, // .
				ConnectorSpec.STATUS_ENABLED);
		for (ConnectorSpec cs : css) {
			final String b = cs.getBalance();
			if (b == null || b.length() == 0) {
				continue;
			}
			if (buf.length() > 0) {
				buf.append(", ");
			}
			buf.append(cs.getName());
			buf.append(": ");
			buf.append(b);
		}

		TextView tw = (TextView) this.findViewById(R.id.freecount);
		tw.setText(this.getString(R.string.free_) + " " + buf.toString() + " "
				+ this.getString(R.string.click_for_update));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void onPause() {
		super.onPause();
		// store input data to persitent stores
		lastMsg = ((EditText) this.findViewById(R.id.text)).getText()
				.toString();
		lastTo = ((EditText) this.findViewById(R.id.to)).getText().toString();

		// store input data to preferences
		SharedPreferences.Editor editor = PreferenceManager
				.getDefaultSharedPreferences(this).edit();
		// common
		editor.putString(PREFS_TO, lastTo);
		editor.putString(PREFS_TEXT, lastMsg);
		// commit changes
		editor.commit();

		this.savePreferences();
	}

	/**
	 * Read static vars holding preferences.
	 */
	private void reloadPrefs() {
		final SharedPreferences p = PreferenceManager
				.getDefaultSharedPreferences(this);
		boolean b = p.getBoolean(PREFS_CHANGE_CONNECTOR_BUTTON, false);
		View v = this.findViewById(R.id.change_connector);
		if (b) {
			v.setVisibility(View.VISIBLE);
		} else {
			v.setVisibility(View.GONE);
		}

		b = !p.getBoolean(PREFS_HIDE_CANCEL_BUTTON, false);
		v = this.findViewById(R.id.cancel);
		if (b) {
			v.setVisibility(View.VISIBLE);
		} else {
			v.setVisibility(View.GONE);
		}

		prefsConnectorID = p.getString(PREFS_CONNECTOR_ID, "");
		prefsConnectorSpec = getConnectorByID(prefsConnectorID);
		if (prefsConnectorSpec != null) {
			prefsSubConnectorSpec = prefsConnectorSpec.getSubConnector(p
					.getString(PREFS_SUBCONNECTOR_ID, ""));
		}

		MobilePhoneAdapter.setMoileNubersObly(p.getBoolean(PREFS_MOBILES_ONLY,
				false));

		prefsNoAds = false;
		String hash = Utils.md5(p.getString(PREFS_SENDER, ""));
		for (String h : NO_AD_HASHS) {
			if (hash.equals(h)) {
				prefsNoAds = true;
				break;
			}
		}
		if (!prefsNoAds && this.getImeiHash() != null) {
			for (String h : NO_AD_HASHS) {
				if (imeiHash.equals(h)) {
					prefsNoAds = true;
					break;
				}
			}
		}

		this.setButtons();
	}

	/**
	 * Show/hide, enable/disable send buttons.
	 */
	private void setButtons() {
		// final ConnectorSpec[] enabled = getConnectors(
		// ConnectorSpec.CAPABILITIES_SEND, ConnectorSpec.STATUS_ENABLED);

		Button btn = (Button) this.findViewById(R.id.send_);
		// show/hide buttons
		btn.setEnabled(prefsConnectorSpec != null
				&& prefsSubConnectorSpec != null);
		btn.setVisibility(View.VISIBLE);

		if (prefsConnectorSpec != null && prefsSubConnectorSpec != null) {
			final boolean sFlashsms = prefsSubConnectorSpec
					.hasFeatures(SubConnectorSpec.FEATURE_FLASHSMS);
			final boolean sCustomsender = prefsSubConnectorSpec
					.hasFeatures(SubConnectorSpec.FEATURE_CUSTOMSENDER);
			final boolean sSendLater = prefsSubConnectorSpec
					.hasFeatures(SubConnectorSpec.FEATURE_SENDLATER);
			if (sFlashsms || sCustomsender || sSendLater) {
				this.findViewById(R.id.extras).setVisibility(View.VISIBLE);
			} else {
				this.findViewById(R.id.extras).setVisibility(View.GONE);
			}
			if (this.showExtras && sFlashsms) {
				this.findViewById(R.id.flashsms).setVisibility(View.VISIBLE);
			} else {
				this.findViewById(R.id.flashsms).setVisibility(View.GONE);
			}
			if (this.showExtras && sCustomsender) {
				this.findViewById(R.id.custom_sender).setVisibility(
						View.VISIBLE);
			} else {
				this.findViewById(R.id.custom_sender).setVisibility(View.GONE);
			}
			if (this.showExtras && sSendLater) {
				this.findViewById(R.id.send_later).setVisibility(View.VISIBLE);
			} else {
				this.findViewById(R.id.send_later).setVisibility(View.GONE);
			}

			String t = this.getString(R.string.app_name) + " - "
					+ prefsConnectorSpec.getName();
			if (prefsSubConnectorSpec != null
					&& prefsConnectorSpec.getSubConnectorCount() > 1) {
				t += " - " + prefsSubConnectorSpec.getName();
			}
			this.setTitle(t);
		}
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
		SharedPreferences.Editor editor = PreferenceManager
				.getDefaultSharedPreferences(this).edit();
		editor.putString(PREFS_TO, "");
		editor.putString(PREFS_TEXT, "");
		// commit changes
		editor.commit();
	}

	/** Save prefs. */
	final void savePreferences() {
		if (prefsConnectorSpec != null) {
			PreferenceManager.getDefaultSharedPreferences(this).edit()
					.putString(PREFS_CONNECTOR_ID, prefsConnectorSpec.getID())
					.commit();
		}
	}

	/**
	 * Run Connector.update().
	 * 
	 * @param forceUpdate
	 *            force update, if false only blank balances will get updated
	 */
	private void updateFreecount(final boolean forceUpdate) {
		final SharedPreferences p = PreferenceManager
				.getDefaultSharedPreferences(this);
		final String defPrefix = p.getString(PREFS_DEFPREFIX, "+49");
		final String defSender = p.getString(PREFS_SENDER, "");
		final ConnectorSpec[] css = getConnectors(
				ConnectorSpec.CAPABILITIES_UPDATE, // .
				ConnectorSpec.STATUS_ENABLED);
		for (ConnectorSpec cs : css) {
			if (!forceUpdate && cs.getBalance() != null) {
				// skip unnecessary updates
				continue;
			}
			final Intent intent = new Intent(cs.getPackage()
					+ Connector.ACTION_RUN_UPDATE);
			ConnectorCommand.update(defPrefix, defSender).setToIntent(intent);
			Log.d(TAG, "send broadcast: " + intent.getAction());
			this.sendBroadcast(intent);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public final void onClick(final View v) {
		switch (v.getId()) {
		case R.id.freecount:
			this.updateFreecount(true);
			return;
		case R.id.send_:
			this.send(prefsConnectorSpec, WebSMS.getSelectedSubConnectorID());
			return;
		case R.id.cancel:
			this.reset();
			return;
		case R.id.change_connector:
			this.changeConnectorMenu();
			return;
		case R.id.extras:
			this.showExtras = !this.showExtras;
			this.setButtons();
			return;
		case R.id.custom_sender:
			final CheckBox cs = (CheckBox) this
					.findViewById(R.id.custom_sender);
			if (cs.isChecked()) {
				this.showDialog(DIALOG_CUSTOMSENDER);
			} else {
				lastCustomSender = null;
			}
			return;
		case R.id.send_later:
			final CheckBox sl = (CheckBox) this.findViewById(R.id.send_later);
			if (sl.isChecked()) {
				this.showDialog(DIALOG_SENDLATER_DATE);
			} else {
				lastSendLater = -1;
			}
			return;
		default:
			return;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final boolean onCreateOptionsMenu(final Menu menu) {
		MenuInflater inflater = this.getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		if (prefsNoAds) {
			menu.removeItem(R.id.item_donate);
		}
		return true;
	}

	/**
	 * Display "change connector" menu.
	 */
	private void changeConnectorMenu() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.change_connector_);
		final ArrayList<String> items = new ArrayList<String>();
		final ConnectorSpec[] css = getConnectors(
				ConnectorSpec.CAPABILITIES_SEND, ConnectorSpec.STATUS_ENABLED);
		for (ConnectorSpec cs : css) {
			final SubConnectorSpec[] scs = cs.getSubConnectors();
			if (scs.length <= 1) {
				items.add(cs.getName());
			} else {
				final String n = cs.getName() + " - ";
				for (SubConnectorSpec sc : scs) {
					items.add(n + sc.getName());
				}
			}
		}

		builder.setItems(items.toArray(new String[0]),
				new DialogInterface.OnClickListener() {
					public void onClick(final DialogInterface d, // .
							final int item) {
						final SubConnectorSpec[] ret = ConnectorSpec
								.getSubConnectorReturnArray();
						prefsConnectorSpec = getConnectorByName(
								items.get(item), ret);
						prefsSubConnectorSpec = ret[0];
						WebSMS.this.setButtons();
						// save user preferences
						final Editor e = PreferenceManager
								.getDefaultSharedPreferences(WebSMS.this)
								.edit();
						e.putString(PREFS_CONNECTOR_ID, prefsConnectorSpec
								.getID());
						e.putString(PREFS_SUBCONNECTOR_ID,
								prefsSubConnectorSpec.getID());
						e.commit();
					}
				});
		builder.create().show();
	}

	/**
	 *{@inheritDoc}
	 */
	@Override
	public final boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
		case R.id.item_about: // start about dialog
			this.showDialog(DIALOG_ABOUT);
			return true;
		case R.id.item_settings: // start settings activity
			this.startActivity(new Intent(this, Preferences.class));
			return true;
		case R.id.item_donate:
			this.showDialog(DIALOG_PREDONATE);
			return true;
		case R.id.item_more:
			try {
				this.startActivity(new Intent(Intent.ACTION_VIEW, Uri
						.parse("market://search?q=pub:\"Felix Bechstein\"")));
			} catch (ActivityNotFoundException e) {
				Log.e(TAG, "no market", e);
			}
			return true;
		case R.id.item_connector:
			this.changeConnectorMenu();
			return true;
		default:
			return false;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final Dialog onCreateDialog(final int id) {
		Dialog d;
		AlertDialog.Builder builder;
		switch (id) {
		case DIALOG_PREDONATE:
			builder = new AlertDialog.Builder(this);
			builder.setIcon(R.drawable.ic_menu_star);
			builder.setTitle(R.string.donate_);
			builder.setMessage(R.string.predonate);
			builder.setPositiveButton(R.string.donate_,
					new DialogInterface.OnClickListener() {
						public void onClick(final DialogInterface dialog,
								final int which) {
							try {
								WebSMS.this.startActivity(new Intent(
										Intent.ACTION_VIEW, Uri.parse(// .
												WebSMS.this.getString(// .
														R.string.donate_url))));
							} catch (ActivityNotFoundException e) {
								Log.e(TAG, "no browser", e);
							} finally {
								WebSMS.this.showDialog(DIALOG_POSTDONATE);
							}
						}
					});
			builder.setNegativeButton(android.R.string.cancel, null);
			return builder.create();
		case DIALOG_POSTDONATE:
			builder = new AlertDialog.Builder(this);
			builder.setIcon(R.drawable.ic_menu_star);
			builder.setTitle(R.string.remove_ads_);
			builder.setMessage(R.string.postdonate);
			builder.setPositiveButton(R.string.send_,
					new DialogInterface.OnClickListener() {
						public void onClick(final DialogInterface dialog,
								final int which) {
							final Intent in = new Intent(Intent.ACTION_SEND);
							in.putExtra(Intent.EXTRA_EMAIL, new String[] {
									WebSMS.this.getString(// .
											R.string.donate_mail), "" });
							// FIXME: "" is a k9 hack. This is fixed in market
							// on 26.01.10. wait some more time..
							in.putExtra(Intent.EXTRA_TEXT, WebSMS.this
									.getImeiHash());
							in.putExtra(Intent.EXTRA_SUBJECT, WebSMS.this
									.getString(// .
									R.string.app_name)
									+ " " + WebSMS.this.getString(// .
											R.string.donate_subject));
							in.setType("text/plain");
							WebSMS.this.startActivity(in);
						}
					});
			builder.setNegativeButton(android.R.string.cancel, null);
			return builder.create();
		case DIALOG_ABOUT:
			d = new Dialog(this);
			d.setContentView(R.layout.about);
			d.setTitle(this.getString(R.string.about_) + " v"
					+ this.getString(R.string.app_version));
			StringBuffer authors = new StringBuffer();
			final ConnectorSpec[] css = getConnectors(
					ConnectorSpec.CAPABILITIES_NONE,
					ConnectorSpec.STATUS_INACTIVE);
			for (ConnectorSpec cs : css) {
				final String a = cs.getAuthor();
				if (a != null && a.length() > 0) {
					authors.append(cs.getName());
					authors.append(":\t");
					authors.append(a);
					authors.append("\n");
				}
			}
			((TextView) d.findViewById(R.id.author_connectors)).setText(authors
					.toString().trim());
			return d;
		case DIALOG_UPDATE:
			builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.changelog_);
			final String[] changes = this.getResources().getStringArray(
					R.array.updates);
			final StringBuilder buf = new StringBuilder(changes[0]);
			for (int i = 1; i < changes.length; i++) {
				buf.append("\n\n");
				buf.append(changes[i]);
			}
			builder.setIcon(android.R.drawable.ic_menu_info_details);
			builder.setMessage(buf.toString());
			builder.setCancelable(true);
			builder.setPositiveButton(android.R.string.ok, null);
			return builder.create();
		case DIALOG_CUSTOMSENDER:
			builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.custom_sender);
			builder.setCancelable(true);
			final EditText et = new EditText(this);
			builder.setView(et);
			builder.setPositiveButton(android.R.string.ok,
					new DialogInterface.OnClickListener() {
						public void onClick(final DialogInterface dialog,
								final int id) {
							WebSMS.lastCustomSender = et.getText().toString();
						}
					});
			builder.setNegativeButton(android.R.string.cancel, null);
			return builder.create();
		case DIALOG_SENDLATER_DATE:
			Calendar c = Calendar.getInstance();
			return new DatePickerDialog(this, this, c.get(Calendar.YEAR), c
					.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
		case DIALOG_SENDLATER_TIME:
			c = Calendar.getInstance();
			return new MyTimePickerDialog(this, this, c
					.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true);
		default:
			return null;
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
	 * Log text.
	 * 
	 * @param text
	 *            text
	 */
	public final void log(final String text) {
		try {
			Toast.makeText(this.getApplicationContext(), text,
					Toast.LENGTH_LONG).show();
		} catch (RuntimeException e) {
			Log.e(TAG, null, e);
		}
	}

	/**
	 * Send text to {@link Connector}.
	 * 
	 * @param connector
	 *            which connector should be used.
	 * @param command
	 *            {@link ConnectorCommand} to push to connector
	 */
	private void send(final ConnectorSpec connector,
			final ConnectorCommand command) {
		try {
			final Intent intent = new Intent(connector.getPackage()
					+ Connector.ACTION_RUN_SEND);
			command.setToIntent(intent);
			prefsConnectorSpec.setToIntent(intent);
			connector.addStatus(ConnectorSpec.STATUS_SENDING);
			Log.d(TAG, "send broadcast: " + intent.getAction());
			this.sendBroadcast(intent);
		} catch (Exception e) {
			Log.e(TAG, null, e);
		} finally {
			this.reset();
			if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(
					PREFS_AUTOEXIT, false)) {
				try {
					Thread.sleep(SLEEP_BEFORE_EXIT);
				} catch (InterruptedException e) {
					Log.e(TAG, null, e);
				}
				this.finish();
			}
		}
	}

	/**
	 * Send text.
	 * 
	 * @param connector
	 *            which connector should be used.
	 * @param subconnector
	 *            selected {@link SubConnectorSpec} ID
	 */
	private void send(final ConnectorSpec connector, // .
			final String subconnector) {
		// fetch text/recipient
		final String to = ((EditText) this.findViewById(R.id.to)).getText()
				.toString();
		final String text = ((EditText) this.findViewById(R.id.text)).getText()
				.toString();
		if (to.length() == 0 || text.length() == 0) {
			return;
		}

		if (!prefsNoAds) {
			// do not display any ads for donators
			// display ads
			((AdView) WebSMS.this.findViewById(R.id.ad))
					.setVisibility(View.VISIBLE);
		}

		CheckBox v = (CheckBox) this.findViewById(R.id.flashsms);
		final boolean flashSMS = (v.getVisibility() == View.VISIBLE)
				&& v.isEnabled() && v.isChecked();
		final SharedPreferences p = PreferenceManager
				.getDefaultSharedPreferences(this);
		final String defPrefix = p.getString(PREFS_DEFPREFIX, "+49");
		final String defSender = p.getString(PREFS_SENDER, "");

		final ConnectorCommand command = ConnectorCommand.send(subconnector,
				defPrefix, defSender, to.split(","), text, flashSMS);
		command.setCustomSender(lastCustomSender);
		command.setSendLater(lastSendLater);
		this.send(connector, command);
	}

	/**
	 * @return ID of selected {@link SubConnectorSpec}
	 */
	private static String getSelectedSubConnectorID() {
		if (prefsSubConnectorSpec == null) {
			return null;
		}
		return prefsSubConnectorSpec.getID();
	}

	/**
	 * A Date was set.
	 * 
	 * @param view
	 *            DatePicker View
	 * @param year
	 *            year set
	 * @param monthOfYear
	 *            month set
	 * @param dayOfMonth
	 *            day set
	 */
	public final void onDateSet(final DatePicker view, final int year,
			final int monthOfYear, final int dayOfMonth) {
		final Calendar c = Calendar.getInstance();
		if (lastSendLater > 0) {
			c.setTimeInMillis(lastSendLater);
		}
		c.set(Calendar.YEAR, year);
		c.set(Calendar.MONTH, monthOfYear);
		c.set(Calendar.DAY_OF_MONTH, dayOfMonth);
		lastSendLater = c.getTimeInMillis();

		MyTimePickerDialog.setOnlyQuaters(prefsSubConnectorSpec
				.hasFeatures(SubConnectorSpec.FEATURE_SENDLATER_QUARTERS));
		this.showDialog(DIALOG_SENDLATER_TIME);
	}

	/**
	 * A Time was set.
	 * 
	 * @param view
	 *            TimePicker View
	 * @param hour
	 *            hour set
	 * @param minutes
	 *            minutes set
	 */
	public final void onTimeSet(final TimePicker view, final int hour,
			final int minutes) {
		if (prefsSubConnectorSpec
				.hasFeatures(SubConnectorSpec.FEATURE_SENDLATER_QUARTERS)
				&& minutes % 15 != 0) {
			Toast.makeText(this, R.string.log_error_o2_sendlater,
					Toast.LENGTH_LONG).show();
			return;
		}

		final Calendar c = Calendar.getInstance();
		if (lastSendLater > 0) {
			c.setTimeInMillis(lastSendLater);
		}
		c.set(Calendar.HOUR_OF_DAY, hour);
		c.set(Calendar.MINUTE, minutes);
		lastSendLater = c.getTimeInMillis();
	}

	/**
	 * Get MD5 hash of the IMEI (device id).
	 * 
	 * @return MD5 hash of IMEI
	 */
	private String getImeiHash() {
		if (imeiHash == null) {
			// get imei
			TelephonyManager mTelephonyMgr = (TelephonyManager) this
					.getSystemService(TELEPHONY_SERVICE);
			final String did = mTelephonyMgr.getDeviceId();
			if (did != null) {
				imeiHash = Utils.md5(did);
			}
		}
		return imeiHash;
	}

	/**
	 * Add or update a {@link ConnectorSpec}.
	 * 
	 * @param connector
	 *            connector
	 */
	public static final void addConnector(final ConnectorSpec connector) {
		synchronized (CONNECTORS) {
			if (connector == null || connector.getID() == null
					|| connector.getName() == null) {
				return;
			}
			ConnectorSpec c = getConnectorByID(connector.getID());
			if (c != null) {
				c.update(connector);
			} else {
				final int l = CONNECTORS.size();
				final String name = connector.getName();
				Log.d(TAG, "add connector with id: " + connector.getID());
				Log.d(TAG, "add connector with name: " + name);
				boolean added = false;
				try {
					for (int i = 0; i < l; i++) {
						final ConnectorSpec cs = CONNECTORS.get(i);
						if (name.compareToIgnoreCase(cs.getName()) < 0) {
							CONNECTORS.add(i, connector);
							added = true;
							break;
						}
					}
				} catch (NullPointerException e) {
					Log.e(TAG, "error while sorting", e);
				}
				if (!added) {
					CONNECTORS.add(connector);
				}
				c = connector;

				if (prefsConnectorSpec == null
						&& prefsConnectorID.equals(connector.getID())) {
					prefsConnectorSpec = connector;
					final SharedPreferences p = PreferenceManager
							.getDefaultSharedPreferences(me);
					prefsSubConnectorSpec = connector.getSubConnector(p
							.getString(PREFS_SUBCONNECTOR_ID, ""));
					me.setButtons();
				}
			}
			final String b = c.getBalance();
			final String ob = c.getOldBalance();
			if (b != null && (ob == null || !b.equals(ob))) {
				me.updateBalance();
			}
		}
	}

	/**
	 * Get {@link ConnectorSpec} by ID.
	 * 
	 * @param id
	 *            ID
	 * @return {@link ConnectorSpec}
	 */
	private static ConnectorSpec getConnectorByID(final String id) {
		synchronized (CONNECTORS) {
			if (id == null) {
				return null;
			}
			final int l = CONNECTORS.size();
			for (int i = 0; i < l; i++) {
				final ConnectorSpec c = CONNECTORS.get(i);
				if (id.equals(c.getID())) {
					return c;
				}
			}
		}
		return null;
	}

	/**
	 * Get {@link ConnectorSpec} by name.
	 * 
	 * @param name
	 *            name
	 * @param returnSelectedSubConnector
	 *            if not null, array[0] will be set to selected
	 *            {@link SubConnectorSpec}
	 * @return {@link ConnectorSpec}
	 */
	private static ConnectorSpec getConnectorByName(final String name,
			final SubConnectorSpec[] returnSelectedSubConnector) {
		synchronized (CONNECTORS) {
			if (name == null) {
				return null;
			}
			final int l = CONNECTORS.size();
			for (int i = 0; i < l; i++) {
				final ConnectorSpec c = CONNECTORS.get(i);
				final String n = c.getName();
				if (name.startsWith(n)) {
					if (name.length() == n.length()) {
						if (returnSelectedSubConnector != null) {
							returnSelectedSubConnector[0] = c
									.getSubConnectors()[0];
						}
						return c;
					} else if (returnSelectedSubConnector != null) {

						final SubConnectorSpec[] scs = c.getSubConnectors();
						if (scs == null || scs.length == 0) {
							continue;
						}
						for (SubConnectorSpec sc : scs) {
							if (name.endsWith(sc.getName())) {
								returnSelectedSubConnector[0] = sc;
								return c;
							}
						}
					}
				}
			}
		}
		return null;
	}

	/**
	 * Get {@link ConnectorSpec}s by capabilities and/or status.
	 * 
	 * @param capabilities
	 *            capabilities needed
	 * @param status
	 *            status required {@link SubConnectorSpec}
	 * @return {@link ConnectorSpec}s
	 */
	static final ConnectorSpec[] getConnectors(final short capabilities,
			final short status) {
		synchronized (CONNECTORS) {
			final ArrayList<ConnectorSpec> ret = new ArrayList<ConnectorSpec>(
					CONNECTORS.size());
			final int l = CONNECTORS.size();
			for (int i = 0; i < l; i++) {
				final ConnectorSpec c = CONNECTORS.get(i);
				if (c.hasCapabilities(capabilities) && c.hasStatus(status)) {
					ret.add(c);
				}
			}
			return ret.toArray(new ConnectorSpec[0]);
		}
	}
}
