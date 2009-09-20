package de.ub0r.android.andGMXsms;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.cookie.Cookie;
import org.apache.http.cookie.CookieOrigin;
import org.apache.http.cookie.MalformedCookieException;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.cookie.BrowserCompatSpec;
import org.apache.http.impl.cookie.CookieSpecBase;
import org.apache.http.message.BasicNameValuePair;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
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
public class AndGMXsms extends Activity {

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
	public static String prefsMail;
	/** Preferences: username. */
	public static String prefsUser;
	/** Preferences: user's password - gmx. */
	public static String prefsPasswordGMX;
	/** Preferences: user's password - o2. */
	public static String prefsPasswordO2;
	/** Preferences: user's phonenumber. */
	public static String prefsSender;
	/** Preferences: default prefix. */
	public static String prefsDefPrefix;
	/** Preferences: ready for gmx? */
	public static boolean prefsReadyGMX = false;
	/** Preferences: ready for o2? */
	public static boolean prefsReadyO2 = false;
	/** Remaining free sms. */
	public static String remFree = null;
	/** Preferences: use softkeys. */
	public static boolean prefsSoftKeys = false;
	/** Preferences: enable gmx. */
	public static boolean prefsEnableGMX = false;
	/** Preferences: enable o2. */
	public static boolean prefsEnableO2 = false;
	/** Preferences: hide ads. */
	public static boolean prefsNoAds = false;

	/** Array of md5(prefsSender) for which no ads should be displayed. */
	private static final String[] noAdHash = {
			"2986b6d93053a53ff13008b3015a77ff", // me
			"f6b3b72300e918436b4c4c9fdf909e8c" // jörg s.
	};

	/** Public Dialog ref. */
	public static Dialog dialog = null;
	/** Dialog String. */
	public static String dialogString = null;

	/** true if preferences got opened. */
	private static boolean doPreferences = false;

	/** Dialog: about. */
	private static final int DIALOG_ABOUT = 0;
	/** Dialog: help. */
	private static final int DIALOG_HELP = 1;

	/** Message for logging. **/
	public static final int MESSAGE_LOG = 0;
	/** Message for update free sms count. **/
	public static final int MESSAGE_FREECOUNT = 1;
	/** Message to send. */
	public static final int MESSAGE_SEND = 2;
	/** Message to bootstrap. */
	public static final int MESSAGE_BOOTSTRAP = 3;
	/** Message to open settings. */
	public static final int MESSAGE_SETTINGS = 4;
	/** Message to reset data. */
	public static final int MESSAGE_RESET = 5;
	/** Message check prefsReady. */
	public static final int MESSAGE_PREFSREADY = 6;
	/** Message display ads. */
	public static final int MESSAGE_DISPLAY_ADS = 7;

	/** Menu: send via GMX. */
	private static final int MENU_SEND_GMX = 1;
	/** Menu: send via O2. */
	private static final int MENU_SEND_O2 = 2;
	/** Menu: cancel. */
	private static final int MENU_CANCEL = 3;

	/** Connector type: GMX. */
	private static final short CONNECTOR_GMX = 0;
	/** Connector type: O2. */
	private static final short CONNECTOR_O2 = 1;

	/** Persistent Message store. */
	private static String lastMsg = null;
	/** Persistent Recipient store. */
	private static String lastTo = null;

	/** Remaining free sms at gmx. */
	public static int smsGMXfree = 0;
	/** Free sms / month at gmx. */
	public static int smsGMXlimit = 0;
	/** Remaining free sms at o2. */
	public static int smsO2free = 0;
	/** Free sms / month at o2. */
	public static int smsO2limit = 0;

	/** Text's label. */
	private TextView textLabel;

	/** Resource @string/text__. */
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
		this.reloadPrefs();

		lastTo = this.preferences.getString(PREFS_TO, "");
		lastMsg = this.preferences.getString(PREFS_TEXT, "");

		// register Listener
		((Button) this.findViewById(R.id.send_gmx))
				.setOnClickListener(this.runSendGMX);
		((Button) this.findViewById(R.id.send_o2))
				.setOnClickListener(this.runSendO2);
		((Button) this.findViewById(R.id.cancel))
				.setOnClickListener(this.cancel);

		this.textLabelRef = this.getResources().getString(R.string.text__);
		this.textLabel = (TextView) this.findViewById(R.id.text_);
		((EditText) this.findViewById(R.id.text))
				.addTextChangedListener(this.textWatcher);

		((TextView) this.findViewById(R.id.freecount))
				.setOnClickListener(this.runGetFree);

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
			if (prefsEnableGMX) {
				String[] params = new String[ConnectorGMX.IDS_BOOTSTR];
				params[ConnectorGMX.ID_MAIL] = prefsMail;
				params[ConnectorGMX.ID_PW] = prefsPasswordGMX;
				new ConnectorGMX().execute(params);
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
		for (String h : noAdHash) {
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

	/** Listener for launching a get-free-sms-count-thread. */
	private OnClickListener runGetFree = new OnClickListener() {
		public void onClick(final View v) {
			if (prefsEnableGMX) {
				new ConnectorGMX().execute((String) null);
			}
			if (prefsEnableO2) {
				new ConnectorO2().execute((String) null);
			}
		}
	};

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
			this.send(CONNECTOR_GMX);
			return true;
		case MENU_SEND_O2:
			this.send(CONNECTOR_O2);
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
			Button button = (Button) myDialog.findViewById(R.id.btn_donate);
			button.setOnClickListener(new OnClickListener() {
				public void onClick(final View view) {
					Uri uri = Uri.parse(AndGMXsms.this
							.getString(R.string.donate_url));
					AndGMXsms.this.startActivity(new Intent(Intent.ACTION_VIEW,
							uri));
				}
			});
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
	 * Preferences.
	 * 
	 * @author flx
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
	 * AndGMXsms's MessageHandler.
	 * 
	 * @author flx
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
					AndGMXsms.remFree = AndGMXsms.smsGMXfree + " / "
							+ AndGMXsms.smsGMXlimit;
				}
				if (AndGMXsms.prefsEnableO2) {
					if (AndGMXsms.remFree.length() > 0) {
						AndGMXsms.remFree += " - ";
					}
					AndGMXsms.remFree += AndGMXsms.smsO2free;
					if (AndGMXsms.smsO2limit > 0) {
						AndGMXsms.remFree += " / " + AndGMXsms.smsO2limit;
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
			case MESSAGE_SEND:
				new ConnectorGMX().execute((String[]) msg.obj);
				return;
			case MESSAGE_BOOTSTRAP:
				if (prefsEnableGMX) {
					new ConnectorGMX().execute((String[]) msg.obj);
				}
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
		String[] params = new String[numbers.length + 1];
		params[ConnectorGMX.ID_TEXT] = text;
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
			t = AndGMXsms.cleanRecipient(t);
			params[i + 1] = t;
			numbers[i] = null;
		}
		// start a Connector Thread
		switch (connector) {
		case CONNECTOR_GMX:
			new ConnectorGMX().execute(params);
			break;
		case CONNECTOR_O2:
			new ConnectorO2().execute(params);
			break;
		default:
			break;
		}
	}

	/** OnClickListener for sending the sms. */
	private OnClickListener runSendGMX = new OnClickListener() {
		public void onClick(final View v) {
			AndGMXsms.this.send(CONNECTOR_GMX);
		}
	};

	/** OnClickListener for sending the sms. */
	private OnClickListener runSendO2 = new OnClickListener() {
		public void onClick(final View v) {
			AndGMXsms.this.send(CONNECTOR_O2);
		}
	};

	/** OnClickListener for cancel. */
	private OnClickListener cancel = new OnClickListener() {
		public void onClick(final View v) {
			// reset input fields
			AndGMXsms.this.reset();
		}
	};

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
	 * Save Message to internal database.
	 * 
	 * @param reciepients
	 *            reciepients. first entry is skipped!
	 * @param text
	 *            text of message.
	 */
	public static final void saveMessage(final String[] reciepients,
			final String text) {
		for (int i = 1; i < reciepients.length; i++) {
			if (reciepients[i] == null || reciepients[i].length() == 0) {
				continue; // skip empty recipients
			}
			// save sms to content://sms/sent
			ContentValues values = new ContentValues();
			values.put(ConnectorGMX.ADDRESS, reciepients[i]);
			// values.put(DATE, "1237080365055");
			values.put(ConnectorGMX.READ, 1);
			// values.put(STATUS, -1);
			values.put(ConnectorGMX.TYPE, ConnectorGMX.MESSAGE_TYPE_SENT);
			values.put(ConnectorGMX.BODY, text);
			// Uri inserted =
			AndGMXsms.me.getContentResolver().insert(
					Uri.parse("content://sms/sent"), values);
		}
	}

	/**
	 * Read in data from Stream into String.
	 * 
	 * @param is
	 *            stream
	 * @return String
	 * @throws IOException
	 *             IOException
	 */
	public static final String stream2String(final InputStream is)
			throws IOException {
		BufferedReader bufferedReader = new BufferedReader(
				new InputStreamReader(is));
		StringBuilder data = new StringBuilder();
		String line = null;
		while ((line = bufferedReader.readLine()) != null) {
			data.append(line + "\n");
		}
		bufferedReader.close();
		return data.toString();
	}

	/**
	 * Get a fresh HTTP-Connection.
	 * 
	 * @param url
	 *            url to open
	 * @param cookies
	 *            cookies to transmit
	 * @param postData
	 *            post data
	 * @param userAgent
	 *            user agent
	 * @return the connection
	 * @throws IOException
	 * @throws ClientProtocolException
	 */
	static HttpResponse getHttpClient(final String url,
			final ArrayList<Cookie> cookies,
			final ArrayList<BasicNameValuePair> postData, final String userAgent)
			throws ClientProtocolException, IOException {
		HttpClient client = new DefaultHttpClient();
		HttpRequestBase request;
		if (postData == null) {
			request = new HttpGet(url);
		} else {
			request = new HttpPost(url);
			((HttpPost) request).setEntity(new UrlEncodedFormEntity(postData));
		}
		request.setHeader("User-Agent", userAgent);

		if (cookies != null && cookies.size() > 0) {
			CookieSpecBase cookieSpecBase = new BrowserCompatSpec();
			for (Header cookieHeader : cookieSpecBase.formatCookies(cookies)) {
				// Setting the cookie
				request.setHeader(cookieHeader);
			}
		}
		return client.execute(request);
	}

	/**
	 * Update cookies from response.
	 * 
	 * @param cookies
	 *            old cookie list
	 * @param headers
	 *            headers from response
	 * @param url
	 *            requested url
	 * @throws URISyntaxException
	 *             malformed uri
	 * @throws MalformedCookieException
	 *             malformed cookie
	 */
	static void updateCookies(final ArrayList<Cookie> cookies,
			final Header[] headers, final String url)
			throws URISyntaxException, MalformedCookieException {
		final URI uri = new URI(url);
		int port = uri.getPort();
		if (port < 0) {
			if (url.startsWith("https")) {
				port = 443;
			} else {
				port = 80;
			}
		}
		CookieOrigin origin = new CookieOrigin(uri.getHost(), port, uri
				.getPath(), false);
		CookieSpecBase cookieSpecBase = new BrowserCompatSpec();
		for (Header header : headers) {
			for (Cookie cookie : cookieSpecBase.parse(header, origin)) {
				// THE cookie
				String name = cookie.getName();
				String value = cookie.getValue();
				if (value == null || value.equals("")) {
					continue;
				}
				for (Cookie c : cookies) {
					if (name.equals(c.getName())) {
						cookies.remove(c);
						cookies.add(cookie);
						name = null;
						break;
					}
				}
				if (name != null) {
					cookies.add(cookie);
				}
			}
		}
	}

	public static String md5(final String s) {
		try {
			// Create MD5 Hash
			MessageDigest digest = java.security.MessageDigest
					.getInstance("MD5");
			digest.update(s.getBytes());
			byte messageDigest[] = digest.digest();
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
			e.printStackTrace();
		}
		return "";
	}
}
