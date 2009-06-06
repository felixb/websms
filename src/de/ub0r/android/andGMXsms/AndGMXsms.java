package de.ub0r.android.andGMXsms;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Contacts.Phones;
import android.provider.Contacts.PhonesColumns;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
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
	/** Preference's name: username. */
	private static final String PREFS_USER = "user";
	/** Preference's name: user's password. */
	private static final String PREFS_PASSWORD = "password";
	/** Preference's name: user's phonenumber. */
	private static final String PREFS_SENDER = "sender";
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

	/** Persistent Message store. */
	private static String lastMsg = null;
	/** Persistent Receiver store. */
	private static String lastTo = null;

	/** Intent id. */
	static final int PICK_CONTACT_REQUEST = 0;

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
	Handler messageHandler;

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
		prefsUser = settings.getString(PREFS_USER, "");
		prefsPassword = settings.getString(PREFS_PASSWORD, "");
		prefsSender = settings.getString(PREFS_SENDER, "");

		// register Listener
		((Button) this.findViewById(R.id.send))
				.setOnClickListener(this.runSend);
		((Button) this.findViewById(R.id.cancel))
				.setOnClickListener(this.cancel);
		((ImageButton) this.findViewById(R.id.contacts))
				.setOnClickListener(this.contacts);

		this.textLabelRef = this.getResources().getString(R.string.text__);
		this.textLabel = (TextView) this.findViewById(R.id.text_);
		((EditText) this.findViewById(R.id.text))
				.addTextChangedListener(this.textWatcher);

		TextView tw = (TextView) this.findViewById(R.id.freecount);
		tw.setOnClickListener(this.runGetFree);
		tw.setText(tw.getText()
				+ " "
				+ AndGMXsms.this.getResources().getString(
						R.string.click_for_update));
	}

	/** Called on activity resume. */
	@Override
	protected final void onResume() {
		super.onResume();
		// set free sms count
		if (remFree != null) {
			TextView tw = (TextView) this.findViewById(R.id.freecount);
			tw.setText(this.getResources().getString(R.string.free_) + " "
					+ remFree);
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

		// check prefs
		if (prefsUser.equals("") || prefsPassword.equals("")
				|| prefsSender.equals("")) {
			prefsReady = false;
			this
					.log(this.getResources().getString(
							R.string.log_empty_settings));
		} else {
			prefsReady = true;
		}

		// enable/disable buttons
		((Button) this.findViewById(R.id.send)).setEnabled(prefsReady);

		// reload text/receiver from local store
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
	 * Resets persistent store.
	 */
	public final void reset() {
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

	/** OnClickListener for sending the sms. */
	private OnClickListener runSend = new OnClickListener() {
		public void onClick(final View v) {
			// fetch text/receiver
			EditText et = (EditText) AndGMXsms.this.findViewById(R.id.to);
			String to = et.getText().toString();
			et = (EditText) AndGMXsms.this.findViewById(R.id.text);
			String text = et.getText().toString();
			// fix number prefix
			if (to != null && text != null && to.length() > 2
					&& !text.equals("")) {

				if (!to.startsWith("+")) {
					if (to.startsWith("00")) {
						to = "+" + to.substring(2);
					} else if (to.startsWith("0")) {
						to = AndGMXsms.prefsPrefix() + to.substring(1);
					}
				}
				// start a Connector Thread
				String[] params = new String[Connector.IDS_SEND];
				params[Connector.ID_TEXT] = text;
				params[Connector.ID_TO] = to;
				Message.obtain(AndGMXsms.me.messageHandler,
						AndGMXsms.MESSAGE_SEND, params).sendToTarget();
			}
		}
	};

	/** OnClickListener for cancel. */
	private OnClickListener cancel = new OnClickListener() {
		public void onClick(final View v) {
			// reset input fields
			AndGMXsms.this.reset();
		}
	};

	/** OnClickListener for launching phonebook. */
	private OnClickListener contacts = new OnClickListener() {
		public void onClick(final View v) {
			AndGMXsms.this.startActivityForResult(new Intent(
					Intent.ACTION_PICK, Phones.CONTENT_URI),
					PICK_CONTACT_REQUEST);
		}
	};

	/**
	 * Handles ActivityResults from Phonebook.
	 * 
	 * @param requestCode
	 *            Intent id
	 * @param resultCode
	 *            result code
	 * @param data
	 *            data
	 */
	protected final void onActivityResult(final int requestCode,
			final int resultCode, final Intent data) {
		if (requestCode == PICK_CONTACT_REQUEST) {
			if (resultCode == RESULT_OK) {
				// get cursor
				Cursor cur = this
						.managedQuery(data.getData(),
								new String[] { PhonesColumns.NUMBER }, null,
								null, null);
				// get EditText
				EditText et = (EditText) this.findViewById(R.id.to);
				// fill EditText if data is available
				if (cur.moveToFirst()) {
					String targetNumber = cur.getString(cur
							.getColumnIndex(PhonesColumns.NUMBER));
					// cleanup number
					targetNumber = targetNumber.replace(" ", "").replace("-",
							"").replace(".", "").replace("(", "").replace(")",
							"");
					et.setText(targetNumber);
					lastTo = targetNumber;
				} else {
					et.setText("");
					lastTo = "";
				}
			}
		}
	}
}
