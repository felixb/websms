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

public class Composer extends Activity {

	public static String lastMsg = null;
	public static String lastTo = null;

	static final int PICK_CONTACT_REQUEST = 0;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.composer);

		Button button = (Button) this.findViewById(R.id.send);
		button.setOnClickListener(this.runSend);

		button = (Button) this.findViewById(R.id.cancel);
		button.setOnClickListener(this.cancel);

		ImageButton ibtn = (ImageButton) this.findViewById(R.id.contacts);
		ibtn.setOnClickListener(this.contacts);
	}

	@Override
	public void onResume() {
		super.onResume();
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

	// Create an anonymous implementation of OnClickListener
	private OnClickListener runSend = new OnClickListener() {
		public void onClick(final View v) {
			EditText et = (EditText) Composer.this.findViewById(R.id.to);
			String to = et.getText().toString();
			et = (EditText) Composer.this.findViewById(R.id.text);
			String text = et.getText().toString();
			if (to != null && text != null && to.length() > 3
					&& !text.equals("")) {

				if (!to.startsWith("+")) {
					if (to.startsWith("00")) {
						to = "+" + to.substring(2);
					} else if (to.startsWith("0")) {
						to = AndGMXsms.prefs_prefix() + to.substring(1);
					}
				}

				AndGMXsms.connector = new Connector(to, text);
				AndGMXsms.connector.start();
				Composer.this.finish();
			}
		}
	};

	// Create an anonymous implementation of OnClickListener
	private OnClickListener cancel = new OnClickListener() {
		public void onClick(final View v) {
			EditText et = (EditText) Composer.this.findViewById(R.id.text);
			et.setText("");
			et = (EditText) Composer.this.findViewById(R.id.to);
			et.setText("");
			Composer.this.finish();
			lastMsg = null;
			lastTo = null;
		}
	};

	// Create an anonymous implementation of OnClickListener
	private OnClickListener contacts = new OnClickListener() {
		public void onClick(final View v) {
			Composer.this.startActivityForResult(new Intent(Intent.ACTION_PICK,
					Phones.CONTENT_URI), PICK_CONTACT_REQUEST);
		}
	};

	protected void onActivityResult(final int requestCode,
			final int resultCode, final Intent data) {
		if (requestCode == PICK_CONTACT_REQUEST) {
			if (resultCode == RESULT_OK) {
				// ContentResolver cr = getContentResolver();
				Cursor cur = this
						.managedQuery(data.getData(),
								new String[] { PhonesColumns.NUMBER }, null,
								null, null);
				EditText et = (EditText) this.findViewById(R.id.to);
				if (cur.moveToFirst()) {
					String targetNumber = cur.getString(cur
							.getColumnIndex(PhonesColumns.NUMBER));
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

	@Override
	public void onPause() {
		super.onPause();
		EditText et = (EditText) this.findViewById(R.id.text);
		lastMsg = et.getText().toString();
		et = (EditText) this.findViewById(R.id.to);
		lastTo = et.getText().toString();
	}
}
