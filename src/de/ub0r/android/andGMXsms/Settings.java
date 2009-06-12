package de.ub0r.android.andGMXsms;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

/**
 * Simple Activity for setting preferences.
 * 
 * @author flx
 */
public class Settings extends Activity {
	/** Local pref. for mail. */
	private static String prMail;
	/** Local pref. for user's password. */
	private static String prPassword;

	/** Dialog: help. */
	private static final int DIALOG_HELP = 0;

	/**
	 * Called when the activity is first created.
	 * 
	 * @param savedInstanceState
	 *            default param
	 */
	@Override
	public final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// inflate XML
		this.setContentView(R.layout.settings);
		// register Listener
		((Button) this.findViewById(R.id.help)).setOnClickListener(this.help);
		((Button) this.findViewById(R.id.ok)).setOnClickListener(this.ok);
		((Button) this.findViewById(R.id.cancel))
				.setOnClickListener(this.cancel);
	}

	/** Called on activity resume. */
	@Override
	public final void onResume() {
		super.onResume();
		// load global prefs if local prefs are empty
		if (prMail == null) {
			prMail = AndGMXsms.prefsMail;
		}
		if (prPassword == null) {
			prPassword = AndGMXsms.prefsPassword;
		}

		// reload EditTexts' text from local prefs
		((EditText) this.findViewById(R.id.mail)).setText(prMail);
		((EditText) this.findViewById(R.id.password)).setText(prPassword);
	}

	/** Called on activity pause. */
	@Override
	public final void onPause() {
		super.onPause();
		// save TextEdits' text to local prefs
		prMail = ((EditText) this.findViewById(R.id.mail)).getText().toString();
		prPassword = ((EditText) this.findViewById(R.id.password)).getText()
				.toString();
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
		case DIALOG_HELP:
			myDialog = new Dialog(this);
			myDialog.setContentView(R.layout.help);
			myDialog.setTitle(this.getResources().getString(R.string.help));
			break;
		default:
			myDialog = null;
		}
		return myDialog;
	}

	/** OnClickListener for launching 'help'. */
	private OnClickListener help = new OnClickListener() {
		@Override
		public void onClick(final View v) {
			Settings.this.showDialog(DIALOG_HELP);
		}
	};

	/** OnClickListener listening for 'ok'. */
	private OnClickListener ok = new OnClickListener() {
		@Override
		public void onClick(final View v) {
			// save prefs from TextEdits
			prMail = ((EditText) Settings.this.findViewById(R.id.mail))
					.getText().toString();
			prPassword = ((EditText) Settings.this.findViewById(R.id.password))
					.getText().toString();

			String[] params = new String[Connector.IDS_BOOTSTR];
			params[Connector.ID_MAIL] = prMail;
			params[Connector.ID_PW] = prPassword;
			if (params[Connector.ID_MAIL].length() < 1
					|| params[Connector.ID_PW].length() < 1) {
				return;
			}
			params[Connector.ID_BOOTSTRAP_NULL] = null;
			AndGMXsms.me.sendMessage(AndGMXsms.MESSAGE_BOOTSTRAP, params);

			// exit activity
			Settings.this.finish();
		}
	};

	/** OnClickListener listening for 'cancel'. */
	private OnClickListener cancel = new OnClickListener() {
		@Override
		public void onClick(final View v) {
			// reload prefs from global
			prMail = AndGMXsms.prefsMail;
			prPassword = AndGMXsms.prefsPassword;

			// exit activity
			Settings.this.finish();
			try {
				// reload prefs into TextEdits
				((EditText) Settings.this.findViewById(R.id.mail))
						.setText(prMail);
				((EditText) Settings.this.findViewById(R.id.password))
						.setText(prPassword);
			} catch (Exception e) {
				// nothing to do
			}
		}
	};

	/**
	 * Reset inner pref. store.
	 */
	public static void reset() {
		prMail = null;
		prPassword = null;
	}
}
