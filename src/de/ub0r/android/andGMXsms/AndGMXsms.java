package de.ub0r.android.andGMXsms;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

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

	/** Length of a prefix. */
	private static final int PREFIX_LEN = 3;

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

	/** Log. */
	private TextView log;
	/** Local log store. */
	private static String logString = "";
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
		// save ref to log
		this.log = (TextView) this.findViewById(R.id.log);
		// register MessageHandler
		this.messageHandler = new MessageHandler();

		// Restore preferences
		SharedPreferences settings = this.getSharedPreferences(PREFS_NAME, 0);
		prefsUser = settings.getString(PREFS_USER, "");
		prefsPassword = settings.getString(PREFS_PASSWORD, "");
		prefsSender = settings.getString(PREFS_SENDER, "");

		// register Listener
		Button button = (Button) this.findViewById(R.id.composer);
		button.setOnClickListener(this.openComposer);
		button = (Button) this.findViewById(R.id.getfree);
		button.setOnClickListener(this.runGetFree);
	}

	/** Called on activity resume. */
	@Override
	protected final void onResume() {
		super.onResume();
		// restore log
		if (this.log != null && logString != null) {
			this.log.setText(logString);
		}

		// check prefs
		if (prefsUser.equals("") || prefsPassword.equals("")
				|| prefsSender.equals("")) {
			prefsReady = false;
			this.lognl(this.getResources().getString(
					R.string.log_empty_settings));
		} else {
			if (!prefsReady) {
				this.log.setText("");
			}
			prefsReady = true;
		}

		// enable/disable buttons
		Button button = (Button) this.findViewById(R.id.composer);
		button.setEnabled(prefsReady);
		button = (Button) this.findViewById(R.id.getfree);
		button.setEnabled(prefsReady);
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

	/** Listener for launching Composer. */
	private OnClickListener openComposer = new OnClickListener() {
		public void onClick(final View v) {
			AndGMXsms.this.startActivity(new Intent(AndGMXsms.this,
					Composer.class));
		}
	};

	/** Listener for launching a get-free-sms-count-thread. */
	private OnClickListener runGetFree = new OnClickListener() {
		public void onClick(final View v) {
			new Connector().execute((String) null);
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
		case R.id.item_clearlog: // clear logs
			TextView tw = (TextView) this.findViewById(R.id.log);
			tw.setText("");
			logString = "";
			return true;
		case R.id.item_about: // start about activity
			this.startActivity(new Intent(this, About.class));
			return true;
		case R.id.item_settings: // start settings activity
			this.startActivity(new Intent(this, Settings.class));
			return true;
		default:
			return false;
		}
	}

	/**
	 * Log text.
	 * 
	 * @param text
	 *            text
	 */
	public final void log(final String text) {
		this.log.append(text);
		logString += text;
	}

	/**
	 * Log text + \n.
	 * 
	 * @param text
	 *            text
	 */
	public final void lognl(final String text) {
		this.log.append(text + "\n");
		logString += text + "\n";
	}

}
