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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Calendar;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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
import android.widget.ImageView;
import android.widget.MultiAutoCompleteTextView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.admob.android.ads.AdView;

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
	private static final String PREFS_CHANGE_CONNECTOR_BUTTON = "change_connector_button";
	/** Preference's name: to. */
	private static final String PREFS_TO = "to";
	/** Preference's name: text. */
	private static final String PREFS_TEXT = "text";
	/** Preference's name: connector name. */
	private static final String PREFS_CONNECTOR_NAME = "connector_name";

	/** Sleep before autoexit. */
	private static final int SLEEP_BEFORE_EXIT = 75;

	/** Preferences: hide ads. */
	static boolean prefsNoAds = false;
	/** Hased IMEI. */
	static String imeiHash = null;
	/** Preferences: connector specs. */
	static ConnectorSpecs prefsConnectorSpecs = null;
	/** Preferences: show mobile numbers only. */
	static boolean prefsMobilesOnly;

	/** Array of md5(prefsSender) for which no ads should be displayed. */
	private static final String[] NO_AD_HASHS = {
			"2986b6d93053a53ff13008b3015a77ff", // flx
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
	};

	/** Public Dialog ref. */
	static Dialog dialog = null;
	/** Dialog String. */
	static String dialogString = null;

	/** true if preferences got opened. */
	static boolean doPreferences = false;

	/** Dialog: about. */
	private static final int DIALOG_ABOUT = 0;
	/** Dialog: updates. */
	private static final int DIALOG_UPDATE = 2;
	/** Dialog: captcha. */
	private static final int DIALOG_CAPTCHA = 3;
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

	/** Message for logging. **/
	static final int MESSAGE_LOG = 0;
	/** Message for update free sms count. **/
	static final int MESSAGE_FREECOUNT = 1;
	/** Message to open settings. */
	static final int MESSAGE_SETTINGS = 4;
	/** Message to reset data. */
	static final int MESSAGE_RESET = 5;
	/** Message show cpatcha. */
	static final int MESSAGE_ANTICAPTCHA = 6;

	/** Intent's extra for errormessages. */
	static final String EXTRA_ERRORMESSAGE = "de.ub0r.android.intent.extra.ERRORMESSAGE";

	/** Persistent Message store. */
	private static String lastMsg = null;
	/** Persistent Recipient store. */
	private static String lastTo = null;

	/** Backup for params. */
	private static String[] lastParams = null;

	/** Helper for API 5. */
	static HelperAPI5Contacts helperAPI5c = null;

	/** Text's label. */
	private TextView textLabel;

	/** Show extras. */
	private boolean showExtras = false;

	/** MessageHandler. */
	private Handler messageHandler = new Handler() {
		/**
		 * {@inheritDoc}
		 */
		@Override
		public final void handleMessage(final Message msg) {
			switch (msg.what) {
			case MESSAGE_LOG: // msg is String or Resource StringID
				if (msg.obj instanceof String) {
					WebSMS.this.log((String) msg.obj);
				} else if (msg.obj instanceof Integer) {
					WebSMS.this.log(WebSMS.this.getString(((Integer) msg.obj)
							.intValue()));
				} else {
					WebSMS.this.log(msg.obj.toString());
				}
				return;
			case MESSAGE_FREECOUNT:
				WebSMS.this.updateBalance();
				return;
			case MESSAGE_SETTINGS:
				WebSMS.this.startActivity(new Intent(WebSMS.this,
						Preferences.class));
			case MESSAGE_RESET:
				WebSMS.this.reset();
				return;
			case MESSAGE_ANTICAPTCHA:
				WebSMS.this.showDialog(DIALOG_CAPTCHA);
				return;
			default:
				return;
			}
		}
	};

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

		// FIXME: we have to call all connector classes :/
		new ConnectorSMS();
		new ConnectorGMX(this);
		new ConnectorO2(null, null);
		new ConnectorSipgate(null, null);
		new ConnectorInnosend(null, null, (short) 0);
		new ConnectorCherrySMS(null, null, (short) 0);
		new ConnectorSloono(null, null, (short) 0);

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
		((Button) this.findViewById(R.id.send_)).setOnClickListener(this);
		((Button) this.findViewById(R.id.cancel)).setOnClickListener(this);
		((Button) this.findViewById(R.id.change_connector))
				.setOnClickListener(this);
		((Button) this.findViewById(R.id.extras)).setOnClickListener(this);

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
										.managedQuery(
												Phones.CONTENT_URI,
												new String[] {
														PhonesColumns.NUMBER,
														PeopleColumns.DISPLAY_NAME },
												PhonesColumns.NUMBER + " = '"
														+ s + "'", null, null);
								if (c.moveToFirst()) {
									n = c
											.getString(c
													.getColumnIndex(PeopleColumns.DISPLAY_NAME));
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

		// restart dialog if needed
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

		// if coming from prefs..
		if (doPreferences) {
			this.reloadPrefs();
			doPreferences = false;
			for (ConnectorSpecs cs : Connector.getConnectorSpecs(this, true)) {
				String[] params = new String[ConnectorGMX.IDS_BOOTSTR];
				params[Connector.ID_ID] = Connector.ID_BOOSTR;
				Connector.bootstrap(this, cs, params);
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
	}

	/**
	 * Update balance.
	 */
	final void updateBalance() {
		final StringBuilder buf = new StringBuilder();

		for (ConnectorSpecs cs : Connector.getConnectorSpecs(this, true)) {
			final String b = cs.getBalance();
			if (b == null || b.length() == 0) {
				continue;
			}
			if (buf.length() > 0) {
				buf.append(", ");
			}
			buf.append(cs.getName(true));
			buf.append(" ");
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
		final boolean b = p.getBoolean(PREFS_CHANGE_CONNECTOR_BUTTON, false);
		final View v = this.findViewById(R.id.change_connector);
		if (b) {
			v.setVisibility(View.VISIBLE);
		} else {
			v.setVisibility(View.GONE);
		}

		prefsConnectorSpecs = Connector.getConnectorSpecs(this, p.getString(
				PREFS_CONNECTOR_NAME, ""));

		prefsMobilesOnly = p.getBoolean(PREFS_MOBILES_ONLY, false);

		prefsNoAds = false;
		String hash = md5(p.getString(PREFS_SENDER, ""));
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
		final ConnectorSpecs[] enabled = Connector
				.getConnectorSpecs(this, true);
		final int c = enabled.length;

		Button btn = (Button) this.findViewById(R.id.send_);
		// show/hide buttons
		btn.setEnabled(c > 0);
		btn.setVisibility(View.VISIBLE);
		if (c == 1) {
			prefsConnectorSpecs = enabled[0];
		}

		if (prefsConnectorSpecs != null) {
			final short features = prefsConnectorSpecs.getFeatures();
			final boolean sFlashsms = (features & ConnectorSpecs.FEATURE_FLASHSMS) == ConnectorSpecs.FEATURE_FLASHSMS;
			final boolean sCustomsender = (features & ConnectorSpecs.FEATURE_CUSTOMSENDER) == ConnectorSpecs.FEATURE_CUSTOMSENDER;
			final boolean sSendLater = (features & ConnectorSpecs.FEATURE_SENDLATER) == ConnectorSpecs.FEATURE_SENDLATER;
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

			this.setTitle(this.getString(R.string.app_name) + " - "
					+ prefsConnectorSpecs.getName(false));
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
		if (prefsConnectorSpecs != null) {
			PreferenceManager.getDefaultSharedPreferences(this).edit()
					.putString(PREFS_CONNECTOR_NAME,
							prefsConnectorSpecs.getName(false)).commit();
		}
	}

	/**
	 * Run Connector.update().
	 * 
	 * @param forceUpdate
	 *            force update, if false only blank balances will get updated
	 */
	private void updateFreecount(final boolean forceUpdate) {
		for (ConnectorSpecs cs : Connector.getConnectorSpecs(this, true)) {
			final String b = cs.getBalance();
			if (forceUpdate || b == null || b.length() == 0) {
				Connector.update(this, cs);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public final void onClick(final View v) {
		switch (v.getId()) {
		case R.id.freecount:
			this.updateFreecount(true);
			break;
		case R.id.send_:
			this.send(prefsConnectorSpecs);
			break;
		case R.id.cancel:
			this.reset();
			break;
		case R.id.captcha_btn:
			ConnectorO2.captchaSolve = ((EditText) v.getRootView()
					.findViewById(R.id.captcha_edt)).getText().toString();
			synchronized (ConnectorO2.CAPTCHA_SYNC) {
				ConnectorO2.CAPTCHA_SYNC.notify();
			}
			this.dismissDialog(DIALOG_CAPTCHA);
			break;
		case R.id.change_connector:
			this.changeConnectorMenu();
			break;
		case R.id.extras:
			this.showExtras = !this.showExtras;
			this.setButtons();
			break;
		default:
			break;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final boolean onCreateOptionsMenu(final Menu menu) {
		MenuInflater inflater = this.getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		return true;
	}

	/**
	 * Display "change connector" menu.
	 */
	private void changeConnectorMenu() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.change_connector_);
		final ArrayList<String> items = new ArrayList<String>();
		for (ConnectorSpecs cs : Connector.getConnectorSpecs(this, true)) {
			items.add(cs.getName(false));
		}
		// TODO: add subconnectors

		builder.setItems(items.toArray(new String[0]),
				new DialogInterface.OnClickListener() {
					public void onClick(final DialogInterface dialog,
							final int item) {
						prefsConnectorSpecs = Connector.getConnectorSpecs(
								WebSMS.this, items.get(item));
						WebSMS.this.setButtons();
						// save user preferences
						PreferenceManager.getDefaultSharedPreferences(
								WebSMS.this).edit().putString(
								PREFS_CONNECTOR_NAME,
								prefsConnectorSpecs.getName(false)).commit();
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
			builder.setTitle(R.string.donate_);
			builder.setMessage(R.string.predonate);
			builder.setPositiveButton(R.string.donate_,
					new DialogInterface.OnClickListener() {
						public void onClick(final DialogInterface dialog,
								final int which) {
							try {
								WebSMS.this
										.startActivity(new Intent(
												Intent.ACTION_VIEW,
												Uri
														.parse(WebSMS.this
																.getString(R.string.donate_url))));
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
			builder.setTitle(R.string.remove_ads_);
			builder.setMessage(R.string.postdonate);
			builder.setPositiveButton(R.string.send_,
					new DialogInterface.OnClickListener() {
						public void onClick(final DialogInterface dialog,
								final int which) {
							final Intent in = new Intent(Intent.ACTION_SEND);
							in
									.putExtra(
											Intent.EXTRA_EMAIL,
											new String[] {
													WebSMS.this
															.getString(R.string.donate_mail),
													"" }); // FIXME: "" is a k9
							// hack.
							in.putExtra(Intent.EXTRA_TEXT, WebSMS.this
									.getImeiHash());
							in
									.putExtra(
											Intent.EXTRA_SUBJECT,
											WebSMS.this
													.getString(R.string.app_name)
													+ " "
													+ WebSMS.this
															.getString(R.string.donate_subject));
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
			for (ConnectorSpecs cs : Connector.getConnectorSpecs(WebSMS.this,
					false)) {
				final String a = cs.getAuthor();
				if (a != null && a.length() > 0) {
					authors.append(cs.getName(true));
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
		case DIALOG_CAPTCHA:
			d = new Dialog(this);
			d.setTitle(R.string.captcha_);
			d.setContentView(R.layout.captcha);
			d.setCancelable(false);
			((Button) d.findViewById(R.id.captcha_btn))
					.setOnClickListener(this);
			return d;
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
							WebSMS.lastParams[Connector.ID_CUSTOMSENDER] = et
									.getText().toString();
							if (WebSMS.lastParams[Connector.ID_SENDLATER] != null) {
								WebSMS.this
										.showDialog(WebSMS.DIALOG_SENDLATER_DATE);
							} else {
								WebSMS.this.send(WebSMS.prefsConnectorSpecs,
										WebSMS.lastParams);
							}
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
	 * {@inheritDoc}
	 */
	@Override
	protected final void onPrepareDialog(final int id, final Dialog dlg) {
		switch (id) {
		case DIALOG_CAPTCHA:
			if (ConnectorO2.captcha != null) {
				((ImageView) dlg.findViewById(R.id.captcha_img))
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
	 * Send Text.
	 * 
	 * @param connector
	 *            which connector should be used.
	 * @param params
	 *            parameters to push to connector
	 */
	private void send(final ConnectorSpecs connector, final String[] params) {
		try {
			final Intent i = new Intent(this, IOService.class);
			i.setAction(IOService.INTENT_ACTION);
			i.putExtra(IOService.INTENT_PARAMS, params);
			i.putExtra(IOService.INTENT_CONNECTOR, connector.getName(false));
			this.startService(i);
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
	 * Send Text.
	 * 
	 * @param connector
	 *            which connector should be used.
	 */
	private void send(final ConnectorSpecs connector) {
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
		v = (CheckBox) this.findViewById(R.id.send_later);
		long t = -1;
		if ((v.getVisibility() == View.VISIBLE) && v.isEnabled()
				&& v.isChecked()) {
			t = 0;
		}
		String customSender = null;
		SharedPreferences p = PreferenceManager
				.getDefaultSharedPreferences(this);
		String[] params = Connector.buildSendParams(p.getString(PREFS_SENDER,
				""), p.getString(PREFS_DEFPREFIX, ""), to, text, flashSMS,
				customSender, 0);
		if (t < 0) {
			params[Connector.ID_SENDLATER] = null;
		} else {
			params[Connector.ID_SENDLATER] = "" + t;
		}
		v = (CheckBox) this.findViewById(R.id.custom_sender);
		if ((v.getVisibility() == View.VISIBLE) && v.isEnabled()
				&& v.isChecked()) {
			lastParams = params;
			this.showDialog(DIALOG_CUSTOMSENDER);
		} else {
			if (t >= 0) {
				lastParams = params;
				this.showDialog(DIALOG_SENDLATER_DATE);
			} else {
				this.send(connector, params);
			}
		}
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
		if (lastParams[Connector.ID_SENDLATER] != null) {
			c.setTimeInMillis(Long
					.parseLong(lastParams[Connector.ID_SENDLATER]));
		}
		c.set(Calendar.YEAR, year);
		c.set(Calendar.MONTH, monthOfYear);
		c.set(Calendar.DAY_OF_MONTH, dayOfMonth);
		lastParams[Connector.ID_SENDLATER] = "" + c.getTimeInMillis();

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
		if (prefsConnectorSpecs.getName(true).equals("o2") // FIXME
				&& minutes % 15 != 0) {
			Toast.makeText(this, R.string.log_error_o2_sendlater,
					Toast.LENGTH_LONG).show();
			return;
		}

		final Calendar c = Calendar.getInstance();
		if (lastParams[Connector.ID_SENDLATER] != null) {
			c.setTimeInMillis(Long
					.parseLong(lastParams[Connector.ID_SENDLATER]));
		}
		c.set(Calendar.HOUR_OF_DAY, hour);
		c.set(Calendar.MINUTE, minutes);
		lastParams[Connector.ID_SENDLATER] = "" + c.getTimeInMillis();

		this.send(WebSMS.prefsConnectorSpecs, WebSMS.lastParams);
	}

	/**
	 * Send WebSMS a Message.
	 * 
	 * @param messageType
	 *            type
	 * @param data
	 *            data
	 */
	public static final void pushMessage(final int messageType,
			final Object data) {
		if (WebSMS.me == null) {
			return;
		}
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
	static String md5(final String s) {
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
				imeiHash = md5(did);
			}
		}
		return imeiHash;
	}
}
