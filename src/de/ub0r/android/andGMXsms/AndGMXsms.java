package de.ub0r.android.andGMXsms;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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

/**
 * Main Activity.
 * 
 * @author flx
 */
public class AndGMXsms extends Activity {

	/** Static reference to running Activity. */
	static AndGMXsms me;
	/** Preference's name. */
	public static final String PREFS_NAME = "andGMXsmsPrefs";
	/** Preference's name: mail. */
	private static final String PREFS_MAIL = "mail";
	/** Preference's name: username. */
	private static final String PREFS_USER = "user";
	/** Preference's name: user's password. */
	private static final String PREFS_PASSWORD = "password";
	/** Preference's name: user's phonenumber. */
	private static final String PREFS_SENDER = "sender";
	/** Preferences: mail. */
	public static String prefsMail;
	/** Preferences: username. */
	public static String prefsUser;
	/** Preferences: user's password. */
	public static String prefsPassword;
	/** Preferences: user's phonenumber. */
	public static String prefsSender;
	/** Preferences ready? */
	public static boolean prefsReady = false;
	/** Remaining free sms. */
	public static String remFree = null;

	/** Length of a prefix. */
	private static final int PREFIX_LEN = 3;

	/** Public Dialog ref. */
	public static Dialog dialog = null;
	/** Dialog String. */
	public static String dialogString = null;

	/** Public Connector. */
	public static AsyncTask<String, Boolean, Boolean> connector;

	/** Dialog: about. */
	private static final int DIALOG_ABOUT = 0;

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

	/** Persistent Message store. */
	private static String lastMsg = null;
	/** Persistent Recipient store. */
	private static String lastTo = null;

	/** Text's label. */
	private TextView textLabel;

	/** Resource @string/text__. */
	private String textLabelRef;

	/**
	 * Preferences: user's default prefix.
	 * 
	 * @return user's default prefix
	 */
	public static String prefsPrefix() {
		if (prefsSender.length() < PREFIX_LEN) {
			return prefsSender;
		}
		return AndGMXsms.prefsSender.substring(0, PREFIX_LEN);
	}

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
		SharedPreferences settings = this.getSharedPreferences(PREFS_NAME, 0);
		prefsMail = settings.getString(PREFS_MAIL, "");
		prefsUser = settings.getString(PREFS_USER, "");
		prefsPassword = settings.getString(PREFS_PASSWORD, "");
		prefsSender = settings.getString(PREFS_SENDER, "");

		// register Listener
		((Button) this.findViewById(R.id.send))
				.setOnClickListener(this.runSend);
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

		this.checkReady();

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
	}

	/**
	 * Check if prefs are set.
	 */
	private void checkReady() {
		// check prefs
		if (prefsMail.length() == 0 || prefsUser.length() == 0
				|| prefsPassword.length() == 0 || prefsSender.length() == 0) {
			prefsReady = false;
			if (!Connector.inBootstrap) {
				this.log(this.getResources().getString(
						R.string.log_empty_settings));
			}
		} else {
			prefsReady = true;
		}

		// enable/disable buttons
		((Button) this.findViewById(R.id.send)).setEnabled(prefsReady);
	}

	/**
	 * Resets persistent store.
	 */
	private void reset() {
		((EditText) this.findViewById(R.id.text)).setText("");
		((EditText) this.findViewById(R.id.to)).setText("");
		lastMsg = null;
		lastTo = null;
	}

	/** Save prefs. */
	final void saveSettings() {
		// save user preferences
		SharedPreferences settings = this.getSharedPreferences(PREFS_NAME, 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString(PREFS_MAIL, prefsMail);
		editor.putString(PREFS_USER, prefsUser);
		editor.putString(PREFS_PASSWORD, prefsPassword);
		editor.putString(PREFS_SENDER, prefsSender);
		// commit changes
		editor.commit();
	}

	/** Listener for launching a get-free-sms-count-thread. */
	private OnClickListener runGetFree = new OnClickListener() {
		public void onClick(final View v) {
			connector = new Connector().execute((String) null);
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
			this.startActivity(new Intent(this, Settings.class));
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
				AndGMXsms.remFree = (String) msg.obj;
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
				AndGMXsms.connector = new Connector()
						.execute((String[]) msg.obj);
				return;
			case MESSAGE_BOOTSTRAP:
				AndGMXsms.connector = new Connector()
						.execute((String[]) msg.obj);
				return;
			case MESSAGE_SETTINGS:
				AndGMXsms.this.startActivity(new Intent(AndGMXsms.this,
						Settings.class));
			case MESSAGE_RESET:
				AndGMXsms.this.reset();
				return;
			case MESSAGE_PREFSREADY:
				AndGMXsms.this.checkReady();
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

	/** OnClickListener for sending the sms. */
	private OnClickListener runSend = new OnClickListener() {
		public void onClick(final View v) {
			// fetch text/recipient
			String to = ((EditText) AndGMXsms.this.findViewById(R.id.to))
					.getText().toString();
			String text = ((EditText) AndGMXsms.this.findViewById(R.id.text))
					.getText().toString();
			if (to.length() == 0 || text.length() == 0) {
				return;
			}
			String[] numbers = AndGMXsms.parseReciepients(to);
			String[] params = new String[numbers.length + 1];
			params[Connector.ID_TEXT] = text;
			// fix number prefix
			for (int i = 0; i < numbers.length; i++) {
				String t = numbers[i];
				if (t != null) {
					if (!t.startsWith("+")) {
						if (t.startsWith("00")) {
							t = "+" + t.substring(2);
						} else if (t.startsWith("0")) {
							t = AndGMXsms.prefsPrefix() + t.substring(1);
						}
					}
				}
				t = AndGMXsms.cleanRecipient(t);
				params[i + 1] = t;
				numbers[i] = null;
			}
			// start a Connector Thread
			AndGMXsms.connector = new Connector().execute(params);
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
	public final static String cleanRecipient(final String recipient) {
		if (recipient == null) {
			return null;
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
}
