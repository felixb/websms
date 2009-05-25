package de.ub0r.android.andGMXsms;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.Contacts.Phones;
import android.provider.Contacts.PhonesColumns;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

/**
 * Simple Activity for sms input.
 * 
 * @author flx
 */
public class Composer extends Activity {

	/** Persistent Message store. */
	private static String lastMsg = null;
	/** Persistent Receiver store. */
	private static String lastTo = null;

	/** Intent id. */
	static final int PICK_CONTACT_REQUEST = 0;

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
		this.setContentView(R.layout.composer);
		// register Listener
		Button button = (Button) this.findViewById(R.id.send);
		button.setOnClickListener(this.runSend);
		button = (Button) this.findViewById(R.id.cancel);
		button.setOnClickListener(this.cancel);
		ImageButton ibtn = (ImageButton) this.findViewById(R.id.contacts);
		ibtn.setOnClickListener(this.contacts);
	}

	/** Called on activity resume. */
	@Override
	public final void onResume() {
		super.onResume();
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

	/** OnClickListener for sending the sms. */
	private OnClickListener runSend = new OnClickListener() {
		public void onClick(final View v) {
			// fetch text/receiver
			EditText et = (EditText) Composer.this.findViewById(R.id.to);
			String to = et.getText().toString();
			et = (EditText) Composer.this.findViewById(R.id.text);
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
				String[] params = new String[2];
				params[0] = text;
				params[1] = to;
				new Connector().execute(params);
				// exit activity
				Composer.this.finish();
			}
		}
	};

	/** OnClickListener for cancel. */
	private OnClickListener cancel = new OnClickListener() {
		public void onClick(final View v) {
			// reset input fields
			EditText et = (EditText) Composer.this.findViewById(R.id.text);
			et.setText("");
			et = (EditText) Composer.this.findViewById(R.id.to);
			et.setText("");
			Composer.this.finish();
			reset();
		}
	};

	/** OnClickListener for launching phonebook. */
	private OnClickListener contacts = new OnClickListener() {
		public void onClick(final View v) {
			Composer.this.startActivityForResult(new Intent(Intent.ACTION_PICK,
					Phones.CONTENT_URI), PICK_CONTACT_REQUEST);
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

	/** Called on activity pause. */
	@Override
	public final void onPause() {
		super.onPause();
		// store input data to persitent stores
		EditText et = (EditText) this.findViewById(R.id.text);
		lastMsg = et.getText().toString();
		et = (EditText) this.findViewById(R.id.to);
		lastTo = et.getText().toString();
	}

	/**
	 * Resets persistent store.
	 */
	public static void reset() {
		lastMsg = null;
		lastTo = null;
	}
}
