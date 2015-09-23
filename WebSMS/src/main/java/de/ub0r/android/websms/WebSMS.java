/*
 * Copyright (C) 2010-2012 Felix Bechstein, Lado Kumsiashvili
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

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.Dialog;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.database.SQLException;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.text.ClipboardManager;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.util.Base64;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.MultiAutoCompleteTextView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.ub0r.android.lib.DonationHelper;
import de.ub0r.android.lib.apis.ContactsWrapper;
import de.ub0r.android.websms.connector.common.Connector;
import de.ub0r.android.websms.connector.common.ConnectorCommand;
import de.ub0r.android.websms.connector.common.ConnectorSpec;
import de.ub0r.android.websms.connector.common.ConnectorSpec.SubConnectorSpec;
import de.ub0r.android.websms.connector.common.Log;
import de.ub0r.android.websms.connector.common.SMSLengthCalculator;
import de.ub0r.android.websms.connector.common.Utils;
import de.ub0r.android.websms.connector.common.WebSMSException;
import de.ub0r.android.websms.rules.PseudoConnectorRules;

/**
 * Main Activity.
 * 
 * @author flx
 */
public class WebSMS extends AppCompatActivity implements OnClickListener,
		OnDateSetListener, OnTimeSetListener, OnLongClickListener {
	/** Tag for output. */
	public static final String TAG = "main";

	private static final String LAST_RUN = "last_run";

	/** Default SMS length calculator. */
	private static final SMSLengthCalculator SMS_LENGTH_CALCULATOR = new DefaultSMSLengthCalculator();

	public static final String DONATION_URL
			= "https://play.google.com/store/apps/details?id=de.ub0r.android.donator";

	private static final String INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-1948477123608376/2064558085";
	private static final int INTERSTITIAL_ADS_RATION = 7;

	/** Static reference to running Activity. */
	private static WebSMS me;
	/** Preference's name: user's phone number. */
	static final String PREFS_SENDER = "sender";
	/** Preference's name: default prefix. */
	static final String PREFS_DEFPREFIX = "defprefix";
	/** Preference's name: update balance on start. */
	static final String PREFS_AUTOUPDATE = "autoupdate";
	/** Preference's name: exit after sending. */
	private static final String PREFS_AUTOEXIT = "autoexit";
	/** Preference's name: show mobile numbers only. */
	private static final String PREFS_MOBILES_ONLY = "mobiles_only";
	/** Preference's name: enable autosend. */
	private static final String PREFS_AUTOSEND = "enable_autosend";
	/** Preference's name: use current connector for autosend. */
	private static final String PREFS_USE_CURRENT_CON = "use_current_connector";
	/** Preference's name: vibrate on sending. */
	static final String PREFS_SEND_VIBRATE = "send_vibrate";
	/** Preference's name: vibrate on failed sending. */
	static final String PREFS_FAIL_VIBRATE = "fail_vibrate";
	/** Preference's name: sound on failed sending. */
	static final String PREFS_FAIL_SOUND = "fail_sound";
	/** Preferemce's name: hide select recipients button. */
	private static final String PREFS_HIDE_SELECT_RECIPIENTS_BUTTON = "hide_select_recipients_button";
	/** Preferemce's name: hide clear recipients button. */
	private static final String PREFS_HIDE_CLEAR_RECIPIENTS_BUTTON = "hide_clear_recipients_button";
	/** Preference's name: hide emoticons button. */
	private static final String PREFS_HIDE_EMO_BUTTON = "hide_emo_button";
	/** Preference's name: hide cancel button. */
	private static final String PREFS_HIDE_CANCEL_BUTTON = "hide_cancel_button";
	/** Preference's name: hide extras button. */
	private static final String PREFS_HIDE_EXTRAS_BUTTON = "hide_extras_button";
	/** Preference's name: hide bg connector. */
	private static final String PREFS_HIDE_BG_CONNECTOR = "hide_bg_connector";
	/** Prefernece's name: hide paste button. */
	private static final String PREFS_HIDE_PASTE = "hide_paste";
	/** Prefernece's name: show toast on balance update. */
	static final String PREFS_SHOW_BALANCE_TOAST = "show_balance_toast";
	/** Cache {@link ConnectorSpec}s. */
	private static final String PREFS_CONNECTORS = "connectors";
	/** Preference's name: try to send invalid characters. */
	private static final String PREFS_TRY_SEND_INVALID = "try_send_invalid";
	/** Preference's name: drop sent messages. */
	static final String PREFS_DROP_SENT = "drop_sent";
	/** Preference's name: backup of last sms. */
	private static final String PREFS_BACKUPLASTTEXT = "backup_last_sms";

	/** Preference's name: default recipient. */
	private static final String PREFS_DEFAULT_RECIPIENT = "default_recipient";
	/** Preference's name: signature. */
	private static final String PREFS_SIGNATURE = "signature";
	/** Preference's name: max resend count. */
	static final String PREFS_MAX_RESEND_COUNT = "max_resend_count";
	/** Preference's name: internal id of the last message. */
	static final String PREFS_LAST_MSG_ID = "last_msg_id";

	/** Preference's name: last time help intro was shown. */
	private static final String PREFS_LASTHELP = "last_help";
	/** Preference's name: selected {@link ConnectorSpec} ID. */
	static final String PREFS_CONNECTOR_ID = "connector_id";
	/** Preference's name: selected {@link SubConnectorSpec} ID. */
	static final String PREFS_SUBCONNECTOR_ID = "subconnector_id";
	/** Preference's name: standard connector. */
	static final String PREFS_STANDARD_CONNECTOR = "std_connector";
	/** Preference's name: standard sub connector. */
	static final String PREFS_STANDARD_SUBCONNECTOR = "std_subconnector";

	private static final String PREFS_ADS_COUNTER = "ads_counter";

	/** Preference's name: to. */
	private static final String EXTRA_TO = "to";
	/** Preference's name: text. */
	private static final String EXTRA_TEXT = "text";

	/** Sleep before autoexit. */
	private static final int SLEEP_BEFORE_EXIT = 75;

	/** Buffersize for saving and loading Connectors. */
	private static final int BUFSIZE = 4096;

	/** Minimum length for showing sms length. */
	private static final int TEXT_LABLE_MIN_LEN = 20;

	/** Preferences: hide ads. */
	private static boolean prefsNoAds = false;
	private static boolean prefsShowAds = false;
	private static boolean prefsInterstitialAd = false;
	/** Preferences: selected {@link ConnectorSpec}. */
	private static ConnectorSpec prefsConnectorSpec = null;
	/** Preferences: selected {@link SubConnectorSpec}. */
	private static SubConnectorSpec prefsSubConnectorSpec = null;

	/** List of available {@link ConnectorSpec}s. */
	private static final ArrayList<ConnectorSpec> CONNECTORS = new ArrayList<ConnectorSpec>();

    /** List of available pseudo-connector. */
    private static final List<ConnectorSpec> PSEUDO_CONNECTORS = new ArrayList<ConnectorSpec>();
    private static PseudoConnectorRules rules = new PseudoConnectorRules();

    /** true if preferences got opened. */
	static boolean doPreferences = false;

	/** Menu item: restore. */
	private static final int ITEM_RESTORE = 1;

	/** Dialog: custom sender. */
	private static final int DIALOG_CUSTOMSENDER = 3;
	/** Dialog: send later: date. */
	private static final int DIALOG_SENDLATER_DATE = 4;
	/** Dialog: send later: time. */
	private static final int DIALOG_SENDLATER_TIME = 5;
	/** Dialog: emo. */
	private static final int DIALOG_EMO = 6;

	/** {@link Activity} result request. */
	private static final int ARESULT_PICK_PHONE = 1;

	/** Size of the emoticons png. */
	private static final int EMOTICONS_SIZE = 50;
	/** Padding for the emoticons png. */
	private static final int EMOTICONS_PADDING = 5;

	/** Intent's extra for error messages. */
	static final String EXTRA_ERRORMESSAGE = "de.ub0r.android.intent.extra.ERRORMESSAGE";
	/** Intent's extra for sending message automatically. */
	static final String EXTRA_AUTOSEND = "AUTOSEND";

	/** Persistent Message store. */
	private String lastMsg = null;
	/** Persistent Recipient store. */
	private String lastTo = null;
	/** Backup for params: custom sender. */
	private static String lastCustomSender = null;
	/** Backup for params: send later. */
	private static long lastSendLater = -1;

	/** {@link MultiAutoCompleteTextView} holding recipients. */
	private MultiAutoCompleteTextView etTo;
	/** {@link EditText} holding text. */
	private EditText etText;
	/** {@link TextView} for pasting text. */
	private TextView tvPaste;
	/** {@link TextView} for deleting text. */
	private TextView tvClear;

	/** {@link View} holding custom sender. */
	private ToggleButton vCustomSender;
	/** {@link View} holding flashsms. */
	private ToggleButton vFlashSMS;
	/** {@link View} holding send later. */
	private ToggleButton vSendLater;

	/** {@link ClipboardManager}. */
	private ClipboardManager cbmgr;

	/** Text's label. */
	private TextView etTextLabel;

	/** Show cancel button. */
	private static boolean prefsShowCancel = true;

	/** An estimate of the number of connectors that are remaining to be added. */
	private static int newConnectorsExpected = 0;

	private Handler threadHandler;

	/** TextWatcher en-/disable send/cancel buttons. */
	private TextWatcher twButtons = new TextWatcher() {
		/**
		 * {@inheritDoc}
		 */
		public void afterTextChanged(final Editable s) {
			final boolean b1 = WebSMS.this.etTo.getText().length() > 0;
			final boolean b2 = WebSMS.this.etText.getText().length() > 0;
			WebSMS.this.findViewById(R.id.clear).setEnabled(b1);
			int v = View.GONE;
			if (prefsShowCancel && (b1 || b2)) {
				v = View.VISIBLE;
			}
			WebSMS.this.tvClear.setVisibility(v);
			WebSMS.this.invalidateOptionsMenu();
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

	/** TextWatcher updating char count on writing. */
	private TextWatcher twCount = new TextWatcher() {
		/**
		 * {@inheritDoc}
		 */
		@SuppressWarnings("deprecation")
		public void afterTextChanged(final Editable s) {
			int len = s.length();
			if (len == 0) {
				WebSMS.this.etTextLabel.setVisibility(View.GONE);
				if (WebSMS.this.cbmgr.hasText()
						&& !PreferenceManager.getDefaultSharedPreferences(
								WebSMS.this)
								.getBoolean(PREFS_HIDE_PASTE, false)) {
					WebSMS.this.tvPaste.setVisibility(View.VISIBLE);
				} else {
					WebSMS.this.tvPaste.setVisibility(View.GONE);
				}
			} else {
				final String sig = PreferenceManager
						.getDefaultSharedPreferences(WebSMS.this).getString(
								PREFS_SIGNATURE, "");
				len += sig.length();
				WebSMS.this.tvPaste.setVisibility(View.GONE);
				if (len > TEXT_LABLE_MIN_LEN) {
					SMSLengthCalculator calc = null;
					if (prefsConnectorSpec != null) {
						calc = prefsConnectorSpec.getSMSLengthCalculator();
					}
					if (calc == null) {
						calc = SMS_LENGTH_CALCULATOR;
					}
					int[] l = calc.calculateLength(s.toString() + sig, false);
					WebSMS.this.etTextLabel.setText(l[0] + "/" + l[2]);
					WebSMS.this.etTextLabel.setVisibility(View.VISIBLE);
				} else {
					WebSMS.this.etTextLabel.setVisibility(View.GONE);
				}

				// If we have a connector selected, check message length limit
				if (prefsConnectorSpec != null) {
					// Get the limit, will be -1 or 0 if it is not set
					int maxLength = prefsConnectorSpec.getLimitLength();
					if (maxLength > 0 && len > maxLength) {
						// Truncate to maxLength-sig.length() chars
						int actualMax = maxLength - sig.length();
						String newText = s.toString().substring(0, actualMax);
						Log.i(TAG,
								"Message text was too long, so truncating from "
										+ s.length() + " to "
										+ newText.length());
						s.replace(0, s.length(), newText);
						if (me != null) {
							String sigText = sig.length() > 0 ? me
									.getString(
											R.string.connector_message_length_reached_signature,
											sig.length())
									: "";
							String messageText = me.getString(
									R.string.connector_message_length_reached,
									maxLength, prefsConnectorSpec.getName(),
									sigText);
							Toast.makeText(me, messageText, Toast.LENGTH_SHORT)
									.show();
						}
					}
				}
			}
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

	/** Show extra button. */
	private static boolean bShowExtras = true;

    private AdView mAdView;

	private InterstitialAd mInterstitialAd;

    /**
	 * Parse data pushed by {@link Intent}.
	 * 
	 * @param intent
	 *            {@link Intent}
	 */
	private void parseIntent(final Intent intent) {
		final String action = intent.getAction();
		Log.d(TAG, "launched with action: " + action);
		if (action == null) {
			return;
		}
		final Uri uri = intent.getData();
		Log.i(TAG, "launched with uri: " + uri);
		if (uri != null && uri.toString().length() > 0) {
			// launched by clicking a sms: link, target number is in URI.
			final String scheme = uri.getScheme();
			if (scheme != null) {
				if (scheme.equals("sms") || scheme.equals("smsto")) {
					final String s = uri.getSchemeSpecificPart();
					this.parseSchemeSpecificPart(s);
				} else if (scheme.equals("content")) {
					this.parseThreadId(uri.getLastPathSegment());
				}
			}
		}
		// check for extras
		String s = intent.getStringExtra("address");
		if (!TextUtils.isEmpty(s)) {
			Log.d(TAG, "got address: " + s);
			this.lastTo = s;
		}
		s = intent.getStringExtra(Intent.EXTRA_TEXT);
		if (s == null) {
			Log.d(TAG, "got sms_body: " + s);
			s = intent.getStringExtra("sms_body");
		}
		if (s == null) {
			final Uri stream = (Uri) intent
					.getParcelableExtra(Intent.EXTRA_STREAM);
			if (stream != null) {
				Log.d(TAG, "got stream: " + stream);
				try {
					InputStream is = this.getContentResolver().openInputStream(
							stream);
					final BufferedReader r = new BufferedReader(
							new InputStreamReader(is));
					StringBuffer sb = new StringBuffer();
					String line;
					while ((line = r.readLine()) != null) {
						sb.append(line + "\n");
					}
					s = sb.toString().trim();
				} catch (IOException e) {
					Log.e(TAG, "IO ERROR", e);
				}

			}
		}
		if (s != null) {
			Log.d(TAG, "set text: " + s);
			((EditText) this.findViewById(R.id.text)).setText(s);
			this.lastMsg = s;
		}
		s = intent.getStringExtra(EXTRA_ERRORMESSAGE);
		if (s != null) {
			Log.e(TAG, "show error: " + s);
			Toast.makeText(this, s, Toast.LENGTH_LONG).show();
		}
		final SharedPreferences p = PreferenceManager
				.getDefaultSharedPreferences(this);
		if (p.getBoolean(PREFS_AUTOSEND, true)) {
			s = intent.getStringExtra(WebSMS.EXTRA_AUTOSEND);
			Log.d(TAG, "try autosend..");
			Log.d(TAG, "s: " + s);
			Log.d(TAG, "lastMsg: " + this.lastMsg);
			Log.d(TAG, "lastTo: " + this.lastTo);

			if (s != null && !TextUtils.isEmpty(this.lastMsg)
					&& !TextUtils.isEmpty(this.lastTo)) {
				// all data is here
				Log.d(TAG, "do autosend");
				if (p.getBoolean(PREFS_USE_CURRENT_CON, true)) {
					// push it to current active connector
					Log.d(TAG, "use current connector");
					if (prefsConnectorSpec != null && prefsSubConnectorSpec != null) {
						Log.d(TAG, "autosend: call send()");
						if (this.send(prefsConnectorSpec, prefsSubConnectorSpec)
								&& !this.isFinishing()) {
							Log.d(TAG, "sent successfully");
							this.finish();
						}
					}
				} else {
					// show connector chooser
					Log.d(TAG, "show connector chooser");
					final AlertDialog.Builder b = new AlertDialog.Builder(this);
					b.setTitle(R.string.change_connector_);
                    final ConnectorLabel[] items = this.getConnectorMenuItems(true /*isIncludePseudoConnectors*/);
					Log.d(TAG, "show #items: " + items.length);
                    if (items.length > 0) {
                        b.setItems(items, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(final DialogInterface dialog, final int which) {
                                final ConnectorLabel sel = items[which];
                                // save old selected connector
                                final ConnectorSpec pr0 = prefsConnectorSpec;
                                final SubConnectorSpec pr1 = prefsSubConnectorSpec;
                                // switch to selected
                                WebSMS.this.saveSelectedConnector(sel.getConnector(), sel.getSubConnector());
                                // send message
                                boolean sent = false;
                                Log.d(TAG, "autosend: call send()");
                                if (prefsConnectorSpec != null && prefsSubConnectorSpec != null) {
                                    sent = WebSMS.this.send(prefsConnectorSpec, prefsSubConnectorSpec);
                                }
                                // restore old connector
                                WebSMS.this.saveSelectedConnector(pr0, pr1);
                                // quit
                                if (sent && !WebSMS.this.isFinishing()) {
                                    Log.d(TAG, "sent successfully");
                                    WebSMS.this.finish();
                                }
                            }
                        });
                        b.setNegativeButton(android.R.string.cancel, null);
                        b.show();
                    }
				}
			}
		}
	}

	/**
	 * parseSchemeSpecificPart from {@link Uri} and initialize WebSMS
	 * properties.
	 * 
	 * @param part
	 *            scheme specific part
	 */
	private void parseSchemeSpecificPart(final String part) {
		Log.d(TAG, "parseSchemeSpecificPart(" + part + ")");
		String s = part;
		if (s == null) {
			return;
		}
		s = s.trim();
		if (s.endsWith(",")) {
			s = s.substring(0, s.length() - 1).trim();
		}
		if (s.indexOf('<') < 0) {
			// try to fetch recipient's name from phone book
			String n = ContactsWrapper.getInstance().getNameForNumber(
					this.getContentResolver(), s);
			if (n != null) {
				s = n + " <" + s + ">, ";
			}
		}
		Log.d(TAG, "parseSchemeSpecificPart(" + part + "): " + s);
		((EditText) this.findViewById(R.id.to)).setText(s);
		this.lastTo = s;
	}

	/**
	 * Load data from Conversation.
	 * 
	 * @param threadId
	 *            ThreadId
	 */
	private void parseThreadId(final String threadId) {
		Log.d(TAG, "thradId: " + threadId);
		final Uri uri = Uri
				.parse("content://mms-sms/conversations/" + threadId);
		final String[] proj = new String[] { "thread_id", "address" };
		Cursor cursor = null;
		try {
			try {
				cursor = this.getContentResolver().query(uri, proj, null, null,
						null);
			} catch (SQLException e) {
				Log.e(TAG, null, e);
				proj[0] = "_id";
				proj[1] = "recipient_address";
				cursor = this.getContentResolver().query(uri, proj, null, null,
						null);
			}
			if (cursor != null && cursor.moveToFirst()) {
				String a;
				do {
					a = cursor.getString(1);
				} while (a == null && cursor.moveToNext());
				Log.d(TAG, "found address: " + a);
				this.parseSchemeSpecificPart(a);
			}
		} catch (IllegalStateException e) {
			Log.e(TAG, "error parsing ThreadId: " + threadId, e);
		}
		if (cursor != null && !cursor.isClosed()) {
			cursor.close();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings({ "unchecked", "deprecation" })
	@Override
	public final void onCreate(final Bundle savedInstanceState) {
		this.requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		this.setTheme(PreferencesActivity.getTheme(this));
		super.onCreate(savedInstanceState);
		Log.d(TAG, "onCreate(" + savedInstanceState + ")");
		this.threadHandler = new Handler();

		// Restore preferences
		de.ub0r.android.lib.Utils.setLocale(this);

		this.cbmgr = (ClipboardManager) this
				.getSystemService(CLIPBOARD_SERVICE);

		// save ref to me.
		me = this;
		final SharedPreferences p = PreferenceManager
				.getDefaultSharedPreferences(this);

		// inflate XML
		this.setContentView(R.layout.main);
		this.getSupportActionBar().setHomeButtonEnabled(true);

		// indeterminate progress bar is spinning by default so stop it,
		// updateProgressBar will start it again if necessary
		this.setSupportProgressBarIndeterminateVisibility(false);

		this.etTo = (MultiAutoCompleteTextView) this.findViewById(R.id.to);
		this.etText = (EditText) this.findViewById(R.id.text);
		this.etTextLabel = (TextView) this.findViewById(R.id.text_);
		this.tvPaste = (TextView) this.findViewById(R.id.text_paste);
		this.tvClear = (TextView) this.findViewById(R.id.text_clear);

		this.vCustomSender = (ToggleButton) this
				.findViewById(R.id.custom_sender);
		this.vFlashSMS = (ToggleButton) this.findViewById(R.id.flashsms);
		this.vSendLater = (ToggleButton) this.findViewById(R.id.send_later);

		if (isNewVersion()) {
            Log.i(TAG, "detected version update");
			SharedPreferences.Editor editor = p.edit();
			editor.remove(PREFS_CONNECTORS); // remove cache
			editor.apply();
            rules.upgrade();
		}

		// get cached Connectors
		String s = p.getString(PREFS_CONNECTORS, null);
		if (TextUtils.isEmpty(s)) {
			this.updateConnectors();

		} else if (CONNECTORS.size() == 0) {
			// skip static remaining connectors
			try {
				ArrayList<ConnectorSpec> cache;
				cache = (ArrayList<ConnectorSpec>) (new ObjectInputStream(
						new BufferedInputStream(new ByteArrayInputStream(
								Base64.decode(s, Base64.DEFAULT)), BUFSIZE))).readObject();
				CONNECTORS.addAll(cache);
				if (p.getBoolean(PREFS_AUTOUPDATE, true)) {
                    updateFreecount();
				}
			} catch (Exception e) {
				Log.d(TAG, "error loading connectors", e);
			}
		}
		Log.d(TAG, "loaded connectors: " + CONNECTORS.size());

        if (PSEUDO_CONNECTORS.size() == 0) {
            PSEUDO_CONNECTORS.add(rules.getSpec(this));
        }

        if (savedInstanceState == null) {
            this.revertPrefsToStdConnector();
            // note: do not revert to std connector on orientation change
        }

        this.reloadPrefs();

        if (savedInstanceState != null) {
            this.lastTo = savedInstanceState.getString(EXTRA_TO);
            this.lastMsg = savedInstanceState.getString(EXTRA_TEXT);
        }

		// register Listener
		this.vCustomSender.setOnClickListener(this);
		this.vSendLater.setOnClickListener(this);
		this.findViewById(R.id.select).setOnClickListener(this);
		View v = this.findViewById(R.id.clear);
		v.setOnClickListener(this);
		v.setOnLongClickListener(this);
		this.findViewById(R.id.emo).setOnClickListener(this);
		this.tvPaste.setOnClickListener(this);
		this.tvClear.setOnClickListener(this);
		this.etText.addTextChangedListener(this.twCount);
		this.etText.addTextChangedListener(this.twButtons);
		this.etTo.addTextChangedListener(this.twButtons);
		this.etTo.setAdapter(new MobilePhoneAdapter(this));
		this.etTo.setTokenizer(new MultiAutoCompleteTextView.CommaTokenizer());
		this.etTo.requestFocus();

		this.parseIntent(this.getIntent());

		boolean checkPrefix = true;
		boolean showIntro = false;
		if (TextUtils.isEmpty(p.getString(PREFS_SENDER, null))
				&& TextUtils.isEmpty(p.getString(PREFS_DEFPREFIX, null))
				&& CONNECTORS.size() == 0) {
			checkPrefix = false;
			showIntro = true;
		}

		if (TextUtils.isEmpty(p.getString(PREFS_SENDER, null))
				|| TextUtils.isEmpty(p.getString(PREFS_DEFPREFIX, null))) {
			TelephonyManager tm = (TelephonyManager) this
					.getSystemService(TELEPHONY_SERVICE);
			String number = tm.getLine1Number();
			Log.i(TAG, "line1: " + number);

			if (number != null && number.startsWith("00")) {
				number = number.replaceFirst("00", "+");
			}
			if (number != null && !TextUtils.isEmpty(number)
					&& (number.startsWith("+"))) {
				Editor e = p.edit();
				if (TextUtils.isEmpty(p.getString(PREFS_SENDER, null))) {
					Log.i(TAG, "set number=" + number);
					e.putString(PREFS_SENDER, number);
				}
				if (TextUtils.isEmpty(p.getString(PREFS_DEFPREFIX, null))) {
					String prefix = de.ub0r.android.lib.Utils
							.getPrefixFromTelephoneNumber(number);
					if (!TextUtils.isEmpty(prefix)) {
						Log.i(TAG, "set prefix=" + prefix);
						e.putString(PREFS_DEFPREFIX, prefix);
					} else {
						Log.w(TAG, "unable to get prefix from number: "
								+ number);
					}
				}
				e.apply();
			}
		}

		// check default prefix
		if (checkPrefix && !p.getString(PREFS_DEFPREFIX, "").startsWith("+")) {
			this.log(R.string.log_wrong_defprefix);
		}

		if (showIntro) {
			// skip help intro for at least 2min
			if (System.currentTimeMillis() > p.getLong(PREFS_LASTHELP, 0L)
					+ de.ub0r.android.lib.Utils.MINUTES_IN_MILLIS * 2) {
				p.edit().putLong(PREFS_LASTHELP, System.currentTimeMillis())
						.apply();
				this.startActivity(new Intent(this, HelpIntroActivity.class));
			}
		}

		mAdView = (AdView) findViewById(R.id.ads);
        mAdView.setVisibility(View.GONE);
        if (!prefsNoAds && prefsShowAds) {
            mAdView.loadAd(new AdRequest.Builder().build());
            mAdView.setAdListener(new AdListener() {
                @Override
                public void onAdLoaded() {
                    mAdView.setVisibility(View.VISIBLE);
                    super.onAdLoaded();
                }
            });

			mInterstitialAd = new InterstitialAd(this);
			mInterstitialAd.setAdUnitId(INTERSTITIAL_AD_UNIT_ID);

			mInterstitialAd.setAdListener(new AdListener() {
				@Override
				public void onAdClosed() {
					requestNewInterstitial();
				}
			});

			requestNewInterstitial();
        } else if (prefsNoAds) {
			findViewById(R.id.cookie_consent).setVisibility(View.GONE);
		}
	}

	private void requestNewInterstitial() {
		if (prefsInterstitialAd) {
			Log.d(TAG, "request new interstitial ad");
			AdRequest adRequest = new AdRequest.Builder()
					.addTestDevice("2FD55F382A3E8A84879767A864A1397C")
					.build();

			mInterstitialAd.loadAd(adRequest);
		}
	}

	private boolean isNewVersion() {
        SharedPreferences p = getPreferences(MODE_PRIVATE);
        if (BuildConfig.VERSION_CODE != p.getInt(LAST_RUN, 0)) {
            p.edit().putInt(LAST_RUN, BuildConfig.VERSION_CODE).apply();
            return true;
        }
        return false;
    }

    @Override
	protected final void onSaveInstanceState(final Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(EXTRA_TO, this.lastTo);
		outState.putString(EXTRA_TEXT, this.lastMsg);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final void onActivityResult(final int requestCode,
			final int resultCode, final Intent data) {
		if (requestCode == ARESULT_PICK_PHONE) {
			if (resultCode == RESULT_OK) {
				final Uri u = data.getData();
				if (u == null) {
					return;
				}
				try {
					final String phone = ContactsWrapper.getInstance()
							.getNameAndNumber(this.getContentResolver(), u)
							+ ", ";
					String t = null;
					if (this.etTo != null) {
						t = this.etTo.getText().toString().trim();
					}
					if (TextUtils.isEmpty(t) && !TextUtils.isEmpty(this.lastTo)) {
						t = this.lastTo.trim();
					}
					if (TextUtils.isEmpty(t)) {
						t = phone;
					} else if (t.endsWith(",")) {
						t += " " + phone;
					} else {
						t += ", " + phone;
					}
					this.lastTo = t;
					this.etTo.setText(t);
				} catch (IllegalStateException e) {
					Log.e(TAG, "failed resolving name and number", e);
				}
			}
		}
	}

	@Override
	protected final void onNewIntent(final Intent intent) {
		super.onNewIntent(intent);
		Log.d(TAG, "onNewIntent(" + intent + ")");
		this.parseIntent(intent);
	}

	/**
	 * Update {@link ConnectorSpec}s.
	 */
	private void updateConnectors() {
		// query for connectors
		final Intent i = new Intent(Connector.ACTION_CONNECTOR_UPDATE);
		i.setFlags(i.getFlags() | Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
		Log.d(TAG, "send broadcast: " + i.getAction());
		newConnectorsExpected = this.getInstalledConnectorsCount()
				- CONNECTORS.size();
		updateProgressBar();
		this.sendBroadcast(i);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final void onResume() {
		super.onResume();
        mAdView.resume();

		// set accounts' balance to gui
		this.updateBalance();

		// if coming from prefs..
		if (doPreferences) {
			this.reloadPrefs();
			this.updateConnectors();
			doPreferences = false;
			final SharedPreferences p = PreferenceManager
					.getDefaultSharedPreferences(this);
			final String defPrefix = p.getString(PREFS_DEFPREFIX, "+49");
			final String defSender = p.getString(PREFS_SENDER, "");
			final ConnectorSpec[] css = getConnectors(
					ConnectorSpec.CAPABILITIES_BOOTSTRAP,
					ConnectorSpec.STATUS_ENABLED | ConnectorSpec.STATUS_READY,
                    false /*isIncludePseudoConnectors*/);
			for (ConnectorSpec cs : css) {
				runCommand(this, cs,
						ConnectorCommand.bootstrap(defPrefix, defSender));
			}
		} else {
			// check is count of connectors changed
			final int s1 = this.getInstalledConnectorsCount();
			final int s2 = CONNECTORS.size();
			if (s1 != s2) {
				Log.d(TAG, "clear connector cache (" + s1 + "/" + s2 + ")");
				CONNECTORS.clear();
				this.updateConnectors();
			}
		}

        // get updated specs for pseudo-connectors
        rules.updateSpec(this);     // this will update the singleton spec

        this.setButtons();

		if (this.lastTo == null || this.lastTo.length() == 0) {
			final SharedPreferences p = PreferenceManager
					.getDefaultSharedPreferences(this);
			this.lastTo = p.getString(PREFS_DEFAULT_RECIPIENT, null);
		}

		// reload text/recipient from local store
		if (TextUtils.isEmpty(this.etText.getText())) {
			if (this.lastMsg != null) {
				this.etText.setText(this.lastMsg);
			} else {
				this.etText.setText("");
			}
		}
		if (TextUtils.isEmpty(this.etTo.getText())) {
			if (this.lastTo != null) {
				this.etTo.setText(this.lastTo);
			} else {
				this.etTo.setText("");
			}
		}

		if (this.lastTo != null && this.lastTo.length() > 0) {
			this.etText.requestFocus();
			this.etText.setSelection(this.etText.getText().length());
		} else {
			this.etTo.requestFocus();
		}
	}

	/**
	 * Update balance.
	 */
	private void updateBalance() {
		Log.d(TAG, "updateBalance()");
		final StringBuilder buf = new StringBuilder();
		final ConnectorSpec[] css = getConnectors(
				ConnectorSpec.CAPABILITIES_UPDATE,
                ConnectorSpec.STATUS_ENABLED,
                false /*isIncludePseudoConnectors*/);
		String singleb = null;
		for (ConnectorSpec cs : css) {
			final String b = cs.getBalance();
			if (b == null || b.length() == 0) {
				continue;
			}
			if (buf.length() > 0) {
				buf.append(", ");
				singleb = null;
			} else {
				singleb = b;
			}
			buf.append(cs.getName());
			buf.append(": ");
			buf.append(b);
		}
		if (singleb != null) {
			buf.replace(0, buf.length(), singleb);
		}

		buf.insert(0, this.getString(R.string.free_) + " ");
		this.getSupportActionBar().setSubtitle(buf.toString());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final void onPause() {
        mAdView.pause();
		// store input data to persistent stores
		this.lastMsg = this.etText.getText().toString();
		this.lastTo = this.etTo.getText().toString();
		this.savePreferences();
        super.onPause();
	}

    @Override
	protected final void onDestroy() {
        mAdView.destroy();
     	final Editor editor = PreferenceManager.getDefaultSharedPreferences(
				this).edit();
		try {
			final ByteArrayOutputStream out = new ByteArrayOutputStream();
			ObjectOutputStream objOut = new ObjectOutputStream(
					new BufferedOutputStream(out, BUFSIZE));
			objOut.writeObject(CONNECTORS);
			objOut.close();
			final String s = Base64.encodeToString(out.toByteArray(), Base64.DEFAULT);
			Log.d(TAG, s);
			editor.putString(PREFS_CONNECTORS, s);
		} catch (Exception e) {
			editor.remove(PREFS_CONNECTORS);
			Log.e(TAG, "IO", e);
		}
		editor.apply();
        super.onDestroy();
    }

	/**
	 * Read static variables holding preferences.
	 */
	private void reloadPrefs() {
		Log.d(TAG, "reloadPrefs()");
		int ts = PreferencesActivity.getTextsize(this);
		if (ts != 0) {
			this.etTo.setTextSize(ts);
			this.etText.setTextSize(ts);
		}
		final SharedPreferences p = PreferenceManager
				.getDefaultSharedPreferences(this);
		final boolean bShowEmoticons = !p.getBoolean(PREFS_HIDE_EMO_BUTTON,
				false);
		prefsShowCancel = !p.getBoolean(PREFS_HIDE_CANCEL_BUTTON, false);
		bShowExtras = !p.getBoolean(PREFS_HIDE_EXTRAS_BUTTON, false);
		final boolean bShowClearRecipients = !p.getBoolean(
				PREFS_HIDE_CLEAR_RECIPIENTS_BUTTON, false);
		final boolean bShowSelectRecipients = !p.getBoolean(
				PREFS_HIDE_SELECT_RECIPIENTS_BUTTON, false);
		View v = this.findViewById(R.id.select);
		if (bShowSelectRecipients) {
			v.setVisibility(View.VISIBLE);
		} else {
			v.setVisibility(View.GONE);
		}
		v = this.findViewById(R.id.clear);
		if (bShowClearRecipients) {
			v.setVisibility(View.VISIBLE);
		} else {
			v.setVisibility(View.GONE);
		}
		v = this.findViewById(R.id.emo);
		if (bShowEmoticons) {
			v.setVisibility(View.VISIBLE);
		} else {
			v.setVisibility(View.GONE);
		}

		v = this.findViewById(R.id.text_connector);
		if (p.getBoolean(PREFS_HIDE_BG_CONNECTOR, false)) {
			v.setVisibility(View.INVISIBLE);
		} else {
			v.setVisibility(View.VISIBLE);
		}

		String prefsConnectorID = p.getString(PREFS_CONNECTOR_ID, "");
        ConnectorSpec cs = getConnectorByID(prefsConnectorID);
		if (cs != null && cs.hasStatus(ConnectorSpec.STATUS_ENABLED)) {
            prefsConnectorSpec = cs;

            String prefsSubConnectorID = p.getString(PREFS_SUBCONNECTOR_ID, "");
            prefsSubConnectorSpec = cs.getSubConnector(prefsSubConnectorID);
			if (prefsSubConnectorSpec == null) {
				prefsSubConnectorSpec = cs.getSubConnectors()[0];
			}
		} else {
			ConnectorSpec[] connectors = getConnectors(
					ConnectorSpec.CAPABILITIES_SEND,
					ConnectorSpec.STATUS_ENABLED,
                    true /*isIncludePseudoConnectors*/);
			if (connectors.length == 1
                    && connectors[0].getSubConnectors().length == 1) {
				prefsConnectorSpec = connectors[0];
				prefsSubConnectorSpec = prefsConnectorSpec.getSubConnectors()[0];
				Toast.makeText(
						this,
						this.getString(R.string.connectors_switch) + " "
								+ prefsConnectorSpec.getName(),
						Toast.LENGTH_LONG).show();
			} else {
				prefsConnectorSpec = null;
				prefsSubConnectorSpec = null;
			}
		}

		MobilePhoneAdapter.setMoileNubersObly(p.getBoolean(PREFS_MOBILES_ONLY, false));

        prefsNoAds = DonationHelper.hideAds(this);
		if (!prefsNoAds) {
			int counter = p.getInt(PREFS_ADS_COUNTER, 15) - 1;
			if (counter >= 0) {
				Log.d(TAG, "write PREFS_ADS_COUNTER: " + counter);
				p.edit().putInt(PREFS_ADS_COUNTER, counter).apply();
			}
			prefsShowAds = counter <= 0;
			final long random = System.currentTimeMillis() % INTERSTITIAL_ADS_RATION;
			prefsInterstitialAd = random == 0 && !p.getBoolean(PREFS_AUTOEXIT, false);

		}
		this.setButtons();
	}

    /**
     * Updates preferences and replaces the selected connector with the default (standard) one.
     * reloadPrefs() should be called afterwards to load the change.
     */
    private void revertPrefsToStdConnector() {
        final SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(this);

        String stdConnector = p.getString(PREFS_STANDARD_CONNECTOR, "");
        String stdSubConnector = p.getString(PREFS_STANDARD_SUBCONNECTOR, "");

        if (!TextUtils.isEmpty(stdConnector)) {
            p.edit()
                .putString(PREFS_CONNECTOR_ID, stdConnector)
                .putString(PREFS_SUBCONNECTOR_ID, stdSubConnector)
                .apply();
        }
    }

	/**
	 * Show/hide, enable/disable send buttons.
	 */
	private void setButtons() {
		if (prefsConnectorSpec != null && prefsSubConnectorSpec != null
				&& prefsConnectorSpec.hasStatus(ConnectorSpec.STATUS_ENABLED)) {
			final boolean sFlashsms = prefsSubConnectorSpec
					.hasFeatures(SubConnectorSpec.FEATURE_FLASHSMS);
			final boolean sCustomsender = prefsSubConnectorSpec
					.hasFeatures(SubConnectorSpec.FEATURE_CUSTOMSENDER);
			final boolean sSendLater = prefsSubConnectorSpec
					.hasFeatures(SubConnectorSpec.FEATURE_SENDLATER);

			if (bShowExtras && (sFlashsms || sCustomsender || sSendLater)) {
				if (sFlashsms) {
					this.vFlashSMS.setVisibility(View.VISIBLE);
				} else {
					this.vFlashSMS.setVisibility(View.GONE);
				}
				if (sCustomsender) {
					this.vCustomSender.setVisibility(View.VISIBLE);
					this.vCustomSender.setChecked(!TextUtils
							.isEmpty(lastCustomSender));
				} else {
					this.vCustomSender.setVisibility(View.GONE);
					this.vCustomSender.setChecked(false);
				}
				if (sSendLater) {
					this.vSendLater.setVisibility(View.VISIBLE);
				} else {
					this.vSendLater.setVisibility(View.GONE);
				}
				this.findViewById(R.id.extraButtons)
						.setVisibility(View.VISIBLE);
			} else {
				this.findViewById(R.id.extraButtons).setVisibility(View.GONE);
			}

			String t = prefsConnectorSpec.getName();
			if (prefsConnectorSpec.getSubConnectorCount() > 1) {
				t += " - " + prefsSubConnectorSpec.getName();
			}
			this.setTitle(t);
			String s = t;
			if (lastSendLater > 0L) {
				Calendar cal = Calendar.getInstance();
				cal.setTimeInMillis(lastSendLater);
				s += "\n@"
						+ DateFormat.getDateFormat(this).format(cal.getTime());
				s += " " + DateFormat.getTimeFormat(this).format(cal.getTime());
				this.vSendLater.setChecked(true);
			} else {
				this.vSendLater.setChecked(false);
			}
			Log.d(TAG, "set backgroundtext: " + s);
			((TextView) this.findViewById(R.id.text_connector)).setText(s);
		} else {
			this.setTitle(R.string.app_name);
			((TextView) this.findViewById(R.id.text_connector)).setText("");
			if (CONNECTORS.size() > 0) {
				Toast.makeText(this, R.string.log_noselectedconnector,
						Toast.LENGTH_SHORT).show();
			}
			this.findViewById(R.id.extraButtons).setVisibility(View.GONE);
		}
    }

	/**
	 * Resets persistent store.
	 * 
	 * @param backupText
	 *            backup text to {@link SharedPreferences}
	 */
	private void reset(final boolean backupText) {
		this.lastMsg = this.etText.getText().toString();

		// save user preferences
		final SharedPreferences.Editor editor = PreferenceManager
				.getDefaultSharedPreferences(this).edit();
		if (backupText) {
			if (!TextUtils.isEmpty(this.lastMsg)) {
				editor.putString(PREFS_BACKUPLASTTEXT, this.lastMsg);
			}
		} else {
			editor.remove(PREFS_BACKUPLASTTEXT);
		}
		editor.apply();

		this.etText.setText("");
		this.etTo.setText("");
		this.lastMsg = null;
		this.lastTo = null;
		lastCustomSender = null;
		lastSendLater = -1;
		this.setButtons();
	}

	/** Save prefs. */
	final void savePreferences() {
		if (prefsConnectorSpec != null && prefsSubConnectorSpec != null) {
			PreferenceManager.getDefaultSharedPreferences(this)
    		    .edit()
                .putString(PREFS_CONNECTOR_ID, prefsConnectorSpec.getPackage())
                .putString(PREFS_SUBCONNECTOR_ID, prefsSubConnectorSpec.getID())
                .commit();
		}
	}

	/**
	 * Run Connector.doUpdate().
	 */
	private void updateFreecount() {
		final SharedPreferences p = PreferenceManager
				.getDefaultSharedPreferences(this);
		final String defPrefix = p.getString(PREFS_DEFPREFIX, "+49");
		final String defSender = p.getString(PREFS_SENDER, "");
		final ConnectorSpec[] css = getConnectors(
				ConnectorSpec.CAPABILITIES_UPDATE,
				ConnectorSpec.STATUS_ENABLED | ConnectorSpec.STATUS_READY,
                false /*isIncludePseudoConnectors*/);
		for (ConnectorSpec cs : css) {
			if (cs.isRunning()) {
				// skip running connectors
				Log.d(TAG, "skip running connector: " + cs.getName());
				continue;
			}
			runCommand(this, cs, ConnectorCommand.update(defPrefix, defSender));
		}
	}

	/**
	 * Send a command as broadcast.
	 * 
	 * @param context
	 *            Current context
	 * @param connector
	 *            {@link ConnectorSpec}
	 * @param command
	 *            {@link ConnectorCommand}
	 */
	static void runCommand(final Context context,
			final ConnectorSpec connector, final ConnectorCommand command) {
		connector.setErrorMessage((String) null);
		final Intent intent = command.setToIntent(null);
		short t = command.getType();
		boolean sendOrdered = false;
		switch (t) {
		case ConnectorCommand.TYPE_BOOTSTRAP:
			sendOrdered = true;
			intent.setAction(connector.getPackage()
					+ Connector.ACTION_RUN_BOOTSTRAP);
			connector.addStatus(ConnectorSpec.STATUS_BOOTSTRAPPING);
			break;
		case ConnectorCommand.TYPE_SEND:
			sendOrdered = true;
			intent.setAction(connector.getPackage() + Connector.ACTION_RUN_SEND);
			connector.setToIntent(intent);
			connector.addStatus(ConnectorSpec.STATUS_SENDING);
			if (command.getResendCount() == 0) {
				WebSMSReceiver.saveMessage(me, connector, command,
						WebSMSReceiver.MESSAGE_TYPE_DRAFT);
			}
			break;
		case ConnectorCommand.TYPE_UPDATE:
			intent.setAction(connector.getPackage()
					+ Connector.ACTION_RUN_UPDATE);
			connector.addStatus(ConnectorSpec.STATUS_UPDATING);
			break;
		default:
			break;
		}
		updateProgressBar();
		intent.setFlags(intent.getFlags()
				| Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
		Log.d(TAG, "send broadcast: " + intent.getAction());
		if (sendOrdered) {
			context.sendOrderedBroadcast(intent, null, new BroadcastReceiver() {
				@Override
				public void onReceive(final Context context, final Intent intent) {
					if (this.getResultCode() != Activity.RESULT_OK) {
						ConnectorCommand command = new ConnectorCommand(intent);
						ConnectorSpec specs = new ConnectorSpec(intent);
						specs.setErrorMessage(// TODO: localize
						"Connector did not react on message");
						WebSMSReceiver.handleSendCommand(context, specs,
                                command);
					}
				}
			}, null, Activity.RESULT_CANCELED, null, null);
		} else {
			context.sendBroadcast(intent);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("deprecation")
	public final void onClick(final View v) {
		CharSequence s;
		switch (v.getId()) {
		case R.id.select:
			this.startActivityForResult(ContactsWrapper.getInstance()
					.getPickPhoneIntent(), ARESULT_PICK_PHONE);
			return;
		case R.id.text_clear:
			this.reset(true);
            this.revertPrefsToStdConnector();
			this.reloadPrefs();
			return;
		case R.id.clear:
			s = this.etTo.getText();
			final String ss = s.toString();
			int i = ss.lastIndexOf(",");
			if (ss.substring(i + 1).trim().length() <= 0) {
				i = ss.substring(0, i).lastIndexOf(",");
			}

			if (i <= 0) {
				this.lastTo = null;
				this.etTo.setText("");
			} else {
				this.lastTo = ss.substring(0, i) + ", ";
				this.etTo.setText(this.lastTo);
				s = this.etTo.getText();
				this.etTo.setSelection(s.length());
				this.lastTo = s.toString();
			}
			return;
		case R.id.custom_sender:
			if (this.vCustomSender.isChecked()) {
				this.showDialog(DIALOG_CUSTOMSENDER);
			} else {
				lastCustomSender = null;
			}
			return;
		case R.id.send_later:
			if (this.vSendLater.isChecked()) {
				this.showDialog(DIALOG_SENDLATER_DATE);
			} else {
				lastSendLater = -1;
			}
			this.setButtons();
			return;
		case R.id.emo:
			this.showDialog(DIALOG_EMO);
			return;
		case R.id.text_paste:
			s = this.cbmgr.getText();
			this.etText.setText(s);
			s = this.etText.getText();
			this.etText.setSelection(s.length());
			this.lastMsg = s.toString();
			return;
		default:
			return;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final boolean onLongClick(final View v) {
		switch (v.getId()) {
		case R.id.clear:
			this.lastTo = null;
			this.etTo.setText("");
			return true;
		default:
			return false;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final boolean onCreateOptionsMenu(final Menu menu) {
		getMenuInflater().inflate(R.menu.menu, menu);
		if (prefsNoAds) {
			menu.removeItem(R.id.item_donate);
		}
		return true;
	}

	/**
	 * Save selected connector.
	 * 
	 * @param cs
	 *            {@link ConnectorSpec}
	 * @param scs
	 *            {@link SubConnectorSpec}
	 */
	private void saveSelectedConnector(final ConnectorSpec cs,
			final SubConnectorSpec scs) {
		prefsConnectorSpec = cs;
		prefsSubConnectorSpec = scs;
		this.setButtons();
		if (cs == null || scs == null) {
			return;
		}
		// save user preferences
		PreferenceManager.getDefaultSharedPreferences(this)
            .edit()
		    .putString(PREFS_CONNECTOR_ID, prefsConnectorSpec.getPackage())
		    .putString(PREFS_SUBCONNECTOR_ID, prefsSubConnectorSpec.getID())
		    .commit();
	}

	/**
	 * Get all enabled {@link ConnectorSpec}s as name.
	 *
     * @param isIncludePseudoConnectors
     *            whether pseudo connectors should be included
	 * @return array of {@link ConnectorLabel}.
	 */
	public static ConnectorLabel[] getConnectorMenuItems(boolean isIncludePseudoConnectors) {
		final ConnectorSpec[] css = getConnectors(
				ConnectorSpec.CAPABILITIES_SEND,
                ConnectorSpec.STATUS_ENABLED,
                isIncludePseudoConnectors);
		final List<ConnectorLabel> items = new ArrayList<>(css.length * 2);
		SubConnectorSpec[] scs;
		for (ConnectorSpec cs : css) {
			scs = cs.getSubConnectors();
			if (scs.length == 1) {
				items.add(new ConnectorLabel(cs, scs[0], true /*isSingleSubConnector*/));
			} else {
				for (SubConnectorSpec sc : scs) {
                    items.add(new ConnectorLabel(cs, sc, false /*isSingleSubConnector*/));
				}
			}
		}
		return items.toArray(new ConnectorLabel[items.size()]);
	}

	/**
	 * Display "change connector" menu.
	 */
	private void changeConnectorMenu() {
		Log.d(TAG, "changeConnectorMenu()");
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setIcon(android.R.drawable.ic_menu_share);
		builder.setTitle(R.string.change_connector_);
		final ConnectorLabel[] items = this.getConnectorMenuItems(true /*isIncludePseudoConnectors*/);
		final int l = items.length;

		if (l == 0) {
			Toast.makeText(this, R.string.log_noreadyconnector, Toast.LENGTH_LONG).show();

		} else if (l == 1) {
			this.saveSelectedConnector(items[0].getConnector(), items[0].getSubConnector());

		} else if (l == 2) {
			// Find actual connector, pick the other one from css
            ConnectorLabel newSelected;
			if (prefsConnectorSpec == null || prefsSubConnectorSpec == null) {
                newSelected = items[0];

            } else if (prefsConnectorSpec.getPackage().equals(items[0].getConnector().getPackage())
                && prefsSubConnectorSpec.getID().equals(items[0].getSubConnector().getID())) {
                newSelected = items[1];

			} else {
                newSelected = items[0];
			}
			this.saveSelectedConnector(newSelected.getConnector(), newSelected.getSubConnector());
			Toast.makeText(this,
					this.getString(R.string.connectors_switch) + " " + newSelected.getName(),
					Toast.LENGTH_SHORT).show();
		} else {
			builder.setItems(items, new DialogInterface.OnClickListener() {
				public void onClick(final DialogInterface d, final int idx) {
					WebSMS.this.saveSelectedConnector(items[idx].getConnector(),
                            items[idx].getSubConnector());
				}
			});
			builder.create().show();
		}
	}

	/**
	 * Save some characters by stripping blanks.
	 */
	private void saveChars() {
		String s = this.etText.getText().toString().trim();
		if (s.length() == 0) {
			return;
		}

		String choice = PreferenceManager.getDefaultSharedPreferences(this)
				.getString("save_chars", "remove_spaces");

		if (choice.contains("remove_diacritics")) {
			s = this.removeDiacritics(s);
		}

		if (choice.contains("remove_spaces")) {
			s = this.removeSpaces(s);
		}

		this.etText.setText(s);
	}

	private String removeSpaces(final String s) {
		StringBuilder buf = new StringBuilder();
		final String[] ss = s.split(" ");
		for (String ts : ss) {
			final int l = ts.length();
			if (l == 0) {
				continue;
			}
			buf.append(Character.toUpperCase(ts.charAt(0)));
			if (l == 1) {
				continue;
			}
			buf.append(ts.substring(1));
		}

		return buf.toString();
	}

	@TargetApi(Build.VERSION_CODES.GINGERBREAD)
	private String removeDiacritics(final String s) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.GINGERBREAD) {
			return s;
		}

		String text = Normalizer.normalize(s, Normalizer.Form.NFD);
		text = text.replaceAll("[^\\p{ASCII}]", "");
		return text;

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final boolean onPrepareOptionsMenu(final Menu menu) {
		final ConnectorSpec[] connectors = getConnectors(
				ConnectorSpec.CAPABILITIES_SEND,
                ConnectorSpec.STATUS_READY | ConnectorSpec.STATUS_ENABLED,
                true /*isIncludePseudoConnectors*/);

        boolean isPrefsConnectorOk = prefsConnectorSpec != null && prefsSubConnectorSpec != null
                && prefsConnectorSpec.hasStatus(ConnectorSpec.STATUS_ENABLED);
        menu.findItem(R.id.item_connector).setVisible(
				connectors.length > 1
				|| (connectors.length == 1 && connectors[0].getSubConnectorCount() > 1)
                || (connectors.length == 1 && !isPrefsConnectorOk));

		boolean hasText = this.etText != null
				&& !TextUtils.isEmpty(this.etText.getText());
		menu.findItem(R.id.item_savechars).setVisible(hasText);
		// only allow to save drafts on API18-
		menu.findItem(R.id.item_draft).setVisible(
				Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT
						&& hasText);
		final boolean showRestore = !TextUtils.isEmpty(PreferenceManager
				.getDefaultSharedPreferences(this).getString(
						PREFS_BACKUPLASTTEXT, null));
		try {
			menu.removeItem(ITEM_RESTORE);
		} catch (Exception e) {
			Log.w(TAG, "error removing item: " + ITEM_RESTORE, e);
		}
		if (showRestore) {
			menu.add(0, ITEM_RESTORE, android.view.Menu.NONE, R.string.restore_);
			menu.findItem(ITEM_RESTORE).setIcon(
					android.R.drawable.ic_menu_revert);
		}
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final boolean onOptionsItemSelected(final MenuItem item) {
		Log.d(TAG, "onOptionsItemSelected(" + item.getItemId() + ")");
		switch (item.getItemId()) {
		case R.id.item_send:
			Log.d(TAG, "send button clicked");
			this.send(prefsConnectorSpec, prefsSubConnectorSpec);
			return true;
		case R.id.item_draft:
			this.saveDraft();
			return true;
		case R.id.item_savechars:
			this.saveChars();
			return true;
		case R.id.item_settings:
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
				this.startActivity(new Intent(this, Preferences11Activity.class));
			} else {
				this.startActivity(new Intent(this, PreferencesActivity.class));
			}
			return true;
		case R.id.item_donate:
			startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(DONATION_URL)));
			return true;
		case R.id.item_connector:
			this.changeConnectorMenu();
			return true;
		case R.id.item_update:
			this.updateFreecount();
			return true;
		case android.R.id.home:
			String s = this.getSupportActionBar().getSubtitle().toString();
			if (s.contains(",")) {
				Builder b = new Builder(this);
				String bs = this.getString(R.string.free_);
				b.setTitle(bs.replaceAll(":", ""));
				b.setMessage(this.getSupportActionBar().getSubtitle()
						.toString().replace(bs, "").replaceAll(", ", "\n")
						.trim());
				b.setCancelable(true);
				b.show();
				return true;
			} else {
				return false;
			}
		case ITEM_RESTORE:
			final SharedPreferences p = PreferenceManager
					.getDefaultSharedPreferences(this);
			s = p.getString(PREFS_BACKUPLASTTEXT, null);
			if (!TextUtils.isEmpty(s)) {
				this.etText.setText(s);
			}
			p.edit().remove(PREFS_BACKUPLASTTEXT).apply();
			return true;
		default:
			return false;
		}
	}

	/**
	 * Create a Emoticons {@link Dialog}.
	 * 
	 * @return Emoticons {@link Dialog}
	 */
	private Dialog createEmoticonsDialog() {
		final Dialog d = new Dialog(this);
		d.setTitle(R.string.emo_);
		d.setContentView(R.layout.emo);
		d.setCancelable(true);
		final String[] emoticons = this.getResources().getStringArray(
				R.array.emoticons);
		final GridView gridview = (GridView) d.findViewById(R.id.gridview);
		gridview.setAdapter(new BaseAdapter() {
			// references to our images
			// keep order and count synced with string-array!
			private Integer[] mThumbIds = { R.drawable.emo_im_angel,
					R.drawable.emo_im_cool, R.drawable.emo_im_crying,
					R.drawable.emo_im_foot_in_mouth, R.drawable.emo_im_happy,
					R.drawable.emo_im_kissing, R.drawable.emo_im_laughing,
					R.drawable.emo_im_lips_are_sealed,
					R.drawable.emo_im_money_mouth, R.drawable.emo_im_sad,
					R.drawable.emo_im_surprised,
					R.drawable.emo_im_tongue_sticking_out,
					R.drawable.emo_im_undecided, R.drawable.emo_im_winking,
					R.drawable.emo_im_wtf, R.drawable.emo_im_yelling };

			@Override
			public long getItemId(final int position) {
				return 0;
			}

			@Override
			public Object getItem(final int position) {
				return null;
			}

			@Override
			public int getCount() {
				return this.mThumbIds.length;
			}

			@Override
			public View getView(final int position, final View convertView,
					final ViewGroup parent) {
				ImageView imageView;
				if (convertView == null) { // if it's not recycled,
					// initialize some attributes
					imageView = new ImageView(WebSMS.this);
					imageView.setLayoutParams(new GridView.LayoutParams(
							EMOTICONS_SIZE, EMOTICONS_SIZE));
					imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
					imageView.setPadding(EMOTICONS_PADDING, EMOTICONS_PADDING,
							EMOTICONS_PADDING, EMOTICONS_PADDING);
				} else {
					imageView = (ImageView) convertView;
				}

				imageView.setImageResource(this.mThumbIds[position]);
				return imageView;
			}
		});
		gridview.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(final AdapterView<?> adapter, final View v,
					final int id, final long arg3) {
				EditText et = WebSMS.this.etText;
				final String e = emoticons[id];
				int i = et.getSelectionStart();
				int j = et.getSelectionEnd();
				if (i > j) {
					int x = i;
					i = j;
					j = x;
				}
				String t = et.getText().toString();
				et.setText(t.substring(0, i) + e + t.substring(j));
				et.setSelection(i + e.length());
				d.dismiss();
				et.requestFocus();
			}
		});
		return d;
	}

	@Override
	protected final Dialog onCreateDialog(final int id) {
		AlertDialog.Builder builder;
		switch (id) {
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
			return new DatePickerDialog(this, this, c.get(Calendar.YEAR),
					c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
		case DIALOG_SENDLATER_TIME:
			c = Calendar.getInstance();
			return new MyTimePickerDialog(this, this,
					c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true);
		case DIALOG_EMO:
			return this.createEmoticonsDialog();
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
	 * Safe draft.
	 */
	private void saveDraft() {
		// fetch text/recipient
		final String to = this.etTo.getText().toString();
		final String text = this.etText.getText().toString();
		if (to.length() == 0 || text.length() == 0) {
			return;
		}

		final String[] tos = Utils.parseRecipients(to);
		final ConnectorCommand command = ConnectorCommand.send(nextMsgId(this),
				null, null, null, tos, text, false);
		WebSMSReceiver.saveMessage(this, null, command,
				WebSMSReceiver.MESSAGE_TYPE_DRAFT);
		this.reset(false);
	}

	/**
	 * Send text.
	 * 
	 * @param connector
	 *            which connector should be used.
	 * @param subconnector
	 *            which subconnector should be used
	 * @return true if message was sent
	 */
	private boolean send(final ConnectorSpec connector,
			final SubConnectorSpec subconnector) {
		Log.d(TAG, "send(" + connector + "," + subconnector + ")");
		if (connector == null || subconnector == null) {
			Log.e(TAG, "connector: " + connector);
			Log.e(TAG, "subconnector: " + subconnector);
			Toast.makeText(this, R.string.error, Toast.LENGTH_LONG).show();
			return false;
		}
		// fetch text/recipient
		final String to = this.etTo.getText().toString();
		String text = this.etText.getText().toString();
		if (TextUtils.isEmpty(to) || TextUtils.isEmpty(text)) {
			Log.e(TAG, "to: " + to);
			Log.e(TAG, "text: " + text);
			return false;
		}
		text = text.trim();
		this.etText.setText(text);

		final SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(this);
		final String signature = p.getString(PREFS_SIGNATURE, null);
		if (signature != null && signature.length() > 0
				&& !text.endsWith(signature)) {
			text = text + signature;
			this.etText.setText(text);
		}

		final String[] tos = Utils.parseRecipients(to);
        if (tos.length == 0) {
            Log.e(TAG, "tos list empty");
            return false;
        }

        if (connector.getPackage().equals(PseudoConnectorRules.ID)) {
            return sendByRules(tos, text);
        } else {
            return sendReal(connector, subconnector, tos, text);
        }
	}

    /**
     * Send text to connector based on rules.
     *
     * @param tos
     *            recipients
     * @param text
     *            message text
     * @return true if message was sent
     */
    private boolean sendByRules(final String[] tos, final String text) {

        // find connector for each of the recipients,
        // group together recipients that will use the same connector
        Map<ConnectorLabel,List<String>> chosenMap = new HashMap<>();
        try {
            for (String to : tos) {
                ConnectorLabel chosenConn = rules.chooseConnector(this, to, text);
                List<String> tosForChosen = chosenMap.get(chosenConn);
                if (tosForChosen == null) {
                    tosForChosen = new ArrayList<>();
                    chosenMap.put(chosenConn, tosForChosen);
                }
                tosForChosen.add(to);
            }
        } catch (WebSMSException e) {
            Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            return false;
        }

        // dispatch
        for (Map.Entry<ConnectorLabel,List<String>> entry : chosenMap.entrySet()) {
            ConnectorLabel chosenConn = entry.getKey();
            String[] tosForChosen = entry.getValue().toArray(new String[entry.getValue().size()]);

            if (PseudoConnectorRules.isTestOnly(this)) {
                Toast.makeText(this,
                    getString(R.string.rules_test_only,
                            Utils.joinRecipients(tosForChosen,","), chosenConn.getName()),
                    Toast.LENGTH_LONG).show();

            } else {
                if (PseudoConnectorRules.isShowDecisionToast(this)) {
                    Toast.makeText(this,
                            getString(R.string.rules_decision,
                                    Utils.joinRecipients(tosForChosen, ","), chosenConn.getName()),
                            Toast.LENGTH_LONG).show();
                }
                boolean ok = sendReal(chosenConn.getConnector(), chosenConn.getSubConnector(),
                    tosForChosen, text);
                if (!ok) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Send text after the real connector is chosen.
     *
     * @param connector
     *            which connector should be used.
     * @param subconnector
     *            which subconnector should be used
     * @param tos
     *            recipients
     * @param text
     *            message text
     * @return true if message was sent
     */
    private boolean sendReal(final ConnectorSpec connector,
            final SubConnectorSpec subconnector,
            final String[] tos,
            final String text) {

        Log.d(TAG, "sendReal(" + connector + "," + subconnector + ")");
        final SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(this);

        if (!p.getBoolean(PREFS_TRY_SEND_INVALID, false)
                && connector.hasCapabilities(ConnectorSpec.CAPABILITIES_CHARACTER_CHECK)) {
            final String valid = connector.getValidCharacters();
            if (valid == null) {
                Log.i(TAG, "valid: null");
                Toast.makeText(this, R.string.log_error_char_nonvalid,
                        Toast.LENGTH_LONG).show();
                return false;
            }
            final Pattern checkPattern = Pattern.compile("[^"
                    + Pattern.quote(valid) + "]+");
            Log.d(TAG, "pattern: " + checkPattern.pattern());
            final Matcher m = checkPattern.matcher(text);
            if (m.find()) {
                final String illigal = m.group();
                Log.i(TAG, "invalid character: " + illigal);
                Toast.makeText(
                        this,
                        this.getString(R.string.log_error_char_notsendable)
                                + ": " + illigal, Toast.LENGTH_LONG).show();
                return false;
            }
        }

        ToggleButton v = (ToggleButton) this.findViewById(R.id.flashsms);
        final boolean flashSMS = (v.getVisibility() == View.VISIBLE)
                && v.isEnabled() && v.isChecked();
        final String defPrefix = p.getString(PREFS_DEFPREFIX, "+49");
        final String defSender = p.getString(PREFS_SENDER, "");

        final ConnectorCommand command = ConnectorCommand.send(nextMsgId(this),
                subconnector.getID(), defPrefix, defSender, tos, text, flashSMS);
        command.setCustomSender(lastCustomSender);
        command.setSendLater(lastSendLater);

        boolean sent = false;
        try {
            if (subconnector.hasFeatures(
                    SubConnectorSpec.FEATURE_MULTIRECIPIENTS)
                    || tos.length == 1) {
                Log.d(TAG, "text: " + text);
                Log.d(TAG, "to: ", tos);
                runCommand(this, connector, command);
            } else {
                ConnectorCommand cc;
                for (String t : tos) {
                    if (t.trim().length() < 1) {
                        continue;
                    }
                    cc = (ConnectorCommand) command.clone();
                    cc.setRecipients(t);
                    Log.d(TAG, "text: " + text);
                    Log.d(TAG, "to: ", tos);
                    runCommand(this, connector, cc);
                }
            }
            sent = true;
        } catch (Exception e) {
            Log.e(TAG, "error running command", e);
            Toast.makeText(this, R.string.error, Toast.LENGTH_LONG).show();
        }
        if (sent) {
            this.reset(false);
            if (p.getBoolean(PREFS_AUTOEXIT, false)) {
				try {
					Thread.sleep(SLEEP_BEFORE_EXIT);
				} catch (InterruptedException e) {
					Log.e(TAG, null, e);
				}
				this.finish();
			} else  {
				showInterstitial();
			}
			return true;
        }
        return false;
    }

	private void showInterstitial() {
		if  (prefsShowAds && prefsInterstitialAd && mInterstitialAd.isLoaded()) {
			mInterstitialAd.show();
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
		this.setButtons();
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
			Toast.makeText(this, R.string.error_sendlater_quater,
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
		this.setButtons();
	}

	/**
	 * Add or update a {@link ConnectorSpec}.
	 * 
	 * @param connector
	 *            connector
	 */
	static void addConnector(final ConnectorSpec connector,
			final ConnectorCommand command) {
		synchronized (CONNECTORS) {
			if (connector == null || connector.getPackage() == null
					|| connector.getName() == null) {
				return;
			}
			ConnectorSpec c = getConnectorByID(connector.getPackage());
			if (c != null) {
				Log.d(TAG, "update connector with id: " + c.getPackage());
				Log.d(TAG, "update connector with name: " + c.getName());
				c.setErrorMessage((String) null); // fix sticky error status
				short wasRunningStatus = c.getRunningStatus();
				c.update(connector);
                if (command.getType() == ConnectorCommand.TYPE_NONE
                        && wasRunningStatus != 0
                        && c.hasStatus(ConnectorSpec.STATUS_ENABLED)) {
                    // if this info is not a response to a command then preserve the running status
                    // unless we've learnt that this connector got disabled
                    Log.d(TAG, "preserving running status");
                    c.addStatus(wasRunningStatus);
                }
                if (me != null) {
					final SharedPreferences p = PreferenceManager
							.getDefaultSharedPreferences(me);
					final String em = c.getErrorMessage();
					if (em != null) {
						if (command.getType() != ConnectorCommand.TYPE_SEND) {
							Toast.makeText(me, em, Toast.LENGTH_LONG).show();
						}
					} else if (p.getBoolean(PREFS_SHOW_BALANCE_TOAST, false)
							&& !TextUtils.isEmpty(c.getBalance())) {
						Toast.makeText(me, c.getName() + ": " + c.getBalance(),
								Toast.LENGTH_LONG).show();
					}
				}
			} else {
				--newConnectorsExpected;
				final String pkg = connector.getPackage();
				final String name = connector.getName();
				if (connector.getSubConnectorCount() == 0 || name == null || pkg == null) {
					Log.w(TAG, "skipped adding defect connector: " + pkg);
					return;
				}
				Log.d(TAG, "add connector with id: " + pkg);
				Log.d(TAG, "add connector with name: " + name);
				boolean added = false;
				final int l = CONNECTORS.size();
				ConnectorSpec cs;
				try {
					for (int i = 0; i < l; i++) {
						cs = CONNECTORS.get(i);
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
				if (me != null) {
					final SharedPreferences p = PreferenceManager
							.getDefaultSharedPreferences(me);

					// update connectors balance if needed
					if (c.getBalance() == null
							&& c.isReady()
							&& !c.isRunning()
							&& c.hasCapabilities(ConnectorSpec.CAPABILITIES_UPDATE)
							&& p.getBoolean(PREFS_AUTOUPDATE, true)) {
						final String defPrefix = p.getString(PREFS_DEFPREFIX, "+49");
						final String defSender = p.getString(PREFS_SENDER, "");
						runCommand(me, c,
								ConnectorCommand.update(defPrefix, defSender));
					}
				}
			}

            if (me != null) {
                if (prefsConnectorSpec == null) {
                    final SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(me);
                    final String prefsConnectorID = p.getString(PREFS_CONNECTOR_ID, "");

                    if (prefsConnectorID.equals(connector.getPackage())) {
                        prefsConnectorSpec = connector;

                        final String prefsSubConnectorID = p.getString(PREFS_SUBCONNECTOR_ID, "");
                        prefsSubConnectorSpec = connector.getSubConnector(prefsSubConnectorID);
                        if (prefsSubConnectorSpec == null) {
                            prefsSubConnectorSpec = connector.getSubConnectors()[0];
                        }

                        me.setButtons();
                    }
                }

				final String b = c.getBalance();
				final String ob = c.getOldBalance();
				if (b != null && (ob == null || !b.equals(ob))) {
					me.updateBalance();
				}
				updateProgressBar();
				if (prefsConnectorSpec != null && prefsConnectorSpec.equals(c)) {
					me.setButtons();
				}
			}
            me.invalidateOptionsMenu();
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
        if (id == null) {
            return null;
        }
        ConnectorSpec c;
		synchronized (CONNECTORS) {
			final int l = CONNECTORS.size();
			for (int i = 0; i < l; i++) {
				c = CONNECTORS.get(i);
				if (id.equals(c.getPackage())) {
					return c;
				}
			}
		}
        synchronized (PSEUDO_CONNECTORS) {
            final int l = PSEUDO_CONNECTORS.size();
            for (int i = 0; i < l; i++) {
                c = PSEUDO_CONNECTORS.get(i);
                if (id.equals(c.getPackage())) {
                    return c;
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
     * @param isIncludePseudoConnectors
     *            whether pseudo connectors should be included
	 * @return {@link ConnectorSpec}s
	 */
	public static ConnectorSpec[] getConnectors(final int capabilities,
			final int status,
            final boolean isIncludePseudoConnectors) {
        final ArrayList<ConnectorSpec> ret = new ArrayList<ConnectorSpec>(
                CONNECTORS.size() + PSEUDO_CONNECTORS.size());
        ConnectorSpec c;
		synchronized (CONNECTORS) {
			final int l = CONNECTORS.size();
			for (int i = 0; i < l; i++) {
				c = CONNECTORS.get(i);
				if (c.hasCapabilities((short) capabilities)
						&& c.hasStatus((short) status)) {
					ret.add(c);
				}
			}
		}
        if (isIncludePseudoConnectors && ret.size() > 0) {
            // note: pseudo-connectors are only included if real connectors are also available
            //       because pseudo-connectors do not make sense without them
            synchronized (PSEUDO_CONNECTORS) {
                final int l = PSEUDO_CONNECTORS.size();
                for (int i = 0; i < l; i++) {
                    c = PSEUDO_CONNECTORS.get(i);
                    if (c.hasCapabilities((short) capabilities)
                            && c.hasStatus((short) status)) {
                        ret.add(c);
                    }
                }
            }
        }
        return ret.toArray(new ConnectorSpec[ret.size()]);
	}

	/**
	 * Get the number of connector applications that are installed on the
	 * system.
	 * 
	 * @return the number of connector applications
	 */
	private int getInstalledConnectorsCount() {
		final List<ResolveInfo> ri = this.getPackageManager()
				.queryBroadcastReceivers(
						new Intent(Connector.ACTION_CONNECTOR_UPDATE), 0);
		return ri.size();
	}

	/**
	 * Enables or disables indeterminate progress bar based on the current state.
	 */
	private static void updateProgressBar() {
		if (me != null) {
			boolean needProgressBar;
			if (newConnectorsExpected > 0) {
				Log.d(TAG, "expecting connector info: " + newConnectorsExpected);
				needProgressBar = true;
			} else {
				ConnectorSpec[] running = getConnectors(
						ConnectorSpec.CAPABILITIES_UPDATE,
						ConnectorSpec.STATUS_ENABLED | ConnectorSpec.STATUS_UPDATING,
                        false /*isIncludePseudoConnectors*/);
				Log.d(TAG, "running connectors: " + running.length);
				if (running.length != 0) {
					needProgressBar = true;
				} else {
					ConnectorSpec[] booting = getConnectors(
							ConnectorSpec.CAPABILITIES_BOOTSTRAP,
							ConnectorSpec.STATUS_ENABLED | ConnectorSpec.STATUS_BOOTSTRAPPING,
                            false /*isIncludePseudoConnectors*/);
					Log.d(TAG, "booting connectors: " + booting.length);
					needProgressBar = (booting.length != 0);
				}
			}
			me.setSupportProgressBarIndeterminateVisibility(needProgressBar);
		}
	}

	/**
	 * Generates unique id for the next message.
	 * 
	 * @param context
	 *            Current context
	 * @return message id
	 */
	private static synchronized long nextMsgId(final Context context) {
		final SharedPreferences p = PreferenceManager
				.getDefaultSharedPreferences(context);
		long nextMsgId = p.getLong(PREFS_LAST_MSG_ID, 0) + 1;
		SharedPreferences.Editor editor = p.edit();
		editor.putLong(PREFS_LAST_MSG_ID, nextMsgId);
		editor.apply();
		return nextMsgId;
	}
}
