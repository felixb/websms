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

public class AndGMXsms extends Activity {

	static AndGMXsms me;
	public static final String PREFS_NAME = "andGMXsmsPrefs";
	private static final String PREFS_USER = "user";
	private static final String PREFS_PASSWORD = "password";
	private static final String PREFS_SENDER = "sender";
	public static String prefs_user;
	public static String prefs_password;
	public static String prefs_sender;
	public static boolean prefs_ready = false;

	public static String prefs_prefix() {
		if (prefs_sender.length() < 3) {
			return prefs_sender;
		}
		return AndGMXsms.prefs_sender.substring(0, 3);
	}

	private TextView log;
	private static String logString = "";
	public static Connector connector = null;
	public Handler messageHandler;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		me = this;
		this.setContentView(R.layout.main);

		this.log = (TextView) this.findViewById(R.id.log);
		this.messageHandler = new MessageHandler();

		// Restore preferences
		SharedPreferences settings = this.getSharedPreferences(PREFS_NAME, 0);
		prefs_user = settings.getString(PREFS_USER, "");
		prefs_password = settings.getString(PREFS_PASSWORD, "");
		prefs_sender = settings.getString(PREFS_SENDER, "");

		Button button = (Button) this.findViewById(R.id.composer);
		button.setOnClickListener(this.openComposer);
		button = (Button) this.findViewById(R.id.getfree);
		button.setOnClickListener(this.runGetFree);
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (this.log != null && logString != null) {
			this.log.setText(logString);
		}

		if (prefs_user.equals("") || prefs_password.equals("")
				|| prefs_sender.equals("")) {
			prefs_ready = false;
			this.lognl(this.getResources().getString(
					R.string.log_empty_settings));
		} else {
			if (!prefs_ready) {
				this.log.setText("");
			}
			prefs_ready = true;
		}

		Button button = (Button) this.findViewById(R.id.composer);
		button.setEnabled(prefs_ready);
		button = (Button) this.findViewById(R.id.getfree);
		button.setEnabled(prefs_ready);
	}

	@Override
	protected void onStop() {
		super.onStop();

		// Save user preferences. We need an Editor object to
		// make changes. All objects are from android.context.Context
		SharedPreferences settings = this.getSharedPreferences(PREFS_NAME, 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString(PREFS_USER, prefs_user);
		editor.putString(PREFS_PASSWORD, prefs_password);
		editor.putString(PREFS_SENDER, prefs_sender);
		// Don't forget to commit your edits!!!
		editor.commit();
	}

	// Create an anonymous implementation of OnClickListener
	private OnClickListener openComposer = new OnClickListener() {
		public void onClick(final View v) {
			AndGMXsms.this.startActivity(new Intent(AndGMXsms.this, Composer.class));
		}
	};

	// Create an anonymous implementation of OnClickListener
	private OnClickListener runGetFree = new OnClickListener() {
		public void onClick(final View v) {
			connector = new Connector();
			connector.start();
		}
	};

	/**
	 * Open menu.
	 */
	public boolean onCreateOptionsMenu(final Menu menu) {
		MenuInflater inflater = this.getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		return true;
	}

	/* Handles item selections */
	public boolean onOptionsItemSelected(final MenuItem item) {
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
		}
		return false;
	}

	public void log(final String text) {
		this.log.append(text);
		logString += text;
	}

	public void lognl(final String text) {
		this.log.append(text + "\n");
		logString += text + "\n";
	}

}