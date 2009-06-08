package de.ub0r.android.andGMXsms;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

/**
 * Simple Activity for setting preferences.
 * 
 * @author flx
 */
public class Settings extends Activity {
	/** Local pref. for user. */
	private static String prUser;
	/** Local pref. for user's password. */
	private static String prPassword;
	/** Local pref. for user's phonenumber. */
	private static String prSender;

	/** Dialog: help. */
	private static final int DIALOG_HELP = 0;
	/** Dialog: bootstrap. */
	private static final int DIALOG_BOOTSTRAP = 1;

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
		((Button) this.findViewById(R.id.bootstrap))
				.setOnClickListener(this.bootstrap);
	}

	/** Called on activity resume. */
	@Override
	public final void onResume() {
		super.onResume();
		// load global prefs if local prefs are empty
		if (prUser == null) {
			prUser = AndGMXsms.prefsUser;
		}
		if (prPassword == null) {
			prPassword = AndGMXsms.prefsPassword;
		}
		if (prSender == null) {
			prSender = AndGMXsms.prefsSender;
		}

		// reload EditTexts' text from local prefs
		((EditText) this.findViewById(R.id.user)).setText(prUser);
		((EditText) this.findViewById(R.id.password)).setText(prPassword);
		((EditText) this.findViewById(R.id.sender)).setText(prSender);

		// Start bootstrap if needed.
		if (prUser.length() < 1 && prPassword.length() < 1
				&& prSender.length() < 1) {
			this.showDialog(DIALOG_BOOTSTRAP);
		}
	}

	/** Called on activity pause. */
	@Override
	public final void onPause() {
		super.onPause();
		// save TextEdits' text to local prefs
		prUser = ((EditText) this.findViewById(R.id.user)).getText().toString();
		prPassword = ((EditText) this.findViewById(R.id.password)).getText()
				.toString();
		prSender = ((EditText) this.findViewById(R.id.sender)).getText()
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
		case DIALOG_BOOTSTRAP:
			myDialog = new Dialog(this);
			myDialog.setContentView(R.layout.bootstrap);
			myDialog.setTitle(this.getResources()
					.getString(R.string.bootstrap_));
			((Button) myDialog.findViewById(R.id.bootstrap_ok))
					.setOnClickListener(new OnClickListener() {
						public void onClick(final View view) {
							String[] params = new String[Connector.IDS_BOOTSTR];
							params[Connector.ID_MAIL] = ((TextView) ((View) view
									.getParent())
									.findViewById(R.id.bootstrap_mail))
									.getText().toString();
							params[Connector.ID_PW] = ((TextView) ((View) view
									.getParent())
									.findViewById(R.id.bootstrap_pw)).getText()
									.toString();
							if (params[Connector.ID_MAIL].length() < 1
									|| params[Connector.ID_PW].length() < 1) {
								return;
							}
							params[Connector.ID_BOOTSTRAP_NULL] = null;
							AndGMXsms.me.sendMessage(
									AndGMXsms.MESSAGE_BOOTSTRAP, params);
							Settings.this.dismissDialog(DIALOG_BOOTSTRAP);
							Settings.this.finish();
						}
					});
			((Button) myDialog.findViewById(R.id.bootstrap_cancel))
					.setOnClickListener(new OnClickListener() {
						public void onClick(final View view) {
							Settings.this.dismissDialog(DIALOG_BOOTSTRAP);
						}
					});
			((Button) myDialog.findViewById(R.id.bootstrap_help))
					.setOnClickListener(new OnClickListener() {
						public void onClick(final View view) {
							Settings.this.showDialog(DIALOG_HELP);
						}
					});
			break;
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
			prUser = ((EditText) Settings.this.findViewById(R.id.user))
					.getText().toString();
			prPassword = ((EditText) Settings.this.findViewById(R.id.password))
					.getText().toString();
			prSender = ((EditText) Settings.this.findViewById(R.id.sender))
					.getText().toString();

			// save prefs to global
			AndGMXsms.prefsUser = prUser;
			AndGMXsms.prefsPassword = prPassword;
			AndGMXsms.prefsSender = prSender;
			// save prefs
			AndGMXsms.me.saveSettings();
			// exit activity
			Settings.this.finish();
		}
	};

	/** OnClickListener listening for 'cancel'. */
	private OnClickListener cancel = new OnClickListener() {
		@Override
		public void onClick(final View v) {
			// reload prefs from global
			prUser = AndGMXsms.prefsUser;
			prPassword = AndGMXsms.prefsPassword;
			prSender = AndGMXsms.prefsSender;

			// reload prefs into TextEdits
			((EditText) Settings.this.findViewById(R.id.user)).setText(prUser);
			((EditText) Settings.this.findViewById(R.id.password))
					.setText(prPassword);
			((EditText) Settings.this.findViewById(R.id.sender))
					.setText(prSender);
			// exit activity
			Settings.this.finish();
		}
	};

	/** OnClickListener listening for 'bootstrap'. */
	private OnClickListener bootstrap = new OnClickListener() {
		@Override
		public void onClick(final View v) {
			Settings.this.showDialog(DIALOG_BOOTSTRAP);
		}

	};

	/**
	 * Reset inner pref. store.
	 */
	public static void reset() {
		prUser = null;
		prPassword = null;
		prSender = null;
	}
}
